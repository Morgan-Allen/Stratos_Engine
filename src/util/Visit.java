

package util;

import java.lang.reflect.Array;
import java.util.Iterator;



public abstract class Visit <T> implements Iterable <T>, Iterator <T> {
  
  public void remove() {}
  public Iterator <T> iterator() { return this; }
  
  
  /**  Visits every point in the given area- a syntactic shortcut for array
    *  loops.
    */
  public static Visit <Coord> grid(
    final int minX, final int minY,
    final int xD  , final int yD  ,
    final int step
  ) {
    final int maxX = minX + xD, maxY = minY + yD;
    final Coord passed = new Coord();
    return new Visit <Coord> () {
      
      int x = minX, y = minY;
      
      final public boolean hasNext() {
        return x < maxX && y < maxY;
      }
      
      final public Coord next() {
        passed.x = x;
        passed.y = y;
        if ((y += step) >= maxY) { y = minY; x += step; }
        return passed;
      }
    };
  }
  
  
  public static Visit <Coord> grid(Box2D b) {
    final int
      minX = (int) (b.xpos() + 0.5f),
      minY = (int) (b.ypos() + 0.5f),
      dimX = (int) (b.xmax() + 0.5f) - minX,
      dimY = (int) (b.ymax() + 0.5f) - minY;
    return grid(minX, minY, dimX, dimY, 1);
  }
  
  
  public static Visit <Coord> perimeter(
    final int minX, final int minY, final int xD, final int yD
  ) {
    final int dirs[] = TileConstants.T_ADJACENT;
    final Coord passed = new Coord();
    return new Visit <Coord> () {
      
      int dirIndex = 0, sideCount = xD;
      int dir = dirs[0], x = minX - 1, y = minY - 1;
      
      public boolean hasNext() {
        return dirIndex != -1;
      }
      
      public Coord next() {
        passed.x = x;
        passed.y = y;
        x += TileConstants.T_X[dir];
        y += TileConstants.T_Y[dir];
        
        if (--sideCount < 0) {
          if (++dirIndex >= dirs.length) { dirIndex = -1; return passed; }
          dir       = dirs[dirIndex];
          sideCount = (dirIndex % 2 == 0) ? xD : yD;
        }
        return passed;
      }
    };
  }
  
  
  public static Visit <Coord> coordsOnLine(
    final Vec2D orig, final Vec2D dest
  ) {
    final Vec2D
      disp = new Vec2D(dest).sub(orig),
      perp = disp.perp(new Vec2D()),
      temp = new Vec2D()
    ;
    final int
      signX = disp.x > 0 ? 1 : -1,
      signY = disp.y > 0 ? 1 : -1
    ;
    final Coord
      last = new Coord(),
      next = new Coord((int) orig.x, (int) orig.y),
      ends = new Coord((int) dest.x, (int) dest.y)
    ;
    final Box2D bounds = new Box2D(next.x, next.y, 0, 0);
    bounds.include(ends.x, ends.y, 0);
    
    return new Visit <Coord> () {
      boolean done = false;
      
      public boolean hasNext() {
        return ! done;
      }
      
      public Coord next() {
        if (ends.matches(next)) {
          done = true;
          return next;
        }
        //
        //  Essentially, we check each corner to see what 'side' of the line it
        //  lies on, and head through whatever goalposts are marked by the
        //  difference.  (NOTE:  The order of operations here is important,
        //  please don't tamper.)
        last.setTo(next);
        final byte
          side00 = cornerSide(0, 0),
          side01 = cornerSide(0, 1),
          side10 = cornerSide(1, 0),
          side11 = cornerSide(1, 1)
        ;
        if      (side11 != side10 && signX > 0) next.x++;
        else if (side11 != side01 && signY > 0) next.y++;
        else if (side00 != side01 && signX < 0) next.x--;
        else if (side00 != side10 && signY < 0) next.y--;
        if (! bounds.contains(next.x, next.y)) done = true;
        if (last.matches(next)) done = true;
        return last;
      }
      
      private byte cornerSide(int offx, int offy) {
        temp.set(next.x + offx - orig.x, next.y + offy - orig.y);
        final float dot = perp.dot(temp);
        return (byte) (dot == 0 ? 0 : (dot > 0 ? 1 : -1));
      }
    };
  }
  
  
  
  /**  Couple of list-utility methods-
    */
  public static Object matchFor(Class matchClass, Series series) {
    for (Object o : series) if (o.getClass() == matchClass) return o;
    return null;
  }
  
  
  public static void appendTo(Series to, Series from) {
    for (Object o : from) to.add(o);
  }
  
  
  public static void appendTo(Series to, Object from[]) {
    for (Object o : from) to.add(o);
  }
  
  

  /**  More utility methods, this time for dealing with arrays-
    */
  public static boolean empty(Object o[]) {
    if (o == null || o.length == 0) return true;
    for (Object i : o) if (i != null) return false;
    return true;
  }
  
  
  public static Object last(Object o[]) {
    if (o == null || o.length == 0) return null;
    return o[o.length - 1];
  }
  
  
  public static int indexOf(Object o, Object a[]) {
    for (int i = a.length; i-- > 0;) if (a[i] == o) return i;
    return -1;
  }
  
  
  public static Object atIndex(int index, Object a[]) {
    if (a == null || index < 0 || index >= a.length) return null;
    else return a[index];
  }
  
  
  public static Object[] compose(Class arrayClass, Object[]... arrays) {
    int length = 0, i = 0;
    for (Object a[] : arrays) length += a.length;
    final Object[] result = (Object[]) Array.newInstance(arrayClass, length);
    for (Object a[] : arrays) {
      for (Object o : a) result[i++] = o;
    }
    return result;
  }
  
  
  public static void wipe(Object array[]) {
    for (int i = array.length; i-- > 0;) array[i] = null;
  }
  
  
  public static boolean arrayIncludes(Object a[], Object e) {
    if (a == null || e == null) return false;
    for (Object o : a) if (o == e) return true;
    return false;
  }
  
  
  public static int countInside(Object m, Object a[]) {
    int count = 0;
    for (Object o : a) if (o == m) count++;
    return count;
  }
  
  
  public static float[] fromFloats(Object[] a) {
    float f[] = new float[a.length];
    for (int i = f.length; i-- > 0;) f[i] = (Float) a[i];
    return f;
  }
  
  
  public static int[] fromIntegers(Object[] a) {
    int f[] = new int[a.length];
    for (int i = f.length; i-- > 0;) f[i] = (Integer) a[i];
    return f;
  }
}





