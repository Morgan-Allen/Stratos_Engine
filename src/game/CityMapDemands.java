

package game;
import util.*;
import static game.CityMap.*;
import java.util.Iterator;




public class CityMapDemands {
  
  
  /**  Data fields, construction and save/load methods-
    */
  final static int
    MIN_NODE_SIZE = 8,
    NODE_SIZE_DIV = 2
  ;
  
  static class Entry {
    Node   parent;
    int    x, y;
    float  amount;
    Object source;
    float  tempDist = -1;  // Used during search...
    boolean leaf() { return true; }
  }
  
  static class Node extends Entry {
    int size;
    Box2D area = new Box2D();
    List <Entry> kids = new List();
    boolean leaf() { return false; }
  }
  
  
  CityMap map;
  Object key;
  
  Node root;
  int total;
  
  
  CityMapDemands(CityMap map, Object key) {
    this.map  = map;
    this.key  = key;
    root      = new Node();
    root.area = new Box2D(-0.5f, -0.5f, map.size, map.size);
    Vec2D c = root.area.centre();
    root.x  = (int) c.x;
    root.y  = (int) c.y;
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
    //
    //  Delete this entry from its parent:
    Node par = e.parent;
    par.kids.remove(e);
    //
    //  If this means the parent is empty, delete that in turn, and so on up
    //  the tree-
    while (par != null && par.kids.empty()) {
      Node above = par.parent;
      above.kids.remove(par);
      par = above;
    }
  }
  
  
  void updateTotalsFrom(Node n) {
    n.amount = 0;
    for (Entry k : n.kids) n.amount += k.amount;
    if (n.parent != null) updateTotalsFrom(n.parent);
  }
  
  
  
  /**  More complex proximity-queries:
    */
  Iterable <Entry> nearbyEntries(final int x, final int y) {
    
    class iteration implements Iterable <Entry>, Iterator <Entry> {
      
      Sorting <Entry> sorting = new Sorting <Entry> () {
        public int compare(Entry a, Entry b) {
          return a.tempDist > b.tempDist ? 1 : -1;
        }
      };
      
      void addEntry(Entry e) {
        float dist = CityMap.distance(x, y, e.x, e.y);
        if (e.leaf()) {
          e.tempDist = dist - 0.5f;
        }
        else {
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
    i.addEntry(root);
    return i;
  }
  
  
  
  /**  Common/basic query and update methods:
    */
  float amountAt(int x, int y) {
    Entry e = findEntryFor(x, y, false);
    return e == null ? 0 : e.amount;
  }
  
  
  float totalAmount() {
    return root.amount;
  }
  
  
  void setAmount(float amount, Object source, int x, int y) {
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
  
}










