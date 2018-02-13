

package game;
import gameUI.play.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class Building extends Element implements Pathing, Employer {
  
  
  /**  Data fields and setup/initialisation-
    */
  static int nextID = 0;
  
  protected String ID;
  
  private int facing = TileConstants.N;
  private Tile entrances[] = new Tile[0];
  
  private int updateGap = 0;
  private City homeCity = null;
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

    facing = s.loadInt();
    int numE = s.loadInt();
    entrances = new Tile[numE];
    for (int i = 0; i < numE; i++) entrances[i] = loadTile(map, s);
    
    updateGap = s.loadInt();
    homeCity = (City) s.loadObject();
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
    
    s.saveInt(facing);
    s.saveInt(entrances.length);
    for (Tile e : entrances) saveTile(e, map, s);
    
    s.saveInt(updateGap);
    s.saveObject(homeCity);
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
  public void setFacing(int facing) {
    this.facing = facing;
  }
  
  
  public int facing() {
    return facing;
  }
  
  
  public void assignHomeCity(City belongs) {
    this.homeCity = belongs;
  }
  
  
  public City homeCity() {
    return homeCity;
  }
  
  
  public void enterMap(CityMap map, int x, int y, float buildLevel, City owns) {
    if (onMap()) {
      I.complain("\nALREADY ON MAP: "+this);
      return;
    }
    if (owns == null) {
      I.complain("\nCANNOT ASSIGN NULL OWNER! "+this);
      return;
    }
    
    super.enterMap(map, x, y, buildLevel, owns);
    map.buildings.add(this);
    assignHomeCity(owns);
  }
  
  
  public void exitMap(CityMap map) {
    
    //I.say("EXITING MAP: "+this);
    
    refreshEntrances(new Tile[0]);
    super.exitMap(map);
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
  public Pathing[] adjacent(Pathing[] temp, CityMap map) {
    if (temp == null) temp = new Pathing[1];
    for (int i = temp.length; i-- > 0;) temp[i] = null;
    if (entrances == null) return temp;
    for (int i = entrances.length; i-- > 0;) temp[i] = entrances[i];
    return temp;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    return Visit.arrayIncludes(entrances, p);
  }
  
  
  public boolean allowsEntry(Actor a) {
    return true;
  }
  
  
  public void setInside(Actor a, boolean is) {
    visitors.toggleMember(a, is);
  }
  
  
  public Series <Actor> inside() {
    return visitors;
  }
  
  
  
  /**  Construction, inventory and upgrade-related methods:
    */
  void onCompletion() {
    refreshEntrances(selectEntrances());
    super.onCompletion();
    updateOnPeriod(0);
  }
  
  
  boolean accessible() {
    return complete() || type().worksBeforeBuilt;
  }
  
  
  float ambience() {
    if (! complete()) return 0;
    return type().ambience;
  }
  
  
  public float setInventory(Good g, float amount) {
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
  
  
  
  /**  Regular updates:
    */
  void update() {
    if (! accessible()) {
      return;
    }
    
    for (int i = 0; i < entrances.length; i++) {
      Tile e = entrances[i];
      if (! checkEntranceOkay(e, i)) {
        refreshEntrances(selectEntrances());
        break;
      }
    }
    
    if (--updateGap <= 0) {
      refreshEntrances(selectEntrances());
      updateOnPeriod(type().updateTime);
      updateWorkers(type().updateTime);
      updateGap = type().updateTime;
    }
  }
  
  
  void updateOnPeriod(int period) {
    return;
  }
  
  
  
  /**  Dealing with entrances and facing:
    */
  protected Tile tileAt(int initX, int initY, int facing) {
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
  
  
  public Tile[] entrances() {
    return entrances;
  }
  
  
  public Tile mainEntrance() {
    if (Visit.empty(entrances)) return null;
    return entrances[0];
  }
  
  
  void refreshEntrances(Tile newE[]) {
    Tile oldE[] = this.entrances;
    
    boolean flagUpdate = true;
    flagUpdate &= Nums.max(oldE.length, newE.length) > 1;
    flagUpdate &= ! Visit.arrayEquals(oldE, newE);
    
    if (flagUpdate) for (Tile e : oldE) {
      map.pathCache.checkPathingChanged(e);
    }
    
    this.entrances = newE;
    
    if (flagUpdate) for (Tile e : newE) {
      map.pathCache.checkPathingChanged(e);
    }
  }
  
  
  boolean checkEntranceOkay(Tile e, int index) {
    if (e.above == this) return true;
    int pathT = e.pathType();
    if (pathT == PATH_FREE || pathT == PATH_PAVE) return true;
    return false;
  }
  
  
  Tile[] selectEntrances() {
    Tile at = at();
    Pick <Tile> pick = new Pick();
    
    for (Coord c : Visit.perimeter(at.x, at.y, type().wide, type().high)) {
      Tile t = map.tileAt(c);
      if (t == null) continue;
      boolean outx = t.x == at.x - 1 || t.x == at.x + type().wide;
      boolean outy = t.y == at.y - 1 || t.y == at.y + type().high;
      if (outx && outy) continue;
      
      int pathT = t.pathType();
      if (pathT != PATH_FREE && pathT != PATH_PAVE) continue;
      
      float rating = 1;
      if (pathT != PATH_PAVE) rating /= 2;
      pick.compare(t, rating);
    }
    
    if (pick.empty()) return new Tile[0];
    return new Tile[] { pick.result() };
  }
  
  
  
  /**  Utility methods for demand-levels, materials and construction/upgrades-
    */
  public float demandFor(Good g) {
    boolean consumes = accessible() && Visit.arrayIncludes(needed(), g);
    float needG = consumes ? stockNeeded(g) : 0;
    float needM = razing() ? 0 : materialNeed(g);
    float hasM  = materialLevel(g);
    float hasG  = inventory.valueFor(g);
    return needM + needG - (hasM + hasG);
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
  
  
  public float stockNeeded(Good need) { return type().maxStock; }
  public float stockLimit (Good made) { return type().maxStock; }
  
  
  public Tally <Good> homeUsed() {
    return new Tally();
  }
  
  
  public float craftProgress() {
    return 0;
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
      if (! hasUpgrade(need)) return false;
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
  
  
  public boolean hasUpgrade(BuildType upgrade) {
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
      if (hasUpgrade(tiers[n])) return tiers[n];
    }
    return null;
  }
  
  
  
  /**  Moderating and udpating recruitment and residency:
    */
  void updateWorkers(int period) {
    for (ActorType w : type().workerTypes.keys()) {
      if (numWorkers(w) < maxWorkers(w) && w.socialClass == CLASS_COMMON) {
        CityBorders.generateMigrant(w, this, false);
      }
    }
  }
  
  
  public int hireCost(ActorType workerType) {
    return workerType.hireCost;
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
    return residents.size();
    //  TODO:  Restore this later once you have multiple housing types...
    //int sum = 0;
    //for (Actor w : residents) if (w.type.socialClass == socialClass) sum++;
    //return sum;
  }
  
  
  public int maxResidents(int socialClass) {
    return type().maxResidents;
    //  TODO:  Restore this later once you have multiple housing types...
    //if (type.homeSocialClass != socialClass) return 0;
    //return type.maxResidents;
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
  
  
  public Series <Actor> recruits() {
    return NO_ACTORS;
  }
  
  
  public void deployOnMission(Mission m, boolean is) {
    return;
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
      return CityMap.adjacent(this, actor);
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
  
  
  public void actorVisits(Actor actor, Building visits) {
    return;
  }
  
  
  
  /**  Handling fog and visibility-
    */
  public float sightLevel() {
    return sightLevel(false);
  }
  
  
  public float maxSightLevel() {
    return sightLevel(true);
  }
  
  
  private float sightLevel(boolean max) {
    Tile at = at();
    float avg = 0;
    for (int x = 2; x-- > 0;) for (int y = 2; y-- > 0;) {
      Tile t = map.tileAt(at.x + (x * type().wide), at.y + (y * type().high));
      avg += max ? map.fog.maxSightLevel(t) : map.fog.sightLevel(t);
    }
    return avg / 4;
  }
  
  
  
  /**  Last-but-not-least, returning available Powers and compiled services:
    */
  public Series <Technique> rulerPowers() {
    Batch <Technique> all = new Batch();
    if (complete()) for (Technique t : type().rulerPowers) {
      all.include(t);
    }
    for (BuildType u : upgrades) if (u != currentUpgrade()) {
      for (Technique t : u.rulerPowers) {
        all.include(t);
      }
    }
    return all;
  }
  
  
  public Series <Good> shopItems() {
    Batch <Good> items = new Batch();
    Visit.appendTo(items, type().shopItems);
    for (BuildType u : upgrades) if (u != currentUpgrade()) {
      Visit.appendTo(items, u.shopItems);
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
    UI.setDetailPane(new VenuePane (UI, this));
    UI.setOptionList(new OptionList(UI, this));
    return true;
  }
  
  
  public void renderElement(Rendering rendering, City base) {
    super.renderElement(rendering, base);
    
    Sprite s = sprite();
    if (s != null) {
      Healthbar h = new Healthbar();
      h.fog = s.fog;
      h.colour = Colour.BLUE;
      h.hurtLevel = 1 - buildLevel();
      h.size = type().maxHealth;
      h.position.setTo(s.position);
      h.position.z += type().deep + 0.5f;
      //h.position.z -= (type().wide - 1) / 2f;
      h.readyFor(rendering);
    }
  }
}



















