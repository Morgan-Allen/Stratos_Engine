

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class ActorAsPerson extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    BOND_CHILD   = 1 << 1,
    BOND_PARENT  = 1 << 3,
    BOND_MARRIED = 1 << 4,
    BOND_MASTER  = 1 << 5,
    BOND_SERVANT = 1 << 6
  ;
  
  static class Level {
    Trait trait;
    float XP;
    float level;
  }
  
  static class Bond {
    Actor with;
    float level;
    int properties;
  }
  
  List <Level> levels = new List();
  List <Bond > bonds  = new List();
  
  
  public ActorAsPerson(Type type) {
    super(type);
  }
  
  
  public ActorAsPerson(Session s) throws Exception {
    super(s);
    for (int n = s.loadInt(); n-- > 0;) {
      Level l = new Level();
      l.trait = (Trait) s.loadObject();
      l.XP    = s.loadFloat();
      l.level = s.loadFloat();
      levels.add(l);
    }
    for (int n = s.loadInt(); n-- > 0;) {
      Bond b = new Bond();
      b.with       = (Actor) s.loadObject();
      b.level      = s.loadFloat();
      b.properties = s.loadInt();
      bonds.add(b);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(levels.size());
    for (Level l : levels) {
      s.saveObject(l.trait);
      s.saveFloat(l.XP);
      s.saveFloat(l.level);
    }
    s.saveInt(bonds.size());
    for (Bond b : bonds) {
      s.saveObject(b.with);
      s.saveFloat(b.level);
      s.saveInt(b.properties);
    }
  }
  
  
  
  /**  Handling skills and traits-
    */
  private Level levelFor(Trait trait, boolean init) {
    for (Level l : levels) {
      if (l.trait == trait) return l;
    }
    if (init) {
      Level l = new Level();
      l.trait = trait;
      levels.add(l);
      return l;
    }
    else return null;
  }
  
  
  void setLevel(Trait trait, float level) {
    Level l = levelFor(trait, true);
    l.level = level = Nums.clamp(level, 0, MAX_SKILL_LEVEL);
    l.XP =  BASE_LEVEL_XP * SKILL_XP_TOTAL[(int) level];
    l.XP += BASE_LEVEL_XP * SKILL_XP_MULTS[(int) level] * (l.level % 1);
  }
  
  
  void setXP(Trait trait, float XP) {
    Level l = levelFor(trait, true);
    l.XP = Nums.clamp(XP, 0, MAX_SKILL_XP);
    for (int i = MAX_SKILL_LEVEL; i >= 0; i--) {
      float remXP = (l.XP * 1f / BASE_LEVEL_XP) - SKILL_XP_TOTAL[i];
      if (remXP > 0) { l.level = i + (remXP / SKILL_XP_MULTS[i]); break; }
    }
  }
  
  
  void gainXP(Trait trait, float XP) {
    Level l = levelFor(trait, true);
    setXP(trait, l.XP + XP);
  }
  
  
  float levelOf(Trait trait) {
    Level l = levelFor(trait, false);
    return l == null ? 0 : l.level;
  }
  
  
  
  /**  Handling bonds with other actors-
    */
  Bond bondWith(Actor with, boolean init) {
    for (Bond b : bonds) {
      if (b.with == with) return b;
    }
    if (init) {
      Bond b = new Bond();
      b.with = with;
      bonds.add(b);
      return b;
    }
    return null;
  }
  
  
  static void setBond(Actor a, Actor b, int typeA, int typeB, float level) {
    if (a instanceof ActorAsPerson && b != null) {
      ((ActorAsPerson) a).setBond(b, level, typeA);
    }
    if (b instanceof ActorAsPerson && a != null) {
      ((ActorAsPerson) b).setBond(a, level, typeB);
    }
  }
  
  
  void setBond(Actor with, float level, int... props) {
    Bond b = bondWith(with, true);
    b.level = level;
    b.properties = 0;
    for (int p : props) b.properties |= p;
  }
  
  
  void deleteBond(Actor with) {
    Bond b = bondWith(with, false);
    if (b != null) bonds.remove(b);
  }
  
  
  boolean hasBondType(Actor with, int type) {
    Bond b = bondWith(with, false);
    return b == null ? false : ((b.properties & type) != 0);
  }
  
  
  float bondLevel(Actor with) {
    Bond b = bondWith(with, false);
    return b == null ? 0 : b.level;
  }
  
  
  Actor bondedWith(int type) {
    for (Bond b : bonds) if ((b.properties & type) != 0) return b.with;
    return null;
  }
  
  
  Series <Actor> allBondedWith(int type) {
    Batch <Actor> all = new Batch();
    for (Bond b : bonds) if ((b.properties & type) != 0) all.add(b.with);
    return all;
  }
  
  
  
  /**  Spawning new behaviours:
    */
  void beginNextBehaviour() {
    //
    //  Establish some facts about the citizen first:
    boolean adult = adult();
    assignTask(null);
    
    //  Adults will search for work and a place to live:
    if ((homeCity == null || homeCity == map.city) && adult) {
      if (work == null) CityBorders.findWork(map, this);
      if (home == null) CityBorders.findHome(map, this);
    }
    
    //  Children and retirees don't work:
    if (work != null && ! adult) {
      work.setWorker(this, false);
    }
    
    //  If you're seriously hungry/beat/tired, try going home:
    Batch <Good> menu = menuAt(home);
    float hurtRating = fatigue + injury + (menu.size() > 0 ? hunger : 0);
    if (hurtRating > (type.maxHealth * (Rand.num() + 0.5f))) {
      beginResting(home);
    }
    
    //  Once home & work have been established, try to derive a task to
    //  perform-
    if (idle() && formation != null && formation.active) {
      formation.selectActorBehaviour(this);
    }
    if (idle() && work != null && work.accessible()) {
      work.selectActorBehaviour(this);
    }
    if (idle() && home != null && home.accessible()) {
      home.selectActorBehaviour(this);
    }
    if (idle() && (hurtRating >= 1 || injury > 0)) {
      beginResting(home);
    }
    if (idle()) {
      startRandomWalk();
    }
  }
  
  
  
  /**  Handling hunger, injury, healing and eating, etc:
    */
  void update() {
    hunger += map.settings.toggleHunger ? (1f / STARVE_INTERVAL ) : 0;
    
    if (jobType() == JOB.RESTING) {
      float rests = 1f / FATIGUE_REGEN;
      float heals = 1f / HEALTH_REGEN ;
      fatigue = Nums.max(0, fatigue - rests);
      injury  = Nums.max(0, injury  - heals);
    }
    else {
      fatigue += map.settings.toggleFatigue ? (1f / FATIGUE_INTERVAL) : 0;
      float heals = 0.5f / HEALTH_REGEN;
      injury = Nums.max(0, injury - heals);
    }
    
    super.update();
  }
  
  
  Batch <Good> menuAt(Building visits) {
    Batch <Good> menu = new Batch();
    if (visits != null) for (Good g : FOOD_TYPES) {
      if (visits.inventory.valueFor(g) >= 1) menu.add(g);
    }
    return menu;
  }
  
  
  protected void onVisit(Building visits) {
    if (jobType() == JOB.RESTING) {
      
      if (hunger >= 1f / HUNGER_REGEN) {
        Batch <Good> menu = menuAt(visits);
        boolean adult = adult();
        
        if (menu.size() > 0) for (Good g : menu) {
          float eats = 1f / (menu.size() * HUNGER_REGEN);
          if (! adult) eats /= 2;
          visits.inventory.add(0 - eats, g);
          hunger -= eats / FOOD_UNIT_PER_HP;
        }
      }
    }
  }
  
  
  
  /**  Handling sight-range:
    */
  void updateVision() {
    if (indoors() || ! onMap()) return;
    
    float range = type.sightRange * (map.fog.lightLevel() + 1f) / 2;
    map.fog.liftFog(at(), range);
    
    //  TODO:  Allow buildings to update fog-of-war as well (possibly on a
    //  different map-overlay for convenience.)
  }
  
  
  
  
  /**  Aging, reproduction and life-cycle methods-
    */
  void updateAging() {
    super.updateAging();
    
    if (pregnancy > 0) {
      pregnancy += 1;
      if (pregnancy > PREGNANCY_LENGTH && home != null && inside == home) {
        float dieChance = AVG_CHILD_MORT / 100f;
        if (Rand.num() >= dieChance) {
          completePregnancy(home);
        }
        else {
          pregnancy = 0;
        }
      }
      if (pregnancy > PREGNANCY_LENGTH + MONTH_LENGTH) {
        pregnancy = 0;
      }
    }
    
    if (ageSeconds % YEAR_LENGTH == 0) {
      if (senior() && Rand.index(100) < AVG_SENIOR_MORT) {
        setAsKilled("Old age");
      }
      
      if (woman() && fertile() && pregnancy == 0 && home != null) {
        float
          ageYears   = ageSeconds / (YEAR_LENGTH * 1f),
          fertSpan   = AVG_MENOPAUSE - AVG_MARRIED,
          fertility  = (AVG_MENOPAUSE - ageYears) / fertSpan,
          wealth     = BuildingForHome.wealthLevel(home),
          chanceRng  = MAX_PREG_CHANCE - MIN_PREG_CHANCE,
          chanceW    = MAX_PREG_CHANCE - (wealth * chanceRng),
          pregChance = fertility * chanceW / 100
        ;
        if (Rand.num() < pregChance) {
          beginPregnancy();
        }
      }
    }
  }
  
  
  boolean pregnant() {
    return pregnancy > 0;
  }
  
  
  void beginPregnancy() {
    pregnancy = 1;
  }
  
  
  void completePregnancy(Building venue) {
    Tile at = venue.at();
    ActorAsPerson child  = (ActorAsPerson) CHILD.generate();
    ActorAsPerson father = (ActorAsPerson) bondedWith(BOND_MARRIED);
    child.enterMap(map, at.x, at.y, 1);
    child.inside = venue;
    setBond(this  , child, BOND_CHILD, BOND_PARENT, 0.5f);
    setBond(father, child, BOND_CHILD, BOND_PARENT, 0.5f);
    venue.setResident(child, true);
    pregnancy = 0;
  }
  
  
  boolean child() {
    return ageYears() < AVG_PUBERTY;
  }
  
  
  boolean senior() {
    return ageYears() > AVG_RETIREMENT;
  }
  
  
  boolean fertile() {
    return ageYears() > AVG_MARRIED && ageYears() < AVG_MENOPAUSE;
  }
  
  
  boolean adult() {
    return ! (child() || senior());
  }
  
  
  boolean man() {
    return (sexData & SEX_MALE) != 0;
  }
  
  
  boolean woman() {
    return (sexData & SEX_FEMALE) != 0;
  }
  
}





