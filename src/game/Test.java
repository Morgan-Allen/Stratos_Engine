

package game;
import util.*;
import static game.CityMapPathCache.*;
import static game.GameConstants.*;
import static game.World.*;



public class Test {
  
  
  /**  Initial setup utilities:
    */
  protected static Base setupTestCity(
    int size, Good goods[], boolean genTerrain, Terrain... gradient
  ) {
    World   world  = new World(goods);
    Locale  locale = world.addLocale(5, 5);
    Base    city   = new Base(world, locale, "Test City");
    AreaMap map    = null;
    
    if (! genTerrain) {
      map = new AreaMap(world, locale, city);
      map.performSetup(size, gradient);
    }
    else {
      map = CityMapTerrain.generateTerrain(city, size, 0, gradient);
      CityMapTerrain.populateFixtures(map);
    }
    
    world.mapHigh = 10;
    world.mapWide = 10;
    world.addCities(city);
    
    return city;
  }
  
  
  protected static Base setupTestCity(
    byte layout[][], byte elevation[][], Good goods[], Terrain... gradient
  ) {
    int wide = layout.length, high = layout[0].length;
    Base city = setupTestCity(Nums.max(wide, high), goods, false, gradient);
    AreaMap map = city.activeMap();
    
    for (Tile t : map.allTiles()) {
      Terrain terr = gradient[layout[t.x][t.y]];
      int elev = elevation[t.x][t.y];
      map.setTerrain(t, terr, (byte) 0, elev);
    }
    
    return city;
  }
  
  
  
  //  TODO:  Move these into the Scenario class!
  
  public static void fillAllVacancies(AreaMap map, ActorType defaultCitizen) {
    for (Building b : map.buildings) if (b.accessible()) {
      fillWorkVacancies(b);
      for (Actor w : b.workers) ActorUtils.findHome(map, w);
    }
    for (Building b : map.buildings) if (b.accessible()) {
      fillHomeVacancies(b, defaultCitizen);
    }
  }
  
  
  public static void fillWorkVacancies(Building b) {
    for (ActorType t : b.type().workerTypes.keys()) {
      while (b.numWorkers(t) < b.maxWorkers(t)) {
        spawnActor(b, t, false);
      }
    }
  }
  
  
  public static void fillHomeVacancies(Building b, ActorType... types) {
    for (ActorType t : types) {
      while (b.numResidents(t.socialClass) < b.maxResidents(t.socialClass)) {
        spawnActor(b, t, true);
      }
    }
  }
  
  
  public static Actor spawnActor(Building b, ActorType type, boolean resident) {
    
    Actor actor = (Actor) type.generate();
    Tile at = b.at();
    
    if (type.isPerson()) {
      type.initAsMigrant((ActorAsPerson) actor);
    }
    
    if (resident) b.setResident(actor, true);
    else          b.setWorker  (actor, true);
    
    if (b.complete()) {
      actor.enterMap(b.map, at.x, at.y, 1, b.base());
      actor.setInside(b, true);
    }
    else {
      Tile t = b.centre();
      actor.enterMap(b.map, t.x, t.y, 1, b.base());
    }
    
    return actor;
  }
  
  
  public static Tile randomTileNear(
    Tile at, float range, AreaMap map, boolean open
  ) {
    int x = (int) (at.x + (range * Rand.range(-1, 1)));
    int y = (int) (at.y + (range * Rand.range(-1, 1)));
    Tile t = map.tileAt(Nums.clamp(x, map.size), Nums.clamp(y, map.size));
    if (open) t = Tile.nearestOpenTile(t, map);
    return t;
  }
  
  
  public static void clearMargins(Building b, int marginWide) {
    Type type = b.type();
    Tile at = b.at();
    AreaMap map = b.map();
    int
      x = at.x - marginWide,
      y = at.y - marginWide,
      w = type.wide + (marginWide * 2),
      h = type.high + (marginWide * 2)
    ;
    for (Tile t : map.tilesUnder(x, y, w, h)) {
      Element e = map.above(t);
      if (e != null && e != b) e.exitMap(map);
    }
  }
  
  
  
  /**  Graphical display and loop-execution:
    */
  final static int FOG_SCALE[][] = new int[10][10];
  static {
    for (int f = 10; f-- > 0;) {
      for (int e = 10; e-- > 0;) {
        FOG_SCALE[f][e] = colour(e, e, 0, Nums.min(10, e + f));
      }
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
  
  protected Tile keyTiles[];
  
  
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
    Box2D full = new Box2D(0, 0, map.size, map.size);
    b.cropBy(full);
    return b;
  }
  
  
  void updateCityMapView(AreaMap map) {
    configGraphic(map.size, map.size);
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      int fill = BLANK_COLOR;
      Tile at = map.tileAt(c.x, c.y);
      
      if (at.above != null) {
        if (at.above.growLevel() == -1) fill = MISSED_COLOR;
        else fill = at.above.debugTint();
      }
      else if (at.terrain != null) {
        fill = at.terrain.tint;
      }
      graphic[c.x][c.y] = fill;
    }
    
    for (Actor a : map.actors) {
      Tile at = a.at();
      //I.say("At: "+at);
      if (at == null || a.indoors()) continue;
      int fill = WALKER_COLOR;
      if      (a.work() != null) fill = a.work().type().tint;
      else if (a.home() != null) fill = a.home().type().tint;
      graphic[at.x][at.y] = fill;
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
      int x = hover.x, y = hover.y, w = type.wide, h = type.high;
      boolean canPlace = builds.canPlace(map, x, y, 0);
      
      for (Coord c : Visit.grid(x, y, w, h, 1)) try {
        graphic[c.x][c.y] = canPlace ? type.tint : NO_BLD_COLOR;
      }
      catch (Exception e) {}
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  void updateCityPathingView(AreaMap map) {
    configGraphic(map.size, map.size);
    
    Tile hovered = map.tileAt(hover.x, hover.y);
    hovered = Tile.nearestOpenTile(hovered, map);
    
    Area area = map.pathCache.rawArea(hovered), around[] = null;
    AreaGroup group = null;
    if (area != null) {
      group  = area.group;
      around = new Area[area.borders.size()];
      int i = 0;
      //for (Area b : area.borders) around[i++] = b;
      for (Border b : area.borders) around[i++] = b.with;
    }
    
    for (Tile t : map.allTiles()) {
      int fill = t.above == null ? BLANK_COLOR : t.above.debugTint();
      if (Visit.arrayIncludes(keyTiles, t)) {
        fill = NO_BLD_COLOR;
      }
      else if (map.blocked(t)) {
        fill = BLACK_COLOR;
      }
      else if (area != null) {
        Area under = map.pathCache.rawArea(t);
        if (area == under) {
          fill = WHITE_COLOR;
        }
        else if (Visit.arrayIncludes(around, under)) {
          fill = WALKER_COLOR;
        }
        else if (t.above != null) {
        }
        else if (under != null && under.group == group) {
          fill = MISSED_COLOR;
        }
      }
      graphic[t.x][t.y] = fill;
    }
  }
  
  
  
  private void updateCityFogLayer(AreaMap map) {
    for (Tile t : map.allTiles()) {
      float sight = 0;
      sight += map.fog.sightLevel(t);
      sight += map.fog.maxSightLevel(t);
      
      int fog  = Nums.clamp((int) ((1 - (sight / 2)) * 10), 10);
      int high = Nums.clamp(t.elevation, 10);
      fogLayer[t.x][t.y] = FOG_SCALE[fog][high];
    }
  }
  
  
  private void updateWorldMapView(AreaMap map) {
    World world = map.world;
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
      graphic[x][y] = j.going.first().base().tint;
    }
    
    for (Base city : world.bases) {
      Base lord = city.currentLord();
      int x = (int) city.locale.mapX * 2, y = (int) city.locale.mapY * 2;
      for (Coord c : Visit.grid(x, y, 2, 2, 1)) graphic[c.x][c.y] = city.tint;
      if (lord != null) graphic[x + 1][y] = lord.tint;
    }
    
    try { graphic[hover.x][hover.y] = WHITE_COLOR; }
    catch (Exception e) {}
  }
  
  
  
  

  
  private static Test currentTest = null;
  private static Base currentCity = null;
  
  public static Test currentTest() {
    return currentTest;
  }
  
  public static Base currentCity() {
    return currentCity;
  }
  
  
  public Base runLoop(
    Base city, int numUpdates, boolean graphics, String filename
  ) {
    AreaMap map = city.activeMap();
    int skipUpdate = 0;
    boolean doQuit = false;
    this.filename = filename;
    
    while (! doQuit) {
      
      Test.currentTest = this;
      Test.currentCity = city;
      
      if (graphics) {
        World world = map.world;
        if (! world.settings.worldView) {
          if (world.settings.viewPathMap) {
            updateCityPathingView(map);
          }
          else {
            updateCityMapView(map);
          }
          updateCityFogLayer(map);
          I.present(VIEW_NAME, 400, 400, graphic, fogLayer);
        }
        else {
          updateWorldMapView(map);
          I.present(VIEW_NAME, 400, 400, graphic);
        }
        hover   = I.getDataCursor(VIEW_NAME, false);
        pressed = I.getKeysPressed(VIEW_NAME);
        
        if (! world.settings.worldView) {
          above = map.above(hover.x, hover.y);
          
          //  TODO:  Have actors register within nearby tiles themselves.
          for (Actor a : map.actors) {
            Tile at = a.at();
            if (at.x == hover.x && at.y == hover.y) above = a;
          }
        }
        else {
          above = map.world.onMap(hover.x / 2, hover.y / 2);
        }
        I.talkAbout = above;
        I.used60Frames = (frames++ % 60) == 0;
        
        if (doBuild) {
          I.presentInfo(reportForBuildMenu(map, city), VIEW_NAME);
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
          I.presentInfo(baseReport(map, city), VIEW_NAME);
        }
        
        if (pressed.includes('p')) {
          world.settings.paused = ! world.settings.paused;
        }
        if (doLoad) {
          city = loadCity(city, filename);
          doLoad = false;
          return city;
        }
      }
      
      World world = map.world;
      if (skipUpdate <= 0 && ! world.settings.paused) {
        int iterUpdates = world.settings.speedUp ? 10 : 1;
        for (int i = iterUpdates; i-- > 0;) map.update();
        
        skipUpdate = world.settings.slowed ? 10 : 1;
        if (numUpdates > 0 && --numUpdates == 0) doQuit = true;
      }
      
      if (graphics) {
        try { Thread.sleep(100); }
        catch (Exception e) {}
      }
      skipUpdate -= 1;
    }
    return city;
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
    report.append("\n  Prestige: "+c.prestige());
    
    List <String> borderRep = new List();
    for (Base other : c.world.bases) if (other != c) {
      Base.POSTURE r = c.posture(other);
      float loyalty = c.loyalty(other);
      borderRep.add("\n  "+other+": "+r+", "+Base.descLoyalty(loyalty));
    }
    if (! borderRep.empty()) {
      report.append("\n\nRelations:");
      for (String s : borderRep) report.append(s);
    }
    
    List <String> goodRep = new List();
    for (Good g : c.world.goodTypes) {
      float amount = c.inventory(g);
      float need   = c.needLevel(g);
      float accept = c.prodLevel(g);
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
    
    if (! c.buildLevel.empty()) {
      report.append("\n\nBuilt:");
      for (BuildType t : c.buildLevel.keys()) {
        int level = (int) c.buildLevel.valueFor(t);
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
      report.append(
        "\n  Melee/Range dmg:  "+t.meleeDamage+"/"+t.rangeDamage+
        "\n  Armour class:     "+t.armourClass+
        "\n  Sight/attack rng: "+t.sightRange+"/"+t.rangeDist+
        "\n  Injury:           "+I.shorten(a.injury , 1)+"/"+t.maxHealth+
        "\n  Fatigue:          "+I.shorten(a.fatigue, 1)+"/"+t.maxHealth+
        "\n  Hunger:           "+I.shorten(a.hunger , 1)+"/"+t.maxHealth
      );
      report.append("\n  Task: "+a.jobDesc());
      report.append("\n  Home: "+a.home());
      report.append("\n  Work: "+a.work());
      
      if (! a.carried().empty()) {
        report.append("\n  Carried:");
        for (Good g : a.carried().keys()) {
          report.append("\n    "+g+": "+a.carried(g));
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
    
    if (b.buildLevel() < 1) {
      report.append("\nBuild level:\n  "+I.percent(b.buildLevel()));
    }
    
    //
    //  Finally, present a tally of goods in demand:
    Tally <Good> homeCons = b.homeUsed();
    List <String> goodRep = new List();
    
    for (Good g : b.map.world.goodTypes) {
      float amount   = b.inventory(g);
      float demand   = b.stockLimit(g);
      float consumes = homeCons.valueFor(g);
      if (amount <= 0 && demand <= 0 && consumes <= 0) continue;
      
      demand += consumes;
      goodRep.add("\n  "+g+": "+I.shorten(amount, 1)+"/"+I.shorten(demand, 1));
    }
    
    if (b instanceof BuildingForCrafts) {
      BuildingForCrafts BC = (BuildingForCrafts) b;
      for (BuildingForCrafts.ItemOrder order : BC.orders()) {
        Good g = order.itemType;
        float amount = order.progress;
        goodRep.add("\n  "+g+": "+I.shorten(amount, 1));
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
      if (pressed.includes('s') && builds.canPlace(map, x, y, 0)) {
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
  
  
  private String baseReport(AreaMap map, Base city) {
    StringBuffer report = new StringBuffer("Home City: "+city);
    WorldSettings settings = map.world.settings;
    
    report.append("\n\nFunds: "+city.funds());
    report.append("\n\nTime: "+map.time);
    report.append("\nPaused: "+settings.paused);
    report.append("\n");
    
    float avgHunger = 0;
    for (Actor a : map.actors) avgHunger += a.hunger / a.type().maxHealth;
    avgHunger /= map.actors.size();
    
    report.append("\nTOTAL POPULATION: "+map.actors.size());
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
      settings.viewPathMap = ! settings.viewPathMap;
    }
    report.append("\n(B) build menu");
    if (pressed.includes('b')) {
      doBuild = true;
    }
    
    report.append("\n(P) un/pause");
    report.append("\n(S) save");
    if (pressed.includes('s')) {
      saveCity(city, filename);
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



