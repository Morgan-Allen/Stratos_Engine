

package game;
import util.*;
import static game.GameConstants.*;



public class Test {
  
  
  /**  Initial setup utilities:
    */
  static CityMap setupTestCity(int size, Terrain... gradient) {
    World   world = new World();
    City    city  = new City(world);
    CityMap map   = null;
    
    if (Visit.empty(gradient)) {
      map = new CityMap(city);
      map.performSetup(size);
    }
    else {
      map = CityMapGenerator.generateTerrain(city, size, gradient);
      CityMapGenerator.populateFixtures(map);
    }
    
    world.mapHigh = 10;
    world.mapWide = 10;
    city.mapX = 5;
    city.mapY = 5;
    world.cities.add(city);
    return map;
  }
  
  
  static void fillAllVacancies(CityMap map) {
    for (Building b : map.buildings) {
      fillWorkVacancies(b);
      for (Actor w : b.workers) CityBorders.findHome(map, w);
    }
  }
  
  
  static void fillWorkVacancies(Building b) {
    for (Type t : b.type.workerTypes) {
      while (b.numWorkers(t) < b.maxWorkers(t)) {
        spawnWalker(b, t, false);
      }
    }
  }
  
  
  static void fillHomeVacancies(Building b, Type... types) {
    for (Type t : types) {
      while (b.numResidents(t.socialClass) < b.maxResidents(t.socialClass)) {
        spawnWalker(b, t, true);
      }
    }
  }
  
  
  static Actor spawnWalker(Building b, Type type, boolean resident) {
    
    Actor walker = (Actor) type.generate();
    type.initAsMigrant(walker);
    walker.enterMap(b.map, b.at.x, b.at.y, 1);
    walker.inside = b;
    
    if (resident) b.setResident(walker, true);
    else          b.setWorker  (walker, true);
    b.visitors.add(walker);
    
    return walker;
  }
  
  
  
  /**  Graphical display and loop-execution:
    */
  final static String
    VIEW_NAME = "Tlatoani";
  final static BuildType BUILD_MENUS[][] = {
    PALACE_BUILDINGS     ,
    INDUSTRIAL_BUILDINGS ,
    ECONOMIC_BUILDINGS   ,
    RESIDENTIAL_BUILDINGS,
    MILITARY_BUILDINGS   ,
    RELIGIOUS_BUILDINGS  
  };
  final static String BUILD_MENU_NAMES[] = {
    "Palace"     , "Industrial", "Economic" ,
    "Residential", "Military"  , "Religious"
  };
  
  static int[][]  graphic   = null;
  static int      frames    = 0   ;
  static boolean  cityView  = true;
  static boolean  paused    = false;
  static Coord    hover     = new Coord(-1, -1);
  static boolean  doBuild   = false;
  static Object   buildMenu = null;
  static Building placing   = null;
  static Object   above     = null;
  static Series <Character> pressed = new Batch();
  
  
  static void configGraphic(int w, int h) {
    if (graphic == null || graphic.length != w || graphic[0].length != h) {
      graphic = new int[w][h];
    }
  }
  
  
  static void updateCityMapView(CityMap map) {
    configGraphic(map.size, map.size);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      int fill = BLANK_COLOR;
      CityMap.Tile at = map.tileAt(c.x, c.y);
      if      (at.above != null  ) fill = at.above.type.tint;
      else if (at.paved          ) fill = PAVE_COLOR;
      else if (at.terrain != null) fill = at.terrain.tint;
      graphic[c.x][c.y] = fill;
    }
    
    for (Actor w : map.walkers) if (w.inside == null) {
      int fill = WALKER_COLOR;
      if      (w.work != null) fill = w.work.type.tint;
      else if (w.home != null) fill = w.home.type.tint;
      graphic[w.at.x][w.at.y] = fill;
    }
    
    if (placing != null) {
      Type type = placing.type;
      int x = hover.x, y = hover.y, w = type.wide, h = type.high;
      boolean canPlace = placing.canPlace(map, x, y);
      
      for (Coord c : Visit.grid(x, y, w, h, 1)) try {
        graphic[c.x][c.y] = canPlace ? type.tint : NO_BLD_COLOR;
      }
      catch (Exception e) {}
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  private static void updateWorldMapView(CityMap map) {
    World world = map.city.world;
    configGraphic(world.mapWide, world.mapHigh);
    
    //  Note- you could just calculate a bounding-box for the map based on the
    //  coordinates of all cities, plus a certain margin.
    
    for (Coord c : Visit.grid(0, 0, world.mapWide, world.mapHigh, 1)) {
      graphic[c.x][c.y] = BLANK_COLOR;
    }
    for (City city : world.cities) {
      int x = (int) city.mapX, y = (int) city.mapY;
      graphic[x][y] = city.tint;
    }
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  static CityMap runGameLoop(
    CityMap map, int numUpdates, boolean graphics, String filename
  ) {
    while (true) {
      
      if (graphics) {
        if (cityView) updateCityMapView (map);
        else          updateWorldMapView(map);
        I.present(graphic, VIEW_NAME, 400, 400);
        hover   = I.getDataCursor(VIEW_NAME, false);
        pressed = I.getKeysPressed(VIEW_NAME);
        
        if (cityView) above = map.above(hover.x, hover.y);
        else above = map.city.world.onMap(hover.x, hover.y);
        I.talkAbout = above;
        I.used60Frames = (frames++ % 60) == 0;
        
        if (doBuild) {
          I.presentInfo(reportForBuildMenu(map), VIEW_NAME);
        }
        else if (above instanceof City) {
          I.presentInfo(reportFor((City) above), VIEW_NAME);
        }
        else if (above instanceof Building) {
          I.presentInfo(reportFor((Building) above), VIEW_NAME);
        }
        else {
          I.presentInfo(baseReport(map, filename), VIEW_NAME);
        }
      }
      
      if (! paused) {
        map.update();
        if (numUpdates > 0 && --numUpdates == 0) break;
      }
      
      if (graphics) {
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
    }
    return map;
  }
  
  
  
  /**  Saving and loading-
    */
  static void saveMap(CityMap map, String filename) {
    try {
      I.say("\nWILL SAVE CURRENT MAP...");
      Session.saveSession(filename, map);
    }
    catch (Exception e) { I.report(e); }
  }
  
  
  static CityMap loadMap(CityMap oldMap, String filename) {
    if (! Session.fileExists(filename)) {
      return oldMap;
    }
    try {
      I.say("\nWILL LOAD SAVED MAP...");
      Session s = Session.loadSession(filename, true);
      CityMap map = (CityMap) s.loaded()[0];
      if (map == null) throw new Exception("No map loaded!");
      return map;
    }
    catch (Exception e) { I.report(e); }
    return oldMap;
  }
  
  
  
  /**  UI outputs-
    */
  private static String reportFor(City c) {
    StringBuffer report = new StringBuffer(""+c);
    
    List <String> borderRep = new List();
    for (City other : c.world.cities) {
      City.RELATION r = c.relations.get(other);
      if (other == c || r == null) continue;
      borderRep.add("\n  "+other+": "+r);
    }
    if (! borderRep.empty()) {
      report.append("\nRelations:");
      for (String s : borderRep) report.append(s);
    }
    
    List <String> goodRep = new List();
    for (Good g : ALL_GOODS) {
      float amount = c.inventory.valueFor(g);
      float demand = c.tradeLevel.valueFor(g);
      if (amount == 0 && demand == 0) continue;
      
      if (demand > 0) goodRep.add(
        "\n  Needs: "+g+": "+I.shorten(amount, 1)+"/"+I.shorten( demand, 1)
      );
      else goodRep.add(
        "\n  Sells: "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(-demand, 1)
      );
    }
    if (! goodRep.empty()) {
      report.append("\n\nTrading:");
      for (String s : goodRep) report.append(s);
    }
    
    return report.toString();
  }
  
  
  private static String reportFor(Building b) {
    
    StringBuffer report = new StringBuffer(""+b+"\n");
    
    if (b.workers.size() > 0) {
      report.append("\nWorkers:");
      for (Actor w : b.workers) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.residents.size() > 0) {
      report.append("\nResidents:");
      for (Actor w : b.residents) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.visitors.size() > 0) {
      report.append("\nVisitors:");
      for (Actor w : b.visitors) {
        report.append("\n  "+w+" ("+w.jobType()+")");
      }
    }
    
    if (b.formation() != null && b.formation().recruits.size() > 0) {
      report.append("\nRecruits:");
      for (Actor w : b.formation().recruits) {
        report.append("\n  "+w);
      }
    }
    
    if (b.buildLevel < 1) {
      report.append("\nBuild level:\n  "+I.percent(b.buildLevel));
    }
    
    if (b.craftProgress() > 0) {
      report.append("\nCraft progress:\n  "+I.percent(b.craftProgress()));
    }
    
    List <String> goodRep = new List();
    for (Good g : ALL_GOODS) {
      float amount = b.inventory.valueFor(g);
      float demand = b.demandFor(g) + amount;
      if (amount <= 0 && demand <= 0) continue;
      goodRep.add("\n  "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
    }
    
    if (! goodRep.empty()) {
      report.append("\nGoods:");
      for (String s : goodRep) report.append(s);
    }
    
    return report.toString();
  }
  
  
  private static String reportForBuildMenu(CityMap map) {
    StringBuffer report = new StringBuffer("");
    
    if (placing != null) {
      report.append("Place Building: "+placing.type.name);
      report.append("\n  (P) place");
      
      int x = hover.x, y = hover.y;
      if (pressed.includes('p') && placing.canPlace(map, x, y)) {
        placing.enterMap(map, x, y, 1);
        placing = null;
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        placing = null;
      }
    }
    
    else if (buildMenu == null) {
      report.append("Build Menu: Main\n");
      int i = 1;
      for (BuildType[] menu : BUILD_MENUS) {
        report.append("\n  ("+i+") "+BUILD_MENU_NAMES[i - 1]);
        if (pressed.includes((char) ('0' + i))) {
          buildMenu = menu;
        }
        i++;
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        doBuild = false;
      }
    }
    
    else {
      int catIndex = Visit.indexOf(buildMenu, BUILD_MENUS);
      report.append("Build Menu: "+BUILD_MENU_NAMES[catIndex]+"\n");
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
  
  
  private static String baseReport(CityMap map, String filename) {
    StringBuffer report = new StringBuffer("Home City: "+map.city);
    
    report.append("\n\nFunds: "+map.city.currentFunds);
    report.append("\n\nTime: "+map.time);
    report.append("\nPaused: "+paused  );
    report.append("\n");

    report.append("\n(C) city view");
    if (pressed.includes('c')) {
      cityView = true;
    }
    report.append("\n(W) world view");
    if (pressed.includes('w')) {
      cityView = false;
    }
    report.append("\n(B) build menu");
    if (pressed.includes('b')) {
      doBuild = true;
    }
    report.append("\n(P) un/pause");
    if (pressed.includes('p')) {
      paused = ! paused;
    }
    report.append("\n(S) save");
    if (pressed.includes('s')) {
      saveMap(map, filename);
    }
    report.append("\n(L) load");
    if (pressed.includes('l')) {
      map = loadMap(map, filename);
    }
    report.append("\n(Q) quit");
    if (pressed.includes('q')) {
      System.exit(0);
    }
    
    return report.toString();
  }
  
}






