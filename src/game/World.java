

package game;
import util.*;
import static game.GameConstants.*;



public class World implements Session.Saveable {
  
  
  /**  Public interfaces-
    */
  public static class Route {
    int distance;
    int moveMode;
  }
  
  public static class Journey {
    
    Base from;
    Base goes;
    int startTime;
    int arriveTime;
    int moveMode;
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
  
  Good      goodTypes   [] = {};
  BuildType buildTypes  [] = {};
  ActorType shipTypes   [] = {};
  ActorType citizenTypes[] = {};
  ActorType soldierTypes[] = {};
  ActorType nobleTypes  [] = {};
  
  int time = 0;
  final public WorldCalendar calendar = new WorldCalendar(this);
  final List <WorldScenario> scenarios = new List();
  
  List <WorldLocale> locales  = new List();
  List <Base       > bases    = new List();
  List <Journey    > journeys = new List();
  List <Event      > history  = new List();
  
  //  Only used for graphical reference...
  int mapWide = 10, mapHigh = 10;
  Table <String, Type> mediaTypes = new Table();
  
  String savePath = "saves/save_locale.str";
  
  
  
  public World(Good goodTypes[]) {
    this.goodTypes  = goodTypes ;
  }
  
  public void assignTypes(
    BuildType builds[], ActorType ships[],
    ActorType citizens[], ActorType soldiers[], ActorType nobles[]
  ) {
    this.buildTypes   = builds  ;
    this.shipTypes    = ships   ;
    this.citizenTypes = citizens;
    this.soldierTypes = soldiers;
    this.nobleTypes   = nobles  ;
  }
  
  public void assignMedia(Object... args) {
    mediaTypes = Table.make(args);
  }
  
  public void assignSavePath(String savePath) {
    this.savePath = savePath;
  }
  
  public String savePath() {
    return savePath;
  }
  
  
  public World(Session s) throws Exception {
    s.cacheInstance(this);
    
    settings.loadState(s);
    goodTypes    = (Good     []) s.loadObjectArray(Good     .class);
    buildTypes   = (BuildType[]) s.loadObjectArray(BuildType.class);
    shipTypes    = (ActorType[]) s.loadObjectArray(ActorType.class);
    citizenTypes = (ActorType[]) s.loadObjectArray(ActorType.class);
    soldierTypes = (ActorType[]) s.loadObjectArray(ActorType.class);
    nobleTypes   = (ActorType[]) s.loadObjectArray(ActorType.class);
    
    time = s.loadInt();
    calendar.loadState(s);
    
    s.loadObjects(scenarios);
    s.loadObjects(locales);
    s.loadObjects(bases);
    
    for (int n = s.loadInt(); n-- > 0;) {
      Journey j = new Journey();
      j.from       = (Base) s.loadObject();
      j.goes       = (Base) s.loadObject();
      j.startTime  = s.loadInt();
      j.arriveTime = s.loadInt();
      j.moveMode   = s.loadInt();
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
    
    savePath = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    settings.saveState(s);
    s.saveObjectArray(goodTypes   );
    s.saveObjectArray(buildTypes  );
    s.saveObjectArray(shipTypes   );
    s.saveObjectArray(citizenTypes);
    s.saveObjectArray(soldierTypes);
    s.saveObjectArray(nobleTypes  );
    
    s.saveInt(time);
    calendar.saveState(s);
    
    s.saveObjects(scenarios);
    s.saveObjects(locales);
    s.saveObjects(bases);
    
    s.saveInt(journeys.size());
    for (Journey j : journeys) {
      s.saveObject(j.from);
      s.saveObject(j.goes);
      s.saveInt(j.startTime );
      s.saveInt(j.arriveTime);
      s.saveInt(j.moveMode  );
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
    
    s.saveString(savePath);
  }
  
  
  
  /**  Accessing types:
    */
  public Type[] buildTypes() { return buildTypes; }
  public Good[] goodTypes () { return goodTypes ; }
  
  
  
  /**  Managing Cities and Locales:
    */
  public static void setupRoute(WorldLocale a, WorldLocale b, int distance, int moveMode) {
    Route route = new Route();
    route.distance = distance;
    route.moveMode = moveMode;
    a.routes.put(b, route);
    b.routes.put(a, route);
  }
  
  
  public WorldLocale addLocale(float mapX, float mapY, String label, boolean homeland) {
    WorldLocale l = new WorldLocale();
    l.mapX  = mapX;
    l.mapY  = mapY;
    l.label = label;
    l.homeland = homeland;
    this.locales.add(l);
    return l;
  }
  
  
  public WorldLocale addLocale(float mapX, float mapY) {
    return addLocale(mapX, mapY, "Locale at "+mapX+"|"+mapY, false);
  }
  
  
  public static Vec2D mapCoords(Base b) {
    return new Vec2D(b.locale.mapX, b.locale.mapY);
  }
  
  
  public void addBases(Base... bases) {
    Visit.appendTo(this.bases, bases);
  }
  
  
  public Base baseNamed(String n) {
    for (Base b : bases) if (b.name.equals(n)) return b;
    return null;
  }
  
  
  public Base baseAt(WorldLocale l) {
    for (Base b : bases) if (b.locale == l) return b;
    return null;
  }
  
  
  public Area activeBaseMap() {
    for (Base c : bases) if (c.activeMap() != null) return c.activeMap();
    return null;
  }
  
  
  public Series <WorldLocale> locales() {
    return locales;
  }
  
  
  public Series <Base> bases() {
    return bases;
  }
  
  
  
  /**  Managing Scenarios-
    */
  public void addScenario(WorldScenario scenario) {
    scenarios.add(scenario);
  }
  
  
  public WorldScenario scenarioFor(WorldLocale locale) {
    for (WorldScenario s : scenarios) if (s.locale == locale) return s;
    return null;
  }
  
  
  public Series <WorldScenario> scenarios() {
    return scenarios;
  }
  
  
  
  /**  Registering and updating journeys:
    */
  public float travelTime(Base from, Base goes, int moveMode) {
    float distance = from.distance(goes, moveMode);
    if (distance < 0) return -100;
    
    distance = Nums.max(1, distance);
    float travelTime = distance;
    if (moveMode == Type.MOVE_LAND ) travelTime *= LAND_TRAVEL_TIME ;
    if (moveMode == Type.MOVE_WATER) travelTime *= WATER_TRAVEL_TIME;
    if (moveMode == Type.MOVE_AIR  ) travelTime *= AIR_TRAVEL_TIME  ;
    
    return travelTime;
  }
  
  
  public Journey beginJourney(
    Base from, Base goes, int moveMode, Journeys... going
  ) {
    if (from == null || goes == null) return null;
    
    float travelTime = travelTime(from, goes, moveMode);
    Journey j = new Journey();
    j.from       = from;
    j.goes       = goes;
    j.startTime  = time;
    j.moveMode   = moveMode;
    j.arriveTime = (int) (j.startTime + travelTime);
    
    for (Journeys g : going) j.going.add(g);
    
    journeys.add(j);
    
    for (Journeys g : going) {
      g.onDeparture(goes, j);
    }
    
    if (reports(j)) {
      I.say("\nBeginning journey: "+j.from+" to "+j.goes);
      I.say("  Embarked: "+j.going);
      I.say("  Time: "+time+", arrival: "+j.arriveTime);
    }
    
    return j;
  }

  
  public Journey beginJourney(
    Base from, Base goes, int moveMode, Series <Journeys> going
  ) {
    return beginJourney(from, goes, moveMode, going.toArray(Journeys.class));
  }
  
  
  public boolean completeJourney(Journey j) {
    journeys.remove(j);
    
    if (reports(j)) {
      I.say("\nCompleted journey: "+j.from+" to "+j.goes);
      I.say("  Embarked: "+j.going);
      I.say("  Time: "+time+", arrival: "+j.arriveTime);
    }
    
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
  
  
  public boolean onJourney(Journeys on) {
    return journeyFor(on) != null;
  }
  
  
  public Series <Journey> journeys() {
    return journeys;
  }
  
  
  public int arriveTime(Actor going) {
    return arriveTime(going, null);
  }
  
  
  public int arriveTime(Actor going, Base goes) {
    Journey j = journeyFor(going);
    if (j != null) return j.arriveTime;
    
    if (going.inside() instanceof Actor) {
      j = journeyFor((Actor) going.inside());
      if (j != null) return j.arriveTime;
    }
    
    Base offmap = going.offmapBase();
    if (offmap != null && offmap.migrants().includes(going)) {
      ActorAsVessel t = offmap.traderFor(going.base());
      j = t == null ? null : journeyFor(t);
      
      if (j != null && j.goes == offmap) {
        int arriveTime = j.arriveTime;
        arriveTime += travelTime(j.from, j.goes, j.moveMode);
        return arriveTime;
      }
    }
    
    return -1;
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
    
    Area active = activeBaseMap();
    if (active != null) {
      active.locals.updateBase();
    }
    for (Base city : bases) {
      city.updateBase();
    }
    
    for (Journey j : journeys) {
      if (time >= j.arriveTime) {
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









