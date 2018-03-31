

package game;
import graphics.common.*;
import graphics.cutout.*;
import util.*;
import static game.GameConstants.*;



public abstract class ActorTechnique extends Trait {
  
  
  /**  Data fields, construction and setup methods-
    */
  final public static int
    NO_PROPS       = 0,
    TARGET_SELF    = 1 << 0,
    TARGET_OTHERS  = 1 << 1,
    TARGET_SKILL   = 1 << 2,
    IS_AREA_EFFECT = 1 << 3,
    IS_PASSIVE     = 1 << 4,
    
    SOURCE_GEAR    = 1 << 5,
    SOURCE_NATURAL = 1 << 6,
    SOURCE_LEARNED = 1 << 7,
    SOURCE_TRAINED = 1 << 8,
    
    NO_POWER       = 0,
    MINOR_POWER    = 2,
    MEDIUM_POWER   = 5,
    MAJOR_POWER    = 8,
    
    NO_AP_COST     = 0,
    MINOR_AP_COST  = 2,
    MEDIUM_AP_COST = 5,
    MAJOR_AP_COST  = 8,
    
    NO_TIRING      = 0,
    MINOR_TIRING   = 1,
    MEDIUM_TIRING  = 3,
    HEAVY_TIRING   = 6,
    
    NO_RANGE       = 0,
    MELEE_RANGE    = 1,
    SHORT_RANGE    = 3,
    LONG_RANGE     = 6
  ;
  
  
  public String info = "";
  public ImageAsset icon = null;
  public String animName = AnimNames.STAND;
  
  public int minLevel = 0;
  public Tally <Trait> skillNeeds = new Tally();
  
  public int costCash =  0;
  public int costAP   =  0;
  public int costTire =  0;
  public int maxRange = -1;
  
  public int   properties = 0;
  public float harmLevel  = 0;
  public float powerLevel = 0;
  
  
  public ActorTechnique(String ID, String name) {
    super(ID, name);
  }
  
  
  public void setProperties(int properties, float harmLevel, float powerLevel) {
    this.properties = properties;
    this.harmLevel  = harmLevel ;
    this.powerLevel = powerLevel;
  }
  
  
  public void setMinLevel(int minLevel, Object... skillNeeds) {
    this.minLevel = minLevel;
    this.skillNeeds.setWith(skillNeeds);
  }
  
  
  public void setCosting(int costCash, int costAP, int costTire, int maxRange) {
    this.costCash = costCash;
    this.costAP   = costAP  ;
    this.costTire = costTire;
    this.maxRange = maxRange;
  }
  
  
  public void attachMedia(
    Class baseClass, String iconPath, String info, String animName
  ) {
    if (baseClass != null && iconPath != null) {
      final String key = entryKey()+"_icon";
      this.icon = ImageAsset.fromImage(baseClass, key, iconPath);
    }
    this.info     = info;
    this.animName = AnimNames.LOOK;
  }
  
  
  
  /**  Property queries-
    */
  public boolean hasProperty(int mask) {
    return (properties & mask) == mask;
  }
  
  
  public boolean targetSelf() {
    return hasProperty(TARGET_SELF);
  }
  
  
  public boolean targetOthers() {
    return hasProperty(TARGET_OTHERS);
  }
  
  
  public boolean canUsePassive(Actor using, Target subject) {
    return false;
  }
  
  
  public boolean canUseActive(Actor using, Target subject) {
    if (using == subject && ! targetSelf()) {
      return false;
    }
    if (using != subject && ! targetOthers()) {
      return false;
    }
    if (costAP > 0 && using.health.cooldown() > 0) {
      return false;
    }
    if (costTire > 0 && using.health.maxHealth() - using.health.fatigue() < costTire) {
      return false;
    }
    return canTarget(subject, false);
  }
  
  
  public boolean canUsePower(Base ruler, Target subject) {
    if (ruler.funds() < costCash) return false;
    return subject != null && canTarget(subject, true);
  }
  
  
  public boolean canTarget(Target subject, boolean asRuler) {
    return true;
  }
  
  
  public boolean canLearn(Actor actor) {
    if (actor.classLevel() < minLevel) {
      return false;
    }
    for (Trait t : skillNeeds.keys()) {
      if (actor.levelOf(t) < skillNeeds.valueFor(t)) {
        return false;
      }
    }
    return true;
  }
  
  
  
  /**  Evaluation and execution-
    */
  public static class Use extends Task {
    
    final ActorTechnique used;
    float rating;
    float success;
    
    
    Use(ActorTechnique used, Actor using) {
      super(using);
      this.used = used;
    }
    
    
    public Use(Session s) throws Exception {
      super(s);
      used    = (ActorTechnique) s.loadObject();
      rating  = s.loadFloat();
      success = s.loadFloat();
    }
    
    
    public void saveState(Session s) throws Exception {
      super.saveState(s);
      s.saveObject(used   );
      s.saveFloat (rating );
      s.saveFloat (success);
    }
    
    
    public float priority() {
      return rating * PARAMOUNT * 1f / ROUTINE;
    }
    
    
    public float harmLevel() {
      return used.harmLevel;
    }
    
    
    protected void onTarget(Target target) {
      Actor actor = (Actor) this.active;
      used.applyFromActor(actor, target);
    }
    
    
    float actionRange() {
      if (used.maxRange <= 0) return super.actionRange();
      return used.maxRange;
    }
    
    
    int motionMode() {
      return Actor.MOVE_RUN;
    }


    public String animName() {
      return used.animName;
    }
    
    
    public String toString() {
      return used.name+": "+target;
    }
    
    
    public void describePlan(Description d) {
      d.appendAll("Using ", used, " on ", target);
    }
  }
  
  
  public float rateUse(Actor using, Target subject) {
    float rating = Task.PARAMOUNT;
    //
    //  Don't use a harmful technique against a subject you want to help, and
    //  try to avoid extreme harm against subjects you only want to subdue, et
    //  cetera.
    if (harmLevel != Task.HARM_NULL) {
      float hostility = TaskCombat.hostility(subject, using);
      if (harmLevel >  0 && hostility <= 0) return -5;
      if (harmLevel <= 0 && hostility >  0) return -5;
      rating /= 1 + Nums.abs(harmLevel - hostility);
    }
    
    float fatCost = costTire / (using.health.maxHealth() - using.health.fatigue());
    if (fatCost > 1) return 0;
    
    rating *= 1 - fatCost;
    rating = powerLevel * rating / 10f;
    return rating;
  }
  
  
  public Use useFor(Actor actor, Target subject) {
    Use use = new Use(this, actor);
    if (use.configTask(null, null, subject, Task.JOB.CASTING, 1) == null) {
      return null;
    }
    use.rating = rateUse(actor, subject);
    return use;
  }
  
  
  public void applyFromRuler(Base ruler, Target subject) {
    ruler.incFunds(0 - costCash);
    applyCommonEffects(subject, ruler, null);
  }
  
  
  public void applyFromActor(Actor actor, Target subject) {
    if (costAP   > 0) actor.health.setCooldown(costAP  );
    if (costTire > 0) actor.health.takeFatigue(costTire);
    applyCommonEffects(subject, null, actor);
  }
  
  
  public void applyCommonEffects(Target subject, Base ruler, Actor actor) {
    return;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}




