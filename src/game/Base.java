

package game;
import static game.GameConstants.*;
import static game.World.*;
import static game.RelationSet.*;
import util.*;



public class Base implements Session.Saveable, Trader, RelationSet.Focus {
  
  
  /**  Data fields, construction and save/load methods-
    */
  String name = "City";
  
  final public World world;
  final public Area area;
  
  private Faction faction;
  private Federation federation = null;
  
  final public BaseCouncil   council   = new BaseCouncil  (this);
  final public BaseRelations relations = new BaseRelations(this);
  final public BaseTrading   trading   = new BaseTrading  (this);
  
  Building headquarters = null;
  private int   currentFunds = 0;
  private float population   = 0;
  private float armyPower    = 0;
  
  Tally <BuildType> buildLevel = new Tally();
  List <Mission> missions = new List();
  
  //
  //  These are specific to off-map bases...
  List <Mission> guarding = new List();
  List <BuildType> buildTypes = new List();
  
  
  
  
  public Base(World world, Area area, Faction faction) {
    this(world, area, faction, "Base???");
  }
  
  
  public Base(World world, Area area, Faction faction, String name) {
    if (world == null) I.complain("CANNOT PASS NULL WORLD:  "+name);
    if (area  == null) I.complain("CANNOT PASS NULL LOCALE: "+name);
    
    this.world   = world  ;
    this.area  = area ;
    this.faction = faction;
    
    this.name = name;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world   = (World  ) s.loadObject();
    area    = (Area   ) s.loadObject();
    faction = (Faction) s.loadObject();
    
    s.loadObjects(buildTypes);
    
    council  .loadState(s);
    relations.loadState(s);
    trading  .loadState(s);
    
    headquarters = (Building) s.loadObject();
    currentFunds = s.loadInt();
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(buildLevel);
    
    s.loadObjects(missions);
    s.loadObjects(guarding);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveObject(area);
    s.saveObject(faction);
    
    s.saveObjects(buildTypes);
    
    council  .saveState(s);
    relations.saveState(s);
    trading  .saveState(s);
    
    s.saveObject(headquarters);
    
    s.saveInt(currentFunds);
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(buildLevel);
    
    s.saveObjects(missions);
    s.saveObjects(guarding);
  }
  
  
  
  /**  Supplemental setup/query methods for economy, trade and geography-
    */
  public Faction faction() {
    return faction;
  }
  
  
  public RelationSet relations(World world) {
    return relations;
  }
  
  
  void assignFaction(Faction faction) {
    if (faction != this.faction) federation = null;
    this.faction = faction;
  }
  
  
  public Federation federation() {
    if (federation != null) return federation;
    federation = world.federation(faction);
    return federation;
  }
  
  
  public AreaMap activeMap() {
    return area.activeMap();
  }
  
  
  
  /**  Assigning build-levels:
    */
  public void assignBuildTypes(BuildType... types) {
    buildTypes.clear();
    Visit.appendTo(buildTypes, types);
  }
  
  
  public Series <BuildType> buildTypes() {
    return buildTypes;
  }
  
  
  public void initBuildLevels(Object... buildLevelArgs) {
    this.buildLevel.setWith(buildLevelArgs);
    this.population = idealPopulation();
    this.armyPower  = idealArmyPower ();
  }
  
  
  public Tally <BuildType> buildLevel() {
    return buildLevel;
  }
  
  
  
  /**  Accessing missions-
   */
  public Series <Mission> missions() {
    return missions;
  }
  
  public void addMission(Mission mission) {
    missions.include(mission);
  }
  
  public void removeMission(Mission mission) {
    missions.remove(mission);
  }
  
  public Mission matchingMission(int objective, Object focus) {
    for (Mission m : missions) {
      if (m.objective != objective) continue;
      if (m.localFocus() == focus) return m;
      if (m.worldFocus() == focus) return m;
    }
    return null;
  }
  
  void toggleGuarding(Mission m, boolean is) {
    //if (is) I.say("ADDING GUARD: "+this);
    //else    I.say("LOSING GUARD: "+this);
    guarding.toggleMember(m, is);
  }
  
  
  
  /**  Setting up basic relations-
    */
  public void setHeadquarters(Building headquarters) {
    this.headquarters = headquarters;
  }
  
  
  public Building headquarters() {
    return headquarters;
  }
  
  
  
  /**  Setting and accessing tribute and trade levels-
    */
  public void initFunds(int funds) {
    currentFunds = funds;
  }
  
  
  public int funds() {
    return currentFunds;
  }
  
  
  public int incFunds(int inc) {
    currentFunds += inc;
    return currentFunds;
  }
  
  
  public Base base() { return this; }
  
  
  
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
  
  
  public void incPopulation(float inc) {
    this.population = Nums.max(0, population + inc);
  }
  
  
  public void incArmyPower(float inc) {
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
  void updateBase() {
    final AreaMap map = area.activeMap();
    final int UPDATE_GAP = map == null ? DAY_LENGTH : 10;
    boolean updateStats = world.time % UPDATE_GAP == 0;
    boolean activeMap   = map != null;
    boolean playerOwns  = faction == world.playerFaction && activeMap;
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      trading.updateLocalStocks();
      
      int citizens = 0;
      for (Actor a : map.actors) if (a.base() == this) {
        citizens += 1;
      }
      this.population = citizens * POP_PER_CITIZEN;
      
      float armyPower = 0;
      for (Building b : map.buildings()) if (b.base() == this) {
        if (b.type().category == Type.IS_ARMY_BLD) {
          armyPower += MissionForStrike.powerSum(b.workers(), map);
        }
      }
      this.armyPower = armyPower;
      
      buildLevel.clear();
      for (Building b : map.buildings()) if (b.base() == this) {
        buildLevel.add(1, b.type());
      }
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && ! activeMap) {
      trading.updateOffmapStocks(UPDATE_GAP);
      
      float popRegen  = UPDATE_GAP * 1f / (LIFESPAN_LENGTH / 2);
      float idealPop  = idealPopulation();
      float idealArmy = idealArmyPower();
      
      if (population < idealPop) {
        population = Nums.min(idealPop, population + popRegen);
      }
      for (Mission f : missions) {
        idealArmy -= MissionForStrike.powerSum(f.recruits(), null);
      }
      for (Mission g : guarding) {
        if (g.worldFocus() != this || g.complete()) toggleGuarding(g, false);
      }
      
      if (idealArmy < 0) {
        idealArmy = 0;
      }
      if (armyPower < idealArmy) {
        armyPower = Nums.min(idealArmy, armyPower + popRegen);
      }
      
      if (relations.isLoyalVassalOf(faction)) {
        if (council.considerRevolt(faction, UPDATE_GAP, this)) {
          relations.toggleRebellion(faction, true);
        }
      }
    }
    //
    //  Either way, we allow prestige and loyalty to return gradually to
    //  defaults over time, and update the council-
    if (updateStats) {
      relations.updateRelations(DAY_LENGTH);
      council.updateCouncil(playerOwns);
    }
    //
    //  And, once per year, tally up any supply obligations to your current
    //  lord (with the possibility of entering a state of revolt if those are
    //  failed.)
    if (world.time % YEAR_LENGTH == 0) {
      relations.updateTribute(YEAR_LENGTH);
      trading.wipeRecords(YEAR_LENGTH);
    }
    //
    //  Update any formations and actors currently active-
    for (Mission f : missions) {
      f.update();
    }
    //
    //  And update traders-
    if (updateStats && ! activeMap) {
      trading.updateOffmapTraders();
    }
  }
  
  

  /**  Last-but-not-least, returning available Powers:
    */
  public Series <ActorTechnique> rulerPowers() {
    Batch <ActorTechnique> all = new Batch();
    if (activeMap() == null) {
      return all;
    }
    for (Building b : activeMap().buildings) if (b.base() == this) {
      for (ActorTechnique t : b.rulerPowers()) if (b.canUsePower(t)) {
        all.include(t);
      }
    }
    return all;
  }
  
  
  
  /**  Boilerplate interface methods-
    */
  public Tally <Good> inventory () { return trading.inventory (); }
  public Tally <Good> needLevels() { return trading.needLevels(); }
  public Tally <Good> prodLevels() { return trading.prodLevels(); }
  
  public float shopPrice(Good good, Task purchase) {
    return trading.shopPrice(good, purchase);
  }
  
  public float importPrice(Good g, Base sells) {
    return trading.importPrice(g, sells);
  }
  
  public float exportPrice(Good g, Base buys) {
    return trading.exportPrice(g, buys);
  }
  
  public boolean allowExport(Good g, Trader buys) {
    return trading.allowExport(g, buys);
  }
  
  
  
  /**  Generating trouble...
    */
  public int posture(Base other) {
    if (other == null) {
      return BOND_NEUTRAL;
    }
    if (other == this) {
      return BOND_ALLY;
    }
    if (other.faction() == this.faction()) {
      Base capital = federation().capital();
      if (this  == capital) return BOND_VASSAL;
      if (other == capital) return BOND_LORD;
      return BOND_ALLY;
    }
    return posture(other.faction());
  }
  
  public int posture(Faction other) {
    return federation().relations.bondProperties(other);
  }
  
  public boolean isVassalOf(Base o) { return posture(o) == BOND_LORD  ; }
  public boolean isLordOf  (Base o) { return posture(o) == BOND_VASSAL; }
  public boolean isEnemyOf (Base o) { return posture(o) == BOND_ENEMY ; }
  public boolean isAllyOf  (Base o) { return posture(o) == BOND_ALLY  ; }
  
  
  public boolean isLoyalVassalOf(Base o) {
    return (posture(o) == BOND_LORD) && relations.isLoyalVassal();
  }
  
  
  public Type type() {
    return faction.type();
  }
  


  /**  Graphical, debug and interface methods-
    */
  public String name() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }
  
  
  public int tint() {
    return faction.tint();
  }
  
  
  public static String descLoyalty(float l) {
    Pick <String> pick = new Pick();
    for (int i = BaseRelations.LOYALTIES.length; i-- > 0;) {
      float dist = Nums.abs(l - BaseRelations.LOYALTIES[i]);
      pick.compare(BaseRelations.LOYALTY_DESC[i], 0 - dist);
    }
    return pick.result();
  }
  
  
  public String toString() {
    return name;
  }
}






