

package game;
import util.*;
import static game.GameConstants.*;



public class World implements Session.Saveable {
  
  
  /**  Public interfaces-
    */
  public static class Locale {
    
    float mapX, mapY;
    Table <Locale, Integer> distances = new Table();
    
    String label;
    
    public float mapX() { return mapX; }
    public float mapY() { return mapY; }
    public String toString() { return label; }
  }
  
  public static class Journey {
    
    Base from;
    Base goes;
    int startTime;
    int arriveTime;
    Batch <Journeys> going = new Batch();
    
    public Base from() { return from; }
    public Base goes() { return goes; }
    public Series <Journeys> going() { return going; }
  }
  
  public static class Event {
    
    String label;
    int time;
    Object[] involved;
  }
  
  final public static String
    KEY_ATTACK_FLAG  = "attack_flag" ,
    KEY_DEFEND_FLAG  = "defend_flag" ,
    KEY_EXPLORE_FLAG = "explore_flag",
    KEY_CONTACT_FLAG = "contact_flag"
  ;
  
  
  /**  Data fields, setup and save/load methods-
    */
  final public WorldSettings settings = new WorldSettings(this);
  
  Good goodTypes   [] = {};
  Type buildTypes  [] = {};
  Type citizenTypes[] = {};
  Type soldierTypes[] = {};
  Type nobleTypes  [] = {};
  
  int time = 0;
  List <Locale > locales  = new List();
  List <Base   > bases    = new List();
  List <Journey> journeys = new List();
  List <Event  > history  = new List();
  
  //  Only used for graphical reference...
  int mapWide = 10, mapHigh = 10;
  Table <String, Type> mediaTypes = new Table();
  
  
  public World(Good goodTypes[]) {
    this.goodTypes  = goodTypes ;
  }
  
  
  public void assignTypes(
    Type buildTypes[], Type citizens[], Type soldiers[], Type nobles[]
  ) {
    this.buildTypes   = buildTypes;
    this.citizenTypes = citizens  ;
    this.soldierTypes = soldiers  ;
    this.nobleTypes   = nobles    ;
  }
  
  
  public void assignMedia(Object... args) {
    mediaTypes = Table.make(args);
  }
  
  
  public World(Session s) throws Exception {
    s.cacheInstance(this);
    
    settings.loadState(s);
    goodTypes    = (Good[]) s.loadObjectArray(Good.class);
    buildTypes   = (Type[]) s.loadObjectArray(Type.class);
    citizenTypes = (Type[]) s.loadObjectArray(Type.class);
    soldierTypes = (Type[]) s.loadObjectArray(Type.class);
    nobleTypes   = (Type[]) s.loadObjectArray(Type.class);
    
    time = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      Locale l = new Locale();
      l.mapX = s.loadFloat();
      l.mapY = s.loadFloat();
      locales.add(l);
    }
    for (Locale l : locales) for (int d = s.loadInt(); d-- > 0;) {
      l.distances.put(locales.atIndex(s.loadInt()), s.loadInt());
    }
    
    s.loadObjects(bases);
    
    for (int n = s.loadInt(); n-- > 0;) {
      Journey j = new Journey();
      j.from       = (Base) s.loadObject();
      j.goes       = (Base) s.loadObject();
      j.startTime  = s.loadInt();
      j.arriveTime = s.loadInt();
      s.loadObjects(j.going);
      journeys.add(j);
    }

    for (int n = s.loadInt(); n-- > 0;) {
      Event e = new Event();
      e.time     = s.loadInt();
      e.label    = s.loadString();
      e.involved = s.loadObjectArray(Object.class);
      history.add(e);
    }
    
    mapWide = s.loadInt();
    mapHigh = s.loadInt();
    
    for (int n = s.loadInt(); n-- > 0;) {
      String key = s.loadString();
      Type type = (Type) s.loadObject();
      mediaTypes.put(key, type);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    
    settings.saveState(s);
    s.saveObjectArray(goodTypes   );
    s.saveObjectArray(buildTypes  );
    s.saveObjectArray(citizenTypes);
    s.saveObjectArray(soldierTypes);
    s.saveObjectArray(nobleTypes  );
    
    s.saveInt(time);
    
    s.saveInt(locales.size());
    for (Locale l : locales) {
      s.saveFloat(l.mapX);
      s.saveFloat(l.mapY);
    }
    for (Locale l : locales) {
      s.saveInt(l.distances.size());
      for (Locale d : l.distances.keySet()) {
        s.saveInt(locales.indexOf(d));
        s.saveInt(l.distances.get(d));
      }
    }
    
    s.saveObjects(bases);
    
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject(j.from);
      s.saveObject(j.goes);
      s.saveInt(j.startTime );
      s.saveInt(j.arriveTime);
      s.saveObjects(j.going);
    }
    
    s.saveInt(history.size());
    for (Event e : history) {
      s.saveString(e.label);
      s.saveInt(e.time);
      s.saveObjectArray(e.involved);
    }
    
    s.saveInt(mapWide);
    s.saveInt(mapHigh);
    
    s.saveInt(mediaTypes.size());
    for (String key : mediaTypes.keySet()) {
      s.saveString(key);
      s.saveObject(mediaTypes.get(key));
    }
  }
  
  
  
  /**  Accessing types:
    */
  public Type[] buildTypes() { return buildTypes; }
  public Good[] goodTypes () { return goodTypes ; }
  
  
  
  /**  Managing Cities and Locales:
    */
  public static void setupRoute(Locale a, Locale b, int distance) {
    a.distances.put(b, distance);
    b.distances.put(a, distance);
  }
  
  
  public Locale addLocale(float mapX, float mapY, String label) {
    Locale l = new Locale();
    l.mapX  = mapX;
    l.mapY  = mapY;
    l.label = label;
    this.locales.add(l);
    return l;
  }
  
  
  public Locale addLocale(float mapX, float mapY) {
    return addLocale(mapX, mapY, "Locale at "+mapX+"|"+mapY);
  }
  
  
  public static Vec2D mapCoords(Base c) {
    return new Vec2D(c.locale.mapX, c.locale.mapY);
  }
  
  
  public void addBases(Base... cities) {
    Visit.appendTo(this.bases, cities);
  }
  
  
  public Base baseNamed(String n) {
    for (Base c : bases) if (c.name.equals(n)) return c;
    return null;
  }
  
  
  public AreaMap activeBaseMap() {
    for (Base c : bases) if (c.activeMap() != null) return c.activeMap();
    return null;
  }
  
  
  public Series <Base> bases() {
    return bases;
  }
  
  
  
  /**  Registering and updating journeys:
    */
  public Journey beginJourney(Base from, Base goes, Journeys... going) {
    if (from == null || goes == null) return null;
    
    float distance = Nums.max(1, from.distance(goes));
    
    Journey j = new Journey();
    j.from       = from;
    j.goes       = goes;
    j.startTime  = time;
    j.arriveTime = (int) (j.startTime + (distance * TRADE_DIST_TIME));
    for (Journeys g : going) j.going.add(g);
    journeys.add(j);
    
    if (reports(j)) {
      I.say("\nBeginning journey: "+j.from+" to "+j.goes);
      I.say("  Embarked: "+j.going);
      I.say("  Time: "+time+", arrival: "+j.arriveTime);
    }
    
    return j;
  }

  
  public Journey beginJourney(Base from, Base goes, Series <Journeys> going) {
    return beginJourney(from, goes, going.toArray(Journeys.class));
  }
  
  
  public boolean completeJourney(Journey j) {
    journeys.remove(j);
    for (Journeys g : j.going) {
      g.onArrival(j.goes, j);
    }
    return true;
  }
  
  
  public boolean isComplete(Journey j) {
    return time >= j.arriveTime;
  }
  
  
  public Journey journeyFor(Journeys on) {
    for (Journey j : journeys) {
      if (j.going.includes(on)) return j;
    }
    return null;
  }
  
  
  public Series <Journey> journeys() {
    return journeys;
  }
  
  
  public int arriveTime(Journeys going) {
    Journey j = journeyFor(going);
    if (j == null) return -1;
    return j.arriveTime;
  }
  
  
  
  /**  Recording events:
    */
  public void recordEvent(String label, Object... involved) {
    Event e = new Event();
    e.label    = label;
    e.time     = time;
    e.involved = involved;
    history.add(e);
  }
  
  
  public void clearHistory() {
    history.clear();
  }
  
  
  public Batch <Event> eventsWithLabel(String label) {
    Batch <Event> matches = new Batch();
    for (Event e : history) if (e.label.equals(label)) matches.add(e);
    return matches;
  }
  
  
  public Series <Event> history() {
    return history;
  }
  
  
  
  /**  Regular updates-
    */
  public void updateWithTime(int time) {
    this.time = time;
    
    AreaMap active = activeBaseMap();
    if (active != null) {
      active.locals.updateCity();
    }
    for (Base city : bases) {
      city.updateCity();
    }
    
    for (Journey j : journeys) {
      if (time >= j.arriveTime) {
        if (reports(j)) {
          I.say("\nCompleted journey: "+j.from+" to "+j.goes);
          I.say("  Embarked: "+j.going);
          I.say("  Time: "+time+", arrival: "+j.arriveTime);
        }
        completeJourney(j);
      }
    }
  }
  
  
  public int time() {
    return time;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports(Journey j) {
    for (Journeys k : j.going) {
      if (k == I.talkAbout) return true;
    }
    return false;
  }
  
  
  public Vec2D journeyPos(Journey j) {
    float timeGone = time - j.startTime;
    float a = timeGone / (j.arriveTime - j.startTime), i = 1 - a;
    
    float initX = j.from.locale.mapX, initY = j.from.locale.mapY;
    float destX = j.goes.locale.mapX, destY = j.goes.locale.mapY;
    
    Vec2D c = new Vec2D();
    c.x = (destX * a) + (initX * i);
    c.y = (destY * a) + (initY * i);
    return c;
  }
  
  
  String descFor(Event e) {
    return e.label+" at time "+e.time+": "+I.list(e.involved);
  }
  
  
  public void setMapSize(int mapW, int mapH) {
    this.mapWide = mapW;
    this.mapHigh = mapH;
  }
  
  
  public int mapWide() { return mapWide; }
  public int mapHigh() { return mapHigh; }
  
  
  public Base onMap(int mapX, int mapY) {
    for (Base city : bases) {
      int x = (int) city.locale.mapX, y = (int) city.locale.mapY;
      if (x == mapX && y == mapY) return city;
    }
    return null;
  }
  
  
  public Type mediaTypeWithKey(String key) {
    return mediaTypes.get(key);
  }
  
}









