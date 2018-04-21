

package game;
import static game.GameConstants.*;
import util.*;




public class BuildingForDock extends Building {

  
  /**  Data-fields, construction, and save/load methods-
    */
  public static class TradeProxy implements Trader {
    
    final Trader trader;
    
    Target near;
    Tally <Good> nearDemand = new Tally();
    Tally <Good> nearSupply = new Tally();
    Tally <Good> inventory  = new Tally();
    
    TradeProxy(Trader trader) {
      this.trader = trader;
    }
    
    public Tally <Good> inventory() {
      return inventory;
    }
    
    public Base base() {
      return trader.base();
    }
    
    public Tally <Good> needLevels() {
      return nearDemand;
    }
    
    public Tally <Good> prodLevels() {
      return nearSupply;
    }
    
    public float importPrice(Good g, Base sells) {
      return base().importPrice(g, sells);
    }
    
    public float exportPrice(Good g, Base buys) {
      return base().exportPrice(g, buys);
    }
    
    public float shopPrice(Good g, Task task) {
      return -1;
    }
    
    public boolean allowExport(Good g, Trader buys) {
      return true;
    }
  }
  
  
  Base tradePartner = null;
  List <TradeProxy> tradeProxies = new List();
  
  ActorAsVessel docking[];
  AreaTile dockPoints[] = null;
  
  
  
  public BuildingForDock(BuildType type) {
    super(type);
    docking = new ActorAsVessel[type.dockPoints.length];
  }
  
  
  public BuildingForDock(Session s) throws Exception {
    super(s);
    
    tradePartner = (Base) s.loadObject();
    
    docking = (ActorAsVessel[]) s.loadObjectArray(ActorAsVessel.class);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObject(tradePartner);
    
    s.saveObjectArray(docking);
  }
  
  
  
  /**  Estimating local supply and demand...
    */
  public void setTradePartner(Base partner) {
    this.tradePartner = partner;
  }
  
  
  TradeProxy tradeProxyFor(ActorAsVessel ship, boolean init) {
    for (TradeProxy proxy : tradeProxies) if (ship == proxy.trader) {
      return proxy;
    }
    if (init) {
      TradeProxy proxy = new TradeProxy(ship);
      tradeProxies.add(proxy);
      return proxy;
    }
    return null;
  }
  
  
  
  /**  Regular updates-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    boolean hasVessel  = false;
    boolean hasUpgrade = false;
    BuildType template = null;
    AreaTile dockPoint = nextFreeDockPoint();
    
    //  TODO:  In essence, you should synchronise that upgrade (and any
    //  dependent upgrades) with the ship.  Implement this.
    
    for (BuildType t : this.upgrades()) {
      if (t.vesselTemplate == null || ! upgradeComplete(t)) continue;
      hasUpgrade = true;
      template = t;
    }
    for (Actor a : workers()) if (a.type().isVessel()) {
      hasVessel = true;
    }
    if (hasUpgrade && dockPoint != null && ! hasVessel) {
      advanceShipConstruction(template, dockPoint);
    }
    
    for (TradeProxy proxy : tradeProxies) {
      proxy.nearDemand.clear();
      proxy.nearSupply.clear();
      proxy.inventory .clear();
      proxy.near = this;
      
      for (Building b : map.buildings) {
        if (b.base() != base() || ! b.type().isTradeBuilding()) continue;
        
        float dist = Area.distance(b, this);
        if (dist > MAX_TRADER_RANGE - radius()) continue;
        
        float weight = Area.distancePenalty(dist);
        Trader trader = (Trader) b;
        
        for (Good g : trader.prodLevels().keys()) {
          if (! trader.allowExport(g, proxy.trader)) continue;
          
          float supply = trader.prodLevels().valueFor(g);
          float amount = trader.inventory ().valueFor(g);
          proxy.nearSupply.add(supply * weight, g);
          proxy.inventory .add(amount * weight, g);
        }
        
        for (Good g : trader.needLevels().keys()) {
          float demand = trader.needLevels().valueFor(g);
          demand = Nums.max(0, demand - trader.inventory().valueFor(g));
          proxy.nearDemand.add(demand * weight, g);
        }
      }
    }
  }
  
  
  void advanceShipConstruction(BuildType template, AreaTile point) {
    ActorAsVessel ship = (ActorAsVessel) template.vesselTemplate.generate();
    setWorker(ship, true);
    ship.enterMap(map, point.x, point.y, 1, base());
    ship.doLanding(point);
  }
  
  
  public Task selectActorBehaviour(Actor actor) {
    
    if (actor.type().isVessel()) {
      ActorAsVessel ship = (ActorAsVessel) actor;
      TradeProxy proxy = tradeProxyFor(ship, true);
      TaskTrading trading = BuildingForTrade.selectTraderBehaviour(
        proxy, ship, tradePartner, false, map
      );
      if (trading != null) return trading;
    }
    
    return super.selectActorBehaviour(actor);
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
    
    //  TODO:  Resident vessels *always* reserve at least one dock-point for
    //  themselves!
    
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
  
  
  public boolean isDocked(ActorAsVessel docks) {
    return Visit.arrayIncludes(docking, docks);
  }
  
  
  public Series <ActorAsVessel> docking() {
    Batch <ActorAsVessel> all = new Batch();
    for (ActorAsVessel a : docking) if (a != null) all.add(a);
    return all;
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






