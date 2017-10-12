

package game;
import util.*;
import static game.CityMap.*;
import static game.Task.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForHome extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  ObjectType currentTier;
  
  
  BuildingForHome(ObjectType type) {
    super(type);
    this.currentTier = type;
  }
  
  
  public BuildingForHome(Session s) throws Exception {
    super(s);
    currentTier = (ObjectType) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(currentTier);
  }
  
  
  
  /**  Passive ambience and access-checks:
    */
  Tally <ObjectType> checkServiceAccess() {
    //
    //  This methods checks for the presence of all required amenities within
    //  a given wander-range.
    Tally <ObjectType> access = new Tally();
    if (entrance == null) return access;
    int maxRange = MAX_WANDER_RANGE;
    
    //  TODO:  Include information about the number of *distinct* venues
    //  providing a given service (like religious access?)
    
    for (Good service : SERVICE_TYPES) {
      for (Building b : map.buildings) {
        if (! Visit.arrayIncludes(b.type.features, service)) continue;
        
        float dist = CityMap.distance(b.entrance, entrance);
        if (dist > maxRange) continue;
        
        WalkerPathSearch search = new WalkerPathSearch(
          map, b.entrance, entrance, maxRange
        );
        search.doSearch();
        if (! search.success()) continue;
        
        access.add(1, b.type);
        access.add(b.type.featureAmount, service);
      }
    }
    return access;
  }
  
  
  boolean performAmbienceCheck(ObjectType tier, Tally <ObjectType> access) {
    //
    //  This method checks the surrounding tiles out to a distance of 6 tiles:
    if (entrance == null) return false;
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
  
  
  boolean performServicesCheck(ObjectType tier, Tally <ObjectType> access) {
    boolean allOK = true;
    for (int i = tier.upgradeNeeds.length; i-- > 0;) {
      ObjectType need   = tier.upgradeNeeds[i];
      int        amount = tier.needAmounts [i];
      if (access.valueFor(need) < amount) allOK = false;
    }
    return allOK;
  }
  
  
  boolean performConsumerCheck(ObjectType tier, Tally <ObjectType> access) {
    for (Good g : tier.consumed) {
      float amount = inventory.valueFor(g);
      if (amount < tier.maxStock) return false;
    }
    return true;
  }
  
  
  ObjectType tierOffset(int off) {
    ObjectType tiers[] = type.upgradeTiers;
    int index = Visit.indexOf(currentTier, tiers);
    if (index == -1) return type;
    return tiers[Nums.clamp(index + off, tiers.length)];
  }
  
  
  Batch <Good> consumedBy(ObjectType tier) {
    Batch <Good> consumes = new Batch();
    consumes.add(WATER);
    for (Good g : tier.consumed) consumes.add(g);
    //  TODO:  Add the various food types as well?
    return consumes;
  }
  
  
  
  /**  Life-cycle functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    ObjectType nextTier = tierOffset(1), lastTier = tierOffset(-1);
    Tally <ObjectType> access = checkServiceAccess();
    
    boolean nextAmbOK = performAmbienceCheck(nextTier   , access);
    boolean nextSerOK = performServicesCheck(nextTier   , access);
    boolean nextConOK = performConsumerCheck(nextTier   , access);
    boolean currAmbOK = performAmbienceCheck(currentTier, access);
    boolean currSerOK = performServicesCheck(currentTier, access);
    boolean currConOK = performConsumerCheck(currentTier, access);
    
    if (! (currAmbOK && currSerOK && currConOK)) {
      currentTier = lastTier;
    }
    else if (nextAmbOK && nextSerOK && nextConOK) {
      currentTier = nextTier;
    }
    
    advanceConsumption(nextTier);
    generateTax(nextTier);
  }
  
  
  void advanceConsumption(ObjectType tier) {
    float conLevel = 1f * residents.size() / tier.consumeTime;
    conLevel *= type.updateTime;
    
    for (Good cons : consumedBy(tier)) {
      float amount = inventory.valueFor(cons);
      amount = Nums.max(0, amount - conLevel);
      inventory.set(cons, amount);
    }
  }
  
  
  void generateTax(ObjectType tier) {
    float conLevel = 1f * residents.size() / tier.consumeTime;
    conLevel *= type.updateTime;
    
    float taxGen = TAX_VALUES[type.homeSocialClass] * conLevel;
    taxGen *= (1 + Visit.indexOf(tier, type.upgradeTiers));
    inventory.add(taxGen  , TAXES    );
    inventory.add(conLevel, NIGHTSOIL);
  }
  
  
  static float wealthLevel(Building home) {
    ObjectType type = home.type;
    if (type.category != ObjectType.IS_HOME_BLD) return 0;
    
    ObjectType currentTier = ((BuildingForHome) home).currentTier;
    float tier = Visit.indexOf(currentTier, type.upgradeTiers);
    tier /= Nums.max(1, type.upgradeTiers.length - 1);
    tier += type.homeSocialClass * 1f / CLASS_NOBLE;
    return tier / 2;
  }
  
  
  
  /**  Orchestrating walker behaviour-
    */
  public void selectWalkerBehaviour(Walker walker) {
    //
    //  Non-adults don't do much-
    if (! walker.adult()) {
      walker.returnTo(this);
    }
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
    ObjectType tier = tierOffset(1);
    class Order { Building b; Good g; }
    Pick <Order> pickS = new Pick();
    
    for (Good cons : consumedBy(tier)) {
      float need = tier.maxStock - inventory.valueFor(cons);
      if (need <= 0) continue;
      
      for (Building b : map.buildings) {
        if (! b.type.hasFeature(IS_MARKET)) continue;
        
        float dist = CityMap.distance(entrance, b.entrance);
        if (dist > 50) continue;
        
        float amount = b.inventory.valueFor(cons);
        if (amount < 1) continue;
        
        float rating = need * amount * CityMap.distancePenalty(dist);
        Order o = new Order();
        o.b = b;
        o.g = cons;
        pickS.compare(o, rating);
      }
    }
    if (! pickS.empty()) {
      Order o = pickS.result();
      walker.embarkOnVisit(o.b, 5, JOB.SHOPPING, this);
      return;
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    //  TODO:  Compare all nearby amenities!
    Pick <Building> pickV = new Pick();
    
    pickV.compare(this, 1.0f * Rand.num());
    Building goes = findNearestWithFeature(DIVERSION, 50);
    if (goes != null) {
      pickV.compare(goes, 1.0f * Rand.num());
    }
    goes = pickV.result();
    
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
    ObjectType tier = tierOffset(1);
    
    if (walker.jobType() == JOB.SHOPPING) {
      
      if (enters == this) {
        walker.offloadGood(walker.carried, this);
      }
      
      else for (Good cons : consumedBy(tier)) {
        if (this.inventory.valueFor(cons) >= tier.maxStock) continue;
        
        float stock = enters.inventory.valueFor(cons);
        if (stock <= 0) continue;
        
        float taken = Nums.min(tier.maxStock, stock / 2);
        walker.beginDelivery(enters, this, JOB.SHOPPING, cons, taken, this);
        break;
      }
    }
  }
  
  
  public void walkerVisits(Walker walker, Building visits) {
    if (walker.jobType() == JOB.BUILDING) {
      BuildingForCrafts.advanceBuilding(walker, type.buildsWith, visits);
    }
  }
  
}







