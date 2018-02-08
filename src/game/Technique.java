

package game;
import graphics.common.*;
import graphics.cutout.*;
import util.*;
import static game.GameConstants.*;





public abstract class Technique extends Trait {
  
  
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
    HEAVY_TIRING   = 6
  ;
  
  
  public String name, info;
  public ImageAsset icon;
  public String animName;
  
  public int costCash;
  public int costAP;
  public int costTire;
  
  public int properties;
  public float harmLevel;
  public float powerLevel;
  
  
  public Technique(String ID) {
    super(ID);
  }
  
  
  public void setProperties(
    int properties, float harmLevel, float powerLevel
  ) {
    this.properties = properties;
    this.harmLevel  = harmLevel ;
    this.powerLevel = powerLevel;
  }
  
  
  public void setCosting(int costCash, int costAP, int costTire) {
    this.costCash = costCash;
    this.costAP   = costAP  ;
    this.costTire = costTire;
  }
  
  
  public void attachMedia(
    String name, String iconPath, String info, String animName
  ) {
    final String key = entryKey()+"_icon";
    this.icon     = ImageAsset.fromImage(baseClass, key, iconPath);
    this.animName = AnimNames.LOOK;
    this.name     = name;
    this.info     = info;
  }
  
  
  public boolean hasProperty(int mask) {
    return (properties & mask) == mask;
  }
  
  
  public boolean targetSelf() {
    return hasProperty(TARGET_SELF);
  }
  
  
  public boolean targetOthers() {
    return hasProperty(TARGET_OTHERS);
  }
  
  
  
  public float rateUse(Actor using, Target subject) {
    
    //  TODO:  Include effects of cooldown/AP limits.
    float rating = Task.PARAMOUNT;
    
    if (harmLevel != Task.HARM_NULL) {
      float hostility = TaskCombat.hostility(subject, using);
      if (harmLevel >  0 && hostility <= 0) return -5;
      if (harmLevel <= 0 && hostility >  0) return -5;
      rating /= 1 + Nums.abs(harmLevel - hostility);
    }
    
    rating = powerLevel * rating / 10f;
    return rating;
    /*
    //
    //  Techniques become less attractive based on the fraction of fatigue or
    //  concentration they would consume.
    final boolean report = I.talkAbout == actor && ActorSkills.techsVerbose;
    final float
      conCost = concentrationCost / actor.health.concentration(),
      fatCost = fatigueCost       / actor.health.fatigueLimit ();
    if (report) I.say("  Con/Fat costs: "+conCost+"/"+fatCost);
    if (conCost > 1 || fatCost > 1) return 0;
    //
    //  Don't use a harmful technique against a subject you want to help, and
    //  try to avoid extreme harm against subjects you only want to subdue, et
    //  cetera.
    float rating = 10;
    
    rating *= ((1 - conCost) + (1 - fatCost)) / 2f;
    rating = powerLevel * rating / 10f;
    if (report) I.say("  Overall rating: "+rating);
    return rating;
    //*/
  }
  
  
  public boolean canUseActive(Actor using, Target subject) {
    return false;
  }
  
  
  public boolean canUsePassive(Actor using, Target subject) {
    return false;
  }
  
  
  public boolean canUsePower(City using, Target subject) {
    return false;
  }
  
  
  public Task useAsActive(Actor using, Target subject) {
    Use use = new Use(this, using, subject, NO_PROPS);
    return use.configTask(null, null, subject, Task.JOB.CASTING, 0);
  }
  
  
  public void applyAsPower(City using, Target subject) {
    final Use use = new Use(this, null, subject, NO_PROPS);
    applyEffects(subject, use);
  }
  

  
  public static class Use extends Task {
    
    final Technique used;
    float rating;
    float success;
    
    
    Use(Technique used, Actor using, Target subject, int properties) {
      super(using);
      this.used   = used;
      this.rating = used.rateUse(using, subject);
    }
    
    
    public Use(Session s) throws Exception {
      super(s);
      used    = (Technique) s.loadObject();
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
      return rating;
    }
    
    
    public float harmLevel() {
      return used.harmLevel;
    }
    
    
    protected void onTarget(Target target) {
      used.applyEffects(target, this);
    }
    
    
    public String animName() {
      return used.animName;
    }


    public void describePlan(Description d) {
      d.appendAll("Using ", used, " on ", target);
    }
  }
  
  
  //
  //  TODO:  Have separate methods for passive, active, power or buff effects?
  protected abstract void applyEffects(Target subject, Use use);
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}










