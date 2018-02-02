

package game;
import static game.CityMap.*;
import static game.GameConstants.*;



public class BuildingForWalls extends Building {
  
  
  boolean tower, gate;
  
  
  public BuildingForWalls(Type type) {
    super(type);
    tower = type.hasFeature(IS_TOWER);
    gate  = type.hasFeature(IS_GATE );
  }
  
  
  public BuildingForWalls(Session s) throws Exception {
    super(s);
    tower = s.loadBool();
    gate  = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveBool(tower);
    s.saveBool(gate );
  }
  
  
  boolean checkEntranceOkay(Tile e, int index) {
    if (super.checkEntranceOkay(e, index)) return true;
    if (tower && index > 0 && e.pathType() == PATH_WALLS) return true;
    return false;
  }
  
  
  Tile[] selectEntrances() {
    int facing = facing();
    
    //  TODO:  Scrub any null entrances from these lists!
    
    if (tower) {
      Tile stair = tileAt(1, -1, facing);
      Tile left  = tileAt(-1, 0, facing);
      Tile right = tileAt(type().wide, 0, facing);
      return new Tile[] { stair, left, right };
    }
    if (gate) {
      Tile front = tileAt(1, -1, facing);
      Tile back  = tileAt(1, type().high, facing);
      return new Tile[] { front, back };
    }
    return super.selectEntrances();
  }
  
  
  public boolean allowsEntry(Actor a) {
    City owner = homeCity();
    if (a.homeCity() != owner && a.guestCity() != owner) {
      return false;
    }
    return super.allowsEntry(a);
  }
  
  
}






