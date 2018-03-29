

package game;
import static game.GameConstants.*;
import util.*;



public class BuildingForDock extends Building {

  
  /**  Data-fields, construction, and save/load methods-
    */
  ActorAsVessel docking[];
  AreaTile dockPoints[] = null;
  
  Tally <Good> nearDemand = new Tally();
  Tally <Good> nearSupply = new Tally();
  
  
  
  public BuildingForDock(BuildType type) {
    super(type);
    docking = new ActorAsVessel[type.dockPoints.length];
  }
  
  
  public BuildingForDock(Session s) throws Exception {
    super(s);
    docking = (ActorAsVessel[]) s.loadObjectArray(ActorAsVessel.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObjectArray(docking);
  }
  
  
  
  /**  Regular updates-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    nearDemand.clear();
    nearSupply.clear();
    
    for (Building b : map.buildings) {
      if (b.base() != base() || ! b.type().isTradeBuilding()) continue;
      
      float dist = Area.distance(b, this);
      if (dist > MAX_TRADER_RANGE - radius()) continue;
      
      float weight = Area.distancePenalty(dist);
      Trader trader = (Trader) b;
      
      for (Good g : trader.prodLevels().keys()) {
        if (! trader.allowExport(g, buys)) continue;
        
        float supply = trader.prodLevels().valueFor(g);
        nearSupply.add(supply * weight, g);
      }
      
      for (Good g : trader.needLevels().keys()) {
        float demand = trader.needLevels().valueFor(g);
        nearDemand.add(demand * weight, g);
      }
    }
  }
  
  
  
  /**  Finding points to dock at:
    */
  AreaTile[] dockPoints() {
    if (! onMap()) return null;
    if (dockPoints != null) return dockPoints;
    
    AreaTile from = at();
    dockPoints = new AreaTile[type().dockPoints.length];
    
    for (int i = 0; i < dockPoints.length; i++) {
      Coord off = type().dockPoints[i];
      dockPoints[i] = map.tileAt(from.x + off.x, from.y + off.y);
    }
    
    return dockPoints;
  }


  public AreaTile nextFreeDockPoint() {
    if (dockPoints() == null) return null;
    
    for (int i = 0; i < dockPoints.length; i++) {
      if (docking[i] != null) continue;
      return dockPoints[i];
    }
    
    return null;
  }
  
  
  public void toggleDocking(ActorAsVessel docks, AreaTile dockAt, boolean is) {
    if (docks == null || dockAt == null || dockPoints() == null) return;
    int index = Visit.indexOf(dockAt, dockPoints);
    if (index == -1) I.complain("\nUnknown dock point! "+dockAt);
    
    if (is) {
      if (docking[index] != null) I.complain("\nPrior vessel still docked!");
      docking[index] = docks;
    }
    else {
      if (docking[index] != docks) I.complain("\nVessel was never docked!");
      docking[index] = null;
    }
  }
  
  
  public Pathing[] adjacent(Pathing[] temp, Area map) {
    
    int numE = entrances() == null ? 0 : entrances().length;
    int numP = numE + docking.length;
    if (temp == null || temp.length < numP) temp = new Pathing[numP];
    
    temp = super.adjacent(temp, map);
    for (ActorAsVessel v : docking) temp[numE++] = v;
    return temp;
  }
  
  
  public boolean allowsEntryFrom(Pathing p) {
    if (super.allowsEntryFrom(p)) return true;
    if (Visit.arrayIncludes(docking, p)) return true;
    return false;
  }
  
  
  
  
}
