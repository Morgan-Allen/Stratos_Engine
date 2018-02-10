

package game;
import graphics.common.*;
import graphics.cutout.*;
import util.*;
import static game.GameConstants.*;





public abstract class Technique extends Trait {
  
  
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
  
  
  public Technique(String ID, String name) {
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
    if (using.cooldown() > 0) return false;
    if (using.maxHealth() - using.fatigue() < costTire) return false;
    if (CityMap.distance(using, subject) > maxRange) return false;
    return canTarget(subject);
  }
  
  
  public boolean canUsePower(City ruler, Target subject) {
    if (ruler.funds() < costCash) return false;
    return canTarget(subject);
  }
  
  
  public boolean canTarget(Target subject) {
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
      used.applyFromActor(actor, target);
    }
    
    
    public String animName() {
      return used.animName;
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
    
    float fatCost = costTire / (using.maxHealth() - using.fatigue());
    if (fatCost > 1) return 0;
    
    rating *= 1 - fatCost;
    rating = powerLevel * rating / 10f;
    return rating;
  }
  
  
  public void applyFromRuler(City ruler, Target subject) {
    ruler.incFunds(0 - costCash);
    applyCommonEffects(subject, ruler, null);
  }
  
  
  public void applyFromActor(Actor actor, Target subject) {
    actor.setCooldown(costAP  );
    actor.takeFatigue(costTire);
    applyCommonEffects(subject, null, actor);
  }
  
  
  public void applyCommonEffects(Target subject, City ruler, Actor actor) {
    return;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}




