

package test;
import util.*;

import java.awt.Color;



public class AltTerrainGenTest implements TileConstants {

  
  final static int colours[] = {
    new Color(0 , 0 , 1f).getRGB(),
    new Color(0 , 1f, 0 ).getRGB(),
    new Color(1f, 1f, 0 ).getRGB(),
    new Color(0.5f, 0.5f, 0.5f).getRGB()
  };
  final static int PATCH = 16, ROUND = 8;
  
  
  int size, res;
  int grid[][];
  int type[][];
  
  static int setVal(int a[][], int x, int y, int v) {
    int size = a.length;
    x = Nums.clamp(x, size);
    y = Nums.clamp(y, size);
    if (v > -1) return a[x][y] = v;
    else return a[x][y];
  }
  
  int gridVal(int x, int y) {
    return setVal(grid, x, y, -1);
  }
  
  int typeVal(int x, int y) {
    return setVal(type, x, y, -1);
  }
  
  
  void doTerrainGenTwo() {

    size = 256;
    res = (size / PATCH) + 1;
    
    int regionsWanted = (res * res) / (4 * 4);
    int lakeAreaWanted = (res * res) / 4;
    
    grid = new int[size][size];
    type = new int[res ][res ];
    
    final boolean display = true;
    final int typeDisplay[][] = new int[res][res];
    
    class Region extends Box2D {
      boolean waterRegion = false;
    };

    Region boxGrid[][] = new Region[res][res];
    List <Region> regions = new List();
    List <Region> working = new List();
    int lakesArea = 0;
    
    
    Region first = new Region();
    first.set(1, 1, res - 2, res - 2);
    working.add(first);
    
    while (working.size() > 0) {
      final Region parent = working.removeFirst();
      
      float splitChance = 1f - (regions.size() * 1f / regionsWanted);
      splitChance += parent.maxSide() / res;
      splitChance /= 2;
      if (parent.maxSide() < 4) splitChance = 0;
      
      float lakeChance = 1 - (parent.maxSide() / (res - 2));
      lakeChance -= lakesArea * 1f / lakeAreaWanted;
      
      
      if (Rand.num() > splitChance) {
        regions.add(parent);
        for (Coord c : Visit.grid(parent)) boxGrid[c.x][c.y] = parent;
        parent.waterRegion = Rand.num() < lakeChance;
        if (parent.waterRegion) lakesArea += parent.area();
      }
      else {
        Region kidA = new Region(), kidB = new Region();
        kidA.setTo(parent);
        kidB.setTo(parent);
        
        if (parent.xdim() > parent.ydim()) {
          int wide = (int) parent.xdim(), cap = Nums.max(1, wide / 4);
          int split = cap + Rand.index(wide - (cap * 2));
          
          kidA.incX(split + 1);
          kidA.incWide(0 - (split + 1));
          kidB.xdim(split);
        }
        else {
          int high = (int) parent.ydim(), cap = Nums.max(1, high / 4);
          int split = cap + Rand.index(high - (cap * 2));
          
          kidA.incY(split + 1);
          kidA.incHigh(0 - (split + 1));
          kidB.ydim(split);
        }
        
        if (Rand.yes()) {
          working.addLast(kidA);
          working.addLast(kidB);
        }
        else {
          working.addLast(kidB);
          working.addLast(kidA);
        }
      }
      
      if (display) {
        for (Coord c : Visit.grid(0, 0, res, res, 1)) {
          typeDisplay[c.x][c.y] = colours[1];
        }
        for (Region r : working) {
          for (Coord c : Visit.perimeter(r)) {
            typeDisplay[c.x][c.y] = colours[3];
          }
        }
        for (Region r : regions) {
          for (Coord c : Visit.perimeter(r)) {
            typeDisplay[c.x][c.y] = colours[3];
          }
        }
        I.present("Area Borders", res * 10, res * 10, typeDisplay);
        
        try { Thread.sleep(10); }
        catch (Exception e) {}
      }
    }
    
    
    final float heightSeed[][] = new float[res][res];
    final int max = res - 1;
    
    for (Coord c : Visit.grid(0, 0, res, res, 1)) {
      Region r = boxGrid[c.x][c.y];
      type[c.x][c.y] = (r != null && r.waterRegion) ? 0 : 1;
    }
    for (Region r : regions) {
      for (Coord c : Visit.perimeter(r)) {
        //
        //  We ensure that the absolute edges of the map are always filled-
        boolean border = c.x == 0 || c.x == max || c.y == 0 || c.y == max;
        int borderType = r.waterRegion ? 1 : 3;
        //
        //  This is needed to ensure borders aren't visited twice when shared
        //  by multiple regions-
        if (c.x > r.xpos() && c.y > r.ypos() && ! border) {
          continue;
        }
        //
        //  Otherwise, decide whether to skip a given border, displace it, or
        //  leave it be-
        int scatter = Rand.index(12);
        if (scatter < 3) {
          if (r.waterRegion) setVal(type, c.x, c.y, 0);
          continue;
        }
        int dir = Nums.clamp(scatter - 3, 5) * 2;
        int x = c.x + T_X[dir], y = c.y + T_Y[dir];
        setVal(type, x, y, borderType);
        if (border) setVal(type, c.x, c.y, borderType);
      }
    }
    
    if (display) {
      for (Coord c : Visit.grid(0, 0, res, res, 1)) {
        typeDisplay[c.x][c.y] = colours[type[c.x][c.y]];
      }
      I.present("Scattered Borders", res * 10, res * 10, typeDisplay);
    }
    
    for (Coord c : Visit.grid(0, 0, res, res, 1)) {
      int typeAt = type[c.x][c.y];
      float high = 0.5f;
      if (typeAt == 0) high = 0;
      if (typeAt == 3) high = 1.0f;
      heightSeed[c.x][c.y] = high;
    }
    
    
    HeightMap mapGrid = new HeightMap(size + 1, heightSeed, 1, 0.5f);
    HeightMap fertile = new HeightMap(size + 1);
    
    final float waterHigh = 0.3f, rocksHigh = 0.7f, treesFert = 0.33f;
    
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      float high = mapGrid.value()[c.x][c.y];
      
      float fert = fertile.value()[c.x][c.y];
      fert = (fert - 0.25f) * 2;
      fert = Nums.min(fert, 0.95f - high);
      boolean tree = fert < treesFert && Rand.index(4) > 0;
      if (fert > treesFert && Rand.index(16) == 0) tree = true;
      
      int index = tree ? 2 : 1;
      if (high < waterHigh) index = 0;
      if (high > rocksHigh) index = 3;
      grid[c.x][c.y] = colours[index];
    }
    
    if (display) {
      I.present("Alt Terrain", size * 2, size * 2, grid);
    }
  }
  
  
  
  public static void main(String args[]) {
    AltTerrainGenTest t = new AltTerrainGenTest();
    t.doTerrainGenTwo();
  }
  
  
}







