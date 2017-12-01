

package game;
import static game.CityMap.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForWalls extends Building {
  
  
  public BuildingForWalls(Type type) {
    super(type);
  }
  
  
  public BuildingForWalls(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  private Tile tileAt(int initX, int initY, int facing) {
    int wide = type.wide - 1, high = type.high - 1;
    int x = 0, y = 0;
    
    switch (facing) {
      case(N): x = initX       ; y = initY       ; break;
      case(E): x = initY       ; y = high - initX; break;
      case(S): x = wide - initX; y = high - initY; break;
      case(W): x = wide - initY; y = initX       ; break;
    }
    
    //I.say("  converted coords: "+initX+"/"+initY+" to: "+x+"/"+y);
    return map.tileAt(x + at().x, y + at().y);
  }
  
  
  boolean checkEntrancesOkay(Tile entrances[]) {
    return true;
  }
  
  
  Tile[] selectEntrances() {
    boolean isTower = type.hasFeature(IS_TOWER);
    boolean isGate  = type.hasFeature(IS_GATE );
    int     facing  = facing();
    //I.say("\nSelecting entrances for "+this);
    
    if (isTower) {
      Tile stair = tileAt(1, -1, facing);
      Tile left  = tileAt(-1, 0, facing);
      Tile right = tileAt(type.wide, 0, facing);
      return new Tile[] { stair, left, right };
    }
    if (isGate) {
      Tile front = tileAt(1, -1, facing);
      Tile back  = tileAt(1, type.high, facing);
      return new Tile[] { front, back };
    }
    return super.selectEntrances();
  }
  
  
  public boolean allowsEntry(Actor a) {
    if (a.homeCity() != map.city && ! a.guest) return false;
    return super.allowsEntry(a);
  }
  
  
}






