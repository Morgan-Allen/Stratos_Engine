

package game;
import static game.GameConstants.*;
import static game.World.*;
import static game.RelationSet.*;
import util.*;



public class Base implements Session.Saveable, Trader, RelationSet.Focus {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  String name = "City";
  
  final public World world;
  final public Area area;
  
  private Faction faction;
  private Federation federation = null;
  
  final public BaseCouncil   council   = new BaseCouncil  (this);
  final public BaseRelations relations = new BaseRelations(this);
  final public BaseTrading   trading   = new BaseTrading  (this);
  final public BaseGrowth    growth    = new BaseGrowth   (this);
  
  Building headquarters = null;
  private int currentFunds = 0;
  List <BuildType> techTypes = new List();
  
  List <Mission> missions = new List();
  List <Mission> guarding = new List();
  
  
  
  
  public Base(World world, Area area, Faction faction) {
    this(world, area, faction, "Base???");
  }
  
  
  public Base(World world, Area area, Faction faction, String name) {
    if (world == null) I.complain("CANNOT PASS NULL WORLD:  "+name);
    if (area  == null) I.complain("CANNOT PASS NULL LOCALE: "+name);
    
    this.world   = world  ;
    this.area  = area ;
    this.faction = faction;
    
    this.name = name;
  }
  
  
  public Base(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world   = (World  ) s.loadObject();
    area    = (Area   ) s.loadObject();
    faction = (Faction) s.loadObject();
    
    council  .loadState(s);
    relations.loadState(s);
    trading  .loadState(s);
    growth   .loadState(s);
    
    headquarters = (Building) s.loadObject();
    currentFunds = s.loadInt();
    s.loadObjects(techTypes);
    
    s.loadObjects(missions);
    s.loadObjects(guarding);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveObject(area);
    s.saveObject(faction);
    
    council  .saveState(s);
    relations.saveState(s);
    trading  .saveState(s);
    growth   .saveState(s);
    
    s.saveObject(headquarters);
    s.saveInt(currentFunds);
    s.saveObjects(techTypes);
    
    s.saveObjects(missions);
    s.saveObjects(guarding);
  }
  
  
  
  /**  Supplemental setup/query methods for economy, trade and geography-
    */
  public Faction faction() {
    return faction;
  }
  
  
  public RelationSet relations(World world) {
    return relations;
  }
  
  
  void assignFaction(Faction faction) {
    if (faction != this.faction) federation = null;
    this.faction = faction;
  }
  
  
  public Federation federation() {
    if (federation != null) return federation;
    federation = world.federation(faction);
    return federation;
  }
  
  
  public AreaMap activeMap() {
    return area.activeMap();
  }
  
  
  
  /**  Assigning build-levels:
    */
  public void assignTechTypes(BuildType... types) {
    techTypes.clear();
    Visit.appendTo(techTypes, types);
  }
  
  
  public Series <BuildType> techTypes() {
    return techTypes;
  }
  
  
  
  /**  Accessing missions-
   */
  public Series <Mission> missions() {
    return missions;
  }
  
  public void addMission(Mission mission) {
    missions.include(mission);
  }
  
  public void removeMission(Mission mission) {
    missions.remove(mission);
  }
  
  public Mission matchingMission(int objective, Object focus) {
    for (Mission m : missions) {
      if (m.objective != objective) continue;
      if (m.localFocus() == focus) return m;
      if (m.worldFocus() == focus) return m;
    }
    return null;
  }
  
  void toggleGuarding(Mission m, boolean is) {
    //if (is) I.say("ADDING GUARD: "+this);
    //else    I.say("LOSING GUARD: "+this);
    guarding.toggleMember(m, is);
  }
  
  
  
  /**  Setting up basic relations-
    */
  public void setHeadquarters(Building headquarters) {
    this.headquarters = headquarters;
  }
  
  
  public Building headquarters() {
    return headquarters;
  }
  
  
  
  /**  Setting and accessing tribute and trade levels-
    */
  public void initFunds(int funds) {
    currentFunds = funds;
  }
  
  
  public int funds() {
    return currentFunds;
  }
  
  
  public int incFunds(int inc) {
    currentFunds += inc;
    return currentFunds;
  }
  
  
  public Base base() { return this; }
  
  
  
  
  /**  Regular updates-
    */
  void updateBase() {
    
    //  TODO:  The updateStats flag may never be true if the world is being
    //  updated in time-increments greater than 1!
    
    final AreaMap map = area.activeMap();
    final int UPDATE_GAP = map == null ? DAY_LENGTH : 10;
    boolean updateStats = world.time % UPDATE_GAP == 0;
    boolean activeMap   = map != null;
    boolean playerOwns  = faction == world.playerFaction && activeMap;
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      trading.updateLocalStocks(map);
      growth .updateLocalGrowth(map);
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && ! activeMap) {
      trading.updateOffmapStocks(UPDATE_GAP);
      growth .updateOffmapGrowth(UPDATE_GAP);
      
      for (Mission g : guarding) {
        if (g.worldFocus() != this || g.complete()) toggleGuarding(g, false);
      }
      
      if (relations.isLoyalVassalOf(faction)) {
        if (council.considerRevolt(faction, UPDATE_GAP, this)) {
          relations.toggleRebellion(faction, true);
        }
      }
    }
    //
    //  Either way, we allow prestige and loyalty to return gradually to
    //  defaults over time, and update the council-
    if (updateStats) {
      relations.updateRelations(DAY_LENGTH);
      council.updateCouncil(playerOwns);
    }
    //
    //  And, once per year, tally up any supply obligations to your current
    //  lord (with the possibility of entering a state of revolt if those are
    //  failed.)
    if (world.time % YEAR_LENGTH == 0) {
      relations.updateTribute(YEAR_LENGTH);
      trading.wipeRecords(YEAR_LENGTH);
    }
    //
    //  Update any formations and actors currently active-
    for (Mission f : missions) {
      f.update();
    }
    //
    //  And update traders-
    if (updateStats && ! activeMap) {
      trading.updateOffmapTraders();
    }
  }
  
  

  /**  Last-but-not-least, returning available Powers:
    */
  public Series <ActorTechnique> rulerPowers() {
    Batch <ActorTechnique> all = new Batch();
    if (activeMap() == null) {
      return all;
    }
    for (Building b : activeMap().buildings) if (b.base() == this) {
      for (ActorTechnique t : b.rulerPowers()) if (b.canUsePower(t)) {
        all.include(t);
      }
    }
    return all;
  }
  
  
  
  /**  Boilerplate interface methods-
    */
  public Tally <Good> inventory () { return trading.inventory (); }
  public Tally <Good> needLevels() { return trading.needLevels(); }
  public Tally <Good> prodLevels() { return trading.prodLevels(); }
  
  public float shopPrice(Good good, Task purchase) {
    return trading.shopPrice(good, purchase);
  }
  
  public float importPrice(Good g, Base sells) {
    return trading.importPrice(g, sells);
  }
  
  public float exportPrice(Good g, Base buys) {
    return trading.exportPrice(g, buys);
  }
  
  public boolean allowExport(Good g, Trader buys) {
    return trading.allowExport(g, buys);
  }
  
  
  
  /**  Generating trouble...
    */
  public int posture(Base other) {
    if (other == null) {
      return BOND_NEUTRAL;
    }
    if (other == this) {
      return BOND_ALLY;
    }
    if (other.faction() == this.faction()) {
      Base capital = federation().capital();
      if (this  == capital) return BOND_VASSAL;
      if (other == capital) return BOND_LORD;
      return BOND_ALLY;
    }
    return posture(other.faction());
  }
  
  public int posture(Faction other) {
    return federation().relations.bondProperties(other);
  }
  
  public boolean isVassalOf(Base o) { return posture(o) == BOND_LORD  ; }
  public boolean isLordOf  (Base o) { return posture(o) == BOND_VASSAL; }
  public boolean isEnemyOf (Base o) { return posture(o) == BOND_ENEMY ; }
  public boolean isAllyOf  (Base o) { return posture(o) == BOND_ALLY  ; }
  
  
  public boolean isAllyOrFaction(Base o) {
    return o.faction() == this.faction() || isAllyOf(o);
  }
  
  public boolean isLoyalVassalOf(Base o) {
    return (posture(o) == BOND_LORD) && relations.isLoyalVassal();
  }
  
  
  public Type type() {
    return faction.type();
  }
  


  /**  Graphical, debug and interface methods-
    */
  public String name() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }
  
  
  public int tint() {
    return faction.tint();
  }
  
  
  public static String descLoyalty(float l) {
    Pick <String> pick = new Pick();
    for (int i = BaseRelations.LOYALTIES.length; i-- > 0;) {
      float dist = Nums.abs(l - BaseRelations.LOYALTIES[i]);
      pick.compare(BaseRelations.LOYALTY_DESC[i], 0 - dist);
    }
    return pick.result();
  }
  
  
  public String toString() {
    return name;
  }
}






