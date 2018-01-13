

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
    Tile entrance = mainEntrance();
    if (entrance == null) return access;
    int maxRange = MAX_WANDER_RANGE;
    
    //  TODO:  Include information about the number of *distinct* venues
    //         providing a given service (like religious access?)
    
    //  TODO:  The efficiency of this needs to be drastically improved for
    //         larger cities to work.
    
    for (Good service : SERVICE_TYPES) {
      for (Building b : map.buildings) {
        if (! Visit.arrayIncludes(b.type.features, service)) continue;
        
        float dist = CityMap.distance(b.mainEntrance(), entrance);
        if (dist > maxRange) continue;
        
        if (! map.pathCache.pathConnects(entrance, b.mainEntrance())) {
          continue;
        }
        
        access.add(1, b.type);
        access.add(b.type.featureAmount, service);
      }
    }
    return access;
  }
  
  
  boolean performAmbienceCheck(Type tier, Tally <Type> access) {
    //
    //  This method checks the surrounding tiles out to a distance of 6 tiles:
    final int MAX_AMBIENCE_DIST = 6;
    final Tally <Pathing> costs = new Tally();
    final Pathing temp[] = new Pathing[9];
    
    Search <Pathing> spread = new Search <Pathing> (this, -1) {
      
      protected Pathing[] adjacent(Pathing spot) {
        return spot.adjacent(temp, map);
      }
      
      protected boolean endSearch(Pathing best) {
        float cost = fullCostEstimate(best);
        if (cost > MAX_AMBIENCE_DIST) return true;
        costs.set(best, cost);
        return false;
      }
      
      protected float cost(Pathing prior, Pathing spot) {
        return 1;
      }
      
      protected float estimate(Pathing spot) {
        return 0;
      }
      
      protected void setEntry(Pathing spot, Entry flag) {
        spot.flagWith(flag);
      }
      
      protected Entry entryFor(Pathing spot) {
        return (Entry) spot.flaggedWith();
      }
    };
    spread.doSearch();
    
    float sumAmb = 0, sumWeights = 0;
    final int AMB_DIRS[] = { N, E, S, W, CENTRE };
    
    for (Pathing p : costs.keys()) if (p.isTile()) {
      Tile tile = (Tile) p;
      float cost = costs.valueFor(tile);
      for (int dir : AMB_DIRS) {
        Tile n = map.tileAt(tile.x + T_X[dir], tile.y + T_Y[dir]);
        if (n == null || n.above == null) continue;
        sumAmb     += n.above.ambience() / (1 + cost);
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
      int  amount = tier.upgradeUsage[i];
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
    for (Good g : tier.homeUseGoods) {
      float amount = inventory.valueFor(g);
      if (amount < maxTierStock(g, tier)) return false;
    }
    return true;
  }
  

  Good[] materials() {
    return currentTier.builtFrom;
  }
  
  
  float materialNeed(Good g) {
    int index = Visit.indexOf(g, currentTier.builtFrom);
    return index == -1 ? 0 : currentTier.builtAmount[index];
  }
  
  
  float maxStock(Good g) {
    return maxTierStock(g, currentTier);
  }
  
  
  float maxTierStock(Good g, Type tier) {
    if (Visit.arrayIncludes(FOOD_TYPES, g)) return 5;
    if (g == WATER) return 1;
    int index = Visit.indexOf(g, tier.homeUseGoods);
    if (index == -1) return 0;
    return tier.homeUsage[index];
  }
  
  
  Batch <Good> usedBy(Type tier) {
    Batch <Good> consumes = new Batch();
    consumes.add(WATER);
    for (Good g : FOOD_TYPES       ) consumes.add(g);
    for (Good g : tier.homeUseGoods) consumes.add(g);
    return consumes;
  }
  
  
  Tally <Good> homeUsed() {
    Type tier = tierOffset(1);
    Batch <Good> consumed = usedBy(tier);
    Tally <Good> cons = new Tally();
    for (Good g : consumed) cons.set(g, maxTierStock(g, tier));
    return cons;
  }
  
  
  
  /**  Life-cycle functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    if (! map.city.world.settings.toggleBuildEvolve) {
      return;
    }
    
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
  
  
  void setCurrentTier(Type tier) {
    if (Visit.arrayIncludes(type.upgradeTiers, tier)) return;
    currentTier = tier;
    
    //  TODO:  Create a distinction between the current and target
    //  tiers...
  }
  
  
  void advanceHomeUse(Type tier) {
    float conLevel = (1 + (residents.size() * 1f / type.maxResidents)) / 2;
    conLevel *= type.updateTime;
    conLevel /= tier.homeUseTime;
    
    for (Good cons : tier.homeUseGoods) {
      float oldAmount = inventory.valueFor(cons);
      float used      = maxTierStock(cons, tier) * conLevel;
      float amount    = Nums.max(0, oldAmount - used);
      inventory.set(cons, amount);
      map.city.usedTotals.add(oldAmount - amount, cons);
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
  
  
  static float wealthLevel(Actor actor) {
    if (actor.onMap()) {
      Building home = actor.home;
      if (home == null) return 0;
      Type type = home.type;
      if (type.category != Type.IS_HOME_BLD) return 0;
      
      Type currentTier = ((BuildingForHome) home).currentTier;
      float tier = Visit.indexOf(currentTier, type.upgradeTiers);
      tier /= Nums.max(1, type.upgradeTiers.length - 1);
      tier += type.homeSocialClass * 1f / CLASS_NOBLE;
      return tier / 2;
    }
    else {
      return actor.type.socialClass * 1f / CLASS_NOBLE;
    }
  }
  
  
  
  /**  Orchestrating actor behaviour-
    */
  public void selectActorBehaviour(Actor actor) {
    //
    //  Non-adults don't do much-
    if (! actor.adult()) {
      returnActorHere(actor);
    }
    //
    //  Failing that, see if you can go shopping:
    Type tier = tierOffset(1);
    class Order { Building b; Good g; float amount; }
    Pick <Order> pickS = new Pick();
    
    for (Good cons : usedBy(tier)) {
      float need = 1 + maxTierStock(cons, tier);
      need -= inventory.valueFor(cons);
      need -= totalFetchedFor(this, cons);
      if (need <= 0) continue;
      
      for (Building b : map.buildings) {
        if (! b.type.hasFeature(IS_VENDOR)) continue;
        
        float dist = CityMap.distance(this, b);
        if (dist > 50) continue;
        
        float amount = b.inventory.valueFor(cons);
        amount -= totalFetchedFrom(b, cons);
        if (amount < 1) continue;
        
        float rating = need * amount * CityMap.distancePenalty(dist);
        Order o = new Order();
        o.b = b;
        o.g = cons;
        o.amount = need;
        pickS.compare(o, rating);
      }
    }
    if (! pickS.empty()) {
      Order o = pickS.result();
      TaskDelivery d = new TaskDelivery(actor);
      d.configDelivery(o.b, this, JOB.SHOPPING, o.g, o.amount, this);
      if (d != null) {
        //I.say("Delivering "+o.amount+" "+o.g+" from "+o.b+" to "+this);
        actor.assignTask(d);
        return;
      }
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    
    //  TODO:  Move this out to the basic AI for citizens.
    //  TODO:  Compare all nearby amenities!
    Pick <Building> pickV = new Pick();
    
    pickV.compare(this, 1.0f * Rand.num());
    Building goes = TaskDelivery.findNearestWithFeature(DIVERSION, 50, this);
    if (goes != null) {
      pickV.compare(goes, 1.0f * Rand.num());
    }
    goes = pickV.result();
    
    if (goes != this && goes != null) {
      actor.embarkOnVisit(goes, 10, JOB.VISITING, this);
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
  
  
  float totalFetchedFor(Building home, Good good) {
    float total = 0;
    for (Actor a : home.residents) {
      if (! (a.task instanceof TaskDelivery)) continue;
      TaskDelivery fetch = (TaskDelivery) a.task;
      if (fetch.carried == good) total += fetch.amount;
    }
    return total;
  }
  
  
  float totalFetchedFrom(Building store, Good good) {
    float total = 0;
    for (Actor a : store.focused()) {
      if (! (a.task instanceof TaskDelivery)) continue;
      TaskDelivery fetch = (TaskDelivery) a.task;
      if (fetch.carried == good) total += fetch.amount;
    }
    return total;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    if (currentTier == null) return super.toString();
    return currentTier.name+" "+ID;
  }
}





