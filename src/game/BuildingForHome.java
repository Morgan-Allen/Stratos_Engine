

package game;
import util.*;
import static game.CityMap.*;
import static game.Walker.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForHome extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  BuildingForHome(ObjectType type) {
    super(type);
  }
  
  
  public BuildingForHome(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  /**  Passive ambience and access-checks:
    */
  boolean performAmbienceCheck(ObjectType tier) {
    //
    //  This method checks the surrounding tiles out to a 
    final int MAX_AMBIENCE_DIST = 6;
    
    final Tally <Tile> costs = new Tally();
    final Tile temp[] = new Tile[8];
    
    Search <Tile> spread = new Search <Tile> (entrance, -1) {
      
      protected Tile[] adjacent(Tile spot) {
        return CityMap.adjacent(spot, temp, map, true);
      }
      
      protected boolean endSearch(Tile best) {
        float cost = fullCostEstimate(best);
        if (cost > MAX_AMBIENCE_DIST) return true;
        costs.set(best, cost);
        return false;
      }
      
      protected float cost(Tile prior, Tile spot) {
        return 1;
      }
      
      protected float estimate(Tile spot) {
        return 0;
      }
      
      protected void setEntry(Tile spot, Entry flag) { spot.flag = flag; }
      protected Entry entryFor(Tile spot) { return (Entry) spot.flag; }
    };
    spread.doSearch();
    
    
    float sumAmb = 0, sumWeights = 0;
    final int AMB_DIRS[] = { N, E, S, W, CENTRE };
    
    for (Tile tile : costs.keys()) {
      float cost = costs.valueFor(tile);
      
      for (int dir : AMB_DIRS) {
        Tile n = map.tileAt(tile.x + T_X[dir], tile.y + T_Y[dir]);
        if (n == null || n.above == null) continue;
        sumAmb     += n.above.type.ambience / (1 + cost);
        sumWeights += 1f / (1 + cost);
      }
    }
    
    float ambience = sumAmb / Nums.max(1, sumWeights);
    return ambience >= tier.homeAmbienceNeed;
  }
  
  
  boolean performAmenitiesCheck(ObjectType tier) {
    //
    //  This methods checks for the presence of all required amenities within
    //  a given wander-range.
    int maxRange  = Walker.MAX_WANDER_RANGE;
    boolean allOK = true;
    
    for (ObjectType t : tier.amenityNeeds) {
      for (Building b : map.buildings) {
        if (b.type != t) continue;
        
        float dist = CityMap.distance(b.entrance, entrance);
        if (dist > maxRange) continue;
        
        WalkerPathSearch search = new WalkerPathSearch(
          map, b.entrance, entrance, maxRange
        );
        search.doSearch();
        
        if (! search.success()) {
          allOK = false;
        }
      }
    }
    
    return allOK;
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void update() {
    super.update();
    advanceConsumption();
  }
  
  
  void advanceConsumption() {
    float conLevel = 1f / type.consumeTime;
    for (Good cons : type.consumed) {
      float amount = inventory.valueFor(cons);
      amount = Nums.max(0, amount - conLevel);
      inventory.set(cons, amount);
    }
  }
  
  
  
  /**  Orchestrating walker behaviour-
    */
  public void selectWalkerBehaviour(Walker walker) {
    //
    //  See if you can repair your own home:
    Building repairs = BuildingForCrafts.selectBuildTarget(
      this, type.buildsWith, new Batch(this)
    );
    if (repairs != null) {
      walker.embarkOnVisit(repairs, 10, JOB.BUILDING, this);
      return;
    }
    //
    //  Failing that, see if you can go shopping:
    Building goes = null;
    for (Good cons : type.consumed) {
      if (inventory.valueFor(cons) >= type.maxStock) continue;
      
      Building tried = findNearestWithFeature(IS_MARKET, 50);
      if (tried == null || tried.inventory.valueFor(cons) < 1) continue;
      
      goes = tried;
    }
    if (goes != null) {
      walker.embarkOnVisit(goes, 5, JOB.SHOPPING, this);
      return;
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    //  TODO:  Compare all nearby amenities!
    
    Pick <Building> pick = new Pick();

    pick.compare(this, 1.0f * Rand.num());
    goes = findNearestWithFeature(IS_AMENITY, 50);
    if (goes != null) {
      pick.compare(goes, 1.0f * Rand.num());
    }
    goes = pick.result();
    
    if (goes != this && goes != null) {
      walker.embarkOnVisit(goes, 25, JOB.VISITING, this);
    }
    else if (goes == this && Rand.yes()) {
      walker.embarkOnVisit(this, 10, JOB.RESTING, this);
    }
    else if (goes == this) {
      walker.startRandomWalk();
    }
    else {
      super.selectWalkerBehaviour(walker);
    }
  }
  
  
  public void walkerEnters(Walker walker, Building enters) {
    
    if (walker.jobType() == JOB.SHOPPING) {
      for (Good cons : type.consumed) {
        float stock = enters.inventory.valueFor(cons);
        
        if (enters == this) {
          walker.offloadGood(cons, this);
        }
        else if (stock > 0) {
          float taken = Nums.min(type.maxStock, stock / 2);
          walker.beginDelivery(enters, this, JOB.SHOPPING, cons, taken, this);
          return;
        }
      }
    }
  }
  
  
  public void walkerVisits(Walker walker, Building visits) {
    if (walker.jobType() == JOB.BUILDING) {
      BuildingForCrafts.advanceBuilding(walker, type.buildsWith, visits);
    }
  }
  
  
}





