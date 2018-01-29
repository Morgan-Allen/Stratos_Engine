/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.terrain;
import graphics.common.*;
import util.*;



public class TerrainSet implements TileConstants {
  
  
  final static int
    DEFAULT_CHUNK_SIZE = 16,
    MAX_CHUNK_SIZE     = 128;
  
  
  final public int sizeX, sizeY, numLayers;
  final int chunkSize, chunkGrid;
  final byte layerIndices[][], varsIndices[][], heightVals[][];
  
  final TerrainChunk chunks[][][];
  final public LayerType layers[];
  
  
  
  public TerrainSet(
      int sizeX, int sizeY, int chunkSize,
      byte layerIndices[][],
      byte varsIndices [][],
      byte heightVals  [][],
      LayerType layers[]
  ) {
    // Basic sanity checks first-
    if (chunkSize > MAX_CHUNK_SIZE) {
      throw new RuntimeException("Chunks too big!");
    }
    else if (chunkSize <= 0) chunkSize = DEFAULT_CHUNK_SIZE;
    //
    // Appropriate dimensions calculated-
    this.numLayers = layers.length;
    this.sizeX     = sizeX;
    this.sizeY     = sizeY;
    this.chunkSize = chunkSize;
    this.chunkGrid = (int) Nums.ceil(Nums.max(sizeX, sizeY) * 1f / chunkSize);
    this.layers    = layers;
    this.layerIndices = layerIndices;
    this.varsIndices  = varsIndices;
    this.heightVals   = heightVals;
    //
    // And finally, the bite-size terrain chunks that actually get
    // rendered on a need-to-see basis-
    this.chunks = new TerrainChunk[chunkGrid][chunkGrid][numLayers];
  }
  
  
  public void dispose() {
    for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
      for (LayerType layer : layers) {
        final TerrainChunk chunk = chunks[c.x][c.y][layer.layerID];
        chunk.dispose();
        if (chunk.fadeOut != null) chunk.fadeOut.dispose();
      }
    }
  }
  
  
  public void refreshAllMeshes() {
    
    for (Coord c : Visit.grid(0, 0, chunkGrid, chunkGrid, 1)) {
      for (LayerType layer : layers) {
        final TerrainChunk oldChunk = chunks[c.x][c.y][layer.layerID];
        if (oldChunk != null && ! oldChunk.needsRefresh()) continue;
        
        final TerrainChunk chunk = new TerrainChunk(
          chunkSize, chunkSize,
          c.x * chunkSize, c.y * chunkSize,
          layer, this
        );
        chunk.generateMeshData();
        chunks[c.x][c.y][layer.layerID] = chunk;
        chunk.fadeOut = oldChunk;
        chunk.fadeIncept = Rendering.activeTime();
      }
    }
    
    /*
    for (LayerType layer : layers) for (ImageAsset image : layer.layerFrames) {
      Assets.checkForRefresh(image, 500);
    }
    //*/
  }
  
  
  public void flagUpdateAt(int x, int y) {
    for (int n : T_ON_CENTRE) {
      final int nX = x + T_X[n], nY = y + T_Y[n];
      for (LayerType layer : layers) flagUpdateAt(nX, nY, layer);
    }
  }
  
  
  public void flagUpdateAt(int x, int y, LayerType layer) {
    final int tx = x / chunkSize, ty = y / chunkSize;
    if (tx < 0 || tx >= chunkGrid) return;
    if (ty < 0 || ty >= chunkGrid) return;
    final TerrainChunk chunk = chunks[tx][ty][layer.layerID];
    if (chunk != null) chunk.flagRefresh();
  }
  
  
  public void renderWithin(Box2D area, Rendering rendering) {
    final int
      minX = (int) ((area.xpos() + 1) / chunkSize),
      minY = (int) ((area.ypos() + 1) / chunkSize),
      dimX = 1 + (int) ((area.xmax() - 1) / chunkSize) - minX,
      dimY = 1 + (int) ((area.ymax() - 1) / chunkSize) - minY;
    for (Coord c : Visit.grid(minX, minY, dimX, dimY, 1)) {
      for (TerrainChunk patch : chunks[c.x][c.y]) {
        patch.readyFor(rendering);
      }
    }
  }
}




