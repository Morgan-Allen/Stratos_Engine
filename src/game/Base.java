

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
  
  Building headquarters = null;
  
  private int   currentFunds = 0;
  private float population   = 0;
  private float armyPower    = 0;
  Tally <Good> needLevel = new Tally();
  Tally <Good> prodLevel = new Tally();
  Tally <Good> inventory = new Tally();
  
  Tally <BuildType> buildLevel = new Tally();
  List <Mission> missions = new List();
  
  //
  //  These are specific to off-map bases...
  List <ActorAsVessel> traders = new List();
  List <Actor> visitors = new List();
  List <Actor> migrants = new List();
  
  List <BuildType> buildTypes = new List();
  
  private boolean active;
  private Area map;
  
  Tally <Type> makeTotals = new Tally();
  Tally <Type> usedTotals = new Tally();
  
  
  
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
    council.loadState(s);
    relations.loadState(s);
    
    headquarters = (Building) s.loadObject();
    
    currentFunds = s.loadInt();
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(needLevel );
    s.loadTally(prodLevel );
    s.loadTally(inventory );
    s.loadTally(buildLevel);
    
    s.loadObjects(missions);
    s.loadObjects(traders );
    s.loadObjects(visitors);
    s.loadObjects(migrants);
    
    s.loadObjects(buildTypes);
    
    active = s.loadBool();
    map    = (Area) s.loadObject();
    
    s.loadTally(makeTotals);
    s.loadTally(usedTotals);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveInt(world.locales.indexOf(locale));
    
    s.saveInt(government.ordinal());
    council.saveState(s);
    relations.saveState(s);
    
    s.saveObject(headquarters);
    
    s.saveInt(currentFunds);
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(needLevel );
    s.saveTally(prodLevel );
    s.saveTally(inventory );
    s.saveTally(buildLevel);
    
    s.saveObjects(missions);
    s.saveObjects(traders );
    s.saveObjects(visitors);
    s.saveObjects(migrants);
    
    s.saveObjects(buildTypes);
    
    s.saveBool(active);
    s.saveObject(map);
    
    s.saveTally(makeTotals);
    s.saveTally(usedTotals);
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
  
  
  public void setTradeLevel(Good g, float need, float accept) {
    needLevel.set(g, need  );
    prodLevel.set(g, accept);
  }
  
  
  public float needLevel(Good g) {
    return needLevel.valueFor(g);
  }
  
  
  public float prodLevel(Good g) {
    return prodLevel.valueFor(g);
  }
  
  
  public void initInventory(Object... args) {
    inventory.setWith(args);
  }
  
  
  public float inventory(Good g) {
    return inventory.valueFor(g);
  }
  
  
  public float setInventory(Good g, float amount) {
    return inventory.set(g, amount);
  }
  
  
  public static void setSuppliesDue(Base a, Base b, Tally <Good> suppliesDue) {
    if (suppliesDue == null) suppliesDue = new Tally();
    Relation r = a.relations.relationWith(b);
    r.suppliesDue = suppliesDue;
  }
  
  
  public static Tally <Good> suppliesDue(Base a, Base b) {
    Relation r = a.relations.relationWith(b);
    if (r == null) return new Tally();
    return r.suppliesDue;
  }
  
  
  public static float goodsSent(Base a, Base b, Good g) {
    Relation r = a.relations.relationWith(b);
    return r == null ? 0 : r.suppliesSent.valueFor(g);
  }
  
  
  public static float suppliesDue(Base a, Base b, Good g) {
    Relation r = a.relations.relationWith(b);
    return r == null ? 0 : r.suppliesDue.valueFor(g);
  }
  
  
  public float totalMade(Good g) {
    return makeTotals.valueFor(g);
  }
  
  
  public float totalUsed(Good g) {
    return usedTotals.valueFor(g);
  }
  

  public Tally <Good> needLevels() { return needLevel; }
  public Tally <Good> prodLevels() { return prodLevel; }
  public Tally <Good> inventory () { return inventory; }
  public Base base() { return this; }
  
  
  public boolean allowExport(Good g, Trader buys) {
    if (buys.base().relations.homeland() == this) return true;
    if (Base.suppliesDue(this, buys.base(), g) > 0) return true;
    return prodLevel.valueFor(g) > 0;
  }
  
  
  public float shopPrice(Good good, Task purchase) {
    return good.price;
  }
  
  
  
  //  Selling to where goods are abundant gets you a lower price.
  //  Buying from where goods are scarce imposes a higher price.
  //  Selling to where goods are scarce gets you a higher price.
  //  Buying from where goods are abundant imposes a lower price.
  
  float scarcityMultiple(Base other, Good g) {
    if (other != this.relations.homeland()) return 1.0f;
    float mult = 1.0f;
    float needS = other.needLevel(g);
    float prodS = other.prodLevel(g);
    if (needS > 0) mult += SCARCE_MARGIN / 100f;
    if (prodS > 0) mult += PLENTY_MARGIN / 100f;
    return mult;
  }
  
  
  public float importPrice(Good g, Base sells) {
    float mult = scarcityMultiple(sells, g);
    mult += TRAVEL_MARGIN / 100f;
    return (int) (g.price * mult);
  }
  
  
  public float exportPrice(Good g, Base buys) {
    float mult = scarcityMultiple(buys, g);
    return (int) (g.price * mult);
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
      
      //I.say("POWER OF "+this+" IS "+armyPower);
      
      inventory .clear();
      buildLevel.clear();
      needLevel .clear();
      prodLevel .clear();
      
      //  TODO:  You might want to update this more frequently.
      //  TODO:  And put in a basic test-routine for this...
      
      //  Once every 10 seconds or so, at least.
      
      for (Building b : map.buildings()) if (b.base() == this) {
        for (Good g : world.goodTypes()) {
          inventory.add(b.inventory(g), g);
        }
        if (b.type().category != Type.IS_TRADE_BLD) {
          for (Good g : b.needed()) {
            needLevel.add(b.stockLimit(g), g);
          }
          for (Good g : b.produced()) {
            prodLevel.add(b.stockLimit(g), g);
          }
        }
        buildLevel.add(1, b.type());
      }
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && ! activeMap) {
      council.updateCouncil(false);
      
      float popRegen  = UPDATE_GAP * 1f / (LIFESPAN_LENGTH / 2);
      float usageInc  = UPDATE_GAP * 1f / YEAR_LENGTH;
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
      
      for (Good g : world.goodTypes) {
        float demand = needLevel.valueFor(g);
        float supply = prodLevel.valueFor(g);
        float amount = inventory.valueFor(g);
        amount -= usageInc * demand;
        amount += usageInc * supply;
        if (amount > demand + supply) amount -= usageInc * 5;
        inventory.set(g, Nums.max(0, amount));
      }
      
      //  TODO:  Move this to the Council class?
      
      if (relations.isLoyalVassalOf(lord)) {
        if (council.considerRevolt(lord, UPDATE_GAP)) {
          relations.toggleRebellion(lord, true);
        }
        //  TODO:  You may have to generate caravans to visit player cities...
        else if (lord.map == null) {
          Relation r = relations.relationWith(lord);
          for (Good g : r.suppliesDue.keys()) {
            float sent = r.suppliesDue.valueFor(g) * usageInc * 1.1f;
            r.suppliesSent.add(sent, g);
          }
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
      updateOffmapTraders();
    }
  }
  
  
  
  /**  Methods for handling traders and migrants-
    */
  public void updateOffmapTraders() {
    if (Visit.empty(world.shipTypes)) return;
    if (! world.settings.toggleShipping) return;
    
    for (Base b : world.bases) {
      
      POSTURE p = this.relations.posture(b);
      boolean shouldTrade =
        p != POSTURE.NEUTRAL &&
        p != POSTURE.ENEMY   &&
        b.activeMap() != null
      ;
      
      ActorAsVessel trader = traderFor(b);
      boolean isHome = trader != null && visitors.includes(trader);
      
      if (trader != null && isHome && ! shouldTrade) {
        toggleVisitor(trader, false);
        traders.remove(trader);
        for (Actor a : trader.crew) toggleVisitor(a, false);
        continue;
      }
      
      if (shouldTrade && trader == null) {
        ActorType forShip = (ActorType) world.shipTypes[0];
        trader = (ActorAsVessel) forShip.generate();
        trader.assignBase(this);
        trader.bonds.assignGuestBase(b);
        trader.setBuildLevel(1);
        traders.add(trader);
        toggleVisitor(trader, true);
      }
      
      if (trader != null && shouldTrade && isHome && trader.readyForTakeoff()) {
        TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
          this, trader, b, true, b.activeMap()
        );
        if (trading != null) {
          I.say(trader+" begins task: "+trading);
          trader.assignTask(trading, this);
          trading.beginFromOffmap(this);
        }
        else {
          I.say(trader+" could not find trading behaviour!");
        }
      }
    }
  }


  public Series <ActorAsVessel> traders() {
    return traders;
  }
  
  
  public ActorAsVessel traderFor(Base other) {
    for (ActorAsVessel t : traders) if (t.guestBase() == other) return t;
    return null;
  }
  
  
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
  
  
  public int hireCost(ActorType workerType) {
    return workerType.hireCost;
  }
  
  
  public void addMigrant(Actor migrant) {
    this.migrants.add(migrant);
    toggleVisitor(migrant, true);
  }
  
  
  public void removeMigrant(Actor migrant) {
    this.migrants.remove(migrant);
    toggleVisitor(migrant, false);
  }
  
  
  public Series <Actor> migrants() {
    return migrants;
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









