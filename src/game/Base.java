

package game;
import static game.GameConstants.*;
import static game.World.*;
import static game.BaseRelations.*;
import util.*;



public class Base implements Session.Saveable, Trader, BaseEvents.Trouble {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum GOVERNMENT {  //  TODO:  Move into the Council class.
    IMPERIAL, FEUDAL, BARBARIAN, REPUBLIC
  }
  
  
  String name = "City";
  int tint = CITY_COLOR;
  
  final public World world;
  final public WorldLocale locale;
  
  GOVERNMENT government = GOVERNMENT.FEUDAL;
  
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
  List <Actor> visitors = new List();
  
  List <BuildType> buildTypes = new List();
  
  private boolean active;
  private Area map;
  
  
  
  public Base(World world, WorldLocale locale) {
    this(world, locale, "Base???");
  }
  
  
  public Base(World world, WorldLocale locale, String name) {
    if (world  == null) I.complain("CANNOT PASS NULL WORLD:  "+name);
    if (locale == null) I.complain("CANNOT PASS NULL LOCALE: "+name);
    
    this.world  = world ;
    this.locale = locale;
    this.name   = name  ;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world = (World) s.loadObject();
    locale = world.locales.atIndex(s.loadInt());
    
    government = GOVERNMENT.values()[s.loadInt()];
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
    s.loadObjects(visitors);
    
    active = s.loadBool();
    map    = (Area) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveInt(world.locales.indexOf(locale));
    
    s.saveInt(government.ordinal());
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
    s.saveObjects(visitors);
    
    s.saveBool(active);
    s.saveObject(map);
  }
  
  
  
  /**  Supplemental setup/query methods for economy, trade and geography-
    */
  public void attachMap(Area map) {
    this.map    = map ;
    this.active = map == null ? false : true;
  }
  
  
  public Area activeMap() {
    return map;
  }
  
  
  public boolean isOffmap() {
    return map == null;
  }
  
  
  public float distance(Base other, int moveMode) {
    if (other.locale == this.locale) return 0;
    Route route = locale.routes.get(other.locale);
    
    if (route == null) return -100;
    if (moveMode != Type.MOVE_AIR && moveMode != route.moveMode) return -100;
    
    return route.distance;
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
  
  
  public Mission matchingMission(int objective, Object focus) {
    for (Mission m : missions) {
      if (m.objective != objective) continue;
      if (m.localFocus() == focus) return m;
      if (m.worldFocus() == focus) return m;
    }
    return null;
  }
  
  
  
  /**  Setting up basic relations-
    */
  public void setGovernment(GOVERNMENT g) {
    this.government = g;
  }
  
  
  public GOVERNMENT government() {
    return government;
  }
  
  
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
    final int UPDATE_GAP = map == null ? DAY_LENGTH : 10;
    boolean updateStats = world.time % UPDATE_GAP == 0;
    boolean activeMap   = map != null;
    Base    lord        = relations.currentLord();
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      council.updateCouncil(true);
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
      council.updateCouncil(false);
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
      if (idealArmy < 0) {
        idealArmy = 0;
      }
      if (armyPower < idealArmy) {
        armyPower = Nums.min(idealArmy, armyPower + popRegen);
      }
      
      //  TODO:  Move this to the Council/Relations class?
      if (relations.isLoyalVassalOf(lord)) {
        if (council.considerRevolt(lord, UPDATE_GAP)) {
          relations.toggleRebellion(lord, true);
        }
      }
    }
    //
    //  Either way, we allow prestige and loyalty to return gradually to
    //  defaults over time:
    if (updateStats) {
      float presDrift = UPDATE_GAP * PRESTIGE_AVG * 1f / PRES_FADEOUT_TIME;
      float presDiff = PRESTIGE_AVG - relations.prestige;
      if (Nums.abs(presDiff) > presDrift) {
        presDiff = presDrift * (presDiff > 0 ? 1 : -1);
      }
      relations.prestige += presDiff;
      
      for (Relation r : relations.relations()) {
        float diff = LOY_CIVIL - r.loyalty;
        diff *= DAY_LENGTH * 1f / LOY_FADEOUT_TIME;
        r.loyalty += diff;
      }
    }
    //
    //  And, once per year, tally up any supply obligations to your current
    //  lord (with the possibility of entering a state of revolt if those are
    //  failed.)
    if (world.time % YEAR_LENGTH == 0) {
      if (relations.isLoyalVassalOf(lord)) {
        Relation r = relations.relationWith(lord);
        int timeAsVassal = world.time - r.madeVassalDate;
        boolean failedSupply = false, doCheck = timeAsVassal >= YEAR_LENGTH;
        
        if (doCheck) for (Good g : r.suppliesDue.keys()) {
          float sent = r.suppliesSent.valueFor(g);
          float due  = r.suppliesDue .valueFor(g);
          if (sent < due) failedSupply = true;
        }
        r.suppliesSent.clear();
        
        if (failedSupply) {
          relations.toggleRebellion(lord, true);
        }
        else {
          incLoyalty(lord, this, LOY_TRIBUTE_BONUS);
        }
      }
      //
      //  Wipe the slate clean for all other relations:
      for (Relation r : relations.relations()) if (r.with != lord) {
        r.suppliesSent.clear();
      }
      //
      //  And, finally, lose prestige based on any vassals in recent revolt-
      //  and if the time gone exceeds a certain threshold, end vassal status.
      for (Base revolts : relations.vassalsInRevolt()) {
        float years = revolts.relations.yearsSinceRevolt(this);
        float maxT = AVG_TRIBUTE_YEARS, timeMult = (maxT - years) / maxT;
        
        if (years < AVG_TRIBUTE_YEARS) {
          incPrestige(this, PRES_REBEL_LOSS * timeMult);
          incLoyalty(this, revolts, LOY_REBEL_PENALTY * 0.5f * timeMult);
        }
        else {
          setPosture(this, revolts, POSTURE.ENEMY, true);
        }
      }
    }
    //
    //  Update any formations and actors currently active-
    for (Mission f : missions) {
      f.update();
    }
    for (Actor a : council.members()) {
      if (a.mission() != null || a.onMap() || a.offmapBase() == this) continue;
      a.updateOffMap(this);
    }
    for (Actor a : visitors) {
      a.updateOffMap(this);
    }
    //
    //  And update traders-
    if (updateStats && ! activeMap) {
      trading.updateOffmapTraders();
    }
  }
  
  
  
  /**  Methods for handling traders and migrants-
    */
  public void toggleVisitor(Actor visitor, boolean is) {
    
    Base offmap = visitor.offmapBase();
    if (offmap != this && ! is) return;
    if (offmap == this &&   is) return;
    if (offmap != null &&   is) offmap.toggleVisitor(visitor, false);
    
    visitors.toggleMember(visitor, is);
    visitor.setOffmap(is ? this : null);
  }
  
  
  public Series <Actor> visitors() {
    return visitors;
  }
  
  

  /**  Last-but-not-least, returning available Powers:
    */
  public Series <ActorTechnique> rulerPowers() {
    Batch <ActorTechnique> all = new Batch();
    if (activeMap() == null) {
      return all;
    }
    for (Building b : map.buildings) if (b.base() == this) {
      for (ActorTechnique t : b.rulerPowers()) if (b.canUsePower(t)) {
        all.include(t);
      }
    }
    return all;
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
    return tint;
  }
  
  
  public void setTint(int tint) {
    this.tint = tint;
  }
  
  
  public static String descLoyalty(float l) {
    Pick <String> pick = new Pick();
    for (int i = LOYALTIES.length; i-- > 0;) {
      float dist = Nums.abs(l - LOYALTIES[i]);
      pick.compare(LOYALTY_DESC[i], 0 - dist);
    }
    return pick.result();
  }
  
  
  public String toString() {
    return name;
  }
}









