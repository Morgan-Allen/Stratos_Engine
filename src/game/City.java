

package game;
import util.*;
import static game.GameConstants.*;



public class City implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum RELATION {
    ENEMY ,
    VASSAL,
    ALLY  ,
    LORD  ,
  };
  
  String name = "City";
  int tint = CITY_COLOR;
  
  World world;
  float mapX, mapY;
  
  CityEvents events = new CityEvents(this);
  Table <City, Integer> distances = new Table();
  Table <City, RELATION> relations = new Table();
  
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
      City     c = (City) s.loadObject();
      RELATION r = RELATION.values()[s.loadInt()];
      relations.put(c, r);
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
    for (City c : relations.keySet()) {
      s.saveObject(c);
      s.saveInt(relations.get(c).ordinal());
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
  
  
  static void setRelations(City a, RELATION RA, City b, RELATION RB) {
    a.relations.put(b, RB);
    b.relations.put(a, RA);
  }
  
  
  boolean hasVassal(City o) { return relations.get(o) == RELATION.VASSAL; }
  boolean hasLord  (City o) { return relations.get(o) == RELATION.LORD  ; }
  boolean hasEnemy (City o) { return relations.get(o) == RELATION.ENEMY ; }
  boolean hasAlly  (City o) { return relations.get(o) == RELATION.ALLY  ; }
  
  
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
    return;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  public String toString() {
    return name;
  }
}






