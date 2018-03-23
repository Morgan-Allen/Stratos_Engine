

package game;
import util.*;
import static game.Area.*;
import java.util.Iterator;




public class AreaDemands {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    MIN_NODE_SIZE = 8,
    NODE_SIZE_DIV = 2
  ;
  
  public static class Entry {
    Node   parent;
    int    x, y;
    float  amount;
    Object source;
    float  tempDist = -1;  // Used during search...
    boolean leaf() { return true; }
    
    public Object source() { return source; }
    public Coord coord() { return new Coord(x, y); }
    public float amount() { return amount; }
  }
  
  static class Node extends Entry {
    int size;
    float absAmount;
    Box2D area = new Box2D();
    List <Entry> kids = new List();
    boolean leaf() { return false; }
  }
  
  
  final Area map;
  final Object key;
  
  Node root;
  
  
  public AreaDemands(Area map, Object key) {
    this.map  = map;
    this.key  = key;
    root      = new Node();
    root.area = new Box2D(-0.5f, -0.5f, map.size, map.size);
    Vec2D c = root.area.centre();
    root.x  = (int) c.x;
    root.y  = (int) c.y;
  }
  
  
  void loadState(Session s) throws Exception {
    root = (Node) loadEntry(s);
  }
  
  
  void saveState(Session s) throws Exception {
    saveEntry(root, s);
  }
  
  
  Entry loadEntry(Session s) throws Exception {
    int t = s.loadInt();
    if (t == -1) return null;
    
    Entry e = t == 0 ? new Entry() : new Node();
    e.x = s.loadInt();
    e.y = s.loadInt();
    e.amount = s.loadFloat();
    e.source = s.loadObject();
    
    if (t == 1) {
      Node n = (Node) e;
      n.size = s.loadInt();
      n.absAmount = s.loadFloat();
      n.area.loadFrom(s.input());
      
      for (int k = s.loadInt(); k-- > 0;) {
        Entry kid = loadEntry(s);
        kid.parent = n;
        n.kids.add(kid);
      }
    }
    
    return e;
  }
  
  
  void saveEntry(Entry e, Session s) throws Exception {
    if (e == null) {
      s.saveInt(-1);
      return;
    }
    
    s.saveInt(e.leaf() ? 0 : 1);
    s.saveInt(e.x);
    s.saveInt(e.y);
    s.saveFloat (e.amount);
    s.saveObject(e.source);
    
    if (! e.leaf()) {
      Node n = (Node) e;
      s.saveInt(n.size);
      s.saveFloat(n.absAmount);
      n.area.saveTo(s.output());
      
      s.saveInt(n.kids.size());
      for (Entry kid : n.kids) {
        saveEntry(kid, s);
      }
    }
  }
  
  
  
  /**  Internal structural update methods-
    */
  Node findNodeFor(int x, int y, boolean adds) {
    //
    //  Descend the tree from the root, looking for the leaf node (if any) that
    //  contains the given coordinates-
    Node par = root;
    while (par.size > MIN_NODE_SIZE) {
      //
      //  First, look to see if there's an existing child to cover this area:
      for (Entry k : par.kids) {
        Node kid = (Node) k;
        if (kid.area.contains(x, y)) {
          par = kid;
          continue;
        }
      }
      //
      //  If that fails, maybe try creating a new child to cover it instead:
      if (adds) {
        Node kid = new Node();
        int size = par.size / NODE_SIZE_DIV;
        kid.parent = par;
        kid.size   = size;
        kid.area.set(0, 0, size, size);
        kid.area.incX(((x / size) * size) - 0.5f);
        kid.area.incY(((y / size) * size) - 0.5f);
        Vec2D c = kid.area.centre();
        kid.x   = (int) c.x;
        kid.y   = (int) c.y;
        par.kids.add(kid);
        par = kid;
        continue;
      }
      //
      //  And if there's no suitable child on this level, exit with null:
      return null;
    }
    return par;
  }
  
  
  Entry findEntryFor(int x, int y, boolean adds) {
    //
    //  First, find the parent node (if any) that covers the broad area for
    //  this coordinate:
    Node par = findNodeFor(x, y, adds);
    if (par == null) return null;
    //
    //  Search any existing entries to see if they match:
    for (Entry k : par.kids) {
      if (k.x == x && k.y == y) return k;
    }
    //
    //  Failing that, add a new entry if you can:
    if (adds) {
      Entry e = new Entry();
      e.parent = par;
      e.x = x;
      e.y = y;
      par.kids.add(e);
      return e;
    }
    //
    //  And failing that, return null:
    return null;
  }
  
  
  void deleteEntry(Entry e) {
    if (e.parent == null) return;
    e.parent.kids.remove(e);
    if (e.parent.kids.size() == 0) deleteEntry(e.parent);
  }
  
  
  void updateTotalsFrom(Node n) {
    n.amount = n.absAmount = 0;
    for (Entry k : n.kids) {
      n.amount += k.amount;
      if (k.leaf()) n.absAmount += Nums.abs(k.amount);
      else          n.absAmount += ((Node) k).absAmount;
    }
    if (n.parent != null) updateTotalsFrom(n.parent);
  }
  
  
  
  /**  Common/basic query and update methods:
    */
  public float amountAt(int x, int y) {
    Entry e = findEntryFor(x, y, false);
    return e == null ? 0 : e.amount;
  }
  
  
  public float totalAmount() {
    return root.amount;
  }
  
  
  public void setAmount(float amount, Object source, int x, int y) {
    if (amount == 0) {
      Entry e = findEntryFor(x, y, false);
      if (e == null) return;
      e.amount = 0;
      updateTotalsFrom(e.parent);
      deleteEntry(e);
    }
    else {
      Entry e = findEntryFor(x, y, true);
      e.amount = amount;
      e.source = source;
      updateTotalsFrom(e.parent);
    }
  }
  
  
  
  /**  More complex proximity-queries:
    */
  public Iterable <Entry> nearbyEntries(final int x, final int y) {
    //final Tile from = map.tileAt(x, y);
    
    class iteration implements Iterable <Entry>, Iterator <Entry> {
      
      Sorting <Entry> sorting = new Sorting <Entry> () {
        public int compare(Entry a, Entry b) {
          return a.tempDist > b.tempDist ? 1 : -1;
        }
      };
      
      void addEntry(Entry e) {
        if (e.leaf()) {
          
          //  TODO:  This might not work.  Flagged tiles will typically
          //  be inside a blocked structure, and entrances are not
          //  always unique.  You'll need to vary the check, depending
          //  on the source object.
          
          //Tile goes = map.tileAt(e.x, e.y);
          //if (! map.pathCache.pathConnects(from, goes)) return;
          
          float dist = Area.distance(x, y, e.x, e.y);
          e.tempDist = dist - 0.5f;
        }
        else {
          float dist = Area.distance(x, y, e.x, e.y);
          e.tempDist = dist - (((Node) e).size / 2f);
        }
        sorting.add(e);
      }
      
      public boolean hasNext() {
        return sorting.size() > 0;
      }
      
      public Entry next() {
        
        while (sorting.size() > 0) {
          Entry e = sorting.removeLeast();
          if (e.leaf()) {
            return e;
          }
          else {
            for (Entry k : ((Node) e).kids) addEntry(k);
          }
        }
        return null;
      }
      
      public void remove() { return; }
      public Iterator<Entry> iterator() { return this; }
    }
    iteration i = new iteration();
    if (root.kids.size() > 0) i.addEntry(root);
    return i;
  }
  
}










