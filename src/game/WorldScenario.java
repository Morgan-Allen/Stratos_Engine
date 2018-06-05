

package game;
import start.*;
import static game.GameConstants.*;
import util.*;
import java.lang.reflect.*;




public class WorldScenario extends Scenario {
  
  
  /**  Data fields, construction and save/load methods-
    */
  World initWorld;
  WorldLocale locale;
  AreaConfig config;
  
  Faction landFaction;
  int landFunds = 0;
  Base landHomeland = null;
  BuildType landBuilt[] = {};
  Tally <Good> landGoods = new Tally();
  List <Actor> landStaff = new List();
  List <Objective> objectives = new List();
  
  List <Building> nests = new List();
  
  String savePath = "saves/save_locale.str";
  
  
  
  public WorldScenario(AreaConfig config, World world, WorldLocale locale) {
    super();
    this.initWorld = world ;
    this.locale    = locale;
    this.config    = config;
  }
  
  
  public WorldScenario(Session s) throws Exception {
    super(s);
    
    initWorld = (World) s.loadObject();
    locale    = (WorldLocale) s.loadObject();
    config    = loadAreaConfig(s);
    
    landFaction  = (Faction) s.loadObject();
    landFunds    = s.loadInt();
    landHomeland = (Base) s.loadObject();
    landBuilt    = (BuildType[]) s.loadObjectArray(BuildType.class);
    s.loadTally(landGoods);
    s.loadObjects(landStaff);
    s.loadObjects(objectives);
    
    s.loadObjects(nests);
    
    savePath = s.loadString();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(initWorld);
    s.saveObject(locale);
    saveAreaConfig(config, s);
    
    s.saveObject(landFaction);
    s.saveInt(landFunds);
    s.saveObject(landHomeland);
    s.saveObjectArray(landBuilt);
    s.saveTally(landGoods);
    s.saveObjects(landStaff);
    s.saveObjects(objectives);
    
    s.saveObjects(nests);
    
    s.saveString(savePath);
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
    BuildType siteType;
    int minCount, maxCount;
    List <SiteConfig> children = new List();
  }
  
  public static SiteConfig siteConfig(
    Faction faction, BuildType type, int minCount, int maxCount
  ) {
    SiteConfig c = new SiteConfig();
    c.belongs  = faction;
    c.siteType = type;
    c.minCount = minCount;
    c.maxCount = maxCount;
    return c;
  }
  
  public static SiteConfig siteConfig(
    SiteConfig parent, BuildType type, int minCount, int maxCount
  ) {
    SiteConfig c = siteConfig(parent.belongs, type, minCount, maxCount);
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
  

  /**  Supplemental setup methods that allow for player-entry and objectives-
    */
  public void setPlayerLanding(
    Faction faction, int funds, Base homeland,
    Series <Actor> staff, Tally <Good> goods, BuildType... buildings
  ) {
    this.landFaction  = faction;
    this.landFunds    = funds;
    this.landHomeland = homeland;
    this.landBuilt    = buildings;
    landGoods.clear();
    landGoods.add(goods);
    landStaff.clear();
    Visit.appendTo(landStaff, staff);
  }
  
  
  public void assignObjectives(Objective... objectives) {
    this.objectives.clear();
    Visit.appendTo(this.objectives, objectives);
  }
  
  
  public Series <Objective> objectives() {
    return objectives;
  }
  
  
  public void assignSavePath(String savePath) {
    this.savePath = savePath;
  }
  
  
  
  /**  Internal setup methods for when the scenario is activated-
    */
  protected String savePath() {
    return savePath;
  }
  
  
  protected World createWorld() {
    return initWorld;
  }
  
  
  protected Area createArea(World world) {
    
    int mapSize = config.mapSize;
    Terrain gradient[] = config.gradient;
    Area map = AreaTerrain.generateTerrain(world, locale, mapSize, 0, gradient);
    AreaTerrain.populateFixtures(map);
    
    return map;
  }
  
  
  protected Base createBase(Area stage, World world) {
    
    Base landing = new Base(world, stage.locale);
    landing.setName("Player Landing");
    landing.initFunds(landFunds);
    landing.setHomeland(landHomeland);
    landing.assignBuildTypes(landFaction.buildTypes());
    
    stage.addBase(landing);
    world.addBases(landing);
    
    Base.setPosture(landHomeland, landing, Base.POSTURE.VASSAL, true);
    landHomeland.updateOffmapTraders();
    
    return landing;
  }
  

  final static int MIN_LAIR_DIST = 32, MAX_PLACE_RANGE = 8;
  
  
  protected void configScenario(World world, Area stage, Base base) {
    
    Building bastion = null;
    AreaTile ideal = stage.tileAt(16, 16);
    Pick <AreaTile> pickLanding = new Pick();
    
    //  TODO:  You need to check to ensure that an area isn't submerged, and
    //  possibly map to a terrain preference.
    
    //
    //  Look for a suitable entry-point within the world...
    for (AreaTile t : stage.allTiles()) {
      AreaTile sited = stage.tileAt(t.x, t.y);
      float dist = Area.distance(sited, ideal);
      pickLanding.compare(t, 0 - dist);
    }
    AreaTile landing = pickLanding.result();
    
    if (landing != null) {
      SiteConfig landSite = siteConfig(landFaction, null, 0, 0);
      for (BuildType b : landBuilt) siteConfig(landSite, b, 1, 1);
      bastion = placeSite(landSite, stage, landing, null, base);
    }
    
    if (bastion != null) {
      base.setHeadquarters(bastion);
      AreaTile goes = bastion.centre();
      
      if (! landGoods.empty()) {
        bastion.inventory().clear();
        bastion.inventory().add(landGoods);
        if (bastion instanceof Trader) {
          Trader t = (Trader) bastion;
          t.needLevels().clear();
          t.needLevels().add(landGoods);
        }
      }
      else for (Good g : bastion.needed()) {
        float need = bastion.demandFor(g);
        bastion.setInventory(g, need);
      }
      
      if (landStaff == null || landStaff.empty()) {
        ActorUtils.fillWorkVacancies(bastion);
      }
      else for (Actor a : landStaff) {
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
      float dist = Area.distance(bastion, at);
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
      placeSite(site, stage, o.at, null, stage.locals);
    }
  }
  
  
  protected Building placeSite(
    SiteConfig site, Area stage, AreaTile at, Building parent, Base base
  ) {
    int numB = Rand.range(site.minCount, site.maxCount);
    Building placed = null, firstKid = null;
    
    if (site.siteType != null) while (numB-- > 0) {
      placed = (Building) site.siteType.generate();
      AreaTile goes = ActorUtils.findEntryPoint(placed, stage, at, MAX_PLACE_RANGE);
      placed.enterMap(stage, goes.x, goes.y, 1, base);
      
      if (site.siteType.isNestBuilding()) {
        BuildingForNest nest = (BuildingForNest) placed;
        nest.parent = parent;
      }
      
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
      //  TODO:  Exit to main screen or give option to continue!
      I.say("\nSCENARIO COMPLETE! "+this);
      I.say("  RESULT: "+checkState);
      MainGame.playScenario(null);
    }
    
  }
  
  
}









