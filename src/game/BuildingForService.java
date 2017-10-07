

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
  
  
  
}