

package game;
import static game.AreaMap.*;
import static game.GameConstants.*;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;
import static util.TileConstants.*;



public class Building extends Element implements Pathing, Employer, Carrier {
  
  
  /**  Data fields and setup/initialisation-
    */
  final public static int
    FACE_INIT   = -2,
    FACE_NONE   = -1,
    FACE_NORTH  =  N / 2,
    FACE_EAST   =  E / 2,
    FACE_SOUTH  =  S / 2,
    FACE_WEST   =  W / 2,
    ALL_FACES[] = { FACE_SOUTH, FACE_EAST, FACE_NORTH, FACE_WEST },
    NUM_FACES   =  ALL_FACES.length
  ;
  
  static int nextID = 0;
  
  protected String ID;
  
  private AreaTile entrances[] = new AreaTile[0];
  
  private int lastUpdateTime = -1;
  List <Actor> workers   = new List();
  List <Actor> residents = new List();
  List <Actor> visitors  = new List();
  
  private Tally <Type> materials = new Tally();
  private Tally <Good> inventory = new Tally();
  
  List <BuildType> upgrades = new List();
  private BuildType upgradeAdds = null;
  private BuildType upgradeTake = null;
  
  
  public Building(BuildType type) {
    super(type);
    this.ID = "#"+nextID++;
  }
  
  
  public Building(Session s) throws Exception {
    super(s);
    ID = s.loadString();

    int numE = s.loadInt();
    entrances = new AreaTile[numE];
    for (int i = 0; i < numE; i++) entrances[i] = loadTile(map, s);
    
    lastUpdateTime = s.loadInt();
    s.loadObjects(workers  );
    s.loadObjects(residents);
    s.loadObjects(visitors );
    
    s.loadTally(materials);
    s.loadTally(inventory);
    
    s.loadObjects(upgrades);
    upgradeAdds = (BuildType) s.loadObject();
    upgradeTake = (BuildType) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveString(ID);
    
    s.saveInt(entrances.length);
    for (AreaTile e : entrances) saveTile(e, map, s);
    
    s.saveInt(lastUpdateTime);
    s.saveObjects(workers  );
    s.saveObjects(residents);
    s.saveObjects(visitors );
    
    s.saveTally(materials);
    s.saveTally(inventory);
    
    s.saveObjects(upgrades);
    s.saveObject(upgradeAdds);
    s.saveObject(upgradeTake);
  }
  
  
  public BuildType type() {
    return (BuildType) super.type();
  }
  
  
  
  /**  World entry and exit-
    */
  public boolean isClaimant() {
    return type().claimMargin > 0;
  }
  
  
  public boolean canPlace(AreaMap map) {
    if (! super.canPlace(map)) return false;
    //
    //  TODO:  The efficiency of this might be improved on larger maps.
    boolean claimant = isClaimant();
    Box2D claims = claimArea();
    for (Building b : (claimant ? map.buildings() : map.claimants())) {
      int margin = Nums.max(type().clearMargin, b.type().clearMargin);
      if (b.claimArea().axisDistance(claims) < margin) return false;
    }
    //
    //  TODO:  Make sure you can grab an entrance here as well!
    return true;
  }
  
  
  public Box2D claimArea() {
    if (at() == null) return null;
    AreaTile at = at();
    BuildType t = type();
    Box2D area = new Box2D(at.x, at.y, t.wide, t.high);
    if (t.claimMargin > 0) area.expandBy(t.claimMargin);
    return area;
  }
  
  
  public void enterMap(AreaMap map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    if (isClaimant()) map.claimants.add(this);
    map.buildings.add(this);
  }
  
  
  public void exitMap(AreaMap map) {
    
    //I.say("EXITING MAP: "+this);
    //I.reportStackTrace();
    
    refreshEntrances(new AreaTile[0]);
    super.exitMap(map);
    
    if (isClaimant()) map.claimants.remove(this);
    map.buildings.remove(this);
    
    for (Actor w : workers) if (w.work() == this) {
      w.setWork(null);
    }
    for (Actor w : residents) if (w.home() == this) {
      w.setHome(null);
    }
    for (Actor a : visitors) if (a.inside() == this) {
      a.setInside(this, false);
    }
    
    if (! inventory.empty()) {
      I.say(this+" exited with goods!");
      I.say("  "+inventory);
    }
  }
  
  
  
  /**  Auxiliary pathing-assist methods:
    */
  public Pathing[] adjacent(Pathing[] temp, AreaMap map) {
    
    int numE = entrances == null ? 0 : entrances.length;
    if (temp == null || temp.length < numE) temp = new Pathing[numE];
    
    for (int i = temp.length; i-- > 0;) temp[i] = null;
    if (entrances == null) return temp;
    
    for (int i = entrances.length; i-- > 0;) temp[i] = entrances[i];
    return temp;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    if (! complete()) return false;
    return Visit.arrayIncludes(entrances, p);
  }
  
  
  public boolean allowsEntry(Actor a) {
    return true;
  }
  
  
  public boolean allowsExit(Actor a) {
    return true;
  }
  
  
  public void setInside(Actor a, boolean is) {
    visitors.toggleMember(a, is);
  }
  
  
  public Series <Actor> allInside() {
    return visitors;
  }
  
  
  
  /**  Construction, inventory and upgrade-related methods:
    */
  void onCompletion() {
    refreshEntrances(selectEntrances());
    super.onCompletion();
    updateOnPeriod(0);
  }
  
  
  float ambience() {
    if (! complete()) return 0;
    return type().ambience;
  }
  
  
  public float setInventory(Good g, float amount) {
    if (amount < 0 && g != CASH) amount = 0;
    return inventory.set(g, amount);
  }
  
  
  public float addInventory(float amount, Good g) {
    return inventory.add(amount, g);
  }
  
  
  public void addInventory(Tally <Good> added) {
    inventory.add(added);
  }
  
  
  public float inventory(Good g) {
    return inventory.valueFor(g);
  }
  
  
  public Tally <Good> inventory() {
    return inventory;
  }
  
  
  public float shopPrice(Good g, Task s) {
    if (type().hasFeature(IS_VENDOR)) {
      return g.price * (1 + (MARKET_MARGIN / 100f));
    }
    else {
      return g.price;
    }
  }
  
  
  
  /**  Regular updates:
    */
  void update() {
    if (! complete()) {
      return;
    }
    
    for (int i = 0; i < entrances.length; i++) {
      AreaTile e = entrances[i];
      if (! checkEntranceOkay(e, i)) {
        refreshEntrances(selectEntrances());
        break;
      }
    }
    
    int time = map.time(), maxGap = type().updateTime;
    if (lastUpdateTime == -1 || time - lastUpdateTime > maxGap) {
      refreshEntrances(selectEntrances());
      updateOnPeriod(type().updateTime);
      updateWorkers(type().updateTime);
      lastUpdateTime = time;
    }
    
    if (base() != map.area.locals) {
      AreaFog fog = map.fogMap(this);
      float fogLift = sightRange() + (radius() / 2);
      fog.liftFog(centre(), fogLift);
    }
  }
  
  
  void updateOnPeriod(int period) {
    return;
  }
  
  
  
  /**  Dealing with entrances and facing:
    */
  protected AreaTile tileAt(int initX, int initY, int facing) {
    int wide = type().wide - 1, high = type().high - 1;
    int x = 0, y = 0;
    
    switch (facing) {
      case(N): x = initX       ; y = initY       ; break;
      case(E): x = initY       ; y = high - initX; break;
      case(S): x = wide - initX; y = high - initY; break;
      case(W): x = wide - initY; y = initX       ; break;
    }
    
    return map.tileAt(x + at().x, y + at().y);
  }
  
  
  public AreaTile[] entrances() {
    return entrances;
  }
  
  
  public AreaTile mainEntrance() {
    if (Visit.empty(entrances)) return null;
    return entrances[0];
  }
  
  
  void refreshEntrances(AreaTile newE[]) {
    AreaTile oldE[] = this.entrances;
    
    boolean flagUpdate = true;
    flagUpdate &= Nums.max(oldE.length, newE.length) > 1;
    flagUpdate &= ! Visit.arrayEquals(oldE, newE);
    
    if (flagUpdate) for (AreaTile e : oldE) {
      map.pathCache.checkPathingChanged(e);
    }
    
    this.entrances = newE;
    
    if (flagUpdate) for (AreaTile e : newE) {
      map.pathCache.checkPathingChanged(e);
    }
  }
  
  
  boolean checkEntranceOkay(AreaTile e, int index) {
    if (e.above == this) return true;
    int pathT = e.pathType();
    if (pathT == Type.PATH_FREE || pathT == Type.PATH_PAVE) return true;
    return false;
  }
  
  
  AreaTile[] selectEntrances() {
    AreaTile at = at();
    
    Pick <AreaTile> pick = new Pick <AreaTile> () {
      public void compare(AreaTile t, float rating) {
        if (t == null) return;
        int pathT = t.pathType();
        if (pathT != Type.PATH_FREE && pathT != Type.PATH_PAVE) return;
        if (pathT != Type.PATH_PAVE) rating /= 2;
        
        super.compare(t, rating);
      }
    };
    
    int faceCoords[] = entranceCoords(type().wide, type().high, type().entranceDir);
    {
      AreaTile e = map.tileAt(at.x + faceCoords[0], at.y + faceCoords[1]);
      pick.compare(e, 1);
      if (! pick.empty()) return new AreaTile[] { pick.result() };
    }
    
    for (Coord c : Visit.perimeter(at.x, at.y, type().wide, type().high)) {
      AreaTile t = map.tileAt(c);
      if (t == null) continue;
      boolean outx = t.x == at.x - 1 || t.x == at.x + type().wide;
      boolean outy = t.y == at.y - 1 || t.y == at.y + type().high;
      if (outx && outy) continue;
      pick.compare(t, 1);
    }
    
    if (pick.empty()) return new AreaTile[0];
    return new AreaTile[] { pick.result() };
  }
  
  
  static int[] entranceCoords(int xdim, int ydim, float face) {
    if (face == FACE_NONE) return new int[] { 0, 0 };
    face = (face + 0.5f) % NUM_FACES;
    float edgeVal = face % 1;
    int enterX = 1, enterY = -1;
    
    if (face < FACE_EAST) {
      //  This is the north edge.
      enterX = xdim;
      enterY = (int) (ydim * edgeVal);
    }
    else if (face < FACE_SOUTH) {
      //  This is the east edge.
      enterX = (int) (xdim * (1 - edgeVal));
      enterY = ydim;
    }
    else if (face < FACE_WEST) {
      //  This is the south edge.
      enterX = -1;
      enterY = (int) (ydim * (1 - edgeVal));
    }
    else {
      //  This is the west edge.
      enterX = (int) (xdim * edgeVal);
      enterY = -1;
    }
    return new int[] { enterX, enterY };
  }
  
  
  
  
  /**  Utility methods for demand-levels, materials and construction/upgrades-
    */
  public float demandFor(Good g) {
    float needD = Visit.arrayIncludes(needed(), g) ? stockLimit(g) : 0;
    float needM = razing() ? 0 : materialNeed(g);
    float hasM  = materialLevel(g);
    float hasG  = inventory.valueFor(g);
    return needD + needM - (hasM + hasG);
  }
  
  
  public float stockLimit(Good g) {
    if (! complete()) return 0;
    boolean needs = Visit.arrayIncludes(needed  (), g);
    boolean prods = Visit.arrayIncludes(produced(), g);
    if (needs || prods) return type().maxStock;
    return 0;
  }
  
  
  public float materialNeed(Good g) {
    return materialNeed(g, null);
  }
  
  
  public float setMaterialLevel(Good material, float level) {
    if (level == -1) {
      return materials.valueFor(material);
    }
    else {
      level = materials.set(material, level);
      updateBuildState();
      return level;
    }
  }
  
  
  void updateBuildState() {
    super.updateBuildState();
    if (upgradeAdds != null && buildLevel() >= 1) {
      upgradeAdds = null;
    }
    if (upgradeTake != null && buildLevel() <= 1) {
      upgradeTake = null;
    }
  }
  
  
  public float maxStock(Good g) {
    return type().maxStock;
  }
  
  
  public Good[] needed  () { return type().needed  ; }
  public Good[] produced() { return type().produced; }
  
  
  public Tally <Good> homeUsed() {
    return null;
  }
  
  
  public boolean canBeginUpgrade(BuildType upgrade, boolean takeDown) {
    if (upgrade == type() || (upgrades.includes(upgrade) && ! takeDown)) {
      return false;
    }
    if (! Visit.arrayIncludes(type().upgradeTiers, upgrade)) {
      if (numSideUpgrades() >= maxUpgrades()) return false;
    }
    for (Good g : upgrade.builtFrom) {
      if (! Visit.arrayIncludes(materials(), g)) return false;
    }
    if (takeDown) for (BuildType t : upgrades) {
      if (Visit.arrayIncludes(t.needsAsUpgrade, upgrade)) return false;
    }
    else for (BuildType need : upgrade.needsAsUpgrade) {
      if (! upgradeComplete(need)) return false;
    }
    if (upgradeAdds != null || upgradeTake != null || ! complete()) {
      return false;
    }
    return true;
  }
  
  
  public boolean beginUpgrade(BuildType upgrade) {
    if (! canBeginUpgrade(upgrade, false)) return false;
    upgrades.add(upgrade);
    upgradeAdds = upgrade;
    updateBuildState();
    return true;
  }
  
  
  public boolean beginRemovingUpgrade(BuildType upgrade) {
    if (! canBeginUpgrade(upgrade, true)) return false;
    if (! upgrades.includes(upgrade)    ) return false;
    upgrades.remove(upgrade);
    upgradeTake = upgrade;
    updateBuildState();
    return true;
  }
  
  
  public boolean applyUpgrade(BuildType upgrade) {
    if (! canBeginUpgrade(upgrade, true)) return false;
    upgrades.add(upgrade);
    for (Good g : materials()) setMaterialLevel(g, materialNeed(g));
    updateBuildState();
    return true;
  }
  
  
  public BuildType currentUpgrade() {
    if (upgradeAdds != null) return upgradeAdds;
    if (upgradeTake != null) return upgradeTake;
    return null;
  }
  
  
  public Series <BuildType> upgrades() {
    return upgrades;
  }
  
  
  public int maxUpgrades() {
    int max = type().maxUpgrades;
    for (BuildType b : upgrades) max += b.maxUpgrades;
    return max;
  }
  
  
  public int numSideUpgrades() {
    BuildType tiers[] = type().upgradeTiers;
    int num = 0;
    for (BuildType b : upgrades) if (! Visit.arrayIncludes(tiers, b)) num += 1;
    return num;
  }
  
  
  public boolean upgradeComplete(BuildType upgrade) {
    if (upgrade == null            ) return false;
    if (upgrade == type()          ) return true ;
    if (upgrade == currentUpgrade()) return false;
    return upgrades.includes(upgrade);
  }
  
  
  private float materialNeed(Good g, BuildType exceptUpgrade) {
    float need = super.materialNeed(g);
    
    for (BuildType u : upgrades) {
      if (u == exceptUpgrade) continue;
      need += u.buildNeed(g);
    }
    return need;
  }
  
  
  public float upgradeProgress() {
    BuildType current = currentUpgrade();
    if (current == null) return 0;
    //
    //  Get a tally of all materials required *minus* the currentUpgrade.
    //  Then compare with actual materials received.
    //  Sum up any excess, and compare with sum of materials required for the
    //  upgrade.  Return the fraction as progress.
    float sumNeed = 0, sumHave = 0;
    for (Good g : materials()) {
      float needMinusUpgrade = materialNeed(g, current);
      sumHave += Nums.max(0, materialLevel(g) - needMinusUpgrade);
      sumNeed += current.buildNeed(g);
    }
    return sumHave / sumNeed;
  }
  
  
  public BuildType currentBuildingTier() {
    BuildType tiers[] = type().upgradeTiers;
    for (int n = tiers.length; n-- > 0;) {
      if (upgradeComplete(tiers[n])) return tiers[n];
    }
    return null;
  }
  
  
  
  /**  Moderating and udpating recruitment and residency:
    */
  void updateWorkers(int period) {
    for (ActorType w : type().workerTypes.keys()) {
      if (numWorkers(w) < maxWorkers(w) && w.socialClass == CLASS_COMMON) {
        ActorUtils.generateMigrant(w, this, false);
      }
    }
  }
  
  
  public int numWorkers(ActorType type) {
    int sum = 0;
    for (Actor w : workers) if (w.type() == type) sum++;
    return sum;
  }
  
  
  public int maxWorkers(ActorType w) {
    return (int) type().workerTypes.valueFor(w);
  }
  
  
  public int numResidents(ActorType type) {
    int sum = 0;
    for (Actor w : residents) if (w.type() == type) sum++;
    return sum;
  }
  
  
  public int numResidents(int socialClass) {
    int sum = 0;
    for (Actor a : residents) if (a.type().socialClass == socialClass) sum++;
    return sum;
  }
  
  
  public boolean allowsResidence(Actor actor) {
    int actorClass   = actor.type().socialClass;
    int maxResidents = type().maxResidents;
    int classAllow[] = type().residentClasses;
    
    boolean matchClass = false;
    for (int c : classAllow) if (c == actorClass) matchClass = true;
    if (! matchClass) return false;
    
    int numR = residents.size();
    if (numR >= maxResidents && actor.home() != this) return false;
    return true;
  }
  
  
  public void setWorker(Actor a, boolean is) {
    a.setWork(is ? this : null);
    workers.toggleMember(a, is);
  }
  
  
  public void setResident(Actor a, boolean is) {
    a.setHome(is ? this : null);
    residents.toggleMember(a, is);
  }
  
  
  public Series <Actor> workers() {
    return workers;
  }
  
  
  public Series <Actor> residents() {
    return residents;
  }
  
  
  public Series <Actor> visitors() {
    return visitors;
  }
  
  
  
  /**  Customising actor behaviour-
    */
  public Task selectActorBehaviour(Actor actor) {
    return returnActorHere(actor);
  }
  
  
  Task returnActorHere(Actor actor) {
    if (actorIsHere(actor)) return null;
    Task t = new Task(actor);
    if (complete()) {
      t.configTask(this, this, null, Task.JOB.RETURNING, 0);
    }
    else {
      t.configTask(this, null, mainEntrance(), Task.JOB.RETURNING, 0);
    }
    return t;
  }
  
  
  boolean actorIsHere(Actor actor) {
    if (complete()) {
      return actor.inside() == this;
    }
    else {
      return AreaMap.adjacent(this, actor);
    }
  }
  
  
  public void visitedBy(Actor actor) {
    return;
  }
  
  
  public void actorUpdates(Actor w) {
    return;
  }
  
  
  public void actorPasses(Actor actor, Building other) {
    return;
  }
  
  
  public void actorTargets(Actor actor, Target other) {
    return;
  }
  
  
  public void actorVisits(Actor actor, Pathing visits) {
    return;
  }
  
  
  
  /**  Handling fog and visibility-
    */
  public float sightLevel(Base views) {
    return sightLevel(false, views);
  }
  
  
  public float maxSightLevel(Base views) {
    return sightLevel(true, views);
  }
  
  
  private float sightLevel(boolean max, Base views) {
    AreaFog fog = map.fogMap(views.faction(), false);
    if (fog == null) return 1;
    
    AreaTile at = at();
    float avg = 0;
    for (int x = 2; x-- > 0;) for (int y = 2; y-- > 0;) {
      AreaTile t = map.tileAt(at.x + (x * type().wide), at.y + (y * type().high));
      avg += max ? fog.maxSightLevel(t) : fog.sightLevel(t);
    }
    return avg / 4;
  }
  
  
  
  /**  Last-but-not-least, returning available Powers and compiled services:
    */
  public Series <ActorTechnique> rulerPowers() {
    Batch <ActorTechnique> all = new Batch();
    if (complete()) for (ActorTechnique t : type().rulerPowers) {
      all.include(t);
    }
    for (BuildType u : upgrades) if (u != currentUpgrade()) {
      for (ActorTechnique t : u.rulerPowers) {
        all.include(t);
      }
    }
    return all;
  }
  
  
  public boolean canUsePower(ActorTechnique t) {
    return true;
  }
  
  
  public Series <Good> shopItems() {
    Batch <Good> items = new Batch();
    
    for (Good g : type().shopItems) items.include(g);
    for (Recipe r : type().recipes) items.include(r.made);
    
    for (BuildType u : upgrades) if (u != currentUpgrade()) {
      for (Good g : u.shopItems) items.include(g);
      for (Recipe r : u.recipes) items.include(r.made);
    }
    
    return items;
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  protected boolean reports() {
    return I.talkAbout == this;
  }
  
  
  public String ID() {
    return ID;
  }
  
  
  public void setID(String newID) {
    this.ID = newID;
  }
  
  
  public String fullName() {
    return toString();
  }
  
  
  public String toString() {
    return type().name+" "+ID;
  }
  
  
  public boolean setSelected(PlayUI UI) {
    UI.setDetailPane(new PaneBuilding(UI, this));
    return true;
  }
  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    Box2D area = area();
    Selection.renderSelectSquare(
      this, area, rendering, Colour.transparency(hovered ? 0.25f : 0.25f)
    );
  }
  
  
  public Sprite sprite() {
    BuildType type = type();
    ModelAsset foundM = type.foundationModel;
    boolean showDone = complete() || foundM == null || ! onMap();
    
    if (sprite == null || (sprite.model() == foundM) != ! showDone) {
      if (showDone) sprite = type.makeSpriteFor(this);
      else sprite = foundM.makeSprite();
      if (sprite != null) type.prepareMedia(sprite, this);
    }
    return sprite;
  }
  
  
  public void renderElement(Rendering rendering, Base base) {
    super.renderElement(rendering, base);
    
    Sprite s = sprite();
    if (s != null) {
      Healthbar h = new Healthbar();
      h.fog = s.fog;
      h.colour = Colour.BLUE;
      h.hurtLevel = 1 - buildLevel();
      h.size = (type().wide + type().high) * 30 / 2f;
      h.position.setTo(s.position);
      h.position.z += type().deep + 0.25f;
      h.readyFor(rendering);
    }
  }
}




