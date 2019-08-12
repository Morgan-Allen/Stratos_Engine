

package test;
import util.*;

import java.awt.Color;



public class AltTerrainGenTest implements TileConstants {

  
  final static int
    IND_WATER = 0,
    IND_SHORE = 1,
    IND_LAND  = 2,
    IND_TREES = 3,
    IND_ROCKS = 4
  ;
  
  
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
  
  
  void doTerrainGen(int mapSize, int seedSize, int gradient[]) {

    size = mapSize;
    res = seedSize + 1;
    
    int regionsWanted = (res * res) / (4 * 4);
    int lakeAreaWanted = (res * res) / 4;
    
    grid = new int[size][size];
    type = new int[res ][res ];
    
    final boolean display = true;
    final int typeDisplay[][] = new int[res][res];
    
    class Region extends Box2D {
      boolean waterRegion = false;
      float sizeRating = 0;
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
      splitChance += parent.maxSide() / (res - 2);
      splitChance /= 2;
      if (parent.maxSide() < 4) splitChance = 0;
      boolean doSplit = Rand.num() <= splitChance;
      
      if (display) {
        I.say("Parent region: "+parent+", splitting: "+doSplit);
      }
      
      if (! doSplit) {
        regions.add(parent);
        for (Coord c : Visit.grid(parent)) boxGrid[c.x][c.y] = parent;
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
      
      if (working.empty()) {
        List <Region> regionSort = new List <Region> () {
          protected float queuePriority(Region r) {
            return r.sizeRating;
          }
        };
        for (Region r : regions) {
          r.sizeRating = Rand.num();
          regionSort.add(r);
        }
        regionSort.queueSort();
        
        for (Region r : regionSort) {
          float area = (r.xdim() + 1) * (r.ydim() + 1);
          if ((lakesArea + (area / 2)) >= lakeAreaWanted) break;
          
          r.waterRegion = true;
          lakesArea += area;
          
          if (display) {
            I.say("Lakes area: "+lakesArea+"/"+lakeAreaWanted);
          }
        }
      }
      
      if (display) {
        for (Coord c : Visit.grid(0, 0, res, res, 1)) {
          Region r = boxGrid[c.x][c.y];
          int tone = gradient[IND_LAND];
          if (r != null && r.waterRegion) tone = gradient[IND_WATER];
          typeDisplay[c.x][c.y] = tone;
        }
        for (Region r : working) {
          for (Coord c : Visit.perimeter(r)) {
            typeDisplay[c.x][c.y] = gradient[IND_ROCKS];
          }
        }
        for (Region r : regions) {
          for (Coord c : Visit.perimeter(r)) {
            typeDisplay[c.x][c.y] = gradient[IND_ROCKS];
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
      type[c.x][c.y] = (r != null && r.waterRegion) ? IND_WATER : IND_LAND;
    }
    for (Region r : regions) {
      for (Coord c : Visit.perimeter(r)) {
        //
        //  We ensure that the absolute edges of the map are always filled-
        boolean border = c.x == 0 || c.x == max || c.y == 0 || c.y == max;
        int borderType = r.waterRegion ? IND_WATER : IND_ROCKS;
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
          if (r.waterRegion) setVal(type, c.x, c.y, IND_LAND);
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
        typeDisplay[c.x][c.y] = gradient[type[c.x][c.y]];
      }
      I.present("Scattered Borders", res * 10, res * 10, typeDisplay);
    }
    
    for (Coord c : Visit.grid(0, 0, res, res, 1)) {
      int typeAt = type[c.x][c.y];
      float high = 0.5f;
      if (typeAt == IND_WATER) high = 0;
      if (typeAt == IND_ROCKS) high = 1.0f;
      heightSeed[c.x][c.y] = high;
    }
    
    
    HeightMap mapGrid = new HeightMap(size + 1, heightSeed, 1, 0.5f);
    HeightMap fertile = new HeightMap(size + 1);
    
    final float waterHigh = 0.33f, rocksHigh = 0.66f;
    final float treesFert = 0.40f, shoreHigh = 0.38f;
    
    for (Coord c : Visit.grid(0, 0, size, size, 1)) {
      float high = mapGrid.value()[c.x][c.y];
      float waterDist = (high - waterHigh) * (1 - waterHigh) * 0.5f;
      
      float fert = fertile.value()[c.x][c.y];
      fert = (fert - 0.25f) * 2;
      fert = Nums.min(fert, 0.95f - high);
      boolean tree = fert < treesFert && (Rand.index(4) > 2 || high > 0.61f);
      if (fert > treesFert && Rand.num() < waterDist) tree = true;
      
      int index = tree ? IND_TREES : IND_LAND;
      if (high < shoreHigh) index = IND_SHORE;
      if (high < waterHigh) index = IND_WATER;
      if (high > rocksHigh) index = IND_ROCKS;
      
      grid[c.x][c.y] = gradient[index];
    }
    
    if (display) {
      I.present("Alt Terrain", 512, 512, grid);
    }
  }
  
  
  final static int colours_tropic[] = {
    new Color(0.0f, 0.0f, 0.7f).getRGB(),
    new Color(0.8f, 0.8f, 0.7f).getRGB(),
    new Color(0.3f, 0.5f, 0.1f).getRGB(),
    new Color(0.0f, 0.4f, 0.0f).getRGB(),
    new Color(0.2f, 0.3f, 0.3f).getRGB()
  };
  
  final static int colours_tundra[] = {
    new Color(0.0f, 0.2f, 0.5f).getRGB(),
    new Color(0.7f, 0.7f, 0.8f).getRGB(),
    new Color(0.5f, 0.5f, 0.6f).getRGB(),
    new Color(0.0f, 0.2f, 0.1f).getRGB(),
    new Color(0.7f, 0.7f, 0.8f).getRGB()
  };
  
  final static int colours_desert[] = {
    new Color(0.0f, 0.2f, 0.5f).getRGB(),
    new Color(0.5f, 0.5f, 0.2f).getRGB(),
    new Color(0.5f, 0.4f, 0.2f).getRGB(),
    new Color(0.0f, 0.3f, 0.1f).getRGB(),
    new Color(0.3f, 0.3f, 0.3f).getRGB()
  };
  
  final static int colours_wastes[] = {
    new Color(0.3f, 0.3f, 0.4f).getRGB(),
    new Color(0.5f, 0.5f, 0.2f).getRGB(),
    new Color(0.3f, 0.3f, 0.3f).getRGB(),
    new Color(0.4f, 0.3f, 0.1f).getRGB(),
    new Color(0.2f, 0.2f, 0.2f).getRGB()
  };
  
  
  public static void main(String args[]) {
    AltTerrainGenTest t = new AltTerrainGenTest();
    t.doTerrainGen(256, 16, colours_tropic);
  }
  
  
}





