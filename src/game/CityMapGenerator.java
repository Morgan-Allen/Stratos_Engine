

package game;
import static game.GameConstants.*;
import static game.CityMap.*;
import util.*;



public class CityMapGenerator {
  
  
  public static CityMap generateTerrain(
    City city, int size, Terrain... gradient
  ) {
    CityMap map = new CityMap(city);
    map.performSetup(size);
    populateTerrain(map, gradient);
    return map;
  }
  
  
  public static void populateTerrain(CityMap map, Terrain... gradient) {
    
    HeightMap mapH = new HeightMap(map.size, 1, 0.5f);
    int numG = gradient.length;
    
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Tile    tile = map.tileAt(c.x, c.y);
      float   high = mapH.value()[c.x][c.y];
      Terrain terr = gradient[Nums.clamp((int) (high * numG), numG)];
      
      tile.terrain = terr;
    }
  }
  
  
  public static void populateFixtures(CityMap map) {
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Tile    tile = map.tileAt(c.x, c.y);
      Terrain terr = tile.terrain;
      
      for (int i = terr.fixtures.length; i-- > 0;) {
        ObjectType t = terr.fixtures[i];
        float      w = terr.weights [i] / (t.wide * t.high);
        
        if (Rand.num() < w && checkPlacingOkay(tile, t, map)) {
          Fixture f = (Fixture) t.generate();
          f.enterMap(map, tile.x, tile.y, 1);
        }
      }
    }
  }
  
  
  public static void populateFixture(ObjectType t, int x, int y, CityMap map) {
    Fixture f = (Fixture) t.generate();
    f.enterMap(map, x, y, 1);
  }
  
  
  static boolean checkPlacingOkay(Tile at, ObjectType t, CityMap map) {
    
    for (Coord c : Visit.grid(at.x, at.y, t.wide, t.high, 1)) {
      Tile u = map.tileAt(c.x, c.y);
      if (u == null || u.above != null) return false;
    }
    
    int inGap = -1, firstTile = -1, numGaps = 0;
    //I.say("\nChecking...");
    
    for (Coord c : Visit.perimeter(at.x, at.y, t.wide, t.high)) {
      
      Tile tile = map.tileAt(c.x, c.y);
      boolean blocked = map.blocked(c.x, c.y) || tile.above != null;
      if (firstTile == -1) firstTile = blocked ? 0 : 1;
      //I.say("  B: "+blocked);
      
      if (blocked) {
        if (inGap != 0) {
          inGap = 0;
        }
      }
      else {
        if (inGap != 1) {
          inGap = 1;
          numGaps += 1;
        }
      }
    }
    
    if (inGap == 1 && firstTile == inGap) numGaps -= 1;
    //I.say("Total gaps: "+numGaps);
    
    return numGaps < 2;
  }
  
}







