

package game;
import static game.GameConstants.*;
import util.*;



public class Area implements Session.Saveable {
  
  
  final public AreaType type;
  final World world;
  
  List <Base> bases = new List();
  final public Base locals;
  
  List <Actor> visitors = new List();
  private boolean active;
  private AreaMap map;
  
  
  
  Area(World world, AreaType type) {
    this.world = world;
    this.type = type;
    
    this.locals = new Base(world, this, FACTION_NEUTRAL, "Locals: "+this);
    addBase(locals);
    
    //locals.council().setGovernment(BaseCouncil.GOVERNMENT.BARBARIAN);
    //locals.council().setTypeAI(BaseCouncil.AI_OFF);
  }
  
  
  public Area(Session s) throws Exception {
    s.cacheInstance(this);
    
    type = (AreaType) s.loadObject();
    world = (World) s.loadObject();
    
    s.loadObjects(bases);
    locals = (Base) s.loadObject();
    
    s.loadObjects(visitors);
    active = s.loadBool();
    map    = (AreaMap) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveObject(type);
    s.saveObject(world);
    
    s.saveObjects(bases);
    s.saveObject(locals);
    
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
  
  
  public void addBase(Base base) {
    bases.include(base);
  }
  
  
  public Series <Base> bases() {
    return bases;
  }
  
  
  public boolean notSettled() {
    return bases.size() == 1 && bases.first() == locals;
  }
  
  
  public Base firstBaseFor(Faction faction) {
    if (faction == FACTION_NEUTRAL) return locals;
    for (Base b : bases) if (b.faction() == faction) return b;
    return null;
  }
  
  
  
  /**  Methods for handling traders and migrants-
    */
  void updateArea() {
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









