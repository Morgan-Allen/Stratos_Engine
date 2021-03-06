

package game;
import static game.Task.*;
import static game.GameConstants.*;
import util.*;



public class ActorAsAnimal extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static float grazeOkay = 0, grazeFail = 0;
  public static boolean reportCycle = false;
  
  Actor master;
  boolean rides;
  
  
  public ActorAsAnimal(ActorType type) {
    super(type);
  }
  
  
  public ActorAsAnimal(Session s) throws Exception {
    super(s);
    master = (Actor) s.loadObject();
    rides  = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(master);
    s.saveBool(rides);
  }
  
  
  
  /**  Behaviour scripting-
    */
  void beginNextBehaviour() {
    //
    //  Establish a few details first...
    float hurtRating = health.fatigue() + health.injury();
    float hunger = health.hunger();
    AreaTile rests = type().organic ? findGrazePoint() : null;
    assignTask(null, this);
    
    if (type().organic) {
      //
      //  If you're seriously tired or hurt, but not hungry, find a place to rest:
      if (hurtRating > type().maxHealth / 2 && hunger < 1 && rests != null) {
        assignTask(targetTask(rests, 10, JOB.RESTING, null), this);
      }
      //
      //  If you're hungry, look for food, either by grazing within your habitat
      //  or seeking prey:
      if (idle() && hunger >= 1) {
        if (type().predator) {
          Actor prey = findPrey();
          TaskCombat hunt = TaskCombat.configHunting(this, prey);
          if (hunt != null) assignTask(hunt, this);
        }
        else if (rests != null) {
          assignTask(targetTask(rests, 1, JOB.FORAGING, null), this);
        }
      }
      //
      //  If you're still in pain, rest up-
      if (idle() && hurtRating >= 0.5 && rests != null) {
        assignTask(targetTask(at(), 10, JOB.RESTING, null), this);
      }
    }
    //
    //  If you're assigned a mission, take it on:
    if (idle() && mission() != null && mission().active()) {
      assignTask(mission().selectActorBehaviour(this), this);
    }
    //
    //  If you have a master, keep them safe:
    Actor master = (Actor) bonds.bondedWith(ActorBonds.BOND_MASTER);
    if (idle() && master != null) {
      Task patrol = TaskPatrol.protectionFor(this, master, null);
      assignTask(patrol, this);
    }
    //
    //  If you have a home, see what that has for you:
    if (idle() && home() != null && home().complete()) {
      assignTask(home().selectActorBehaviour(this), this);
    }
    //
    //  If that all fails, wander about a little-
    if (idle()) {
      assignTask(TaskWander.nextWandering(this), this);
    }
  }
  
  
  void updateReactions() {
    if (! map.world.settings.toggleReacts) return;
    
    Series <Active> assessed = map.activeInRange(at(), sightRange());
    
    if (jobType() != Task.JOB.RETREAT) {
      if (! Task.inCombat(this)) {
        TaskCombat combat = TaskCombat.nextReaction(this, assessed);
        if (combat != null) assignTask(combat, this);
      }
      
      float oldPriority = jobPriority();
      TaskRetreat retreat = TaskRetreat.configRetreat(this);
      if (retreat != null && retreat.priority() > oldPriority) {
        assignTask(retreat, this);
        if (mission() != null) mission().toggleRecruit(this, false);
      }
    }
  }
  
  

  /**  Custom behaviour-scripting...
    */
  AreaTile findGrazePoint() {
    
    Pick <AreaTile> pick = new Pick();
    for (AreaTile t : AreaMap.adjacent(at(), null, map)) {
      if (t == null || map.blocked(t)) continue;
      if (! Visit.arrayIncludes(type().habitats, t.terrain)) continue;
      pick.compare(t, Rand.num());
    }
    
    if (! pick.empty()) {
      grazeOkay += 1;
      return pick.result();
    }
    
    grazeFail += 1;
    return AreaTerrain.findGrazePoint(type(), map);
  }
  
  
  Actor findPrey() {
    //
    //  TODO:  Try and sample a smaller number of targets.
    Pick <Actor> pick = new Pick();
    
    //  NOTE:  We generously assume that hunters are chivalrous enough not to
    //  target pregnant animals (for the sake of ensuring that populations can
    //  regenerate.)
    
    for (Actor a : map.actors) {
      int category = a.type().category;
      if (a.type().predator) continue;
      if (a.health.pregnancy != 0) continue;
      
      float dist   = AreaMap.distance(a.at(), at());
      float rating = AreaMap.distancePenalty(dist);
      if (category != Type.IS_ANIMAL_ACT) rating /= 2;
      
      pick.compare(a, rating);
    }
    return pick.result();
  }
  
  
  static float meatYield(Actor prey) {
    return AVG_ANIMAL_YIELD * prey.growLevel();
  }
  
  
  protected void onTarget(Target target) {
    
    if (jobType() == JOB.RESTING) {
      return;
    }
    
    if (jobType() == JOB.FORAGING) {
      health.liftHunger(1f / (STARVE_INTERVAL / 4));
    }
    
    if (jobType() == JOB.HUNTING) {
      
      //  TODO:  Check for this dynamically, or extend TaskCombat...
      Actor prey = (Actor) ((TaskCombat) task()).primary;
      
      if (prey.health.alive()) {
        TaskCombat hunt = TaskCombat.configHunting(this, prey);
        if (hunt != null) assignTask(hunt, this);
      }
      else {
        float oldH = health.hunger(), yield = meatYield(prey);
        health.liftHunger(yield * 1f / FOOD_UNIT_PER_HP);
        
        if (reportCycle) {
          I.say(this+" ATE PREY: "+prey);
          I.add(", yield: "+yield+", hunger: "+oldH+"->"+health.hunger());
        }
      }
    }
  }
  
  
  
  /**  Regular updates and life-cycle:
    */
  void update() {
    //  TODO:  Add any special functions here...
    super.update();
  }
  
  
  ActorHealth initHealth() {
    return new ActorHealth(this) {
      void updateLifeCycle(Area locale, boolean onMap) {
        super.updateLifeCycle(locale, onMap);
        //
        //  Once per month, check to see if breeding conditions are correct.  (In
        //  the event that your numbers are really low, pregnancy is set to -1 to
        //  discourage predators.)
        if (ageSeconds % DAY_LENGTH == 0) {
          if (growLevel() == 1 && pregnancy <= 0) {
            
            float idealPop = AreaTerrain.idealPopulation(type(), map);
            float actualPop = 0;
            for (Actor a : map.actors) if (a.type() == type()) {
              actualPop += a.health.pregnancy > 0 ? 2 : 1;
            }
            
            float birthChance = 1.25f - (actualPop / idealPop);
            if (Rand.num() < birthChance) {
              pregnancy = 1;
            }
            
            if (pregnancy == 0) {
              pregnancy = (actualPop < idealPop / 2) ? -1 : 0;
            }
          }
          //
          //  But if you're too damned old, just die-
          boolean canDie = base().world.settings.toggleAging;
          if (canDie && ageSeconds > type().lifespan) {
            setAsKilled("Old age");
          }
        }
        //
        //  If you've come to term, give birth:
        if (pregnancy > 0) {
          pregnancy += 1;
          if (pregnancy > ANIMAL_PREG_TIME) {
            pregnancy = 0;
            
            Actor child = (ActorAsAnimal) type().generate();
            AreaTile at = actor.at();
            child.enterMap(map, at.x, at.y, 1, base());
            if (reportCycle) I.say(this+" GAVE BIRTH");
          }
        }
      }
      
      
      public boolean adult() {
        return ageSeconds > ANIMAL_MATURES;
      }
      
      
      public float growLevel() {
        return Nums.min(1, ageSeconds * 1f / ANIMAL_MATURES);
      }
      
      
      public void setAsKilled(String cause) {
        super.setAsKilled(cause);
        if (reportCycle) I.say(this+" DIED FROM CAUSE: "+cause);
      }
    };
  }
  
  
}






