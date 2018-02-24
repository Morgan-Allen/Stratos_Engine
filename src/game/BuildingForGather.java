

package game;
import util.*;
import static game.AreaMap.*;
import static game.GameConstants.*;




public class BuildingForGather extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  static class Plot extends Box2D {
    Type planted;
  };
  
  List <Box2D> plantAreas = new List();
  
  
  public BuildingForGather(BuildType type) {
    super(type);
  }
  
  
  public BuildingForGather(Session s) throws Exception {
    super(s);
    for (int i = s.loadInt(); i-- > 0;) {
      plantAreas.add(new Box2D().loadFrom(s.input()));
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(plantAreas.size());
    for (Box2D a : plantAreas) a.saveTo(s.output());
  }
  
  
  
  /**  Utility methods for filling up crop areas:
    */
  public void enterMap(AreaMap map, int x, int y, float buildLevel, Base owns) {
    super.enterMap(map, x, y, buildLevel, owns);
    
    if (isClaimant() && TaskGathering.canPlant(this)) {
      Box2D limit = area().expandBy(type().claimMargin);
      
      //  TODO:  Ensure planted areas don't overlap with your own footprint.
      
      //  TODO:  Also, you need to ensure that the right mixture of goods is
      //  harvested.  The simplest way is to just pick at random to ensure the
      //  correct balance.  Either that, or stipple the rows.
      
      //  You can set the correct balance at the venue via upgrades.
      
      
      
      
      this.plantAreas = divideIntoPlots(limit, 3, 4);
    }
  }


  private List <Box2D> divideIntoPlots(
    Box2D area, int prefSpacing, int maxSideRatio
  ) {
    final int idealSplit = 1 + (prefSpacing * 2);
    prefSpacing  = Nums.max(prefSpacing , 1);
    maxSideRatio = Nums.max(maxSideRatio, 1);
    final List <Box2D> bigPlots = new List(), finePlots = new List();
    bigPlots.add(area);
    
    while (bigPlots.size() > 0) {
      for (Box2D plot : bigPlots) {
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
    Box2D plot, boolean across, float split, List <Box2D> plots
  ) {
    final int
      side  = (int) (across ? plot.xdim() : plot.ydim()),
      sideA = (int) (side * split),
      sideB = side - (1 + sideA);
    final Box2D
      plotA = new Box2D(plot),
      plotB = new Box2D(plot);
    
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
    Task coming = returnActorHere(actor);
    if (coming != null) return coming;
    
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) {
      return delivery;
    }
    
    Object[] crops = type().produced;
    Task pick = TaskGathering.pickNextCrop(this, actor, false, crops);
    if (pick != null) {
      return pick;
    }
    
    //  TODO:  Modify the pick-plant method to apply to all eligible tiles
    //  within a certain area.
    
    Task plant = TaskGathering.pickPlantPoint(this, actor, false, true);
    if (plant != null) {
      return plant;
    }
    
    return null;
  }
}







