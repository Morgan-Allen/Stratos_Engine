

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
  
  static class Relation {
    City with;
    POSTURE posture;
    
    Tally <Good> tributeDue = new Tally();
    Tally <Good> goodsSent  = new Tally();
    Tally <Good> goodsFrom  = new Tally();
    //  Note:  The sums above are reset each year.
  }
  
  String name = "City";
  int tint = CITY_COLOR;
  
  World world;
  float mapX, mapY;
  
  CityEvents events = new CityEvents(this);
  Table <City, Integer > distances = new Table();
  Table <City, Relation> relations = new Table();
  
  int currentFunds = 0;
  Tally <Good> tradeLevel = new Tally();
  Tally <Good> inventory  = new Tally();
  
  List <Formation> formations = new List();
  int population = AVG_POPULATION;
  int armyPower  = AVG_ARMY_POWER;
  
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
      r.with     = (City) s.loadObject();
      r.posture = POSTURE.values()[s.loadInt()];
      s.loadTally(r.tributeDue);
      s.loadTally(r.goodsSent );
      s.loadTally(r.goodsFrom );
      relations.put(r.with, r);
    }
    
    currentFunds = s.loadInt();
    s.loadTally(tradeLevel);
    s.loadTally(inventory);
    
    s.loadObjects(formations);
    population = s.loadInt();
    armyPower  = s.loadInt();
    
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
      s.saveTally(r.tributeDue);
      s.saveTally(r.goodsSent );
      s.saveTally(r.goodsFrom );
    }
    
    s.saveInt(currentFunds);
    s.saveTally(tradeLevel);
    s.saveTally(inventory);
    
    s.saveObjects(formations);
    s.saveInt(population);
    s.saveInt(armyPower );
    
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
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  
  
  public City tradeOrigin() {
    return this;
  }
  
  
  Relation relationWith(City other) {
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with     = other;
      r.posture = POSTURE.NEUTRAL;
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
  
  
  static void setTribute(City a, City b, Tally <Good> tributeDue) {
    if (tributeDue == null) tributeDue = new Tally();
    a.relationWith(b).tributeDue = tributeDue;
  }
  
  
  boolean isVassal(City o) { return posture(o) == POSTURE.VASSAL; }
  boolean isLord  (City o) { return posture(o) == POSTURE.LORD  ; }
  boolean isEnemy (City o) { return posture(o) == POSTURE.ENEMY ; }
  boolean isAlly  (City o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  void setArmyPower(int power) {
    this.armyPower = power;
  }
  
  
  void assignMap(CityMap map) {
    this.map = map;
  }
  
  
  
  /**  Regular updates-
    */
  void updateFrom(CityMap map) {
    //  TODO:  Power will have to be updated whenever a formation leaves or
    //  rejoins the city instead.
    /*
    armyPower = 0;
    for (Formation f : formations) {
      f.update();
      armyPower += f.formationPower();
    }
    //*/
    
    for (Formation f : formations) {
      f.update();
    }
    
    if (map.time % YEAR_LENGTH == 0) {
      for (Relation r : relations.values()) {
        r.goodsFrom.clear();
        r.goodsSent.clear();
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






