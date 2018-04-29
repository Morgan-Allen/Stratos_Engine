

package game;
import static game.GameConstants.*;
import static game.ActorHealth.*;
import util.*;



public class ActorTraits {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static int
    BOND_CHILD   = 1 << 1,
    BOND_PARENT  = 1 << 2,
    BOND_MARRIED = 1 << 3,
    BOND_MASTER  = 1 << 4,
    BOND_SERVANT = 1 << 5
  ;
  
  static class Level {
    Trait trait;
    float XP;
    float level;
    float bonus;
    
    public String toString() { return trait+": "+level; }
  }
  
  static class Bond {
    Actor with;
    float level;
    float novelty;
    int properties;
    
    public String toString() { return with+": "+level; }
  }
  
  
  final Actor actor;
  
  float classXP = 0;
  int classLevel = 0;
  List <Level> levels = new List();
  List <Bond > bonds  = new List();
  List <ActorTechnique> known = new List();
  List <Trait> affecting = new List();
  
  
  
  ActorTraits(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    
    classXP = s.loadFloat();
    classLevel = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Level l = new Level();
      l.trait = (Trait) s.loadObject();
      l.XP    = s.loadFloat();
      l.level = s.loadFloat();
      l.bonus = s.loadFloat();
      levels.add(l);
    }
    for (int n = s.loadInt(); n-- > 0;) {
      Bond b = new Bond();
      b.with       = (Actor) s.loadObject();
      b.level      = s.loadFloat();
      b.novelty    = s.loadFloat();
      b.properties = s.loadInt();
      bonds.add(b);
    }
    s.loadObjects(known);
    s.loadObjects(affecting);
  }
  
  
  void saveState(Session s) throws Exception {
    
    s.saveFloat(classXP);
    s.saveInt(classLevel);
    
    s.saveInt(levels.size());
    for (Level l : levels) {
      s.saveObject(l.trait);
      s.saveFloat(l.XP);
      s.saveFloat(l.level);
      s.saveFloat(l.bonus);
    }
    s.saveInt(bonds.size());
    for (Bond b : bonds) {
      s.saveObject(b.with);
      s.saveFloat(b.level);
      s.saveFloat(b.novelty);
      s.saveInt(b.properties);
    }
    s.saveObjects(known);
    s.saveObjects(affecting);
  }
  
  
  
  /**  Regular updates-
    */
  public void updateTraits() {
    //
    //  Check to see if a new technique can be learned spontaneously-
    for (ActorTechnique t : actor.type().classTechniques) {
      if (known.includes(t)) continue;
      if (t.canLearn(actor)) known.add(t);
    }
    //
    //  Update all relations-
    float tick = 1f / actor.map().ticksPS;
    for (Bond b : bonds) {
      b.novelty += tick / BOND_NOVEL_TIME;
      b.novelty = Nums.clamp(b.novelty, 0, 1);
    }
    //
    //  Then compile a list of all traits affecting this actor-
    affecting.clear();
    for (ActorTechnique t : known) {
      affecting.add(t);
    }
    for (Condition c : actor.health.conditions()) {
      affecting.include(c.basis);
    }
    for (Good g : actor.outfit.carried().keys()) {
      for (ActorTechnique t : g.allows) {
        affecting.include(t);
      }
    }
    //
    //  And calculate current class level and skill-multiples:
    ActorType classType = actor.type();
    int typeLevelXP = classType.classLevelXP;
    for (int l = MAX_CLASS_LEVEL; l-- > 0;) {
      if (SKILL_XP_TOTAL[l] <= (classXP / typeLevelXP)) {
        classLevel = l;
        break;
      }
    }
    float classMult = 0;
    if (classLevel < 1) classMult = classXP * 0.5f / typeLevelXP;
    else classMult = (1 + ((classLevel - 1) / (MAX_CLASS_LEVEL - 1f))) / 2;
    //
    //  And then apply any passive effects those may have-
    for (Level l : levels) {
      l.bonus = 0;
      l.bonus += (int) (classType.coreSkills.valueFor(l.trait) * classMult);
      for (Trait t : affecting) {
        l.bonus += t.passiveBonus(l.trait);
      }
    }
    for (Trait t : affecting) {
      t.passiveEffect(actor);
    }
  }
  
  
  
  /**  Handling skills and traits-
    */
  public int classLevel() {
    return classLevel;
  }
  
  
  public void setClassLevel(int level) {
    int typeLevelXP = actor.type().classLevelXP;
    level = Nums.clamp(level, MAX_CLASS_LEVEL + 1);
    this.classLevel = level;
    this.classXP = SKILL_XP_TOTAL[level] * typeLevelXP;
  }
  
  
  public float classLevelProgress() {
    int typeLevelXP = actor.type().classLevelXP;
    int floor = SKILL_XP_TOTAL[classLevel];
    int gapXP = SKILL_XP_MULTS[classLevel];
    return ((classXP / typeLevelXP) - floor) / gapXP;
  }
  
  
  public int classLevelFullXP() {
    int typeLevelXP = actor.type().classLevelXP;
    return SKILL_XP_MULTS[classLevel] * typeLevelXP;
  }
  
  
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
  
  
  public void setLevel(Trait trait, float level) {
    Level l = levelFor(trait, true);
    l.level = level = Nums.clamp(level, -1, MAX_SKILL_LEVEL);
    if (level <= 0) {
      l.XP = 0;
    }
    else {
      l.XP =  BASE_LEVEL_XP * SKILL_XP_TOTAL[(int) level];
      l.XP += BASE_LEVEL_XP * SKILL_XP_MULTS[(int) level] * (l.level % 1);
    }
  }
  
  
  public void setXP(Trait trait, float XP) {
    Level l = levelFor(trait, true);
    l.XP = Nums.clamp(XP, 0, MAX_SKILL_XP);
    for (int i = MAX_SKILL_LEVEL; i >= 0; i--) {
      float remXP = (l.XP * 1f / BASE_LEVEL_XP) - SKILL_XP_TOTAL[i];
      if (remXP > 0) { l.level = i + (remXP / SKILL_XP_MULTS[i]); break; }
    }
  }
  
  
  public void gainXP(Trait trait, float XP) {
    Level l = levelFor(trait, true);
    setXP(trait, l.XP + XP);
    
    ActorType classType = actor.type();
    float coreLevel = classType.coreSkills.valueFor(trait);
    float coreXP = XP * coreLevel / MAX_SKILL_LEVEL;
    classXP += coreXP;
  }
  
  
  public Series <Trait> allTraits() {
    Batch <Trait> traits = new Batch();
    for (Level l : levels) traits.add(l.trait);
    return traits;
  }
  
  
  public float levelOf(Trait trait) {
    Level l = levelFor(trait, false);
    return l == null ? 0 : (l.level + l.bonus);
  }
  
  
  public Series <ActorTechnique> known() {
    return known;
  }
  
  
  
  /**  Handling bonds with other actors-
    */
  Bond bondWith(Actor with, boolean init) {
    for (Bond b : bonds) {
      if (b.with == with) return b;
    }
    if (init) {
      Bond b = new Bond();
      b.with    = with;
      b.novelty = INIT_NOVELTY / 100f;
      b.level   = INIT_BONDING;
      bonds.add(b);
      return b;
    }
    return null;
  }
  
  
  public static void setBond(
    Actor a, Actor b, int roleA, int roleB, float level
  ) {
    if (a != null && b != null) a.traits.setBond(b, level, roleB);
    if (b != null && a != null) b.traits.setBond(a, level, roleA);
  }
  
  
  public void setBond(Actor with, float level, int... props) {
    Bond b = bondWith(with, true);
    b.level = level;
    b.properties = 0;
    for (int p : props) b.properties |= p;
  }
  
  
  public void incBond(Actor with, float inc, float maxRange) {
    Bond b = bondWith(with, true);
    if (b.level > maxRange || b.level < -maxRange) return;
    b.level = Nums.clamp(b.level + inc, -maxRange, maxRange);
  }
  
  
  public void incNovelty(Actor with, float inc) {
    Bond b = bondWith(with, true);
    b.novelty  = Nums.clamp(b.novelty + inc, 0, 1);
  }
  
  
  public void deleteBond(Actor with) {
    Bond b = bondWith(with, false);
    if (b != null) bonds.remove(b);
  }
  
  
  public boolean hasBondType(Actor with, int type) {
    Bond b = bondWith(with, false);
    return b == null ? false : ((b.properties & type) != 0);
  }
  
  
  public float bondLevel(Actor with) {
    Bond b = bondWith(with, false);
    return b == null ? INIT_BONDING : b.level;
  }
  
  
  public float bondNovelty(Actor with) {
    Bond b = bondWith(with, false);
    return b == null ? (INIT_NOVELTY / 100f) : b.novelty;
  }
  
  
  public boolean hasBond(Actor with) {
    return bondWith(with, false) != null;
  }
  
  
  public Actor bondedWith(int type) {
    for (Bond b : bonds) if ((b.properties & type) != 0) return b.with;
    return null;
  }
  
  
  public Series <Actor> allBondedWith(int type) {
    Batch <Actor> all = new Batch();
    for (Bond b : bonds) {
      if ((b.properties & type) != 0) all.add(b.with);
    }
    return all;
  }
  
  
  public float solitude() {
    float bondTotal = 0;
    for (Bond b : bonds) {
      bondTotal += Nums.max(0, b.level);
    }
    bondTotal = Nums.clamp(bondTotal / AVG_NUM_BONDS, 0, 1);
    return 1 - bondTotal;
  }
  
}













