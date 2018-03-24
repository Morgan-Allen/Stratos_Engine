

package game;
import util.*;
import static game.GameConstants.*;



public class Base implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum GOVERNMENT {  //  TODO:  Move into the Council class.
    IMPERIAL, FEUDAL, BARBARIAN, REPUBLIC
  }
  public static enum POSTURE {
    ENEMY  ,
    ALLY   ,
    VASSAL ,
    LORD   ,
    NEUTRAL,
    TRADING,
  };
  final public static float
    LOY_DEVOTED  =  1.0F,
    LOY_FRIENDLY =  0.5F,
    LOY_CIVIL    =  0.0F,
    LOY_STRAINED = -0.5F,
    LOY_NEMESIS  = -1.0F,
    LOYALTIES[]  = { 1f, 0.5f, 0, -0.5f, -1 }
  ;
  final static String LOYALTY_DESC[] = {
    "Devoted", "Friendly", "Civil", "Strained", "Nemesis"
  };
  final public static float
    LOY_ATTACK_PENALTY  = -0.25f,
    LOY_CONQUER_PENALTY = -0.50f,
    LOY_REBEL_PENALTY   = -0.25f,
    LOY_TRIBUTE_BONUS   =  0.05f,
    LOY_FADEOUT_TIME    =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2,
    
    PRESTIGE_MAX        =  100,
    PRESTIGE_AVG        =  50,
    PRESTIGE_MIN        =  0,
    
    PRES_VICTORY_GAIN   =  25,
    PRES_DEFEAT_LOSS    = -15,
    PRES_REBEL_LOSS     = -10,
    PRES_FADEOUT_TIME   =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2
  ;
  
  public static class Relation {
    Base    with;
    POSTURE posture;
    float   loyalty;
    
    int madeVassalDate = -1;
    int lastRebelDate  = -1;
    
    Tally <Good> suppliesDue  = new Tally();
    Tally <Good> suppliesSent = new Tally();
  }
  
  String name = "City";
  int tint = CITY_COLOR;
  
  final public World world;
  final public World.Locale locale;
  
  GOVERNMENT government = GOVERNMENT.FEUDAL;
  float prestige = PRESTIGE_AVG;
  Base homeland = null;
  final public BaseCouncil council = new BaseCouncil(this);
  final Table <Base, Relation> relations = new Table();
  
  private int   currentFunds = 0;
  private float population   = 0;
  private float armyPower    = 0;
  Tally <Good> needLevel = new Tally();
  Tally <Good> prodLevel = new Tally();
  Tally <Good> inventory = new Tally();
  Tally <BuildType> buildLevel = new Tally();
  
  List <BuildType> buildTypes = new List();
  
  List <Mission> missions = new List();
  
  private boolean active;
  private Area map;
  
  Tally <Type> makeTotals = new Tally();
  Tally <Type> usedTotals = new Tally();
  
  
  
  public Base(World world, World.Locale locale) {
    this(world, locale, "Base???");
  }
  
  
  public Base(World world, World.Locale locale, String name) {
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
    prestige = s.loadFloat();
    homeland = (Base) s.loadObject();
    council.loadState(s);
    
    for (int n = s.loadInt(); n-- > 0;) {
      Relation r = new Relation();
      r.with    = (Base) s.loadObject();
      r.posture = POSTURE.values()[s.loadInt()];
      r.loyalty = s.loadFloat();
      r.madeVassalDate = s.loadInt();
      r.lastRebelDate  = s.loadInt();
      s.loadTally(r.suppliesDue );
      s.loadTally(r.suppliesSent);
      relations.put(r.with, r);
    }
    
    currentFunds = s.loadInt();
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(needLevel );
    s.loadTally(prodLevel );
    s.loadTally(inventory );
    s.loadTally(buildLevel);
    
    s.loadObjects(buildTypes);
    
    s.loadObjects(missions);
    
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
    s.saveFloat(prestige);
    s.saveObject(homeland);
    council.saveState(s);
    
    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      s.saveObject(r.with);
      s.saveInt(r.posture.ordinal());
      s.saveFloat(r.loyalty);
      s.saveInt(r.madeVassalDate);
      s.saveInt(r.lastRebelDate );
      s.saveTally(r.suppliesDue );
      s.saveTally(r.suppliesSent);
    }
    
    s.saveInt(currentFunds);
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(needLevel );
    s.saveTally(prodLevel );
    s.saveTally(inventory );
    s.saveTally(buildLevel);
    
    s.saveObjects(buildTypes);
    
    s.saveObjects(missions);
    
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
  
  
  public float distance(Base other) {
    if (other.locale == this.locale) return 0;
    Integer dist = locale.distances.get(other.locale);
    if (dist != null) return (float) dist;
    
    float dx = locale.mapX - other.locale.mapX;
    float dy = locale.mapY - other.locale.mapY;
    return (int) Nums.sqrt((dx * dx) + (dy * dy));
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
  
  
  public void setHomeland(Base homeland) {
    this.homeland = homeland;
  }
  
  
  public Base homeland() {
    return homeland;
  }
  
  
  Relation relationWith(Base other) {
    if (other == null) return null;
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with    = other;
      r.posture = POSTURE.NEUTRAL;
      r.loyalty = LOY_CIVIL;
    }
    return r;
  }
  
  
  public POSTURE posture(Base other) {
    if (other == null) return null;
    return relationWith(other).posture;
  }
  
  
  public float loyalty(Base other) {
    if (other == null) return 0;
    return relationWith(other).loyalty;
  }
  
  
  public static void setPosture(Base a, Base b, POSTURE p, boolean symmetric) {
    if (p == null) p = POSTURE.NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (p == POSTURE.LORD) {
      Base formerLord = a.currentLord();
      if (formerLord == b) return;
      if (formerLord != null) setPosture(a, formerLord, POSTURE.NEUTRAL, true);
      a.relationWith(b).madeVassalDate = a.world.time;
    }
    
    a.relationWith(b).posture = p;
    
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric) {
      POSTURE reverse = POSTURE.NEUTRAL;
      if (p == POSTURE.VASSAL) reverse = POSTURE.LORD  ;
      if (p == POSTURE.LORD  ) reverse = POSTURE.VASSAL;
      if (p == POSTURE.ALLY  ) reverse = POSTURE.ALLY  ;
      if (p == POSTURE.ENEMY ) reverse = POSTURE.ENEMY ;
      setPosture(b, a, reverse, false);
    }
  }
  
  
  public void toggleRebellion(Base lord, boolean is) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD) return;
    
    if (is) {
      r.lastRebelDate = world.time;
      r.suppliesSent.clear();
      incLoyalty(lord, this, LOY_REBEL_PENALTY / 2);
    }
    else {
      r.lastRebelDate = -1;
    }
  }
  
  
  public boolean isVassalOf(Base o) { return posture(o) == POSTURE.LORD  ; }
  public boolean isLordOf  (Base o) { return posture(o) == POSTURE.VASSAL; }
  public boolean isEnemyOf (Base o) { return posture(o) == POSTURE.ENEMY ; }
  public boolean isAllyOf  (Base o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  public static void incLoyalty(Base a, Base b, float inc) {
    Relation r = a.relationWith(b);
    float loyalty = Nums.clamp(r.loyalty + inc, -1, 1);
    r.loyalty = loyalty;
  }
  
  
  public void initPrestige(float level) {
    this.prestige = level;
  }
  
  
  public float prestige() {
    return prestige;
  }
  
  
  public static void incPrestige(Base c, float inc) {
    c.prestige = Nums.clamp(c.prestige + inc, PRESTIGE_MIN, PRESTIGE_MAX);
  }
  
  
  public Base currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == POSTURE.LORD) return r.with;
    }
    return null;
  }
  
  
  public Base capitalLord() {
    Base c = this;
    while (true) {
      Base l = c.currentLord();
      if (l == null) return c;
      else c = l;
    }
  }
  
  
  public boolean isVassalOfSameLord(Base o) {
    Base lord = capitalLord();
    return lord != null && o.capitalLord() == lord;
  }
  
  
  public boolean isLoyalVassalOf(Base o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != POSTURE.LORD) return false;
    return r.lastRebelDate == -1;
  }
  
  
  public Series <Base> relationsWith() {
    Batch <Base> all = new Batch();
    for (Base c : relations.keySet()) all.add(c);
    return all;
  }
  
  
  public Series <Base> vassalsInRevolt() {
    Batch <Base> all = new Batch();
    for (Relation r : relations.values()) {
      if (r.with.yearsSinceRevolt(this) > 0) all.add(r.with);
    }
    return all;
  }
  
  
  public float yearsSinceRevolt(Base lord) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD         ) return -1;
    if (r == null || r.lastRebelDate == -1) return -1;
    return (world.time - r.lastRebelDate) * 1f / YEAR_LENGTH;
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
    needLevel  .set(g, need  );
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
    Relation r = a.relationWith(b);
    r.suppliesDue = suppliesDue;
  }
  
  
  public static Tally <Good> suppliesDue(Base a, Base b) {
    Relation r = a.relationWith(b);
    if (r == null) return new Tally();
    return r.suppliesDue;
  }
  
  
  public static float goodsSent(Base a, Base b, Good g) {
    Relation r = a.relationWith(b);
    return r == null ? 0 : r.suppliesSent.valueFor(g);
  }
  
  
  public static float suppliesDue(Base a, Base b, Good g) {
    Relation r = a.relationWith(b);
    return r == null ? 0 : r.suppliesDue.valueFor(g);
  }
  
  
  public float totalMade(Good g) {
    return makeTotals.valueFor(g);
  }
  

  public Tally <Good> needLevels() { return needLevel; }
  public Tally <Good> prodLevels() { return prodLevel; }
  public Tally <Good> inventory () { return inventory; }
  public Base base() { return this; }
  
  
  public boolean allowExport(Good g, Trader buys) {
    if (buys.base().homeland() == this) return true;
    if (Base.suppliesDue(this, buys.base(), g) > 0) return true;
    return prodLevel.valueFor(g) > 0;
  }
  
  
  
  //  Selling to where goods are abundant gets you a lower price.
  //  Buying from where goods are scarce imposes a higher price.
  //  Selling to where goods are scarce gets you a higher price.
  //  Buying from where goods are abundant imposes a lower price.
  
  float scarcityMultiple(Base other, Good g) {
    if (other != this.homeland) return 1.0f;
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
      if (t.isArmyOrWallsBuilding()) {
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
  void updateCity() {
    boolean updateStats = world.time % DAY_LENGTH == 0;
    boolean activeMap   = map != null;
    Base    lord        = currentLord();
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      council.updateCouncil(true);
      
      int citizens = 0;
      for (Actor a : map.actors) if (a.base() == this) {
        citizens += 1;
      }
      population = citizens * POP_PER_CITIZEN;
      
      float armyPower = 0;
      for (Building b : map.buildings) {
        if (b.type().category == Type.IS_ARMY_BLD) {
          armyPower += MissionStrike.powerSum(b.workers(), map);
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
      
      for (Building b : map.buildings) {
        for (Good g : world.goodTypes) {
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
      
      float popRegen  = DAY_LENGTH * 1f / (LIFESPAN_LENGTH / 2);
      float usageInc  = DAY_LENGTH * 1f / YEAR_LENGTH;
      float idealPop  = idealPopulation();
      float idealArmy = idealArmyPower();
      
      if (population < idealPop) {
        population = Nums.min(idealPop, population + popRegen);
      }
      for (Mission f : missions) {
        idealArmy -= MissionStrike.powerSum(f.recruits(), null);
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
      
      if (isLoyalVassalOf(lord)) {
        if (council.considerRevolt(lord, DAY_LENGTH)) {
          toggleRebellion(lord, true);
        }
        //  TODO:  You may have to generate caravans to visit player cities...
        else if (lord.map == null) {
          Relation r = relationWith(lord);
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
      float presDrift = DAY_LENGTH * PRESTIGE_AVG * 1f / PRES_FADEOUT_TIME;
      float presDiff = PRESTIGE_AVG - prestige;
      if (Nums.abs(presDiff) > presDrift) {
        presDiff = presDrift * (presDiff > 0 ? 1 : -1);
      }
      prestige += presDiff;
      
      for (Relation r : relations.values()) {
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
      if (isLoyalVassalOf(lord)) {
        Relation r = relationWith(lord);
        int timeAsVassal = world.time - r.madeVassalDate;
        boolean failedSupply = false, doCheck = timeAsVassal >= YEAR_LENGTH;
        
        if (doCheck) for (Good g : r.suppliesDue.keys()) {
          float sent = r.suppliesSent.valueFor(g);
          float due  = r.suppliesDue .valueFor(g);
          if (sent < due) failedSupply = true;
        }
        r.suppliesSent.clear();
        
        if (failedSupply) {
          toggleRebellion(lord, true);
        }
        else {
          incLoyalty(lord, this, LOY_TRIBUTE_BONUS);
        }
      }
      //
      //  Wipe the slate clean for all other relations:
      for (Relation r : relations.values()) if (r.with != lord) {
        r.suppliesSent.clear();
      }
      //
      //  And, finally, lose prestige based on any vassals in recent revolt-
      //  and if the time gone exceeds a certain threshold, end vassal status.
      for (Base revolts : vassalsInRevolt()) {
        float years = revolts.yearsSinceRevolt(this);
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
    //  And update any formations and actors currently active-
    for (Mission f : missions) {
      f.update();
    }
    for (Actor a : council.members()) {
      if (a.mission() != null || a.onMap()) continue;
      a.updateOffMap(this);
    }
  }
  
  

  /**  Last-but-not-least, returning available Powers:
    */
  public Series <ActorTechnique> rulerPowers() {
    Batch <ActorTechnique> all = new Batch();
    if (activeMap() == null) {
      return all;
    }
    for (Building b : map.buildings) if (b.base() == this) {
      for (ActorTechnique t : b.rulerPowers()) all.include(t);
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









