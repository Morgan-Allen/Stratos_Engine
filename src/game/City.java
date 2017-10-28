

package game;
import util.*;
import static game.GameConstants.*;



public class City implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum GOVERNMENT {
    IMPERIAL, FEUDAL, BARBARIAN
  }
  public static enum POSTURE {
    ENEMY  ,
    VASSAL ,
    ALLY   ,
    LORD   ,
    NEUTRAL,
  };
  final static float
    LOY_DEVOTED  =  1.0F,
    LOY_FRIENDLY =  0.5F,
    LOY_NEUTRAL  =  0.0F,
    LOY_STRAINED = -0.5F,
    LOY_NEMESIS  = -1.0F,
    LOY_ATTACK_PENALTY  = -0.25f,
    LOY_CONQUER_PENALTY = -0.50f,
    LOY_REBEL_PENALTY   = -0.25f,
    LOY_TRIBUTE_BONUS   =  0.05f,
    LOY_FADEOUT_TIME    = AVG_TRIBUTE_YEARS * 2
  ;
  
  static class Relation {
    City    with;
    POSTURE posture;
    float   loyalty;
    int lastRebelDate = -1;
    
    Tally <Good> suppliesDue  = new Tally();
    Tally <Good> suppliesSent = new Tally();
    int nextSupplyDate = -1;
  }
  
  String name = "City";
  int tint = CITY_COLOR;
  
  World world;
  float mapX, mapY;
  
  GOVERNMENT government = GOVERNMENT.FEUDAL;
  CityEvents events = new CityEvents(this);
  Table <City, Integer > distances = new Table();
  Table <City, Relation> relations = new Table();
  
  int   currentFunds = 0;
  float population   = 0;
  float armyPower    = 0;
  Tally <Good> tradeLevel  = new Tally();
  Tally <Good> inventory   = new Tally();
  Tally <Type> buildLevels = new Tally();
  
  List <Formation> formations = new List();
  
  boolean active;
  CityMap map;
  
  Tally <Type> makeTotals = new Tally();
  Tally <Type> usedTotals = new Tally();
  
  
  
  City(World world) {
    this.world = world;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world = (World) s.loadObject();
    mapX  = s.loadFloat();
    mapY  = s.loadFloat();
    
    government = GOVERNMENT.values()[s.loadInt()];
    events.loadState(s);
    for (int n = s.loadInt(); n-- > 0;) {
      distances.put((City) s.loadObject(), s.loadInt());
    }
    for (int n = s.loadInt(); n-- > 0;) {
      Relation r = new Relation();
      r.with    = (City) s.loadObject();
      r.posture = POSTURE.values()[s.loadInt()];
      r.loyalty = s.loadFloat();
      r.lastRebelDate = s.loadInt();
      s.loadTally(r.suppliesDue);
      s.loadTally(r.suppliesSent );
      r.nextSupplyDate = s.loadInt();
      relations.put(r.with, r);
    }
    
    currentFunds = s.loadInt();
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(tradeLevel );
    s.loadTally(inventory  );
    s.loadTally(buildLevels);
    
    s.loadObjects(formations);
    
    active = s.loadBool();
    map    = (CityMap) s.loadObject();
    
    s.loadTally(makeTotals);
    s.loadTally(usedTotals);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveFloat(mapX);
    s.saveFloat(mapY);
    
    s.saveInt(government.ordinal());
    events.saveState(s);
    s.saveInt(distances.size());
    for (City c : distances.keySet()) {
      s.saveObject(c);
      s.saveInt(distances.get(c));
    }
    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      s.saveObject(r.with);
      s.saveInt(r.posture.ordinal());
      s.saveFloat(r.loyalty);
      s.saveInt(r.lastRebelDate);
      s.saveTally(r.suppliesDue);
      s.saveTally(r.suppliesSent );
      s.saveInt(r.nextSupplyDate);
    }
    
    s.saveInt(currentFunds);
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(tradeLevel );
    s.saveTally(inventory  );
    s.saveTally(buildLevels);
    
    s.saveObjects(formations);
    
    s.saveBool(active);
    s.saveObject(map);
    
    s.saveTally(makeTotals);
    s.saveTally(usedTotals);
  }
  
  
  
  /**  Supplemental setup methods for trade and geography-
    */
  static void setupRoute(City a, City b, int distance) {
    a.distances.put(b, distance);
    b.distances.put(a, distance);
  }
  
  
  void setWorldCoords(float mapX, float mapY) {
    this.mapX = mapX;
    this.mapY = mapY;
  }
  
  
  void attachMap(CityMap map) {
    this.map    = map ;
    this.active = true;
  }
  
  
  void initBuildLevels(Object... buildLevelArgs) {
    this.buildLevels.setWith(buildLevelArgs);
    this.population = buildLevels.valueFor(HOUSE   ) * AVG_HOUSE_POP;
    this.armyPower  = buildLevels.valueFor(GARRISON) * AVG_ARMY_POWER;
  }
  
  
  
  /**  Setting up basic relations-
    */
  Relation relationWith(City other) {
    if (other == null) return null;
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with    = other;
      r.posture = POSTURE.NEUTRAL;
      r.loyalty = LOY_NEUTRAL;
    }
    return r;
  }
  
  
  POSTURE posture(City other) {
    Relation r = relations.get(other);
    return r == null ? POSTURE.NEUTRAL : r.posture;
  }
  
  
  static void setPosture(City a, City b, POSTURE p) {
    if (p == null) p = POSTURE.NEUTRAL;
    POSTURE reverse  = POSTURE.NEUTRAL;
    City formerLord = a.currentLord();
    
    if (p == POSTURE.VASSAL) reverse = POSTURE.LORD  ;
    if (p == POSTURE.LORD  ) reverse = POSTURE.VASSAL;
    if (p == POSTURE.ALLY  ) reverse = POSTURE.ALLY  ;
    if (p == POSTURE.ENEMY ) reverse = POSTURE.ENEMY ;
    
    //  TODO:  You might consider flagging this as a form of rebellion?
    //  You cannot have more than one Lord at a time:
    if (p == POSTURE.LORD && formerLord != null) {
      setPosture(a, formerLord, POSTURE.NEUTRAL);
    }
    
    a.relationWith(b).posture = p;
    b.relationWith(a).posture = reverse;
  }
  
  
  static void incLoyalty(City a, City b, float inc) {
    Relation r = a.relationWith(b);
    float loyalty = Nums.clamp(r.loyalty + inc, -1, 1);
    r.loyalty = loyalty;
  }
  
  
  boolean isVassal(City o) { return posture(o) == POSTURE.VASSAL; }
  boolean isLord  (City o) { return posture(o) == POSTURE.LORD  ; }
  boolean isEnemy (City o) { return posture(o) == POSTURE.ENEMY ; }
  boolean isAlly  (City o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  City currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == POSTURE.LORD) return r.with;
    }
    return null;
  }
  
  
  boolean isVassalOfSameLord(City o) {
    City lord = currentLord();
    return lord != null && o.currentLord() == lord;
  }
  
  
  boolean isLoyalVassalOf(City o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != POSTURE.LORD) return false;
    return r.lastRebelDate == -1;
  }
  
  
  void enterRevoltAgainst(City lord) {
    Relation r = relationWith(lord);
    r.lastRebelDate  = world.time;
    r.nextSupplyDate = -1;
    r.suppliesDue .clear();
    r.suppliesSent.clear();
    incLoyalty(lord, this, LOY_REBEL_PENALTY);
  }
  
  
  float yearsSinceRevolt(City lord) {
    Relation r = relationWith(lord);
    if (r == null || r.lastRebelDate == -1) return -1;
    return (world.time - r.lastRebelDate) * 1f / YEAR_LENGTH;
  }
  
  
  
  /**  Setting and accessing tribute and trade levels-
    */
  static void setSuppliesDue(
    City a, City b, Tally <Good> suppliesDue, int timeDue
  ) {
    if (suppliesDue == null) suppliesDue = new Tally();
    Relation r = a.relationWith(b);
    r.suppliesDue      = suppliesDue;
    r.nextSupplyDate = timeDue   ;
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  
  
  public City tradeOrigin() {
    return this;
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    boolean  updateStats = world.time % MONTH_LENGTH == 0;
    boolean  activeMap   = map != null;
    City     lord        = currentLord();
    Relation fealty      = relationWith(lord);
    boolean  supplyDue   = isLoyalVassalOf(lord);
    supplyDue &= fealty.nextSupplyDate == world.time;
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      
      int citizens = 0;
      for (Actor a : map.actors) if (a.homeCity == this) {
        citizens += 1;
      }
      population = citizens * POP_PER_CITIZEN;
      
      float armyPower = 0;
      for (Formation f : formations) armyPower += f.formationPower();
      this.armyPower = armyPower;
      
      inventory  .clear();
      buildLevels.clear();
      for (Building b : map.buildings) {
        if (b.type.category == Type.IS_TRADE_BLD) {
          BuildingForTrade post = (BuildingForTrade) b;
          for (Good g : post.inventory.keys()) {
            inventory.add(post.inventory.valueFor(g), g);
          }
        }
        buildLevels.add(1, b.type);
      }
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && ! activeMap) {
      events.updateEvents();
      
      float popRegen  = LIFESPAN_LENGTH / MONTH_LENGTH;
      float usageInc  = YEAR_LENGTH / MONTH_LENGTH;
      float idealPop  = buildLevels.valueFor(HOUSE   ) * AVG_HOUSE_POP ;
      float idealArmy = buildLevels.valueFor(GARRISON) * AVG_ARMY_POWER;
      
      if (population < idealPop) {
        population = Nums.min(idealPop , population + popRegen);
      }
      
      idealArmy -= formations.size() * AVG_ARMY_POWER;
      if (idealArmy < 0) idealArmy = 0;
      
      if (armyPower < idealArmy) {
        armyPower  = Nums.min(idealArmy, armyPower + popRegen);
      }
      
      for (Good g : tradeLevel.keys()) {
        float demand = tradeLevel.valueFor(g);
        float amount = inventory .valueFor(g);
        if (demand <= 0) continue;
        
        amount -= usageInc * tradeLevel.valueFor(g);
        inventory.set(g, Nums.max(0, amount));
      }
      
      if (lord != null) for (Good g : fealty.suppliesDue.keys()) {
        float sent = fealty.suppliesDue.valueFor(g) * usageInc * 1.1f;
        fealty.suppliesSent.add(sent, g);
      }
    }
    //
    //  But formations and relationships get updated similarly either way-
    for (Formation f : formations) {
      f.update();
    }
    //
    //  Once per year, check to see if tribute-requirements have been met with
    //  you lord (if any.)
    
    if (supplyDue && activeMap) {
      boolean failedSupply = false;
      
      for (Good g : fealty.suppliesDue.keys()) {
        float sent = fealty.suppliesSent.valueFor(g);
        float due  = fealty.suppliesDue .valueFor(g);
        if (sent < due) failedSupply = true;
      }
      
      if (failedSupply) {
        enterRevoltAgainst(lord);
      }
      else {
        fealty.suppliesSent.clear();
        fealty.nextSupplyDate += YEAR_LENGTH;
        incLoyalty(lord, this, LOY_TRIBUTE_BONUS);
      }
    }
    //
    //  In the case of AI-controlled cities, you need to check the appeal of
    //  rebellion-
    if (supplyDue && ! activeMap) {
      if (events.considerRevolt(lord)) {
        enterRevoltAgainst(lord);
      }
      else {
        fealty.suppliesSent.clear();
        fealty.nextSupplyDate += YEAR_LENGTH;
        incLoyalty(lord, this, LOY_TRIBUTE_BONUS);
      }
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}



