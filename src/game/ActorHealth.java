

package game;
import game.Task.JOB;
import static game.GameConstants.*;
import util.*;



public class ActorHealth {
  
  
  /**  Data-fields, construction and save/load methods-
    */
  final static float
    PAIN_PERCENT       = 50,
    KNOCKOUT_PERCENT   = 100,
    DECOMP_PERCENT     = 150,
    BLEED_HURT_PERCENT = 50,
    BLEED_PER_SECOND   = 0.25f,
    DECOMP_TIME        = DAY_LENGTH
  ;
  final public static int
    STATE_OKAY   = 1,
    STATE_KO     = 2,
    STATE_DEAD   = 3,
    STATE_DECOMP = 4
  ;
  
  static class Condition {
    Active source;
    Trait basis;
    float expireTime;
  }
  
  
  final Actor actor;
  
  int sexData    = -1;
  int ageSeconds =  0;
  int pregnancy  =  0;
  float injury  ;
  float bleed   ;
  float hunger  ;
  float fatigue ;
  float stress  ;
  float cooldown;
  int   state = STATE_OKAY;
  
  List <Condition> conditions = new List();
  
  
  ActorHealth(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    
    sexData    = s.loadInt();
    ageSeconds = s.loadInt();
    pregnancy  = s.loadInt();
    
    injury   = s.loadFloat();
    bleed    = s.loadFloat();
    hunger   = s.loadFloat();
    fatigue  = s.loadFloat();
    stress   = s.loadFloat();
    cooldown = s.loadFloat();
    state    = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Condition c = new Condition();
      c.source     = (Active) s.loadObject();
      c.basis      = (Trait ) s.loadObject();
      c.expireTime = s.loadFloat();
      conditions.add(c);
    }
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveInt(sexData   );
    s.saveInt(ageSeconds);
    s.saveInt(pregnancy );
    
    s.saveFloat(injury  );
    s.saveFloat(bleed   );
    s.saveFloat(hunger  );
    s.saveFloat(fatigue );
    s.saveFloat(stress  );
    s.saveFloat(cooldown);
    s.saveInt  (state   );
    
    s.saveInt(conditions.size());
    for (Condition c : conditions) {
      s.saveObject(c.source);
      s.saveObject(c.basis);
      s.saveFloat(c.expireTime);
    }
  }
  
  
  
  /**  Supplemental setup methods-
    */
  public void setSexData(int sexData) {
    this.sexData = sexData;
  }
  
  
  public void setAgeYears(float ageYears) {
    this.ageSeconds = (int) (ageYears * YEAR_LENGTH);
  }
  
  
  public float ageYears() {
    float years = ageSeconds / (YEAR_LENGTH * 1f);
    return years;
  }
  
  
  
  /**  Regular state updates-
    */
  void updateHealth() {
    //
    //  Obtain some basic settings first-
    Area map = actor.map();
    WorldSettings settings = map.world.settings;
    boolean organic = actor.type().organic && ! actor.type().isVessel();
    float tick = 1f / map.ticksPS;
    float time = map.time() + map.timeInUpdate();
    //
    //  Adjust health-parameters accordingly-
    if (! alive()) {
      float decay = tick * 1f * maxHealth();
      decay *= (DECOMP_PERCENT - KNOCKOUT_PERCENT) / 100f;
      bleed = 0;
      injury += decay;
    }
    else if (organic) {
      hunger += settings.toggleHunger ? (tick / STARVE_INTERVAL) : 0;
      cooldown = Nums.max(0, cooldown - tick);
      
      if (bleed > 0) {
        float bleedInc = tick * BLEED_PER_SECOND;
        injury += bleedInc;
        bleed -= bleedInc;
      }
      
      boolean resting = state == STATE_KO;
      resting |= actor.jobType() == JOB.RESTING && actor.task().inContact();
      
      if (resting) {
        float rests = tick / FATIGUE_REGEN;
        float heals = tick / HEALTH_REGEN ;
        fatigue = Nums.max(0, fatigue - rests);
        injury  = Nums.max(0, injury  - heals);
      }
      else {
        fatigue += settings.toggleFatigue ? (tick / FATIGUE_INTERVAL) : 0;
        float heals = tick * 0.5f / HEALTH_REGEN;
        injury = Nums.max(0, injury - heals);
      }
    }
    else {
      float rests = tick / FATIGUE_REGEN;
      bleed = 0;
      fatigue = Nums.max(0, fatigue - rests);
      cooldown = Nums.max(0, cooldown - tick);
    }
    //
    //  And update conditions-
    for (Condition c : conditions) {
      if (c.expireTime <= time) conditions.remove(c);
    }
  }
  
  
  
  /**  Stub methods for handling growth and life-cycle:
    */
  void updateLifeCycle(Base city, boolean onMap) {
    ageSeconds += 1;
  }
  
  
  public boolean pregnant() {
    return pregnancy > 0;
  }
  
  
  public void beginPregnancy() {
    pregnancy = 1;
  }
  
  
  public void completePregnancy(Building venue, boolean onMap) {
    return;
  }
  
  
  public float growLevel() {
    return 1;
  }
  
  
  public boolean adult() {
    return true;
  }
  
  
  public boolean child() {
    return false;
  }
  
  
  public boolean senior() {
    return false;
  }
  
  
  public boolean fertile() {
    return false;
  }
  
  
  public boolean man() {
    return false;
  }
  
  
  public boolean woman() {
    return false;
  }
  
  
  
  /**  Handling conditions-
    */
  Condition conditionFor(Trait trait) {
    for (Condition c : conditions) if (c.basis == trait) return c;
    return null;
  }
  
  
  public void addCondition(Active source, Trait trait, float duration) {
    Area map = actor.map();
    float time = map.time() + map.timeInUpdate();
    
    Condition c = conditionFor(trait);
    if (c == null) conditions.add(c = new Condition());
    
    c.source = source;
    c.basis = trait;
    c.expireTime = time + duration;
  }
  
  
  public Series <Condition> conditions() {
    return conditions;
  }
  
  
  
  /**  Handling state-changes after injury, hunger, fatigue, etc-
    */
  void checkHealthState() {
    float maxHealth = maxHealth() * KNOCKOUT_PERCENT / 100f;
    float maxIntact = maxHealth() * DECOMP_PERCENT   / 100f;
    
    if (injury + hunger > maxIntact) {
      this.state = STATE_DECOMP;
      if (actor.map() != null) actor.exitMap(actor.map());
      actor.setDestroyed();
    }
    if (injury + hunger > maxHealth) {
      setAsKilled("Injury: "+injury+" Hunger "+hunger);
    }
    else if (injury + hunger + fatigue > maxHealth) {
      this.state = STATE_KO;
    }
    else if (alive()) {
      this.state = STATE_OKAY;
    }
  }
  
  
  public void liftDamage(float damage) {
    takeDamage(0 - damage);
  }
  
  
  public void liftFatigue(float tire) {
    takeFatigue(0 - tire);
  }
  
  
  public void takeDamage(float damage) {
    Area map = actor.map();
    if (map == null || ! map.world.settings.toggleInjury) return;
    injury += damage;
    injury = Nums.clamp(injury, 0, maxHealth() + 1);
    if (damage > 0) incBleed(damage * BLEED_HURT_PERCENT / 100f);
    checkHealthState();
  }
  
  
  public void incBleed(float bleed) {
    this.bleed += bleed;
    if (this.bleed < 0) this.bleed = 0;
  }
  
  
  public void takeFatigue(float tire) {
    Area map = actor.map();
    if (map == null || ! map.world.settings.toggleFatigue) return;
    fatigue += tire;
    fatigue = Nums.clamp(tire, 0, maxHealth() + 1);
    checkHealthState();
  }
  
  
  public void setCooldown(float cool) {
    this.cooldown = cool;
  }
  
  
  public void liftHunger(float inc) {
    this.hunger = Nums.clamp(hunger - inc, 0, maxHealth());
  }
  
  
  public void setHungerLevel(float level) {
    this.hunger = maxHealth() * level;
  }
  
  
  public void setHunger(float hunger) {
    this.hunger = hunger;
  }
  
  
  public void setAsKilled(String cause) {
    if (actor.reports()) I.say(this+" DIED: "+cause);
    state = STATE_DEAD;
  }
  
  
  
  /**  General public access-methods-
    */
  public float hungerLevel() {
    return hunger / maxHealth();
  }
  
  public float maxHealth() {
    return actor.type().maxHealth;
  }
  
  public float injury  () { return injury  ; }
  public float bleed   () { return bleed   ; }
  public float fatigue () { return fatigue ; }
  public float cooldown() { return cooldown; }
  public float hunger  () { return hunger  ; }
  
  public boolean active() { return state == STATE_OKAY; }
  public boolean alive () { return state <= STATE_KO  ; }
  public boolean dead  () { return state >= STATE_DEAD; }
  
}







