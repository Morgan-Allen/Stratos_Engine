

package game;
import static game.GameConstants.*;
import util.*;



public class TaskFirstAid extends Task {
  
  
  /**  Data-fields, construction and save/load methods-
    */
  Actor patient;
  Trait treated;
  float skillTest = -1;
  Building refuge = null;
  
  
  public TaskFirstAid(Active actor, Actor patient, Building refuge) {
    super(actor);
    this.patient = patient;
    this.refuge  = refuge;
  }
  
  
  public TaskFirstAid(Session s) throws Exception {
    super(s);
    patient   = (Actor) s.loadObject();
    treated   = (Trait) s.loadObject();
    skillTest = s.loadFloat();
    refuge    = (Building) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(patient  );
    s.saveObject(treated  );
    s.saveFloat (skillTest);
    s.saveObject(refuge   );
  }
  
  
  
  /**  Factory methods for outside use-
    */
  public static TaskFirstAid nextSickbayTask(Actor actor, Building refuge) {
    if (! refuge.type().hasFeature(IS_SICKBAY)) return null;
    return configFirstAid(actor, refuge);
  }
  
  
  private static TaskFirstAid configFirstAid(Actor actor, Building refuge) {
    
    Area map = actor.map();
    Pick <Actor> pick = new Pick(0);
    
    for (Actor other : map.actors()) {
      float rating = rateAid(actor, other, false);
      pick.compare(other, rating);
    }
    
    Actor patient = pick.result();
    if (patient != null) {
      if (refuge == null) refuge = findRefuge(map, null, actor, patient);
      TaskFirstAid aid = new TaskFirstAid(actor, patient, refuge);
      
      if (aid.configTask(refuge, null, patient, JOB.HEALING, 1) != null) {
        return aid;
      }
    }
    
    return null;
  }
  
  
  //  TODO:  This should be cached for efficiency...
  
  static Building findRefuge(
    Area map, Building old, Actor actor, Actor patient
  ) {
    Pick <Building> pick = new Pick();
    
    for (Building b : map.buildings) {
      if (b.base() != actor.base()) continue;
      
      float rating = 0;
      if (b == actor.home() || b == old  ) rating += 0.25f;
      if (b.type().hasFeature(IS_REFUGE) ) rating += 0.5f;
      if (b.type().hasFeature(IS_SICKBAY)) rating += 1.0f;
      if (rating == 0) continue;
      
      rating *= Area.distancePenalty(b, patient);
      pick.compare(b, rating);
    }
    
    return pick.result();
  }
  
  
  
  /**  Assessing priority-
    */
  static float rateAid(Actor actor, Actor other, boolean midTask) {
    if (other.health.active() || other == actor     ) return -1;
    if (other.indoors() && other.health.bleed() <= 0) return -1;
    
    //  TODO:  Also allow treatment if the actor is seeking aid for their
    //  injuries/disease at a sickbay...
    
    float bandageLevel = other.outfit.carried(BANDAGES);
    if (bandageLevel >= (midTask ? 1 : 0.5f)) return -1;
    
    float injury   = other.health.injury() / other.health.maxHealth();
    float relation = 0.5f + (actor.traits.levelOf(TRAIT_EMPATHY) / 2);
    float distMult = Area.distancePenalty(actor, other);
    
    if (TaskCombat.allied (actor, other)) relation += 0.5f;
    if (TaskCombat.hostile(actor, other)) relation -= 0.5f;
    
    //  TODO:  Take danger into account as well.
    
    return injury * relation * distMult;
  }
  
  
  protected float successPriority() {
    Actor actor = (Actor) active;
    
    float rating = rateAid(actor, patient, true);
    if (rating <= 0) return -1;
    
    if (refuge == actor.work()) rating += 0.5f;
    return rating * PARAMOUNT;
  }
  
  
  protected float successChance() {
    Actor actor = (Actor) active;
    return (1 + (actor.traits.levelOf(SKILL_HEAL) / MAX_SKILL_LEVEL)) / 2f;
  }
  
  
  public boolean emergency() {
    return true;
  }
  
  
  
  /**  Behaviour execution-
    */
  protected void onTarget(Target target) {
    
    Actor   actor    = (Actor) active;
    float   bandages = patient.outfit.carried(BANDAGES);
    boolean injured  = patient.health.injury() > 0;
    
    if (skillTest == -1) {
      float skill = actor.traits.levelOf(SKILL_HEAL) / MAX_SKILL_LEVEL;
      float obstacle = 0.5f;
      float chance = Nums.clamp(skill + 1 - obstacle, 0, 2) / 2;
      
      skillTest = 0.33f;
      skillTest += Rand.num() < chance ? 0.33f : 0;
      skillTest += Rand.num() < chance ? 0.33f : 0;
    }
    
    if (injured && bandages < 1) {
      float healInc = skillTest * 2f / AVG_TREATMENT_TIME;
      patient.outfit.setCarried(BANDAGES, bandages + healInc);
      patient.health.incBleed(0 - BLEED_ACTION_HEAL * skillTest);
      configTask(origin, null, patient, JOB.HEALING, 1);
    }
    
    if (bandages >= 1 || ! injured) {
      if (
        (refuge != null && ! actor.isPassenger()) &&
        ! TaskCombat.hostile(actor, patient)
      ) {
        actor.setPassenger(patient, true);
        configTask(origin, refuge, null, JOB.DELIVER, 1);
      }
    }
  }
  
  
  protected void onVisit(Building visits) {
    Actor actor = (Actor) active;
    actor.setPassenger(patient, false);
    patient.setInside(visits, true);
  }
  
}


