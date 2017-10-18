

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class ActorAsPerson extends Actor {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  public ActorAsPerson(Type type) {
    super(type);
  }
  
  
  public ActorAsPerson(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Spawning new behaviours:
    */
  void beginNextBehaviour() {
    //
    //  Establish some facts about the citizen first:
    boolean adult = adult();
    job = null;
    
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
    if (job == null && formation != null && formation.active) {
      formation.selectWalkerBehaviour(this);
    }
    if (job == null && work != null) {
      work.selectWalkerBehaviour(this);
    }
    if (job == null && home != null) {
      home.selectWalkerBehaviour(this);
    }
    if (job == null && (hurtRating >= 1 || injury > 0)) {
      beginResting(home);
    }
    if (job == null) {
      startRandomWalk();
    }
    
    //  And report afterward...
    if (reports()) {
      I.say("\n"+this+" BEGAN NEW BEHAVIOUR: "+jobType()+", TIME: "+map.time);
      if (job != null) {
        if (job.visits != null) I.say("  VISITING:  "+job.visits);
        if (job.target != null) I.say("  TARGETING: "+job.target);
      }
    }
  }
  
  
  
  /**  Handling hunger, injury, healing and eating, etc:
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
  
  
  Batch <Good> menuAt(Building visits) {
    Batch <Good> menu = new Batch();
    if (visits != null) for (Good g : FOOD_TYPES) {
      if (visits.inventory.valueFor(g) >= 1) menu.add(g);
    }
    return menu;
  }
  
  
  protected void onVisit(Building visits) {
    if (jobType() == Task.JOB.RESTING) {
      
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
  
  
  
  /**  Aging, reproduction and life-cycle methods-
    */
  void updateAging() {
    super.updateAging();
    
    if (pregnancy > 0) {
      pregnancy += 1;
      if (pregnancy > PREGNANCY_LENGTH && home != null && inside == home) {
        
        float dieChance = AVG_CHILD_MORT / 100f;
        if (Rand.num() >= dieChance) {
          Tile at = home.at();
          Actor child = (Actor) CHILD.generate();
          child.enterMap(map, at.x, at.y, 1);
          child.inside = home;
          home.setResident(child, true);
        }
        pregnancy = 0;
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
          pregnancy = 1;
        }
      }
    }
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





