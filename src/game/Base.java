

package game;
import static game.GameConstants.*;
import static game.World.*;
import static game.BaseRelations.*;
import util.*;



public class Base implements
  Session.Saveable, Trader, BaseEvents.Trouble, BaseRelations.Postured
{
  
  
  /**  Data fields, construction and save/load methods-
    */
  String name = "City";
  
  final public World world;
  final public WorldLocale locale;
  
  private Faction faction;
  private BaseCouncil factionCouncil = null;
  
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
  
  
  
  public Base(World world, WorldLocale locale, Faction faction) {
    this(world, locale, faction, "Base???");
  }
  
  
  public Base(World world, WorldLocale locale, Faction faction, String name) {
    if (world  == null) I.complain("CANNOT PASS NULL WORLD:  "+name);
    if (locale == null) I.complain("CANNOT PASS NULL LOCALE: "+name);
    
    this.world   = world  ;
    this.locale  = locale ;
    this.faction = faction;
    
    this.name = name;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world   = (World) s.loadObject();
    locale  = (WorldLocale) s.loadObject();
    faction = (Faction) s.loadObject();
    
    s.loadObjects(buildTypes);
    
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
    s.saveObject(locale);
    s.saveObject(faction);
    
    s.saveObjects(buildTypes);
    
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
  
  
  public Faction faction() {
    return faction;
  }
  
  
  public BaseRelations relations(World world) {
    return relations;
  }
  
  
  void assignFaction(Faction faction) {
    this.faction = faction;
    factionCouncil = null;
  }
  
  
  public BaseCouncil council() {
    if (factionCouncil != null) return factionCouncil;
    factionCouncil = world.factionCouncil(faction);
    return factionCouncil;
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
      if (idealArmy < 0) {
        idealArmy = 0;
      }
      if (armyPower < idealArmy) {
        armyPower = Nums.min(idealArmy, armyPower + popRegen);
      }
      
      if (relations.isLoyalVassalOf(faction)) {
        if (council().considerRevolt(faction, UPDATE_GAP, this)) {
          relations.toggleRebellion(faction, true);
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
      if (relations.isLoyalVassalOf(faction)) {
        Relation r = relations.relationWith(faction);
        int timeAsVassal = world.time - r.madeVassalDate;
        boolean failedSupply = false, doCheck = timeAsVassal >= YEAR_LENGTH;
        
        if (doCheck) for (Good g : r.suppliesDue.keys()) {
          float sent = r.suppliesSent.valueFor(g);
          float due  = r.suppliesDue .valueFor(g);
          if (sent < due) failedSupply = true;
        }
        r.suppliesSent.clear();
        
        if (failedSupply) {
          relations.toggleRebellion(faction, true);
        }
        else {
          faction.relations(world).incLoyalty(this, LOY_TRIBUTE_BONUS);
        }
      }
      //
      //  Wipe the slate clean for all other relations:
      for (Relation r : relations.relations()) if (r.with != faction) {
        r.suppliesSent.clear();
      }
    }
    //
    //  Update any formations and actors currently active-
    for (Mission f : missions) {
      f.update();
    }
    for (Actor a : visitors) if (! a.onMap()) {
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
  BaseRelations.POSTURE posture(Base other) {
    if (other.faction() == this.faction()) {
      Base capital = council().capital();
      if (this  == capital) return POSTURE.VASSAL;
      if (other == capital) return POSTURE.LORD;
      return POSTURE.ALLY;
    }
    return council().relations.posture(other.faction());
  }
  
  public boolean isVassalOf(Base o) { return posture(o) == POSTURE.LORD  ; }
  public boolean isLordOf  (Base o) { return posture(o) == POSTURE.VASSAL; }
  public boolean isEnemyOf (Base o) { return posture(o) == POSTURE.ENEMY ; }
  public boolean isAllyOf  (Base o) { return posture(o) == POSTURE.ALLY  ; }
  
  public boolean isLoyalVassalOf(Base o) {
    return posture(o) == POSTURE.LORD && relations.isLoyalVassalOf(o.faction());
  }
  
  
  public float troublePower() {
    return armyPower;
  }
  
  
  public void generateTrouble(Area activeMap, float factionPower) {
    //  TODO:  Fill this in!
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









