
package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForWalls extends Building {
  
  
  int facing;
  
  
  public BuildingForWalls(Type type) {
    super(type);
  }
  
  
  public BuildingForWalls(Session s) throws Exception {
    super(s);
    facing = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(facing);
  }
  
  
  
  private Coord rotatedCoord(int initX, int initY) {
    int wide = type.wide, high = type.high;
    int x = 0, y = 0;
    switch (facing) {
      case(N):
      break;
      case(E):
        x = initY;
        y = high - initX;
      break;
      case(S):
        x = wide - initX;
        y = high - initY;
      break;
      case(W):
        x = wide - initY;
        y = initX;
      break;
    }
    return new Coord(x + at().x, y + at().y);
  }
  
  
  Tile[] selectEntrances() {
    boolean isTower = type.hasFeature(IS_TOWER);
    boolean isGate  = type.hasFeature(IS_GATE );
    
    if (isTower) {
      Coord door  = rotatedCoord(1, -1);
      Coord hatch = rotatedCoord(1, 1);
      return new Tile[] { map.tileAt(door), map.tileAt(hatch) };
    }
    if (isGate) {
      Coord front = rotatedCoord(1, -1);
      Coord back  = rotatedCoord(1, type.high);
      return new Tile[] { map.tileAt(front), map.tileAt(back) };
    }
    return super.selectEntrances();
  }
  
  
  public boolean allowsEntry(Actor a) {
    if (a.homeCity() != map.city && ! a.guest) return false;
    return super.allowsEntry(a);
  }
  
  
}






