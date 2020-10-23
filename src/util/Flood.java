

package util;



public abstract class Flood <T extends Flood.Fill> {
  
  
  public static interface Fill {
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
  
  
  public T[] covered(Class typeClass) {
    return covered.toArray(typeClass);
  }
  
  
  protected int numCovered() {
    return covered.size();
  }
  
  
  protected void tryAdding(T item) {
    if (item == null) return;
    
    final Object flag = item.flaggedWith();
    if (flag == this) return;
    
    covered.add(item);
    frontier.add(item);
    item.flagWith(this);
  }
  
  
  protected abstract void addSuccessors(T front);
  
}



