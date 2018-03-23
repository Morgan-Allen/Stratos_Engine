

package game;
import util.*;
import static game.Task.*;
import static game.Area.*;
import static game.GameConstants.*;



public class ActorAsPerson extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final public static int
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
  List <Task > todo   = new List();
  
  List <ActorTechnique> known = new List();
  
  String customName = "";
  
  
  public ActorAsPerson(ActorType type) {
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
    s.loadObjects(todo);
    
    s.loadObjects(known);
    
    customName = s.loadString();
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
    s.saveObjects(todo);
    
    s.saveObjects(known);
    
    s.saveString(customName);
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
  
  
  public void setLevel(Trait trait, float level) {
    Level l = levelFor(trait, true);
    l.level = level = Nums.clamp(level, -1, MAX_SKILL_LEVEL);
    l.XP =  BASE_LEVEL_XP * SKILL_XP_TOTAL[(int) level];
    l.XP += BASE_LEVEL_XP * SKILL_XP_MULTS[(int) level] * (l.level % 1);
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
  }
  
  
  public Series <Trait> allTraits() {
    Batch <Trait> traits = new Batch();
    for (Level l : levels) traits.add(l.trait);
    return traits;
  }
  
  
  public float levelOf(Trait trait) {
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
  
  
  public static void setBond(
    Actor a, Actor b, int typeA, int typeB, float level
  ) {
    if (a instanceof ActorAsPerson && b != null) {
      ((ActorAsPerson) a).setBond(b, level, typeA);
    }
    if (b instanceof ActorAsPerson && a != null) {
      ((ActorAsPerson) b).setBond(a, level, typeB);
    }
  }
  
  
  public void setBond(Actor with, float level, int... props) {
    Bond b = bondWith(with, true);
    b.level = level;
    b.properties = 0;
    for (int p : props) b.properties |= p;
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
    return b == null ? 0 : b.level;
  }
  
  
  public Actor bondedWith(int type) {
    for (Bond b : bonds) if ((b.properties & type) != 0) return b.with;
    return null;
  }
  
  
  public Series <Actor> allBondedWith(int type) {
    Batch <Actor> all = new Batch();
    for (Bond b : bonds) if ((b.properties & type) != 0) all.add(b.with);
    return all;
  }
  
  
  
  /**  Spawning new behaviours:
    */
  void beginNextBehaviour() {
    //
    //  TODO:  You will need to ensure that work/home/formation venues are
    //  present on the same map to derive related bahaviours!
    //
    //  Establish some facts about the citizen first:
    boolean adult = adult();
    assignTask(null);
    //
    //  Adults will search for work and a place to live:
    //  Children and retirees don't work:
    if (adult && work() == null) ActorUtils.findWork(map, this);
    if (adult && home() == null) ActorUtils.findHome(map, this);
    if (work() != null && ! adult) work().setWorker(this, false);
    //
    //  If you're seriously hungry/beat/tired, try going home:
    //  TODO:  Work this in as an emergency reaction...
    Building rests = TaskResting.findRestVenue(this, map);
    float needRest = TaskResting.restUrgency(this, rests);
    if (needRest > Rand.num() + 0.5f) {
      assignTask(TaskResting.configResting(this, rests));
    }
    //
    //  See if there's a missions worth joining.  Or, if you have an active
    //  mission, undertake the next associated task-
    if (idle() && mission() == null && type().socialClass == CLASS_SOLDIER) {
      Pick <Mission> pick = new Pick(Task.ROUTINE * Rand.num());
      
      //  TODO:  Use a Choice for this?
      for (Mission f : base().missions) {
        if (! f.rewards.isBounty()) continue;
        Task t = f.selectActorBehaviour(this);
        float priority = t == null ? 0 : t.priority();
        pick.compare(f, priority * (0.5f + Rand.num()));
      }
      
      Mission joins = pick.result();
      if (joins != null) joins.toggleRecruit(this, true);
    }
    if (idle() && mission() != null && mission().active()) {
      assignTask(mission().selectActorBehaviour(this));
    }
    //
    //  Failing that, see if your home, place of work, purchases or other idle
    //  impulses have anything to say.
    if (idle()) {
      ActorChoice choice = new ActorChoice(this);
      
      choice.add(TaskPurchase.nextPurchase(this));
      choice.add(TaskWander.configWandering(this));
      
      if (work() != null && work().complete()) {
        choice.add(work().selectActorBehaviour(this));
      }
      if (home() != null && home().complete()) {
        choice.add(home().selectActorBehaviour(this));
      }
      if (rests != null) {
        choice.add(TaskResting.configResting(this, rests));
      }
      assignTask(choice.weightedPick());
    }
  }
  
  
  void updateReactions() {
    if (! map.world.settings.toggleReacts) return;
    
    if (jobType() != Task.JOB.RETREAT) {
      if (armed() && ! Task.inCombat(this)) {
        TaskCombat combat = TaskCombat.nextReaction(this);
        if (combat != null) assignTask(combat);
      }
      
      float oldPriority = Nums.max(jobPriority(), 0);
      TaskRetreat retreat = TaskRetreat.configRetreat(this);
      if (retreat != null && retreat.priority() > oldPriority) {
        assignTask(retreat);
        if (mission() != null) mission().toggleRecruit(this, false);
      }
    }
    
    //
    //  TODO:  You might consider using actual task/reactions for this.
    //
    //  TODO:  This might be a little intensive, computationally, as well?
    
    if (cooldown() == 0 && map.world.settings.toggleReacts) {
      
      class Reaction { ActorTechnique used; Target subject; float rating; }
      Pick <Reaction> pick = new Pick(0);
      
      for (Active other : map.activeInRange(at(), sightRange())) {
        for (ActorTechnique used : known) {
          if (used.canUseActive(this, other)) {
            Reaction r = new Reaction();
            r.rating  = used.rateUse(this, other);
            r.subject = other;
            r.used    = used;
            pick.compare(r, r.rating);
          }
        }
        for (Good g : carried.keys()) for (ActorTechnique used : g.allows) {
          if (used.canUseActive(this, other)) {
            Reaction r = new Reaction();
            r.rating  = used.rateUse(this, other);
            r.subject = other;
            r.used    = used;
            pick.compare(r, r.rating);
          }
        }
      }

      if (! pick.empty()) {
        Reaction r = pick.result();
        r.used.applyFromActor(this, r.subject);
      }
    }
  }
  
  
  
  /**  Handling hunger, injury, healing and eating, etc:
    */
  void update() {
    for (ActorTechnique t : type().classTechniques) {
      if (known.includes(t)) continue;
      if (t.canLearn(this)) known.add(t);
    }
    super.update();
  }
  
  
  
  /**  Handling sight-range and combat-stats:
    */
  public int meleeDamage() {
    Good weapon = type().weaponType;
    if (weapon != null) {
      int damage = weapon.meleeDamage;
      damage += carried(weapon);
      return damage;
    }
    else return super.meleeDamage();
  }
  
  
  public int rangeDamage() {
    Good weapon = type().weaponType;
    if (weapon != null) {
      int damage = weapon.rangeDamage;
      damage += carried(weapon);
      return damage;
    }
    else return super.rangeDamage();
  }
  
  
  public int armourClass() {
    Good armour = type().weaponType;
    if (armour != null) {
      int amount = armour.armourClass;
      amount += carried(armour);
      return amount;
    }
    else return super.armourClass();
  }
  
  
  public boolean armed() {
    return Nums.max(meleeDamage(), rangeDamage()) > 0;
  }
  
  
  
  /**  Aging, reproduction and life-cycle methods-
    */
  void updateLifeCycle(Base city, boolean onMap) {
    super.updateLifeCycle(city, onMap);
    
    WorldSettings settings = city.world.settings;
    
    if (pregnancy > 0) {
      boolean canBirth = (home() != null && inside() == home()) || ! onMap;
      pregnancy += 1;
      if (pregnancy > PREGNANCY_LENGTH && canBirth) {
        float dieChance = AVG_CHILD_MORT / 100f;
        if (! settings.toggleChildMort) dieChance = 0;
        
        //I.say(this+" FINISHED TERM...");
        
        if (Rand.num() >= dieChance) {
          completePregnancy(home(), onMap);
        }
        else {
          pregnancy = 0;
          //I.say(this+" LOST THEIR CHILD.");
        }
      }
      if (pregnancy > PREGNANCY_LENGTH + DAY_LENGTH) {
        pregnancy = 0;
        //I.say(this+" CANCELLED PREGNANCY!");
      }
    }
    
    if (ageSeconds % YEAR_LENGTH == 0) {
      
      boolean canDie = settings.toggleAging;
      if (senior() && canDie && Rand.index(100) < AVG_SENIOR_MORT) {
        setAsKilled("Old age");
      }
      
      boolean canConceive = home() != null || ! onMap;
      if (woman() && fertile() && pregnancy == 0 && canConceive) {
        float
          ageYears   = ageSeconds / (YEAR_LENGTH * 1f),
          fertSpan   = AVG_MENOPAUSE - AVG_MARRIED,
          fertility  = (AVG_MENOPAUSE - ageYears) / fertSpan,
          wealth     = BuildingForHome.wealthLevel(this),
          chanceRng  = MAX_PREG_CHANCE - MIN_PREG_CHANCE,
          chanceW    = MAX_PREG_CHANCE - (wealth * chanceRng),
          pregChance = fertility * chanceW / 100
        ;
        if (Rand.num() < pregChance) {
          beginPregnancy();
          //I.say(this+" BECAME PREGNANT!  TIME TO TERM: "+(PREGNANCY_LENGTH+map.time()));
        }
      }
    }
  }
  
  
  public boolean pregnant() {
    return pregnancy > 0;
  }
  
  
  public void beginPregnancy() {
    pregnancy = 1;
  }
  
  
  public void completePregnancy(Building venue, boolean onMap) {
    
    ActorAsPerson child  = (ActorAsPerson) type().childType().generate();
    ActorAsPerson father = (ActorAsPerson) bondedWith(BOND_MARRIED);
    setBond(this  , child, BOND_CHILD, BOND_PARENT, 0.5f);
    setBond(father, child, BOND_CHILD, BOND_PARENT, 0.5f);
    pregnancy = 0;
    
    if (onMap) {
      AreaTile at = venue.at();
      child.enterMap(map, at.x, at.y, 1, base());
      child.setInside(venue, true);
      venue.setResident(child, true);
    }
    
    //I.say(this+" GAVE BIRTH TO "+child);
  }
  
  
  public boolean child() {
    return ageYears() < AVG_PUBERTY;
  }
  
  
  public boolean senior() {
    return ageYears() > AVG_RETIREMENT;
  }
  
  
  public boolean fertile() {
    return ageYears() > AVG_MARRIED && ageYears() < AVG_MENOPAUSE;
  }
  
  
  public boolean adult() {
    return ! (child() || senior());
  }
  
  
  public boolean man() {
    return (sexData & SEX_MALE) != 0;
  }
  
  
  public boolean woman() {
    return (sexData & SEX_FEMALE) != 0;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String fullName() {
    if (customName.length() > 0) return customName;
    return super.fullName();
  }
  
  
  public void setCustomName(String name) {
    this.customName = name;
  }
}












