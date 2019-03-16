

package game;
import util.*;


public class Area implements Session.Saveable {
  
  
  final public AreaType type;
  
  List <Actor> visitors = new List();
  private boolean active;
  private AreaMap map;
  
  
  
  Area(AreaType type) {
    this.type = type;
  }
  
  
  public Area(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (AreaType) s.loadObject();
    
    s.loadObjects(visitors);
    active = s.loadBool();
    map    = (AreaMap) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    
    s.saveObjects(visitors);
    s.saveBool(active);
    s.saveObject(map);
  }
  
  
  
  
  public void attachMap(AreaMap map) {
    this.map    = map;
    this.active = map == null ? false : true;
  }
  
  
  public AreaMap activeMap() {
    return map;
  }
  
  
  public boolean isOffmap() {
    return map == null;
  }
  
  
  
  /**  Methods for handling traders and migrants-
    */
  void updateLocale() {
    for (Actor a : visitors) if (! a.onMap()) {
      a.updateOffMap(this);
    }
  }
  
  
  public void toggleVisitor(Actor visitor, boolean is) {
    
    Area offmap = visitor.offmap();
    if (offmap != this && ! is) return;
    if (offmap == this &&   is) return;
    if (offmap != null &&   is) offmap.toggleVisitor(visitor, false);
    
    visitors.toggleMember(visitor, is);
    visitor.setOffmap(is ? this : null);
  }
  
  
  public Series <Actor> visitors() {
    return visitors;
  }
}









