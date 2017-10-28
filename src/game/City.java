

package game;
import util.*;
import static game.GameConstants.*;



public class City implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
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
    LOY_STRAINED = -0.5f,
    LOY_NEMESIS  = -1.0F
  ;
  
  static class Relation {
    City    with;
    POSTURE posture;
    float   loyalty;
    int lastRebelDate = -1;
    
    Tally <Good> tributeDue = new Tally();
    Tally <Good> goodsSent  = new Tally();
    int nextTributeDate = -1;
  }
  
  String name = "City";
  int tint = CITY_COLOR;
  
  World world;
  float mapX, mapY;
  
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
      s.loadTally(r.tributeDue);
      s.loadTally(r.goodsSent );
      r.nextTributeDate = s.loadInt();
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
      s.saveTally(r.tributeDue);
      s.saveTally(r.goodsSent );
      s.saveInt(r.nextTributeDate);
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
    
    if (p == POSTURE.VASSAL) reverse = POSTURE.LORD  ;
    if (p == POSTURE.LORD  ) reverse = POSTURE.VASSAL;
    if (p == POSTURE.ALLY  ) reverse = POSTURE.ALLY  ;
    if (p == POSTURE.ENEMY ) reverse = POSTURE.ENEMY ;
    
    a.relationWith(b).posture = p;
    b.relationWith(a).posture = reverse;
  }
  
  
  boolean isVassal(City o) { return posture(o) == POSTURE.VASSAL; }
  boolean isLord  (City o) { return posture(o) == POSTURE.LORD  ; }
  boolean isEnemy (City o) { return posture(o) == POSTURE.ENEMY ; }
  boolean isAlly  (City o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  
  /**  Setting and accessing tribute and trade levels-
    */
  static void setTribute(City a, City b, Tally <Good> tributeDue, int timeDue) {
    if (tributeDue == null) tributeDue = new Tally();
    Relation r = a.relationWith(b);
    r.tributeDue      = tributeDue;
    r.nextTributeDate = timeDue   ;
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  
  
  public City tradeOrigin() {
    return this;
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    boolean updateStats = world.time % MONTH_LENGTH == 0;
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && map != null) {
      
      int citizens = 0;
      for (Actor a : map.actors) if (a.homeCity == this) {
        citizens += 1;
      }
      this.population = citizens * POP_PER_CITIZEN;
      
      float armyPower = 0;
      for (Formation f : formations) armyPower += f.formationPower();
      this.armyPower = armyPower;
      
      this.inventory  .clear();
      this.buildLevels.clear();
      for (Building b : map.buildings) {
        if (b.type.category == Type.IS_TRADE_BLD) {
          BuildingForTrade post = (BuildingForTrade) b;
          for (Good g : post.inventory.keys()) {
            this.inventory.add(post.inventory.valueFor(g), g);
          }
        }
        this.buildLevels.add(1, b.type);
      }
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && map == null) {
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
    }
    //
    //  But formations and relationships get updated similarly either way-
    for (Formation f : formations) {
      f.update();
    }
    for (Relation r : relations.values()) {
      
      boolean paysTribute = r.nextTributeDate != -1;
      if (paysTribute && map.time == r.nextTributeDate) {
        r.goodsSent.clear();
        r.nextTributeDate += YEAR_LENGTH;
        //
        //  TODO:  Update relations based on success/failure in meeting
        //  tribute.
      }
    }
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}






