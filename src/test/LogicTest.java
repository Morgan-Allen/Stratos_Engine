

package test;
import game.*;
import static game.AreaPathCache.*;
import static game.GameConstants.*;
import static game.World.*;
import util.*;




public class LogicTest {
  
  
  /**  Initial setup utilities:
    */
  final public static AreaType
    BASE = areaType(2, 2, false, "base"),
    AWAY = areaType(3, 2, false, "away"),
    NEUT = areaType(2, 3, false, "neut"),
    DISTANT = areaType(5, 5, true, "distant")
  ;
  static {
    AreaType.setupRoute(BASE, AWAY, 1, Type.MOVE_LAND);
    AreaType.setupRoute(BASE, NEUT, 1, Type.MOVE_LAND);
    AreaType.setupRoute(NEUT, AWAY, 1, Type.MOVE_LAND);
    AreaType.setupRoute(BASE, DISTANT, 3, Type.MOVE_AIR);
  }
  
  
  static AreaType areaType(int x, int y, boolean homeland, String ID) {
    AreaType t = new AreaType(LogicTest.class, "TA_"+ID, "Test Area "+ID);
    t.initPosition(x, y, homeland);
    return t;
  }
  
  
  protected static Base setupTestBase(
    AreaType typeA, Faction faction, Good goods[],
    int size, boolean genTerrain, Terrain... gradient
  ) {
    
    World   world = new World(goods);
    Area    area  = world.addArea(typeA);
    Base    base  = new Base(world, area, faction, "Test Base");
    AreaMap map   = null;
    
    if (! genTerrain) {
      map = new AreaMap(world, area, base);
      map.performSetup(size, gradient);
    }
    else {
      map = AreaTerrain.generateTerrain(base, size, 0, gradient);
      AreaTerrain.populateFixtures(map);
    }
    
    world.setMapSize(10, 10);
    world.addBases(base);
    world.setPlayerFaction(faction);
    
    return base;
  }
  
  
  protected static Base setupTestBase(
    AreaType typeA, Faction faction, Good goods[],
    byte layout[][], byte elevation[][], Terrain... gradient
  ) {
    int wide = layout.length, high = layout[0].length;
    Base base = setupTestBase(typeA, faction, goods, Nums.max(wide, high), false, gradient);
    AreaMap map = base.activeMap();
    
    for (AreaTile t : map.allTiles()) {
      Terrain terr = gradient[layout[t.x][t.y]];
      int elev = elevation[t.x][t.y];
      map.setTerrain(t, terr, (byte) 0, elev);
    }
    
    return base;
  }
  
  
  
  /**  Graphical display and loop-execution:
    */
  final static int FOG_SCALE[][] = new int[10][10];
  final static int DANGER_SCALE[] = new int[10];
  static {
    for (int f = 10; f-- > 0;) {
      for (int e = 10; e-- > 0;) {
        FOG_SCALE[f][e] = colour(e, e, 0, Nums.min(10, e + f));
      }
    }
    for (int d = 10; d-- > 0;) {
      DANGER_SCALE[d] = colour(d, 0, 0, 10);
    }
  }
  
  
  final static String VIEW_NAME = "TESTING";
  
  BuildType buildMenus[][] = new BuildType[0][0];
  String buildMenuNames[] = new String[0];
  
  public void attachBuildMenu(
    BuildType buildMenus[][],
    String buildMenuNames[]
  ) {
    this.buildMenus = buildMenus;
    this.buildMenuNames = buildMenuNames;
  }
  
  
  String filename = "";
  boolean doLoad = false;
  
  int[][]  graphic   = null;
  int[][]  fogLayer  = null;
  int      frames    = 0   ;
  Coord    hover     = new Coord(-1, -1);
  boolean  doBuild   = false;
  Object   buildMenu = null;
  Coord    drawnTile = null;
  Object   placing   = null;
  Object   above     = null;
  Series <Character> pressed = new Batch();
  
  protected AreaTile keyTiles[];
  protected boolean viewPathMap   = false;
  protected boolean viewPlanMap   = false;
  protected boolean viewDangerMap = false;
  
  
  void configGraphic(int w, int h) {
    if (graphic == null || graphic.length != w || graphic[0].length != h) {
      graphic  = new int[w][h];
      fogLayer = new int[w][h];
    }
  }
  
  
  Box2D drawnBox(AreaMap map) {
    Box2D b = new Box2D(hover.x, hover.y, 0, 0);
    if (drawnTile != null) b.include(drawnTile.x, drawnTile.y, 0);
    b.incHigh(1);
    b.incWide(1);
    Box2D full = new Box2D(0, 0, map.size(), map.size());
    b.cropBy(full);
    return b;
  }
  
  
  void updateAreaView(AreaMap map, Base base) {
    configGraphic(map.size(), map.size());
    
    AreaDanger dangerMap = map.dangerMap(base.faction(), false);
    
    for (AreaTile at : map.allTiles()) {
      int fill = BLANK_COLOR;
      
      if (viewDangerMap && dangerMap != null) {
        float danger = dangerMap.fuzzyLevel(at.x, at.y) * 2;
        danger *= 10 / dangerMap.maxValue();
        fill = DANGER_SCALE[Nums.clamp((int) danger, 10)];
      }
      else {
        Element above;
        if (viewPlanMap) above = map.planning.objectAt(at);
        else above = map.above(at);
        
        if (above != null) {
          if (above.growLevel() == -1) fill = MISSED_COLOR;
          else fill = above.debugTint();
          
          if (hover.x == at.x && hover.y == at.y) {
            this.above = above;
          }
        }
        else if (at.terrain() != null && ! viewPlanMap) {
          fill = at.terrain().tint;
        }
      }
      
      graphic[at.x][at.y] = fill;
    }
    
    if (! viewPlanMap) for (Actor a : map.actors()) {
      AreaTile at = a.at();
      if (at == null || a.indoors()) continue;
      
      Type type = a.type();
      int fill = type.tint;
      
      if (! type.isVessel()) {
        fill = WALKER_COLOR;
        if      (a.work() != null) fill = ((Element) a.work()).type().tint;
        else if (a.home() != null) fill = a.home().type().tint;
      }
      
      if (type.wide == 1 && type.high == 1) {
        graphic[at.x][at.y] = fill;

        if (hover.x == at.x && hover.y == at.y) {
          this.above = a;
        }
      }
      else {
        for (Coord c : Visit.grid(0, 0, type.wide, type.high, 1)) try {
          graphic[at.x + c.x][at.y + c.y] = fill;
          
          if (hover.x == at.x + c.x && hover.y == at.y + c.y) {
            this.above = a;
          }
        }
        catch (Exception e) {}
      }
    }
    
    /*
    if (placing == ROADS0) {
      for (Coord c : Visit.grid(drawnBox(map))) {
        if (map.blocked(c.x, c.y)) continue;
        graphic[c.x][c.y] = PAVE_COLOR;
      }
    }
    
    else if (placing == DEMOLITION) {
      for (Coord c : Visit.grid(drawnBox(map))) {
        if (map.above(c) != null) {
          graphic[c.x][c.y] = NO_BLD_COLOR;
        }
      }
    }
    //*/
    //else
    
    if (placing != null) {
      Building builds = (Building) placing;
      Type type = builds.type();
      builds.setLocation(map.tileAt(hover), map);
      boolean canPlace = builds.canPlace(map);
      for (AreaTile t : builds.footprint(map, false)) try {
        graphic[t.x][t.y] = canPlace ? type.tint : NO_BLD_COLOR;
      }
      catch (Exception e) {}
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  void updatePathingView(AreaMap map, Base base) {
    configGraphic(map.size(), map.size());
    
    AreaTile hovered = map.tileAt(hover.x, hover.y);
    hovered = AreaTile.nearestOpenTile(hovered, map);
    
    Zone area = map.pathCache.rawZone(hovered), around[] = null;
    ZoneGroup group = null;
    if (area != null) {
      group  = area.group();
      around = new Zone[area.borders().size()];
      int i = 0;
      for (Border b : area.borders()) around[i++] = b.with();
    }
    
    for (AreaTile t : map.allTiles()) {
      Element above = map.above(t);
      int fill = above == null ? BLANK_COLOR : above.debugTint();
      if (Visit.arrayIncludes(keyTiles, t)) {
        fill = NO_BLD_COLOR;
      }
      else if (map.blocked(t)) {
        fill = BLACK_COLOR;
      }
      else if (area != null) {
        Zone under = map.pathCache.rawZone(t);
        if (area == under) {
          fill = WHITE_COLOR;
        }
        else if (Visit.arrayIncludes(around, under)) {
          fill = WALKER_COLOR;
        }
        else if (above != null) {
        }
        else if (under != null && under.group() == group) {
          fill = MISSED_COLOR;
        }
      }
      graphic[t.x][t.y] = fill;
    }
  }
  
  
  private void updateCityFogLayer(AreaMap map, Base base) {
    AreaFog fogMap = map.fogMap(base.faction(), false);
    if (fogMap == null) return;
    
    for (AreaTile t : map.allTiles()) {
      float sight = 0;
      sight += fogMap.sightLevel(t);
      sight += fogMap.maxSightLevel(t);
      
      int fog  = Nums.clamp((int) ((1 - (sight / 2)) * 10), 10);
      int high = Nums.clamp(t.elevation(), 10);
      fogLayer[t.x][t.y] = FOG_SCALE[fog][high];
    }
  }
  
  
  private void updateWorldMapView(AreaMap map) {
    World world = map.world;
    int wide = world.mapWide() * 2, high = world.mapHigh() * 2;
    configGraphic(wide, high);
    
    //  Note- you could just calculate a bounding-box for the map based on the
    //  coordinates of all cities, plus a certain margin.
    
    for (Coord c : Visit.grid(0, 0, wide, high, 1)) {
      graphic[c.x][c.y] = BLANK_COLOR;
    }
    
    for (World.Journey j : world.journeys()) {
      Vec2D c = world.journeyPos(j);
      int x = 1 + (int) (c.x * 2), y = 1 + (int) (c.y * 2);
      graphic[x][y] = j.going().first().base().tint();
    }
    
    for (Base city : world.bases()) {
      int x = (int) city.area.type.mapX() * 2;
      int y = (int) city.area.type.mapY() * 2;
      for (Coord c : Visit.grid(x, y, 2, 2, 1)) {
        graphic[c.x][c.y] = city.tint();
        if (hover.matches(c)) above = city;
      }
      graphic[x + 1][y] = city.faction().tint();
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  
  

  
  private static LogicTest currentTest = null;
  private static Base currentBase = null;
  
  public static LogicTest currentTest() {
    return currentTest;
  }
  
  public static Base currentCity() {
    return currentBase;
  }
  
  
  public Base runLoop(
    Base base, int numUpdates, boolean graphics, String filename
  ) {
    AreaMap map = base.activeMap();
    int skipUpdate = 0;
    boolean doQuit = false;
    this.filename = filename;
    
    while (! doQuit) {
      
      LogicTest.currentTest = this;
      LogicTest.currentBase = base;
      
      if (graphics) {
        World world = map.world;
        above = null;
        hover   = I.getDataCursor(VIEW_NAME, false);
        pressed = I.getKeysPressed(VIEW_NAME);
        
        if (! world.settings.worldView) {
          if (viewPathMap) {
            updatePathingView(map, base);
          }
          else {
            updateAreaView(map, base);
          }
          updateCityFogLayer(map, base);
          I.present(VIEW_NAME, 400, 400, graphic, fogLayer);
        }
        else {
          updateWorldMapView(map);
          I.present(VIEW_NAME, 400, 400, graphic);
        }
        
        I.talkAbout = above;
        I.used60Frames = (frames++ % 60) == 0;
        
        if (doBuild) {
          I.presentInfo(reportForBuildMenu(map, base), VIEW_NAME);
        }
        else if (above instanceof Base) {
          I.presentInfo(reportFor((Base) above), VIEW_NAME);
        }
        else if (above instanceof Building) {
          I.presentInfo(reportFor((Building) above), VIEW_NAME);
        }
        else if (above instanceof Element) {
          I.presentInfo(reportFor((Element) above), VIEW_NAME);
        }
        else {
          I.presentInfo(baseReport(map, base), VIEW_NAME);
        }
        
        if (pressed.includes('p')) {
          world.settings.paused = ! world.settings.paused;
        }
        if (doLoad) {
          base = loadCity(base, filename);
          doLoad = false;
          return base;
        }
      }
      
      World world = map.world;
      if (skipUpdate <= 0 && ! world.settings.paused) {
        int iterUpdates = world.settings.speedUp ? 10 : 1;
        for (int i = iterUpdates; i-- > 0;) map.update(1);
        
        skipUpdate = world.settings.slowed ? 10 : 1;
        if (numUpdates > 0 && --numUpdates == 0) doQuit = true;
      }
      
      if (graphics) {
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
      skipUpdate -= 1;
    }
    return base;
  }
  
  
  
  /**  Saving and loading-
    */
  protected static void saveCity(Base city, String filename) {
    try {
      I.say("\nWILL SAVE CURRENT CITY...");
      Session.saveSession(filename, city);
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  protected static Base loadCity(Base oldCity, String filename) {
    if (! Session.fileExists(filename)) {
      return oldCity;
    }
    try {
      I.say("\nWILL LOAD SAVED CITY...");
      Session s = Session.loadSession(filename, true);
      Base city = (Base) s.loaded()[0];
      if (city == null) throw new Exception("No map loaded!");
      return city;
    }
    catch (Exception e) { I.report(e); }
    return oldCity;
  }
  
  
  
  /**  UI outputs-
    */
  private String reportFor(Base c) {
    StringBuffer report = new StringBuffer(""+c);
    
    report.append("\n  Population: "+c.population());
    report.append("\n  Military: "+c.armyPower());
    report.append("\n  Prestige: "+c.federation().relations.prestige());
    
    List <String> borderRep = new List();
    for (Base other : c.world.bases()) if (other != c) {
      int r = c.posture(other);
      float loyalty = c.relations.bondLevel(other.faction());
      borderRep.add("\n  "+other+": "+r+", "+Base.descLoyalty(loyalty));
    }
    if (! borderRep.empty()) {
      report.append("\n\nRelations:");
      for (String s : borderRep) report.append(s);
    }
    
    List <String> goodRep = new List();
    for (Good g : c.world.goodTypes()) {
      float amount = c.trading.inventory(g);
      float need   = c.trading.needLevel(g);
      float accept = c.trading.prodLevel(g);
      //float demand = c.tradeLevel.valueFor(g);
      if (amount == 0 && need == 0 && accept == 0) continue;
      
      if (need > 0) goodRep.add(
        "\n  Needs: "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(need, 1)
      );
      if (accept > 0) goodRep.add(
        "\n  Sells: "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(accept, 1)
      );
    }
    if (! goodRep.empty()) {
      report.append("\n\nTrading:");
      for (String s : goodRep) report.append(s);
    }
    
    if (! c.buildLevel().empty()) {
      report.append("\n\nBuilt:");
      for (BuildType t : c.buildLevel().keys()) {
        int level = (int) c.buildLevel().valueFor(t);
        report.append("\n  "+level+"x "+t);
      }
    }
    
    return report.toString();
  }
  
  
  private String reportFor(Element e) {
    StringBuffer report = new StringBuffer(""+e+"\n");
    
    if (e instanceof Actor) {
      Actor a = (Actor) e;
      Type t = e.type();
      float maxHP = a.health.maxHealth();
      report.append(
        "\n  Melee/Range dmg:  "+t.meleeDamage+"/"+t.rangeDamage+
        "\n  Armour class:     "+t.armourClass+
        "\n  Sight/attack rng: "+t.sightRange+"/"+t.rangeDist+
        "\n  Injury:           "+I.shorten(a.health.injury() , 1)+"/"+maxHP+
        "\n  Fatigue:          "+I.shorten(a.health.fatigue(), 1)+"/"+maxHP+
        "\n  Hunger:           "+I.shorten(a.health.hunger() , 1)+"/"+maxHP+
        "\n  Fear Level:       "+I.percent(a.fearLevel())
      );
      
      float priority = a.jobPriority() / Task.PARAMOUNT;
      boolean urgent = a.inEmergency();
      
      report.append("\n  Task: "+a.jobDesc());
      report.append(" ("+I.percent(priority)+(urgent ? " E" : "")+")");
      report.append("\n  Home: "+a.home());
      report.append("\n  Work: "+a.work());
      
      if (t.isVessel()) {
        ActorAsVessel v = (ActorAsVessel) a;
        report.append("\n  Cargo:");
        for (Good g : e.map().world.goodTypes()) {
          float amount = v.outfit.carried(g);
          float demand = v.needLevels().valueFor(g);
          if (amount <= 0 && demand <= 0) continue;
          report.append("\n    "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
        }
        report.append("\n  Crew:");
        for (Actor w : v.crew()) {
          report.append("\n    "+w+" ("+w.jobType()+")");
        }
        report.append("\n  Passengers:");
        for (Actor w : v.allInside()) {
          if (w.work() == v) continue;
          report.append("\n    "+w+" ("+w.jobType()+")");
        }
      }
      else if (! a.outfit.carried().empty()) {
        report.append("\n  Carried:");
        for (Good g : a.outfit.carried().keys()) {
          report.append("\n    "+g+": "+a.outfit.carried(g));
        }
      }
    }
    else {
      if (e.type().growRate > 0) {
        report.append("\n  Growth: "+I.percent(e.growLevel()));
      }
      else {
        report.append("\n  Health: "+I.percent(e.buildLevel()));
      }
    }
    
    return report.toString();
  }
  
  
  private String reportFor(Building b) {
    
    StringBuffer report = new StringBuffer(""+b+"\n");
    
    int maxHP = b.type().maxHealth;
    int HP = (int) b.buildLevel() * maxHP;
    
    report.append("\n\nHealth: "+HP+"/"+maxHP);
    
    if (b.workers().size() > 0) {
      report.append("\nWorkers:");
      for (Actor w : b.workers()) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.residents().size() > 0) {
      report.append("\nResidents:");
      for (Actor w : b.residents()) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.allInside().size() > 0) {
      report.append("\nVisitors:");
      for (Actor w : b.allInside()) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.buildLevel() < 1) {
      report.append("\nBuild level:\n  "+I.percent(b.buildLevel()));
    }
    
    //
    //  Finally, present a tally of goods in demand:
    Tally <Good> homeCons = b.homeUsed();
    List <String> goodRep = new List();
    
    for (Good g : b.map().world.goodTypes()) {
      float amount   = b.inventory(g);
      float demand   = b.stockLimit(g);
      float consumes = homeCons == null ? 0 : homeCons.valueFor(g);
      if (amount == 0 && demand <= 0 && consumes <= 0) continue;
      
      demand += consumes;
      goodRep.add("\n  "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
    }
    
    if (b instanceof BuildingForCrafts) {
      BuildingForCrafts BC = (BuildingForCrafts) b;
      for (Object order : BC.orders()) {
        goodRep.add("\n  "+BC.descOrder(order));
      }
    }
    
    if (! goodRep.empty()) {
      report.append("\nGoods:");
      for (String s : goodRep) report.append(s);
    }
    
    report.append("\nMaterials: ");
    for (Good g : b.materials()) {
      float amount = b.materialLevel(g);
      float demand = b.materialNeed(g);
      report.append("\n  "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
    }
    
    report.append("\nEntrances: "+I.list(b.entrances()));
    
    return report.toString();
  }
  
  
  private String reportForBuildMenu(AreaMap map, Base city) {
    StringBuffer report = new StringBuffer("");
    
    /*
    if (placing == ROADS0) {
      report.append("Place Roads");
      if (drawnTile == null) {
        report.append("\n  (S) select start");
        if (pressed.includes('s')) {
          drawnTile = new Coord(hover);
        }
      }
      else {
        report.append("\n  (E) select end");
        if (pressed.includes('e')) {
          CityMapPlanning.placeStructure(ROAD, map, drawnBox(map), true);
          drawnTile = null;
        }
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        drawnTile = null;
        placing   = null;
      }
    }
    else if (placing == DEMOLITION) {
      report.append("Demolition");
      
      if (drawnTile == null) {
        report.append("\n  (S) select start");
        if (pressed.includes('s')) {
          drawnTile = new Coord(hover);
        }
      }
      else {
        report.append("\n  (E) select end");
        if (pressed.includes('e')) {
          CityMapPlanning.markDemolish(map, true, drawnBox(map));
          drawnTile = null;
        }
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        drawnTile = null;
        placing   = null;
      }
    }
    else
    //*/
    if (placing != null) {
      Building builds = (Building) placing;
      report.append("Place Building: "+builds.type().name);
      report.append("\n  (S) confirm site");
      
      int x = hover.x, y = hover.y;
      builds.setLocation(map.tileAt(hover), map);
      
      if (pressed.includes('s') && builds.canPlace(map)) {
        builds.enterMap(map, x, y, 1, city);
        placing = (Building) builds.type().generate();
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        placing = null;
      }
    }
    
    else if (buildMenu == null) {
      report.append("Build Menu: Main\n");
      int i = 1;
      for (BuildType[] menu : buildMenus) {
        report.append("\n  ("+i+") "+buildMenuNames[i - 1]);
        if (pressed.includes((char) ('0' + i))) {
          buildMenu = menu;
        }
        i++;
      }
      
      /*
      report.append("\n  (R) roads");
      if (pressed.includes('r')) {
        placing = ROADS0;
      }
      
      report.append("\n  (D) demolish");
      if (pressed.includes('d')) {
        placing = DEMOLITION;
      }
      //*/
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        doBuild = false;
      }
    }
    
    else {
      int catIndex = Visit.indexOf(buildMenu, buildMenus);
      report.append("Build Menu: "+buildMenuNames[catIndex]+"\n");
      int i = 1;
      for (BuildType b : (BuildType[]) buildMenu) {
        report.append("\n  ("+i+") "+b.name);
        if (pressed.includes((char) ('0' + i))) {
          placing = (Building) b.generate();
        }
        i++;
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        buildMenu = null;
      }
    }
    
    return report.toString();
  }
  
  
  private String baseReport(AreaMap map, Base base) {
    StringBuffer report = new StringBuffer("Home Base: "+base);
    WorldSettings settings = map.world.settings;
    
    report.append("\n\nFunds: "+base.funds());
    report.append("\n\nTime: "+map.time());
    report.append("\nPaused: "+settings.paused);
    report.append("\n");
    
    float avgHunger = 0;
    for (Actor a : map.actors()) avgHunger += a.health.hunger() / a.type().maxHealth;
    avgHunger /= map.actors().size();
    
    report.append("\nTOTAL POPULATION: "+map.actors().size());
    report.append("\nHUNGER LEVEL: "+I.percent(avgHunger)+"\n\n");
    
    report.append("\n(C) city view");
    if (pressed.includes('c')) {
      settings.worldView = false;
    }
    report.append("\n(W) world view");
    if (pressed.includes('w')) {
      settings.worldView = true;
    }
    report.append("\n(T) toggle pathing view");
    if (pressed.includes('t')) {
      viewPathMap = ! viewPathMap;
    }
    report.append("\n(N) toggle planning view");
    if (pressed.includes('n')) {
      viewPlanMap = ! viewPlanMap;
    }
    report.append("\n(D) toggle danger view");
    if (pressed.includes('d')) {
      viewDangerMap = ! viewDangerMap;
    }
    report.append("\n(F) toggle fog map");
    if (pressed.includes('f')) {
      map.world.settings.toggleFog = ! map.world.settings.toggleFog;
    }
    report.append("\n(B) build menu");
    if (pressed.includes('b')) {
      doBuild = true;
    }
    
    report.append("\n(P) un/pause");
    report.append("\n(S) save");
    if (pressed.includes('s')) {
      saveCity(base, filename);
    }
    report.append("\n(L) load");
    if (pressed.includes('l')) {
      doLoad = true;
    }
    report.append("\n(Q) quit");
    if (pressed.includes('q')) {
      System.exit(0);
    }
    
    return report.toString();
  }
  
}



