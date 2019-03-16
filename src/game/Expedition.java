/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import util.*;



//  TODO:  Merge this with the MissionForColony class?


public class Expedition implements Session.Saveable {
  

  /**  Data fields, constants, constructors and save/load methods-
    */
  Faction faction;
  Base homeland = null;
  Area landing = null;
  
  int funds = 0;
  BuildType built[] = {};
  Tally <Good> goods = new Tally();
  
  Actor leader = null;
  List <Actor> staff = new List();
  
  
  
  public Expedition() {
    return;
  }
  
  
  public Expedition(Session s) throws Exception {
    s.cacheInstance(this);
    
    faction  = (Faction) s.loadObject();
    homeland = (Base) s.loadObject();
    landing   = (Area) s.loadObject();
    
    funds    = s.loadInt();
    built    = (BuildType[]) s.loadObjectArray(BuildType.class);
    s.loadTally(goods);
    
    leader = (Actor) s.loadObject();
    s.loadObjects(staff);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(faction);
    s.saveObject(homeland);
    s.saveObject(landing);
    
    s.saveInt(funds);
    s.saveObjectArray(built);
    s.saveTally(goods);
    
    s.saveObject(leader);
    s.saveObjects(staff);
  }
  
  
  
  /**  Basic no-brainer access methods-
    */
  public Actor leader() { return leader; }
  public Series <Actor> staff() { return staff; }
  
  public Base homeland() { return homeland; }
  public Area landing() { return landing; }
  
  
  
  /**  Configuration utilities for external use-
    */
  public void configTravel(Base homeland, Area landing) {
    setHomeland(homeland);
    setLanding(landing);
  }
  
  
  public void setHomeland(Base homeland) {
    this.homeland = homeland;
  }
  
  public void setLanding(Area landing) {
    this.landing = landing;
  }
  
  
  public void configAssets(
    Faction faction, int funds,
    Tally <Good> goods, BuildType... buildings
  ) {
    this.faction = faction;
    this.funds   = funds;
    this.built   = buildings;
    this.goods.clear();
    this.goods.add(goods);
  }
  
  
  public void configStaff(Actor leader, Series <Actor> staff) {
    this.leader = leader;
    this.staff.clear();
    Visit.appendTo(this.staff, staff);
  }
  
  
  public void toggleStaff(Actor a, boolean on) {
    staff.toggleMember(a, on);
  }
  
}





