

package game;
import util.*;



public abstract class Flood <T extends Flood.Fill> {
  
  
  static interface Fill {
    void flagWith(Object o);
    Object flaggedWith();
  }
  
  
  private Batch <T> covered = new Batch();
  private List <T> frontier = new List();
  
  
  public Series <T> floodFrom(T init) {
    tryAdding(init);
    
    while (! frontier.empty()) {
      T front = frontier.removeFirst();
      addSuccessors(front);
    }
    
    for (T t : covered) t.flagWith(null);
    return covered;
  }
  
  
  void tryAdding(T item) {
    if (item == null || item.flaggedWith() != null) return;
    covered.add(item);
    frontier.add(item);
    item.flagWith(this);
  }
  
  
  abstract void addSuccessors(T front);
  
}



