

package util;



public abstract class TilePattern implements TileConstants {
  
  abstract int faceIndex(int x, int y, float level, int nearMask);
}