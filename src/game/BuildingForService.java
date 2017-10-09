

package game;



public class BuildingForService extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForService(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForService(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Assigning walker behaviours:
    */
  //  Water, sanitation, and taxes.  Buildings advertise their need for this,
  //  and someone comes along to satisfy it.  Job done.
  
  
  public void selectWalkerBehaviour(Walker walker) {
  }
  
  
  public void walkerPasses(Walker walker, Building other) {
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
  }
  
}







