

package game;
import start.*;
import static game.GameConstants.*;
import util.*;
import java.lang.reflect.*;




public class WorldScenario extends Scenario {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int MIN_LAIR_DIST = 32, MAX_PLACE_RANGE = 8;
  
  World initWorld;
  Area area;
  AreaConfig config;
  
  MissionExpedition expedition = null;
  List <Objective> objectives = new List();
  List <Building> nests = new List();
  
  
  
  public WorldScenario(AreaConfig config, World world, Area locale) {
    super();
    this.initWorld = world ;
    this.area      = locale;
    this.config    = config;
  }
  
  
  public WorldScenario(Session s) throws Exception {
    super(s);
    
    initWorld = (World) s.loadObject();
    area      = (Area) s.loadObject();
    config    = loadAreaConfig(s);
    
    expedition = (MissionExpedition) s.loadObject();
    s.loadObjects(objectives);
    s.loadObjects(nests);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(initWorld);
    s.saveObject(area);
    saveAreaConfig(config, s);
    
    s.saveObject(expedition);
    s.saveObjects(objectives);
    s.saveObjects(nests);
  }
  
  
  public World world() {
    return initWorld;
  }
  
  
  public Area locale() {
    return area;
  }
  
  
  
  /**  Defining and evaluating objectives-
    */
  final public static int
    COMPLETE_NONE    = -1,
    COMPLETE_FAILED  =  0,
    COMPLETE_SUCCESS =  1
  ;

  public static class Objective extends Constant {
    
    String description;
    
    Class baseClass;
    Method checkMethod;
    
    
    public Objective(
      Class baseClass, String ID, String description, String checkMethod
    ) {
      super(null, ID, IS_STORY);
      
      this.baseClass = baseClass;
      try { this.checkMethod = baseClass.getDeclaredMethod(checkMethod); }
      catch (Exception e) { this.checkMethod = null; }
      
      this.description = description;
    }
    
    public int checkCompletion(Scenario scenario) {
      if (checkMethod == null) return COMPLETE_NONE;
      try { return (Integer) checkMethod.invoke(scenario); }
      catch (Exception e) { return COMPLETE_NONE; }
    }
    
    public String description() {
      return description;
    }
  }
  
  
  public void assignObjectives(Objective... objectives) {
    this.objectives.clear();
    Visit.appendTo(this.objectives, objectives);
  }
  
  
  public Series <Objective> objectives() {
    return objectives;
  }
  
  
  public void assignExpedition(MissionExpedition e, World world) {
    this.expedition = e;
    this.initWorld = world;
  }
  
  
  
  /**  Initialising area-information:
    */
  public static class AreaConfig {
    int mapSize;
    Terrain gradient[];
    List <SiteConfig> sites = new List();
  }
  
  public static AreaConfig areaConfig(
    int mapSize,
    Terrain gradient[],
    SiteConfig... sites
  ) {
    AreaConfig c = new AreaConfig();
    c.mapSize = mapSize;
    c.gradient = gradient;
    for (SiteConfig s : sites) c.sites.add(s);
    return c;
  }
  
  static AreaConfig loadAreaConfig(Session s) throws Exception {
    AreaConfig c = new AreaConfig();
    c.mapSize = s.loadInt();
    c.gradient = (Terrain[]) s.loadObjectArray(Terrain.class);
    for (int n = s.loadInt(); n-- > 0;) c.sites.add(loadSiteConfig(s));
    return c;
  }
  
  static void saveAreaConfig(AreaConfig c, Session s) throws Exception {
    s.saveInt(c.mapSize);
    s.saveObjectArray(c.gradient);
    s.saveInt(c.sites.size());
    for (SiteConfig site : c.sites) saveSiteConfig(site, s);
  }
  
  
  
  /**  Initialising site-information:
    */
  public static class SiteConfig {
    Faction belongs;
    boolean isBase;
    BuildType siteType;
    int minCount, maxCount;
    List <SiteConfig> children = new List();
  }
  
  public static SiteConfig siteConfig(
    Faction faction, boolean isBase, BuildType type, int minCount, int maxCount
  ) {
    SiteConfig c = new SiteConfig();
    c.belongs  = faction;
    c.isBase   = isBase;
    c.siteType = type;
    c.minCount = minCount;
    c.maxCount = maxCount;
    return c;
  }
  
  public static SiteConfig siteConfig(
    SiteConfig parent, BuildType type, int minCount, int maxCount
  ) {
    SiteConfig c = siteConfig(parent.belongs, false, type, minCount, maxCount);
    parent.children.add(c);
    return c;
  }
  
  static SiteConfig loadSiteConfig(Session s) throws Exception {
    SiteConfig c = new SiteConfig();
    c.belongs  = (Faction  ) s.loadObject();
    c.siteType = (BuildType) s.loadObject();
    c.minCount = s.loadInt();
    c.maxCount = s.loadInt();
    for (int n = s.loadInt(); n-- > 0;) c.children.add(loadSiteConfig(s));
    return c;
  }
  
  static void saveSiteConfig(SiteConfig c, Session s) throws Exception {
    s.saveObject(c.belongs );
    s.saveObject(c.siteType);
    s.saveInt(c.minCount);
    s.saveInt(c.maxCount);
    s.saveInt(c.children.size());
    for (SiteConfig site : c.children) saveSiteConfig(site, s);
  }
  
  
  
  /**  Internal setup methods for when the scenario is activated-
    */
  protected World createWorld() {
    return initWorld;
  }
  
  
  protected AreaMap createMap(World world) {
    
    int mapSize = config.mapSize;
    Terrain gradient[] = config.gradient;
    AreaMap map = AreaTerrain.generateTerrain(world, area, mapSize, 0, gradient);
    AreaTerrain.populateFixtures(map);
    
    return map;
  }
  
  
  protected Base createBase(AreaMap stage, World world) {
    
    Base homeland = expedition.homeBase();
    Base landing = new Base(world, stage.area, expedition.faction());
    
    landing.setName("Player Landing");
    landing.initFunds(expedition.funds);
    landing.federation().assignHomeland(homeland);
    landing.assignBuildTypes(expedition.faction().buildTypes());
    
    stage.area.addBase(landing);
    world.addBases(landing);
    
    homeland.trading.updateOffmapTraders();
    return landing;
  }
  
  
  protected void configScenario(World world, AreaMap stage, Base base) {
    
    Building bastion = null;
    AreaTile ideal = stage.tileAt(16, 16);
    Pick <AreaTile> pickLanding = new Pick();
    
    //  TODO:  You need to check to ensure that an area isn't submerged, and
    //  possibly map to a terrain preference.
    
    //
    //  Look for a suitable entry-point within the world...
    for (AreaTile t : stage.allTiles()) {
      AreaTile sited = stage.tileAt(t.x, t.y);
      float dist = AreaMap.distance(sited, ideal);
      pickLanding.compare(t, 0 - dist);
    }
    AreaTile landing = pickLanding.result();
    
    if (landing != null) {
      SiteConfig landSite = siteConfig(expedition.faction(), false, null, 0, 0);
      for (BuildType b : expedition.built) siteConfig(landSite, b, 1, 1);
      bastion = placeSite(landSite, stage, landing, null, base);
    }
    
    if (bastion != null) {
      base.setHeadquarters(bastion);
      AreaTile goes = bastion.centre();
      
      if (! expedition.goods.empty()) {
        bastion.inventory().clear();
        bastion.inventory().add(expedition.goods);
        if (bastion instanceof Trader) {
          Trader t = (Trader) bastion;
          t.needLevels().clear();
          t.needLevels().add(expedition.goods);
        }
      }
      else for (Good g : bastion.needed()) {
        float need = bastion.demandFor(g);
        bastion.setInventory(g, need);
      }
      
      ActorUtils.fillWorkVacancies(bastion);
      
      for (Actor a : expedition.recruits()) {
        a.enterMap(stage, goes.x, goes.y, 1, base);
        a.setInside(bastion, true);
        if (bastion.numWorkers(a.type()) < bastion.maxWorkers(a.type())) {
          bastion.setWorker(a, true);
        }
      }
    }
    
    //
    //  Then look for suitable sites to place other buildings-
    class SiteOption { AreaTile at; float rating; }
    List <SiteOption> options = new List <SiteOption> () {
      protected float queuePriority(SiteOption r) {
        return r.rating;
      }
    };
    for (Coord c : Visit.grid(0, 0, stage.size(), stage.size(), 8)) {
      AreaTile at = stage.tileAt(c);
      float dist = AreaMap.distance(bastion, at);
      if (dist <= MIN_LAIR_DIST) continue;
      
      float rating = 16f * Rand.num() * dist;
      SiteOption option = new SiteOption();
      option.at = at;
      option.rating = rating;
      options.add(option);
    }
    
    options.queueSort();
    
    
    for (SiteConfig site : config.sites) if (options.size() > 0) {
      SiteOption o = options.removeFirst();
      
      Base siteBase = stage.area.firstBaseFor(site.belongs);
      if (site.isBase || siteBase == null) {
        siteBase = new Base(world, stage.area, site.belongs);
        stage.area.addBase(siteBase);
      }
      
      placeSite(site, stage, o.at, null, stage.area.locals);
    }
  }
  
  
  protected Building placeSite(
    SiteConfig site, AreaMap stage, AreaTile at, Building parent, Base base
  ) {
    int numB = Rand.range(site.minCount, site.maxCount);
    Building placed = null, firstKid = null;
    
    if (site.siteType != null) while (numB-- > 0) {
      placed = (Building) site.siteType.generate();
      AreaTile goes = ActorUtils.findEntryPoint(placed, stage, at, MAX_PLACE_RANGE);
      placed.enterMap(stage, goes.x, goes.y, 1, base);
      nests.add(placed);
    }
    
    for (SiteConfig kidSite : site.children) {
      Building kid = placeSite(kidSite, stage, at, placed, base);
      if (firstKid == null) firstKid = kid;
    }
    if (placed == null) placed = firstKid;
    
    return placed;
  }
  
  
  protected Series <Building> nests() {
    return (Series) nests;
  }
  
  
  public void updateScenario() {
    super.updateScenario();
    
    int checkState = COMPLETE_NONE;
    boolean allObjects = true;
    
    for (Objective o : objectives) {
      int check = o.checkCompletion(this);
      if (check == COMPLETE_FAILED ) checkState = COMPLETE_FAILED;
      if (check != COMPLETE_SUCCESS) allObjects = false;
    }
    if (allObjects) checkState = COMPLETE_SUCCESS;
    
    if (checkState != COMPLETE_NONE) {
      //
      //  TODO:  Give visual dialog to let user exit to main screen or continue.
      //  TODO:  If they exit to the main screen, you should have a bonus to
      //  your next expedition based on the last sector's resources, et cetera.
      
      I.say("\nSCENARIO COMPLETE! "+this);
      I.say("  RESULT: "+checkState);
      MainGame.playScenario(null, initWorld);
    }
    
  }
  
  
}









