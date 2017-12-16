

package game;
import util.*;
import static game.Task.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class ActorAsAnimal extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  static float grazeOkay = 0, grazeFail = 0;
  static boolean reportCycle = false;
  
  
  public ActorAsAnimal(Type type) {
    super(type);
  }
  
  
  public ActorAsAnimal(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Behaviour scripting-
    */
  void beginNextBehaviour() {
    //
    //  Establish a few details first...
    float hurtRating = fatigue + injury;
    Tile rests = findGrazePoint();
    assignTask(null);
    //
    //  If you're seriously tired or hurt, but not hungry, find a place to rest:
    if (hurtRating > type.maxHealth / 2 && hunger < 1 && rests != null) {
      embarkOnTarget(rests, 10, JOB.RESTING, null);
    }
    //
    //  If you're hungry, look for food, either by grazing within your habitat
    //  or seeking prey:
    if (idle() && hunger >= 1) {
      if (type.predator) {
        Actor prey = findPrey();
        if (prey != null) beginAttack(prey, JOB.HUNTING, null);
      }
      else if (rests != null) {
        embarkOnTarget(rests, 1, JOB.FORAGING, null);
      }
    }
    //
    //  If you're still in pain, rest up-
    if (idle() && hurtRating >= 0.5 && rests != null) {
      embarkOnTarget(at(), 10, JOB.RESTING, null);
    }
    //
    //  If that all fails, wander about a little-
    if (idle()) {
      startRandomWalk();
    }
  }
  
  
  Tile findGrazePoint() {
    
    Pick <Tile> pick = new Pick();
    for (Tile t : CityMap.adjacent(at(), null, map)) {
      if (t == null || map.blocked(t)) continue;
      if (! Visit.arrayIncludes(type.habitats, t.terrain)) continue;
      pick.compare(t, Rand.num());
    }
    
    if (! pick.empty()) {
      grazeOkay += 1;
      return pick.result();
    }
    
    grazeFail += 1;
    return CityMapTerrain.findGrazePoint(type, map);
  }
  
  
  Actor findPrey() {
    //
    //  TODO:  Try and sample a smaller number of targets.
    Pick <Actor> pick = new Pick();
    
    //  NOTE:  We generously assume that hunters are chivalrous enough not to
    //  target pregnant animals (for the sake of ensuring that populations can
    //  regenerate.)
    
    for (Actor a : map.actors) {
      int category = a.type.category;
      if (a.type.predator) continue;
      if (a.pregnancy != 0) continue;
      
      float dist   = CityMap.distance(a.at(), at());
      float rating = CityMap.distancePenalty(dist);
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
      hunger -= 1f / (STARVE_INTERVAL / 4);
    }
    
    if (jobType() == JOB.HUNTING) {
      
      //  TODO:  Check for this dynamically, or extend TaskCombat...
      Actor prey = (Actor) target;
      performAttack(prey, true);
      
      if (prey.alive()) {
        beginAttack(prey, JOB.HUNTING, null);
      }
      else {
        float oldH = hunger, yield = meatYield(prey);
        hunger -= yield * 1f / FOOD_UNIT_PER_HP;
        
        if (reportCycle) {
          I.say(this+" ATE PREY: "+prey);
          I.add(", yield: "+yield+", hunger: "+oldH+"->"+hunger);
        }
      }
    }
  }
  
  
  
  /**  Regular updates and life-cycle:
    */
  boolean adult() {
    return ageSeconds > ANIMAL_MATURES;
  }
  
  
  float growLevel() {
    return Nums.min(1, ageSeconds * 1f / ANIMAL_MATURES);
  }
  
  
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
  
  
  void updateAging() {
    super.updateAging();
    //
    //  Once per month, check to see if breeding conditions are correct.  (In
    //  the event that your numbers are really low, pregnancy is set to -1 to
    //  discourage predators.)
    if (ageSeconds % MONTH_LENGTH == 0) {
      if (growLevel() == 1 && pregnancy <= 0) {
        
        float idealPop = CityMapTerrain.idealPopulation(type, map);
        float actualPop = 0;
        for (Actor a : map.actors) if (a.type == type) {
          actualPop += a.pregnancy > 0 ? 2 : 1;
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
      if (ageSeconds > type.lifespan) {
        setAsKilled("Old age");
      }
    }
    //
    //  If you've come to term, give birth:
    if (pregnancy > 0) {
      pregnancy += 1;
      if (pregnancy > ANIMAL_PREG_TIME) {
        pregnancy = 0;
        
        Actor child = (ActorAsAnimal) type.generate();
        Tile at = this.at();
        child.enterMap(map, at.x, at.y, 1);
        if (reportCycle) I.say(this+" GAVE BIRTH");
      }
    }
  }
  
  
  void setAsKilled(String cause) {
    super.setAsKilled(cause);
    if (reportCycle) I.say(this+" DIED FROM CAUSE: "+cause);
  }
  
  
}






