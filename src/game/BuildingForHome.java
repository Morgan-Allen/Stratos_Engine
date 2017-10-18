

package game;
import util.*;
import static game.CityMap.*;
import static game.Task.*;
import static game.GameConstants.*;
import static util.TileConstants.*;



public class BuildingForHome extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Type currentTier;
  
  
  BuildingForHome(Type type) {
    super(type);
    this.currentTier = type;
  }
  
  
  public BuildingForHome(Session s) throws Exception {
    super(s);
    currentTier = (Type) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(currentTier);
  }
  
  
  
  /**  Passive ambience and access-checks:
    */
  Tally <Type> checkServiceAccess() {
    //
    //  This methods checks for the presence of all required amenities within
    //  a given wander-range.
    Tally <Type> access = new Tally();
    if (entrance == null) return access;
    int maxRange = MAX_WANDER_RANGE;
    
    //  TODO:  Include information about the number of *distinct* venues
    //  providing a given service (like religious access?)
    
    for (Good service : SERVICE_TYPES) {
      for (Building b : map.buildings) {
        if (! Visit.arrayIncludes(b.type.features, service)) continue;
        
        float dist = CityMap.distance(b.entrance, entrance);
        if (dist > maxRange) continue;
        
        ActorPathSearch search = new ActorPathSearch(
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
  
  
  boolean performAmbienceCheck(Type tier, Tally <Type> access) {
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
  
  
  boolean performServicesCheck(Type tier, Tally <Type> access) {
    boolean allOK = true;
    for (int i = tier.upgradeNeeds.length; i-- > 0;) {
      Type need   = tier.upgradeNeeds[i];
      int        amount = tier.needAmounts [i];
      if (access.valueFor(need) < amount) allOK = false;
    }
    return allOK;
  }
  
  
  boolean performConsumerCheck(Type tier, Tally <Type> access) {
    for (Good g : tier.consumed) {
      float amount = inventory.valueFor(g);
      if (amount < tier.maxStock) return false;
    }
    return true;
  }
  
  
  Type tierOffset(int off) {
    Type tiers[] = type.upgradeTiers;
    int index = Visit.indexOf(currentTier, tiers);
    if (index == -1) return type;
    return tiers[Nums.clamp(index + off, tiers.length)];
  }
  
  
  Batch <Good> consumedBy(Type tier) {
    Batch <Good> consumes = new Batch();
    consumes.add(WATER);
    for (Good g : FOOD_TYPES   ) consumes.add(g);
    for (Good g : tier.consumed) consumes.add(g);
    return consumes;
  }
  
  
  Tally <Good> homeConsumption() {
    Type tier = tierOffset(1);
    Batch <Good> consumed = consumedBy(tier);
    Tally <Good> cons = new Tally();
    for (Good g : consumed) cons.set(g, tier.maxStock);
    return cons;
  }
  
  
  
  /**  Life-cycle functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    Type nextTier = tierOffset(1), lastTier = tierOffset(-1);
    Tally <Type> access = checkServiceAccess();
    
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
    generateOutputs(nextTier);
  }
  
  
  void advanceConsumption(Type tier) {
    float conLevel = residents.size() * 1f / tier.consumeTime;
    conLevel *= type.updateTime;
    
    for (Good cons : tier.consumed) {
      float amount = inventory.valueFor(cons);
      amount = Nums.max(0, amount - conLevel);
      inventory.set(cons, amount);
    }
  }
  
  
  void generateOutputs(Type tier) {
    float conLevel = 1f * residents.size() / tier.consumeTime;
    conLevel *= type.updateTime;
    inventory.add(conLevel, SOIL);
    
    float taxGen = TAX_VALUES[type.homeSocialClass] * conLevel;
    taxGen *= (1 + Visit.indexOf(tier, type.upgradeTiers));
    inventory.add(taxGen, CASH);
  }
  
  
  static float wealthLevel(Building home) {
    Type type = home.type;
    if (type.category != Type.IS_HOME_BLD) return 0;
    
    Type currentTier = ((BuildingForHome) home).currentTier;
    float tier = Visit.indexOf(currentTier, type.upgradeTiers);
    tier /= Nums.max(1, type.upgradeTiers.length - 1);
    tier += type.homeSocialClass * 1f / CLASS_NOBLE;
    return tier / 2;
  }
  
  
  
  /**  Orchestrating walker behaviour-
    */
  public void selectWalkerBehaviour(Actor walker) {
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
    Type tier = tierOffset(1);
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
  
  
  public void walkerEnters(Actor walker, Building enters) {
    Type tier = tierOffset(1);
    
    if (walker.jobType() == JOB.SHOPPING) {
      
      if (enters == this) {
        walker.offloadGood(walker.carried, this);
      }
      
      else for (Good cons : consumedBy(tier)) {
        if (this.inventory.valueFor(cons) >= tier.maxStock) continue;
        
        float stock = enters.inventory.valueFor(cons);
        if (stock <= 0) continue;
        
        float taken = tier.maxStock;
        if (Visit.arrayIncludes(FOOD_TYPES, cons)) taken *= 2;
        taken = Nums.min(taken, stock / 2);
        
        walker.beginDelivery(enters, this, JOB.SHOPPING, cons, taken, this);
        break;
      }
    }
  }
  
  
  public void walkerVisits(Actor walker, Building visits) {
    if (walker.jobType() == JOB.BUILDING) {
      BuildingForCrafts.advanceBuilding(walker, type.buildsWith, visits);
    }
  }
  
}







