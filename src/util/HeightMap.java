/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package util;
import util.*;



/**  Creates a randomly-generated fractal height-map, either from scratch or
  *  using predetermined 'seed' values as a starting point.
  */
public class HeightMap {
  
  
  private int size;
  private float maxHigh;
  private float mapHigh[][];
  private float limit;
  
  
  /**  Most basic constructor, using maximum height of 1 and step ratio of 0.5.
    *  Size must be a power of 2, plus 1.
    */
  public HeightMap(int size) {
    this(size, 1, 0.5f);
  }
  
  
  /**  Constructs a height map using the given seed values as a starting point.
    *  (The seed, like the size, should equal in dimension (2^n)+1 for some
    *  value of n, and it's overall height should be proportionate to maxHigh.)
    */
  public HeightMap(int minSize, float seed[][], float maxHigh, float stepRatio) {
    //
    //  size must be a power of 2, + 1:
    size = 1; while (size + 1 < minSize) size *= 2; size++;
    this.maxHigh = maxHigh;
    this.mapHigh = new float[size][size];
    //
    //  We need to insert the seed values provided at appropriate intervals
    //  within the height map (to serve as a basis for subsequent fractal
    //  descent.)
    if (seed.length > 1) {
      final int step = (size - 1) / (seed.length - 1);
      for (int x = 0; x * step < size; x++)
        for (int y = 0; y * step < size; y++)
          mapHigh[x * step][y * step] = seed[x][y];
      fractalGen(step, maxHigh, stepRatio);
    }
    else {
      final int step = size - 1;
      fractalGen(step, maxHigh, stepRatio);
    }
  }
  
  
  /**  Constructs a height map of the given minimum size, maximum height, and
    *  step ratio.
    */
  public HeightMap(int minSize, float maxHigh, float stepRatio) {
    //
    //  size must be a power of 2, + 1:
    size = 1; while (size + 1 < minSize) size *= 2; size++;
    this.mapHigh = new float[size][size];
    this.maxHigh = maxHigh;
    final int step = size - 1;
    //
    //  Firstly, we initialise seed values at the corners:
    mapHigh[0   ][0   ] = nextHigh();
    mapHigh[0   ][step] = nextHigh();
    mapHigh[step][0   ] = nextHigh();
    mapHigh[step][step] = nextHigh();
    fractalGen(step, maxHigh, stepRatio);
  }
  
  
  final private static int
    DAC[] = { 1, 1, 1, -1, -1, -1, -1, 1 }, // Diamond-adjacent-coordinates,
    SAC[] = { 0, 1, 1, 0, 0, -1, -1, 0 };  // and Square-adjacent-coordinates.
  
  
  /**  Performs actual fractal height generation.
    */
  private void fractalGen(int step, float maxHigh, float stepRatio) {
    //
    //  You need a pass for each level of the map, doubling in detail each
    //  step.  You need a pass for each coordinate at that resolution, with a
    //  single diamond and two square passes (which ensure even averaging of
    //  heigh values along and between both axes.)  Each time, you reduce the
    //  step size and limit for random height adjustment.
    int halfStep;
    limit = maxHigh;
    //
    //  Then iterate down in detail-
    while (step > 1) {
      halfStep = step / 2;
      limit *= stepRatio;
      //
      //  The diamond step:
      for (int x = 0; x < size; x += step)
        for (int y = 0; y < size; y += step)
          avgVal(x + halfStep, y + halfStep, halfStep, DAC);
      //
      //  The square step:
      for (int x = 0; x < size; x += step)
        for (int y = 0; y < size; y += step) {
          avgVal(x + halfStep, y, halfStep, SAC);
          avgVal(x, y + halfStep, halfStep, SAC);
        }
      step = halfStep;
    }
    //
    //  Then we must adjust the entire map to exactly fit into the desired
    //  height range (0 to maxHeight.)
    float
      max = Float.NEGATIVE_INFINITY,
      min = Float.POSITIVE_INFINITY,
      high;
    for (int x = size; x-- > 0;)
      for (int y = size; y-- > 0;) {
        high = mapHigh[x][y];
        max = Nums.max(max, high);
        min = Nums.min(min, high);
      }
    if (max == min) max = min + 1;  // Hugely unlikely, but anyway...
    for (int x = size; x-- > 0;)
      for (int y = size; y-- > 0;)
        mapHigh[x][y] = maxHigh * (mapHigh[x][y] - min) / (max - min);
  }
  
  
  /**  Averages the adjacent values (either diamond or square-adjacent) for a
    *  given tile of the map, adds a random adjustment (within specified
    *  limits,) and stores the value back into that point on the map. (The
    *  'step' argument specifies the resolution at which such averaging is
    *  being performed.)
    */
  private void avgVal(int x, int y, int step, int AC[]) {
    //
    //  Return if the point is outside the grid-
    float val = val(mapHigh, x, y);
    if (val == Float.NEGATIVE_INFINITY) return;
    //
    //  Otherwise:
    float average = 0;
    int num = 0;
    for (int n = 0; n < AC.length;) {
      val = val(mapHigh, x + (AC[n++] * step), y + (AC[n++] * step));
      //
      //  Again, ignore points outside the grid-
      if (val == Float.NEGATIVE_INFINITY) continue;
      num++;
      average += val;
    }
    average /= num;
    mapHigh[x][y] = average + nextHigh();
  }
  
  
  private static float val(final float map[][], final int x, final int y) {
    try { return map[x][y]; }
    catch (Exception e) { return Float.NEGATIVE_INFINITY; }
  }
  
  
  /**  Supplies random height adjustments for 'seeding' the map at each
    *  resolution.
    */
  private float nextHigh() { return ((Rand.num() - 0.5f) * limit); }
  
  
  /**  Returns the finished product of this computation.
    */
  public float[][] value() {
    return mapHigh;
  }
  
  
  /**  Returns the map's value set in byte format:
    */
  public byte[][] asScaledBytes(float scaleHigh) {
    byte result[][] = new byte[size][size];
    for (int x = size; x-- > 0;) for (int y = size; y-- > 0;) {
      result[x][y] = (byte) ((mapHigh[x][y] * scaleHigh) / this.maxHigh);
    }
    return result;
  }
  
  
  /**  Returns the amount of ground spanned by this height map.
    */
  public int span() {
    return size - 1;
  }
  
  
  /**  Prints out the height values of the terrain generated.
    */
  void report() {
    I.add("\nPrinting height values:");
    for (int y = 0; y < size; y++) {
      I.add("\n");
      for (int x = 0; x < size; x++)
        I.add(" " + (int) (mapHigh[x][y] * 10));
    }
  }

}







