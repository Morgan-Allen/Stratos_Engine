

package game;
import static game.World.*;
import static game.GameConstants.*;
import start.*;
import util.*;




public class MissionExpedition extends Mission {
  
  
  /**  Data-fields, construction and save/load methods-
    */
  boolean withPlayer;
  
  int funds = 0;
  BuildType built[] = {};
  Tally <Good> goods = new Tally();
  
  Actor leader = null;
  
  
  
  public MissionExpedition(Base belongs, boolean playing) {
    super(Mission.OBJECTIVE_COLONY, belongs);
    withPlayer = playing;
  }
  
  
  public MissionExpedition(Session s) throws Exception {
    super(s);
    
    funds    = s.loadInt();
    built    = (BuildType[]) s.loadObjectArray(BuildType.class);
    s.loadTally(goods);
    
    leader = (Actor) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveInt(funds);
    s.saveObjectArray(built);
    s.saveTally(goods);
    
    s.saveObject(leader);
  }
  
  
  
  /**  Basic no-brainer access methods-
    */
  public void setPlaying(boolean playing) {
    this.withPlayer = playing;
  }
  
  
  public Actor leader() {
    return leader;
  }
  
  
  public Faction faction() {
    return homeBase().faction();
  }
  
  
  public void configAssets(
    int funds, Tally <Good> goods, BuildType... buildings
  ) {
    if (Visit.empty(buildings)) buildings = new BuildType[0];
    
    this.funds = funds;
    this.built = buildings;
    this.goods.clear();
    this.goods.add(goods);
  }
  
  
  public void configStaff(Actor leader, Series <Actor> staff) {
    this.leader = leader;
    this.recruits.clear();
    Visit.appendTo(recruits, staff);
  }
  
  
  
  
  /**  Mission-configuration-
    */
  
  public boolean allowsFocus(Object focus) {
    Area a = (Area) I.cast(focus, Area.class);
    return a != null && a.notSettled();
  }
  
  
  Task nextLocalMapBehaviour(Actor actor) {
    return null;
  }
  
  
  void handleOffmapDeparture(Area from, Journey journey) {
    return;
  }
  
  
  void handleOffmapArrival(Area goes, Journey journey) {
    
    World world = homeBase().world;
    WorldScenario scenario = world.scenarioFor(goes);
    
    //  TODO:  Scrub any connection that party-members have with the previous
    //  area and/or actors therein.
    
    if (scenario != null && withPlayer) {
      Scenario old = MainGame.currentScenario();
      if (old != null) old.wipeScenario();
      
      for (Actor a : recruits) {
        //  TODO:  Implement this...
        //a.detachFromMap();
      }
      
      scenario.assignExpedition(this, world);
      scenario.initScenario(MainGame.mainGame());
      MainGame.playScenario(scenario, world);
      
      disbandMission();
    }
    
    else {
      
      Tally <BuildType> buildLevels = new Tally();
      for (BuildType t : built) buildLevels.add(1, t);
      BuildType techTypes[] = homeBase().techTypes().toArray(BuildType.class);
      
      Base landing = new Base(world, goes, faction());
      world.addBases(landing);
      landing.assignTechTypes(techTypes);
      
      landing.initFunds(funds);
      landing.trading.inventory().add(goods);
      landing.growth.initBuildLevels(buildLevels);
      
      disbandMission();
    }
    
  }
  
  
}





