

package graphics.terrain;
import graphics.common.*;
import util.*;
import com.badlogic.gdx.graphics.*;



public class FlatTerrain implements TileConstants {
  
  public static final int
    VERTEX_SIZE = 3 + 3 + 2,  //  (position, normal and texture coords.)
    SIZE = 4 * VERTEX_SIZE,   //  (4 vertices, 1 per corner.)
    X0 = 0, Y0 = 1, Z0 = 2,
    N1 = 3, N2 = 4, N3 = 5,
    U0 = 6, V0 = 7,
    CHUNK_SIZE = 16
  ;
  
  //  The diagonal is root-2.
  //      .
  // \   /
  //   .
  //    \
  //      .
  //  root-2 + root-2, square'em , add, and root, and you get root 4 = 2.
  
  final public static float VERT_PATTERN[] = {
    -1,  0, 0,
     0,  1, 0,
     0, -1, 0,
     1,  0, 0,
  };
  final static float UV_PATTERN[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  };
  final public static short VERT_INDICES[] = {
    0, 2, 1, 1, 2, 3
  };
  
  
  final Texture texture;
  final int numTiles;
  final int animStart, animFrames;
  final int mapSize;
  
  static class Chunk {
    int mapX, mapY;
    Stitching frameGeom[];
    FlatTerrain belongs;
    boolean renderFlag = false;
  }
  
  private Chunk chunks[][];
  Vec3D tempV = new Vec3D(), norm = new Vec3D(0, 1, 0);
  
  
  
  public FlatTerrain(Texture texture, int numTiles, int mapSize) {
    this(texture, numTiles, -1, -1, mapSize);
  }
  
  
  public FlatTerrain(Texture texture, int numTiles, int animStart, int animEnd, int mapSize) {
    
    this.texture    = texture;
    this.numTiles   = numTiles;
    this.animStart  = animStart;
    this.animFrames = animEnd + 1 - animStart;
    this.mapSize    = mapSize;
    
    int gridSize = Nums.ceil(mapSize * 1f / CHUNK_SIZE);
    chunks = new Chunk[gridSize][gridSize];
  }
  
  
  public void appendTile(int tileID, int mapX, int mapY, int frame) {
    
    int gridX = mapX / CHUNK_SIZE, gridY = mapY / CHUNK_SIZE;
    Chunk chunk = chunks[gridX][gridY];
    
    if (chunk == null) {
      chunks[gridX][gridY] = chunk = new Chunk();
      chunk.mapX = gridX * CHUNK_SIZE;
      chunk.mapY = gridY * CHUNK_SIZE;
      chunk.frameGeom = new Stitching[this.animFrames];
      for (int f = animFrames; f-- > 0;) {
        chunk.frameGeom[f] = new Stitching(true, CHUNK_SIZE * CHUNK_SIZE);
      }
      chunk.belongs = this;
    }
    
    tileID = tileID % numTiles;
    final int frameW = 8, frameH = Nums.ceil(numTiles / 8f);
    final int offU = tileID % 8, offV = tileID / 8;
    final Stitching frameGeom = chunk.frameGeom[frame];
    
    for (int i = 0; i < 4; i++) {
      
      tempV.set(
        VERT_PATTERN[(i * 3) + 0] + 0.5f + mapX,
        VERT_PATTERN[(i * 3) + 1] + 0.5f + mapY,
        VERT_PATTERN[(i * 3) + 2]
      );
      
      float u = (UV_PATTERN[(i * 3) + 0] + offU) / frameW;
      float v = (UV_PATTERN[(i * 3) + 1] + offV) / frameH;
      
      frameGeom.appendDefaultVertex(tempV, norm, u, v, true);
    }
  }
  
  
  public void renderWithin(Box2D area, Rendering rendering) {
    final int
      minX = (int) ((area.xpos() + 1) / CHUNK_SIZE),
      minY = (int) ((area.ypos() + 1) / CHUNK_SIZE),
      dimX = 1 + (int) ((area.xmax() - 1) / CHUNK_SIZE) - minX,
      dimY = 1 + (int) ((area.ymax() - 1) / CHUNK_SIZE) - minY
    ;
    for (Coord c : Visit.grid(minX, minY, dimX, dimY, 1)) {
      Chunk chunk = chunks[c.x][c.y];
      if (chunk != null && ! chunk.renderFlag) {
        chunk.renderFlag = true;
        rendering.terrainPass.register(chunk);
      }
    }
  }
  
  
  
  /**  Used to customise terrain-generation!
    */
  public void appendTilesCustom(int x, int y, int w, int h, int layerID) {
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      final int tileID = selectCustomTileID(c.x, c.y, layerID);
      if (tileID < 0) continue;
      
      if (animStart == -1) {
        appendTile(tileID, c.x, c.y, 0);
      }
      else {
        for (int f = 0; f < animFrames; f++) {
          boolean cycle = tileID >= animStart && tileID < (animStart + animFrames);
          int frameID = tileID;
          if (cycle) frameID = ((tileID + f - animStart) % animFrames) + animStart;
          appendTile(frameID, c.x, c.y, f);
        }
      }
    }
  }
  
  protected int selectCustomTileID(int x, int y, int layerID) {
    return 0;
  }
}












