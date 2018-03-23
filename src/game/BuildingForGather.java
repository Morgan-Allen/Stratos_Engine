

package game;
import util.*;
import static game.Area.*;
import static game.GameConstants.*;




public class BuildingForGather extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static class Plot extends Box2D {
    Type plantOnly = null;
  };
  
  List <Plot> plots = new List();
  private AreaTile temp[] = new AreaTile[9];
  
  
  public BuildingForGather(BuildType type) {
    super(type);
  }
  
  
  public BuildingForGather(Session s) throws Exception {
    super(s);
    for (int i = s.loadInt(); i-- > 0;) {
      Plot p = new Plot();
      p.loadFrom(s.input());
      p.plantOnly = (Type) s.loadObject();
      plots.add(p);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(plots.size());
    for (Plot p : plots) {
      p.saveTo(s.output());
      s.saveObject(p.plantOnly);
    }
  }
  
  
  
  /**  Utility methods for filling up crop areas:
    */
  public void enterMap(Area map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    
    if (isClaimant() && TaskGathering.canPlant(this)) {
      Plot limit = (Plot) new Plot().setTo(claimArea());
      this.plots = divideIntoPlots(limit, 3, 4);
    }
  }
  
  
  public Series <Plot> plots() {
    return plots;
  }
  
  
  boolean canPlant(AreaTile at) {
    if (at == null) return false;
    if (at.terrain.pathing != Type.PATH_FREE) return false;
    if (at.above != null && ! at.above.type().isClearable()) return false;
    
    for (AreaTile t : Area.adjacent(at, temp, map())) {
      if (t == null || t.above == null) continue;
      if (t.above.type().isClearable()) continue;
      if (t.above.type().pathing <= Type.PATH_FREE) continue;
      return false;
    }
    
    return true;
  }
  
  
  Good seedType(AreaTile t) {
    Good crops[] = this.type().produced;
    float index = t.x % 5;
    index += (t.y % 5) / 5f;
    index *= crops.length / 5f;
    return crops[(int) index];
  }
  
  
  private List <Plot> divideIntoPlots(
    Plot area, int prefSpacing, int maxSideRatio
  ) {
    final int idealSplit = 1 + (prefSpacing * 2);
    prefSpacing  = Nums.max(prefSpacing , 1);
    maxSideRatio = Nums.max(maxSideRatio, 1);
    final List <Plot> bigPlots = new List(), finePlots = new List();
    bigPlots.add(area);
    
    while (bigPlots.size() > 0) {
      for (Plot plot : bigPlots) {
        final float minSide = plot.minSide();
        
        if (minSide > prefSpacing) {
          boolean across = plot.xdim() < plot.ydim();
          if (minSide < idealSplit) across = ! across;
          dividePlot(plot, across, 0.5f, bigPlots);
        }
        else if (plot.maxSide() > minSide * maxSideRatio) {
          final float split = 0.5f;
          dividePlot(plot, plot.xdim() > plot.ydim(), split, bigPlots);
        }
        else {
          bigPlots.remove(plot);
          finePlots.add(plot);
        }
      }
    }
    return finePlots;
  }
  
  
  private void dividePlot(
    Plot plot, boolean across, float split, List <Plot> plots
  ) {
    final int
      side  = (int) (across ? plot.xdim() : plot.ydim()),
      sideA = (int) (side * split),
      sideB = side - (1 + sideA)
    ;
    final Plot
      plotA = (Plot) new Plot().setTo(plot),
      plotB = (Plot) new Plot().setTo(plot)
    ;
    if (across) {
      plotA.xdim(sideA);
      plotB.xdim(sideB);
      plotB.incX(sideA + 1);
    }
    else {
      plotA.ydim(sideA);
      plotB.ydim(sideB);
      plotB.incY(sideA + 1);
    }
    plots.remove(plot);
    plots.add(plotA);
    plots.add(plotB);
  }
  
  
  
  //  TODO:  Consider whether this is still needed
  /*
  public static Tile[] applyPlanting(
    Base city, int x, int y, int w, int h, Good... crops
  ) {
    Batch <Tile> planted = new Batch();
    AreaMap map = city.activeMap();
    for (Coord c : Visit.grid(x, y, w, h, 1)) {
      
      Tile t = map.tileAt(c);
      if (t == null) continue;
      if (t.above != null && ! t.above.type().isFlora()) continue;
      
      Good seed = seedType(t, crops);
      Element crop = new Element(seed);
      crop.enterMap(map, c.x, c.y, 1, city);
      crop.setGrowLevel(-1);
      planted.add(t);
    }
    return planted.toArray(Tile.class);
  }
  
  
  public static Good seedType(Tile t, Good crops[]) {
    float index = t.x % 5;
    index += (t.y % 5) / 5f;
    index *= crops.length / 5f;
    return crops[(int) index];
  }
  //*/
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  public Task selectActorBehaviour(Actor actor) {
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, 2, produced());
    if (delivery != null) return delivery;
    
    Object[] crops = type().produced;
    Task pick = TaskGathering.pickNextCrop(this, actor, false, crops);
    if (pick != null) return pick;
    
    Task plant = TaskGathering.pickPlantPoint(this, actor, false, true);
    if (plant != null) return plant;
    
    delivery = TaskDelivery.pickNextDelivery(actor, this, 0, produced());
    if (delivery != null) return delivery;
    
    return null;
  }
}







