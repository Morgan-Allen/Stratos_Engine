

package game;
import static game.GameConstants.*;
import util.*;



public class BaseGrowth {
  
  
  /**  Data fields and save/load methods-
    */
  final Base base;
  
  private float maxPopulation = 0;
  private float maxArmyPower  = 0;
  private float population    = 0;
  private float employment    = 0;
  private float armyPower     = 0;
  
  Tally <BuildType> buildLevel = new Tally();
  
  
  BaseGrowth(Base base) {
    this.base = base;
  }
  
  void loadState(Session s) throws Exception {
    maxPopulation = s.loadFloat();
    maxArmyPower  = s.loadFloat();
    population    = s.loadFloat();
    employment    = s.loadFloat();
    armyPower     = s.loadFloat();
    s.loadTally(buildLevel);
  }
  
  void saveState(Session s) throws Exception {
    s.saveFloat(maxPopulation);
    s.saveFloat(maxArmyPower );
    s.saveFloat(population   );
    s.saveFloat(employment   );
    s.saveFloat(armyPower    );
    s.saveTally(buildLevel);
  }

  
  
  /**  Handling army strength and population (for off-map cities-)
    */
  public void initBuildLevels(Tally <BuildType> buildLevels) {
    this.buildLevel.clear();
    this.buildLevel.add(buildLevels);
    updateOffmapGrowth(0);
    this.population = maxPopulation;
    this.armyPower  = maxArmyPower;
  }
  
  
  public void initBuildLevels(Object... buildLevelArgs) {
    Tally <BuildType> t = new Tally();
    t.setWith(buildLevelArgs);
    initBuildLevels(t);
  }
  
  
  public float maxPopulation() {
    return maxPopulation;
  }
  
  
  public float maxArmyPower() {
    return maxArmyPower;
  }
  
  
  public float population() {
    return population;
  }
  
  
  public float employment() {
    return employment;
  }
  
  
  public float armyPower() {
    return armyPower;
  }
  
  
  public Tally <BuildType> buildLevel() {
    return buildLevel;
  }
  
  
  public void setPopulation(float pop) {
    this.population = pop;
  }
  
  
  public void setArmyPower(float power) {
    this.armyPower = power;
  }
  
  
  public void addPopulation(float inc) {
    this.population = Nums.max(0, population + inc);
  }
  
  
  public void addArmyPower(float inc) {
    this.armyPower = Nums.max(0, armyPower + inc);
  }
  
  
  float wallsLevel() {
    float sum = 0;
    for (BuildType t : buildLevel.keys()) {
      if (t.category == Type.IS_WALLS_BLD) {
        float l = buildLevel.valueFor(t);
        sum += l * 1;
      }
    }
    return sum;
  }
  
  
  
  /**  Regular updates-
    */
  void updateLocalGrowth(AreaMap activeMap) {

    int citizens = 0;
    for (Actor a : activeMap.actors) if (a.base() == base) {
      citizens += 1;
    }
    this.population = citizens * POP_PER_CITIZEN;
    
    float armyPower = 0;
    for (Building b : activeMap.buildings()) if (b.base() == base) {
      if (b.type().category == Type.IS_ARMY_BLD) {
        armyPower += MissionForStrike.powerSum(b.workers(), activeMap);
      }
    }
    this.armyPower = armyPower;
    
    buildLevel.clear();
    for (Building b : activeMap.buildings()) if (b.base() == base) {
      buildLevel.add(1, b.type());
    }
  }
  
  
  void updateOffmapGrowth(int updateGap) {
    
    //
    //  Okay.  First thing's first.  Establish broad rates of population growth
    //  and build structures to satisfy it.
    float absMaxPopulation = MAX_POPULATION;
    float popGrowth = 0;
    
    if (updateGap > 0 && base.techTypes().size() > 0) {
      
      class Rating {
        BuildType type;
        float rating;
        public String toString() { return type+": "+rating; }
      }
      
      List <Rating> ratings = new List <Rating> () {
        protected float queuePriority(Rating r) {
          return r.rating;
        }
      };
      
      popGrowth = absMaxPopulation / POP_MAX_YEARS;
      popGrowth *= 1 - (population / absMaxPopulation);
      popGrowth *= updateGap * 1f / YEAR_LENGTH;
      
      for (BuildType type : base.techTypes()) {
        if (type.isUpgrade || ! type.hasPrerequisites(base)) continue;
        if (type.uniqueBuilding && buildLevel.valueFor(type) > 0) continue;

        float numGuilds = buildLevel.valueFor(type);
        Rating r = new Rating();
        r.type = type;
        ratings.add(r);
        
        if (type.maxResidents > 0) {
          r.rating += type.maxResidents * (population - maxPopulation);
        }
        if (! type.workerTypes.empty()) {
          r.rating = type.workerTypes.total() * (population - employment);
        }
        r.rating /= (3 + numGuilds) / 3f;
        r.rating /= 10 + type.buildCostEstimate();
      }
      
      ratings.queueSort();
      for (Rating r : ratings) if (r.rating > 0) {
        buildLevel.add(1, r.type);
        ///I.say("\nAdded "+r.type+" to "+base+", build-levels: "+buildLevel);
        break;
      }
    }
    
    //
    //  Then, determine what your ideal population and army-power would be-
    float sumPop  = 0;
    float sumJobs = 0;
    float sumArmy = 0;
    
    for (BuildType t : buildLevel.keys()) {
      float l = buildLevel.valueFor(t);
      
      sumPop += l * t.maxResidents;
      
      for (ActorType w : t.workerTypes.keys()) {
        float maxW = t.workerTypes.valueFor(w);
        sumArmy += l * TaskCombat.attackPower(w) * maxW;
        sumJobs += l * maxW;
      }
    }
    employment    = sumJobs * POP_PER_CITIZEN;
    maxPopulation = sumPop  * POP_PER_CITIZEN;
    maxArmyPower  = sumArmy * POP_PER_CITIZEN;
    
    //
    //  Then increment total population and army-power-
    if (population < maxPopulation) popGrowth *= 2;
    population = Nums.clamp(population + popGrowth, 0, absMaxPopulation);
    
    float armyCap = maxArmyPower;
    
    for (Mission f : base.missions()) {
      armyCap -= MissionForStrike.powerSum(f.recruits(), null);
    }
    if (armyCap < 0) {
      armyCap = 0;
    }
    if (armyPower < armyCap) {
      armyPower = Nums.min(armyCap, armyPower + popGrowth);
    }
  }
  
  
}





