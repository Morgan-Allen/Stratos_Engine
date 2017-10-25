

package game;
import util.*;
import static game.CityMap.*;
import static game.Task.*;
import static game.GameConstants.*;
import static util.TileConstants.*;

import game.GameConstants.Good;



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
      
      protected void setEntry(Tile spot, Entry flag) { spot.pathFlag = flag; }
      protected Entry entryFor(Tile spot) { return (Entry) spot.pathFlag; }
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
  
  
  Type tierOffset(int off) {
    Type tiers[] = type.upgradeTiers;
    int index = Visit.indexOf(currentTier, tiers);
    if (index == -1) return type;
    return tiers[Nums.clamp(index + off, tiers.length)];
  }
  
  
  
  /**  Stock and consumption-related checks:
    */
  boolean performConsumerCheck(Type tier, Tally <Type> access) {
    for (Good g : tier.homeUsed) {
      float amount = inventory.valueFor(g);
      if (amount < maxStock(g)) return false;
    }
    return true;
  }
  
  
  float maxStock(Good g) {
    if (Visit.arrayIncludes(FOOD_TYPES, g)) return 5;
    return type.maxStock;
  }
  
  
  Batch <Good> usedBy(Type tier) {
    Batch <Good> consumes = new Batch();
    consumes.add(WATER);
    for (Good g : FOOD_TYPES   ) consumes.add(g);
    for (Good g : tier.homeUsed) consumes.add(g);
    return consumes;
  }
  
  
  Tally <Good> homeUsed() {
    Type tier = tierOffset(1);
    Batch <Good> consumed = usedBy(tier);
    Tally <Good> cons = new Tally();
    for (Good g : consumed) cons.set(g, maxStock(g));
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
    
    advanceHomeUse (nextTier);
    generateOutputs(nextTier);
  }
  
  
  void advanceHomeUse(Type tier) {
    float conLevel = (1 + (residents.size() * 1f / type.maxResidents)) / 2;
    conLevel *= type.updateTime;
    conLevel /= tier.homeUseTime;
    
    for (Good cons : tier.homeUsed) {
      float amount = inventory.valueFor(cons);
      amount = Nums.max(0, amount - conLevel);
      inventory.set(cons, amount);
      map.city.usedTotals.add(conLevel, cons);
    }
  }
  
  
  void generateOutputs(Type tier) {
    float soilLevel = residents.size();
    soilLevel *= type.updateTime;
    soilLevel /= FECES_UNIT_TIME;
    inventory.add(soilLevel, SOIL);
    map.city.makeTotals.add(soilLevel, SOIL);

    float taxLevel = residents.size() * TAX_VALUES[type.homeSocialClass];
    taxLevel *= (1 + Visit.indexOf(tier, type.upgradeTiers));
    taxLevel *= type.updateTime;
    taxLevel /= TAX_INTERVAL;
    inventory.add(taxLevel, CASH);
    map.city.makeTotals.add(taxLevel, CASH);
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
  
  
  
  /**  Orchestrating actor behaviour-
    */
  public void selectActorBehaviour(Actor actor) {
    //
    //  Non-adults don't do much-
    if (! actor.adult()) {
      actor.returnTo(this);
    }
    //
    //  See if you can repair your own home:
    Building repairs = BuildingForCrafts.selectBuildTarget(
      this, type.buildsWith, new Batch(this)
    );
    if (repairs != null) {
      actor.embarkOnVisit(repairs, 10, JOB.BUILDING, this);
      return;
    }
    //
    //  Failing that, see if you can go shopping:
    Type tier = tierOffset(1);
    class Order { Building b; Good g; }
    Pick <Order> pickS = new Pick();
    
    for (Good cons : usedBy(tier)) {
      float need = maxStock(cons) + 1 - inventory.valueFor(cons);
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
      actor.embarkOnVisit(o.b, 5, JOB.SHOPPING, this);
      return;
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    //  TODO:  Compare all nearby amenities!
    Pick <Building> pickV = new Pick();
    
    pickV.compare(this, 1.0f * Rand.num());
    Building goes = TaskDelivery.findNearestWithFeature(DIVERSION, 50, this);
    if (goes != null) {
      pickV.compare(goes, 1.0f * Rand.num());
    }
    goes = pickV.result();
    
    if (goes != this && goes != null) {
      actor.embarkOnVisit(goes, 25, JOB.VISITING, this);
    }
    else if (goes == this && Rand.yes()) {
      actor.embarkOnVisit(this, 10, JOB.RESTING, this);
    }
    else if (goes == this) {
      actor.startRandomWalk();
    }
    else {
      super.selectActorBehaviour(actor);
    }
  }
  
  
  public void actorEnters(Actor actor, Building enters) {
    Type tier = tierOffset(1);
    
    if (actor.jobType() == JOB.SHOPPING) {
      
      if (enters == this) {
        actor.offloadGood(actor.carried, this);
      }
      
      else for (Good cons : usedBy(tier)) {
        float maxStock = maxStock(cons) + 1;
        if (inventory.valueFor(cons) >= maxStock) continue;
        
        float stock = enters.inventory.valueFor(cons);
        if (stock <= 0) continue;
        
        float taken = Nums.min(maxStock, stock / 2);
        
        TaskDelivery d = new TaskDelivery(actor);
        d.configDelivery(enters, this, JOB.SHOPPING, cons, taken, this);
        if (d != null) actor.assignTask(d);
        
        break;
      }
    }
  }
  
  
  public void actorVisits(Actor actor, Building visits) {
    if (actor.jobType() == JOB.BUILDING) {
      BuildingForCrafts.advanceBuilding(actor, type.buildsWith, visits);
    }
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    if (currentTier == null) return super.toString();
    return currentTier.name+" "+ID;
  }
}










