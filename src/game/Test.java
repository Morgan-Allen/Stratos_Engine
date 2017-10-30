

package game;
import util.*;
import static game.CityMap.*;
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
      map = CityMapTerrain.generateTerrain(city, size, gradient);
      CityMapTerrain.populateFixtures(map);
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
    
    Actor actor = (Actor) type.generate();
    Tile at = b.at();
    type.initAsMigrant(actor);
    actor.enterMap(b.map, at.x, at.y, 1);
    actor.inside = b;
    
    if (resident) b.setResident(actor, true);
    else          b.setWorker  (actor, true);
    b.visitors.add(actor);
    
    return actor;
  }
  
  
  
  /**  Graphical display and loop-execution:
    */
  final static int FOG_SCALE[] = new int[10];
  static { for (int i = 10; i-- > 0;) FOG_SCALE[i] = colour(0, 0, 0, i); }
  
  final static String
    VIEW_NAME = "Tlatoani";
  final static BuildType BUILD_MENUS[][] = {
    PALACE_BUILDINGS     ,
    INDUSTRIAL_BUILDINGS ,
    ECONOMIC_BUILDINGS   ,
    RESIDENTIAL_BUILDINGS,
    MILITARY_BUILDINGS   ,
    RELIGIOUS_BUILDINGS  ,
  };
  final static String BUILD_MENU_NAMES[] = {
    "Palace"     , "Industrial", "Economic" ,
    "Residential", "Military"  , "Religious"
  };
  final static String
    ROADS      = "Roads",
    DEMOLITION = "Demolition"
  ;
  
  static int[][]  graphic   = null;
  static int[][]  fogLayer  = null;
  static int      frames    = 0   ;
  static Coord    hover     = new Coord(-1, -1);
  static boolean  doBuild   = false;
  static Object   buildMenu = null;
  static Coord    drawnTile = null;
  static Object   placing   = null;
  static Object   above     = null;
  static Series <Character> pressed = new Batch();
  
  
  static void configGraphic(int w, int h) {
    if (graphic == null || graphic.length != w || graphic[0].length != h) {
      graphic  = new int[w][h];
      fogLayer = new int[w][h];
    }
  }
  
  
  static Box2D drawnBox(CityMap map) {
    Box2D b = new Box2D(hover.x, hover.y, 0, 0);
    if (drawnTile != null) b.include(drawnTile.x, drawnTile.y, 0);
    b.incHigh(1);
    b.incWide(1);
    Box2D full = new Box2D(0, 0, map.size, map.size);
    b.cropBy(full);
    return b;
  }
  
  
  static void updateCityMapView(CityMap map) {
    configGraphic(map.size, map.size);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      int fill = BLANK_COLOR;
      CityMap.Tile at = map.tileAt(c.x, c.y);
      
      if (at.above != null) {
        if (at.above.buildLevel() == -1) fill = MISSED_COLOR;
        else fill = at.above.type.tint;
      }
      else if (at.paved) {
        fill = PAVE_COLOR;
      }
      else if (at.terrain != null) {
        fill = at.terrain.tint;
      }
      graphic[c.x][c.y] = fill;
    }
    
    for (Actor w : map.actors) if (w.inside == null) {
      Tile at = w.at();
      int fill = WALKER_COLOR;
      if      (w.work != null) fill = w.work.type.tint;
      else if (w.home != null) fill = w.home.type.tint;
      graphic[at.x][at.y] = fill;
    }
    
    if (placing == ROADS) {
      for (Coord c : Visit.grid(drawnBox(map))) {
        if (map.blocked(c.x, c.y)) continue;
        graphic[c.x][c.y] = PAVE_COLOR;
      }
    }
    
    else if (placing == DEMOLITION) {
      for (Coord c : Visit.grid(drawnBox(map))) {
        if (map.above(c.x, c.y) != null || map.paved(c.x, c.y)) {
          graphic[c.x][c.y] = NO_BLD_COLOR;
        }
      }
    }
    
    else if (placing != null) {
      Building builds = (Building) placing;
      Type type = builds.type;
      int x = hover.x, y = hover.y, w = type.wide, h = type.high;
      boolean canPlace = builds.canPlace(map, x, y);
      
      for (Coord c : Visit.grid(x, y, w, h, 1)) try {
        graphic[c.x][c.y] = canPlace ? type.tint : NO_BLD_COLOR;
      }
      catch (Exception e) {}
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  private static void updateCityFogLayer(CityMap map) {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Tile t = map.tileAt(c.x, c.y);
      float sight = 0;
      sight += map.fog.sightLevel(t);
      sight += map.fog.maxSightLevel(t);
      float fog = 1 - (sight / 2);
      fogLayer[c.x][c.y] = FOG_SCALE[Nums.clamp((int) (fog * 10), 10)];
    }
  }
  
  
  private static void updateWorldMapView(CityMap map) {
    World world = map.city.world;
    int wide = world.mapWide * 2, high = world.mapHigh * 2;
    configGraphic(wide, high);
    
    //  Note- you could just calculate a bounding-box for the map based on the
    //  coordinates of all cities, plus a certain margin.
    
    for (Coord c : Visit.grid(0, 0, wide, high, 1)) {
      graphic[c.x][c.y] = BLANK_COLOR;
    }
    
    for (World.Journey j : world.journeys) {
      Vec2D c = world.journeyPos(j);
      int x = 1 + (int) (c.x * 2), y = 1 + (int) (c.y * 2);
      graphic[x][y] = j.going.first().homeCity().tint;
    }
    
    for (City city : world.cities) {
      City lord = city.currentLord();
      int x = (int) city.mapX * 2, y = (int) city.mapY * 2;
      for (Coord c : Visit.grid(x, y, 2, 2, 1)) graphic[c.x][c.y] = city.tint;
      if (lord != null) graphic[x + 1][y] = lord.tint;
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  static CityMap runGameLoop(
    CityMap map, int numUpdates, boolean graphics, String filename
  ) {
    int skipUpdate = 0;
    
    while (true) {
      
      if (graphics) {
        if (! map.settings.worldView) {
          updateCityMapView(map);
          updateCityFogLayer(map);
          I.present(VIEW_NAME, 400, 400, graphic, fogLayer);
        }
        else {
          updateWorldMapView(map);
          I.present(VIEW_NAME, 400, 400, graphic);
        }
        hover   = I.getDataCursor(VIEW_NAME, false);
        pressed = I.getKeysPressed(VIEW_NAME);
        
        if (! map.settings.worldView) {
          above = map.above(hover.x, hover.y);
          
          //  TODO:  Have actors register within nearby tiles themselves.
          for (Actor a : map.actors) {
            Tile at = a.at();
            if (at.x == hover.x && at.y == hover.y) above = a;
          }
        }
        else {
          above = map.city.world.onMap(hover.x / 2, hover.y / 2);
        }
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
        else if (above instanceof Element) {
          I.presentInfo(reportFor((Element) above), VIEW_NAME);
        }
        else {
          Vars.Ref <CityMap> ref = new Vars.Ref(map);
          I.presentInfo(baseReport(ref, filename), VIEW_NAME);
          map = ref.value;
        }
        
        if (pressed.includes('p')) {
          map.settings.paused = ! map.settings.paused;
        }
      }
      
      if (skipUpdate <= 0 && ! map.settings.paused) {
        int iterUpdates = map.settings.speedUp ? 10 : 1;
        for (int i = iterUpdates; i-- > 0;) map.update();
        
        skipUpdate = map.settings.slowed ? 10 : 1;
        if (numUpdates > 0 && --numUpdates == 0) break;
      }
      
      if (graphics) {
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
      skipUpdate -= 1;
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
    
    report.append("\n  Population: "+c.population);
    report.append("\n  Military: "+c.armyPower);
    report.append("\n  Prestige: "+c.prestige);
    
    List <String> borderRep = new List();
    for (City other : c.world.cities) if (other != c) {
      City.POSTURE r = c.posture(other);
      float loyalty = c.loyalty(other);
      borderRep.add("\n  "+other+": "+r+", "+City.descLoyalty(loyalty));
    }
    if (! borderRep.empty()) {
      report.append("\n\nRelations:");
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
    
    if (! c.buildLevel.empty()) {
      report.append("\n\nBuilt:");
      for (Type t : c.buildLevel.keys()) {
        int level = (int) c.buildLevel.valueFor(t);
        report.append("\n  "+level+"x "+t);
      }
    }
    
    return report.toString();
  }
  
  
  private static String reportFor(Element e) {
    StringBuffer report = new StringBuffer(""+e+"\n");
    
    if (e instanceof Actor) {
      Actor a = (Actor) e;
      report.append("\n  Growth: "+I.percent(a.growLevel()));
      report.append("\n  Task: "+a.jobDesc());
      
      if (a.carried != null) {
        report.append("\n  Carried: "+a.carried+": "+a.carryAmount);
      }
      if (a.cargo != null) {
        report.append("\n  Cargo:");
        for (Good g : a.cargo.keys()) {
          report.append("\n    "+g+": "+a.cargo.valueFor(g));
        }
      }
    }
    else {
      report.append("\n  Build level: "+I.percent(e.buildLevel()));
    }
    
    return report.toString();
  }
  
  
  private static String reportFor(Building b) {
    
    StringBuffer report = new StringBuffer(""+b+"\n");
    
    if (b.workers.size() > 0) {
      report.append("\nWorkers:");
      for (Actor w : b.workers) {
        report.append("\n  "+w+" ("+w.jobDesc()+")");
      }
    }
    
    if (b.residents.size() > 0) {
      report.append("\nResidents:");
      for (Actor w : b.residents) {
        report.append("\n  "+w+" ("+w.jobDesc()+")");
      }
    }
    
    if (b.visitors.size() > 0) {
      report.append("\nVisitors:");
      for (Actor w : b.visitors) {
        report.append("\n  "+w+" ("+w.jobDesc()+")");
      }
    }
    
    if (b.formation() != null && b.formation().recruits.size() > 0) {
      report.append("\nRecruits:");
      for (Actor w : b.formation().recruits) {
        report.append("\n  "+w);
      }
    }
    
    if (b.buildLevel() < 1) {
      report.append("\nBuild level:\n  "+I.percent(b.buildLevel()));
    }
    
    if (b.craftProgress() > 0) {
      report.append("\nCraft progress:\n  "+I.percent(b.craftProgress()));
    }
    
    //
    //  Finally, present a tally of goods in demand:
    Tally <Good> homeCons = b.homeUsed();
    List <String> goodRep = new List();
    
    for (Good g : ALL_GOODS) {
      float amount   = b.inventory.valueFor(g);
      float demand   = b.demandFor(g) + amount;
      float consumes = homeCons.valueFor(g);
      if (amount <= 0 && demand <= 0 && consumes <= 0) continue;
      
      demand += consumes;
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
    
    if (placing == ROADS) {
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
          for (Coord c : Visit.grid(drawnBox(map))) {
            if (map.blocked(c.x, c.y)) continue;
            map.tileAt(c.x, c.y).paved = true;
          }
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
          for (Coord c : Visit.grid(drawnBox(map))) {
            Element above = map.above(c.x, c.y);
            if (above != null) above.exitMap(map);
            if (map.paved(c.x, c.y)) map.tileAt(c.x, c.y).paved = false;
          }
          drawnTile = null;
        }
      }
      
      report.append("\n  (X) cancel");
      if (pressed.includes('x')) {
        drawnTile = null;
        placing   = null;
      }
    }
    else if (placing != null) {
      Building builds = (Building) placing;
      report.append("Place Building: "+builds.type.name);
      report.append("\n  (S) confirm site");
      
      int x = hover.x, y = hover.y;
      if (pressed.includes('s') && builds.canPlace(map, x, y)) {
        builds.enterMap(map, x, y, 1);
        placing = (Building) builds.type.generate();
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
      
      report.append("\n  (R) roads");
      if (pressed.includes('r')) {
        placing = ROADS;
      }
      
      report.append("\n  (D) demolish");
      if (pressed.includes('d')) {
        placing = DEMOLITION;
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
  
  
  private static String baseReport(Vars.Ref <CityMap> ref, String filename) {
    CityMap map = ref.value;
    StringBuffer report = new StringBuffer("Home City: "+map.city);
    
    //*
    report.append("\n\nFunds: "+map.city.currentFunds);
    report.append("\n\nTime: "+map.time);
    report.append("\nPaused: "+map.settings.paused);
    report.append("\n");
    //*/
    
    float avgHunger = 0;
    for (Actor a : map.actors) avgHunger += a.hunger / a.type.maxHealth;
    avgHunger /= map.actors.size();
    
    report.append("\nTOTAL POPULATION: "+map.actors.size());
    report.append("\nHUNGER LEVEL: "+I.percent(avgHunger)+"\n\n");
    //for (Actor a : map.walkers) report.append("\n  "+a);
    
    
    report.append("\n(C) city view");
    if (pressed.includes('c')) {
      map.settings.worldView = false;
    }
    report.append("\n(W) world view");
    if (pressed.includes('w')) {
      map.settings.worldView = true;
    }
    report.append("\n(B) build menu");
    if (pressed.includes('b')) {
      doBuild = true;
    }
    
    report.append("\n(P) un/pause");
    report.append("\n(S) save");
    if (pressed.includes('s')) {
      saveMap(map, filename);
    }
    report.append("\n(L) load");
    if (pressed.includes('l')) {
      ref.value = map = loadMap(map, filename);
    }
    report.append("\n(Q) quit");
    if (pressed.includes('q')) {
      System.exit(0);
    }
    
    return report.toString();
  }
  
}



