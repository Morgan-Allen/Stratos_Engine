

package game;
import util.*;
import static game.Task.*;
import static game.Area.*;
import static game.ActorTraits.*;
import static game.GameConstants.*;



public class ActorAsPerson extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  List <Task> todo = new List();
  
  String customName = "";
  
  
  public ActorAsPerson(ActorType type) {
    super(type);
  }
  
  
  public ActorAsPerson(Session s) throws Exception {
    super(s);
    
    s.loadObjects(todo);
    
    customName = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObjects(todo);
    
    s.saveString(customName);
  }
  
  
  
  
  /**  Spawning new behaviours:
    */
  void beginNextBehaviour() {
    //
    //  TODO:  You will need to ensure that work/home/formation venues are
    //  present on the same map to derive related bahaviours!
    //
    //  Establish some facts about the citizen first:
    boolean adult = health.adult();
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
      choice.add(selectTechniqueUse(false, (Series) map.actors()));
      
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
    
    //  TODO:  You need to use the wouldSwitch method here, to account for
    //  any and all emergency activities...
    float oldPriority = Nums.max(jobPriority(), 0);
    
    if (jobType() != Task.JOB.RETREAT) {
      if (armed() && ! Task.inCombat(this)) {
        TaskCombat combat = TaskCombat.nextReaction(this, seen());
        if (combat != null) assignTask(combat);
      }
      
      TaskRetreat retreat = TaskRetreat.configRetreat(this);
      if (retreat != null && retreat.priority() > oldPriority) {
        assignTask(retreat);
        if (mission() != null) mission().toggleRecruit(this, false);
      }
    }
    
    TaskDialog dialog = TaskDialog.nextCasualDialog(this, seen());
    if (dialog != null && dialog.priority() > oldPriority) {
      assignTask(dialog);
    }
    
    if (health.cooldown() == 0) {
      Task reaction = selectTechniqueUse(true, seen());
      if (reaction != null) assignReaction(reaction);
    }
  }
  
  
  Task selectTechniqueUse(boolean reaction, Series <Active> assessed) {
    class Reaction { ActorTechnique used; Target subject; float rating; }
    Pick <Reaction> pick = new Pick(0);
    
    for (Active other : assessed) {
      for (ActorTechnique used : traits.known()) {
        if (used.canUseActive(this, other)) {
          Reaction r = new Reaction();
          r.rating  = used.rateUse(this, other);
          r.subject = other;
          r.used    = used;
          pick.compare(r, r.rating);
        }
      }
      for (Good g : outfit.carried.keys()) for (ActorTechnique used : g.allows) {
        if (used.canUseActive(this, other)) {
          Reaction r = new Reaction();
          r.rating  = used.rateUse(this, other);
          r.subject = other;
          r.used    = used;
          pick.compare(r, r.rating);
        }
      }
    }
    if (pick.empty()) return null;
    
    Reaction r = pick.result();
    return r.used.useFor(this, r.subject);
  }
  
  
  float updateFearLevel() {
    backup().clear();
    return TaskRetreat.fearLevel(this, backup());
  }
  
  
  
  /**  Handling sight-range and combat-stats:
    */
  public int meleeDamage() {
    Good weapon = type().weaponType;
    if (weapon != null) {
      int damage = weapon.meleeDamage;
      damage += outfit.carried(weapon);
      return damage;
    }
    else return super.meleeDamage();
  }
  
  
  public int rangeDamage() {
    Good weapon = type().weaponType;
    if (weapon != null) {
      int damage = weapon.rangeDamage;
      damage += outfit.carried(weapon);
      return damage;
    }
    else return super.rangeDamage();
  }
  
  
  public int armourClass() {
    Good armour = type().weaponType;
    if (armour != null) {
      int amount = armour.armourClass;
      amount += outfit.carried(armour);
      return amount;
    }
    else return super.armourClass();
  }
  
  
  public boolean armed() {
    return Nums.max(meleeDamage(), rangeDamage()) > 0;
  }
  
  
  
  /**  Aging, reproduction and life-cycle methods-
    */
  ActorHealth initHealth() {
    return new ActorHealth(this) {
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
              wealth     = BuildingForHome.wealthLevel(actor),
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
      
      
      public void completePregnancy(Building venue, boolean onMap) {
        
        ActorAsPerson child  = (ActorAsPerson) type().childType().generate();
        ActorAsPerson father = (ActorAsPerson) traits.bondedWith(BOND_MARRIED);
        setBond(actor , child, BOND_CHILD, BOND_PARENT, 0.5f);
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
      
      
      public float growLevel() {
        return Nums.min(1, ageYears() / AVG_MARRIED);
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
      
    };
    
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












