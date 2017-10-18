

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class ActorAsAnimal extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
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
    //  If you're seriously tired or hurt, but not hungry, find a place to rest:
    float hurtRating = fatigue + injury;
    Tile rests = findGrazePoint();
    if (hurtRating > type.maxHealth / 2 && hunger < 1 && rests != null) {
      embarkOnTarget(at(), 10, Task.JOB.RESTING, null);
    }
    //
    //  If you're hungry, look for food, either by grazing within your habitat
    //  or seeking prey:
    if (job == null && hunger >= 1) {
      if (type.predator) {
        Actor prey = findPrey();
        if (prey != null) beginAttack(prey, Task.JOB.HUNTING, null);
      }
      else if (rests != null) {
        embarkOnTarget(rests, 1, Task.JOB.FORAGING, null);
      }
    }
    //
    //  If you're still in pain, rest up-
    if (job == null && hurtRating >= 0.5 && rests != null) {
      embarkOnTarget(at(), 10, Task.JOB.RESTING, null);
    }
    //
    //  If that all fails, wander about a little-
    if (job == null) {
      startRandomWalk();
    }
  }
  
  
  Actor findPrey() {
    //
    //  TODO:  This is pretty inefficient- you shouldn't be iterating over the
    //  entire map!
    Pick <Actor> pick = new Pick();
    for (Actor a : map.walkers) {
      if (a.type.predator) continue;
      float dist = CityMap.distance(a.at(), at());
      
      pick.compare(a, CityMap.distancePenalty(dist));
    }
    return pick.result();
  }
  
  
  Tile findGrazePoint() {
    
    Pick <Tile> pick = new Pick();
    for (Tile t : CityMap.adjacent(at(), null, map, false)) {
      if (t == null || map.blocked(t.x, t.y)) continue;
      if (! Visit.arrayIncludes(type.habitats, t.terrain)) continue;
      pick.compare(t, Rand.num());
    }
    
    if (! pick.empty()) return pick.result();
    
    return CityMapTerrain.findGrazePoint(type, map);
  }
  
  
  protected void onTarget(Target target) {
    
    if (jobType() == Task.JOB.RESTING) {
      return;
    }
    
    if (jobType() == Task.JOB.FORAGING) {
      float eatTime = STARVE_INTERVAL / (TILES_PER_GRAZER * 4);
      hunger -= 1f / eatTime;
    }
    
    if (jobType() == Task.JOB.HUNTING) {
      Actor prey = (Actor) target;
      performAttack(prey);
      
      if (prey.alive()) {
        beginAttack(prey, Task.JOB.HUNTING, null);
      }
      else {
        hunger -= AVG_ANIMAL_YIELD * 1f / FOOD_UNIT_PER_HP;
      }
    }
  }
  
  
  
  /**  Regular updates and life-cycle:
    */
  void update() {
    super.update();
    
    hunger += GameSettings.toggleHunger ? (1f / STARVE_INTERVAL ) : 0;
    
    if (jobType() == Task.JOB.RESTING) {
      float rests = 1f / FATIGUE_REGEN;
      float heals = 1f / HEALTH_REGEN ;
      
      fatigue = Nums.max(0, fatigue - rests);
      injury  = Nums.max(0, injury  - heals);
    }
    else {
      fatigue += GameSettings.toggleFatigue ? (1f / FATIGUE_INTERVAL) : 0;
      float heals = 0.5f / HEALTH_REGEN;
      injury = Nums.max(0, injury - heals);
    }
  }
  
  
  void updateAging() {
    super.updateAging();
    
    if (ageSeconds % MONTH_LENGTH == 0) {
      if (ageSeconds > type.animalLifespan / 2 && pregnancy == 0) {
        
        float idealPop = CityMapTerrain.idealPopulation(type, map);
        float actualPop = 0;
        for (Actor a : map.walkers) if (a.type == type) {
          actualPop += a.pregnancy > 0 ? 2 : 1;
        }
        
        if (idealPop < actualPop) {
          pregnancy = 1;
        }
      }
    }
    
    if (pregnancy > 0) {
      pregnancy += 1;
      if (pregnancy > ANIMAL_PREG_TIME) {
        pregnancy = 0;
        Tile at = home.at();
        Actor child = (Actor) CHILD.generate();
        child.enterMap(map, at.x, at.y, 1);
      }
    }
    
    if (ageSeconds > type.animalLifespan) {
      setAsKilled("Old age");
    }
  }
  
  
}









