

package game;
import static game.GameConstants.*;
import util.*;



public class BaseGrowth {
  
  
  final Base base;
  
  private float population = 0;
  private float armyPower  = 0;
  
  Tally <BuildType> buildLevel = new Tally();
  
  
  BaseGrowth(Base base) {
    this.base = base;
  }
  
  void loadState(Session s) throws Exception {
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(buildLevel);
  }
  
  void saveState(Session s) throws Exception {
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(buildLevel);
  }
  
  
  public void initBuildLevels(Object... buildLevelArgs) {
    this.buildLevel.setWith(buildLevelArgs);
    this.population = idealPopulation();
    this.armyPower  = idealArmyPower ();
  }
  
  
  public Tally <BuildType> buildLevel() {
    return buildLevel;
  }
  

  
  
  /**  Handling army strength and population (for off-map cities-)
    */
  public float idealPopulation() {
    //  TODO:  Cache this?
    float sum = 0;
    for (BuildType t : buildLevel.keys()) {
      float l = buildLevel.valueFor(t);
      sum += l * t.maxResidents;
    }
    return sum * POP_PER_CITIZEN;
  }
  
  
  public float idealArmyPower() {
    //  TODO:  Cache this?
    float sum = 0;
    for (BuildType t : buildLevel.keys()) {
      if (t.isMilitaryBuilding()) {
        float l = buildLevel.valueFor(t);
        for (ActorType w : t.workerTypes.keys()) {
          float maxW = t.workerTypes.valueFor(w);
          sum += l * TaskCombat.attackPower(w) * maxW;
        }
      }
    }
    
    return sum * POP_PER_CITIZEN;
  }
  
  
  public float population() {
    return population;
  }
  
  
  public float armyPower() {
    return armyPower;
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
    
    float popRegen  = updateGap * 1f / (LIFESPAN_LENGTH / 2);
    float idealPop  = idealPopulation();
    float idealArmy = idealArmyPower();
    
    if (population < idealPop) {
      population = Nums.min(idealPop, population + popRegen);
    }
    for (Mission f : base.missions()) {
      idealArmy -= MissionForStrike.powerSum(f.recruits(), null);
    }
    
    if (idealArmy < 0) {
      idealArmy = 0;
    }
    if (armyPower < idealArmy) {
      armyPower = Nums.min(idealArmy, armyPower + popRegen);
    }
  }
  
}








