

package game;
import util.*;
import static game.AreaMap.*;
import static game.Task.*;
import static game.GameConstants.*;
import static game.TaskDelivery.*;
import static util.TileConstants.*;



public class BuildingForHome extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public BuildingForHome(BuildType type) {
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
        if (! Visit.arrayIncludes(b.type().features, service)) continue;
        
        float dist = AreaMap.distance(b.mainEntrance(), entrance);
        if (dist > maxRange) continue;
        
        if (! map.pathCache.pathConnects(entrance, b.mainEntrance())) {
          continue;
        }
        
        access.add(1, b.type());
        access.add(b.type().featureAmount, service);
      }
    }
    return access;
  }
  
  
  boolean performAmbienceCheck(BuildType tier, Tally <Type> access) {
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
  
  
  boolean performServicesCheck(BuildType tier, Tally <Type> access) {
    boolean allOK = true;
    for (Type need : tier.serviceNeeds.keys()) {
      float amount = tier.serviceNeeds.valueFor(need);
      if (access.valueFor(need) < amount) allOK = false;
    }
    return allOK;
  }
  
  
  BuildType tierOffset(int off) {
    BuildType tiers[] = type().upgradeTiers;
    int index = Visit.indexOf(currentBuildingTier(), tiers);
    if (index == -1) return type();
    return tiers[Nums.clamp(index + off, tiers.length)];
  }
  
  
  
  /**  Stock and consumption-related checks:
    */
  boolean performConsumerCheck(BuildType tier, Tally <Type> access) {
    for (Good g : tier.homeUseGoods.keys()) {
      float amount = inventory(g);
      if (amount < maxTierStock(g, tier)) return false;
    }
    return true;
  }
  
  
  public float maxStock(Good g) {
    return maxTierStock(g, currentBuildingTier());
  }
  
  
  float maxTierStock(Good g, BuildType tier) {
    if (Visit.arrayIncludes(type().homeFoods, g)) return 5;
    return tier.homeUseGoods.valueFor(g);
  }
  
  
  public Batch <Good> usedBy(BuildType tier) {
    Batch <Good> consumes = new Batch();
    for (Good g : type().homeFoods        ) consumes.add(g);
    for (Good g : tier.homeUseGoods.keys()) consumes.add(g);
    return consumes;
  }
  
  
  public Tally <Good> homeUsed() {
    BuildType tier = tierOffset(1);
    Batch <Good> consumed = usedBy(tier);
    Tally <Good> cons = new Tally();
    for (Good g : consumed) cons.set(g, maxTierStock(g, tier));
    return cons;
  }
  
  
  
  /**  Life-cycle functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    if (period == 0 || ! map.world.settings.toggleBuildEvolve) {
      return;
    }
    
    BuildType currentTier = currentBuildingTier();
    BuildType nextTier = tierOffset(1), lastTier = tierOffset(-1);
    Tally <Type> access = checkServiceAccess();
    
    boolean nextAmbOK = performAmbienceCheck(nextTier   , access);
    boolean nextSerOK = performServicesCheck(nextTier   , access);
    boolean nextConOK = performConsumerCheck(nextTier   , access);
    boolean currAmbOK = performAmbienceCheck(currentTier, access);
    boolean currSerOK = performServicesCheck(currentTier, access);
    boolean currConOK = performConsumerCheck(currentTier, access);
    
    if (
      canBeginUpgrade(lastTier, true) &&
      ! (currAmbOK && currSerOK && currConOK)
    ) {
      beginRemovingUpgrade(currentTier);
    }
    else if (
      nextAmbOK && nextSerOK && nextConOK &&
      canBeginUpgrade(nextTier, false)
    ) {
      beginUpgrade(nextTier);
    }
    
    advanceHomeUse (nextTier);
    generateOutputs(nextTier);
  }
  
  
  public void setCurrentTier(BuildType tier) {
    BuildType tiers[] = type().upgradeTiers;
    if (Visit.arrayIncludes(tiers, tier)) return;
    
    for (BuildType t : tiers) {
      applyUpgrade(t);
      if (t == tier) break;
    }
  }
  
  
  void advanceHomeUse(BuildType tier) {
    float conLevel = (1 + (residents.size() * 1f / type().maxResidents)) / 2;
    conLevel *= type().updateTime;
    conLevel /= tier.homeUseTime;
    
    for (Good cons : tier.homeUseGoods.keys()) {
      float oldAmount = inventory(cons);
      float used      = maxTierStock(cons, tier) * conLevel;
      float amount    = Nums.max(0, oldAmount - used);
      setInventory(cons, amount);
      base().usedTotals.add(oldAmount - amount, cons);
    }
  }
  
  
  void generateOutputs(BuildType tier) {
    float taxLevel = residents.size() * TAX_VALUES[type().homeSocialClass];
    taxLevel *= (1 + Visit.indexOf(tier, type().upgradeTiers));
    taxLevel *= type().updateTime;
    taxLevel /= TAX_INTERVAL;
    addInventory(taxLevel, CASH);
    base().makeTotals.add(taxLevel, CASH);
  }
  
  
  static float wealthLevel(Actor actor) {
    if (actor.onMap()) {
      Building home = actor.home();
      if (home == null) return 0;
      BuildType type = home.type();
      if (type.category != Type.IS_HOME_BLD) return 0;
      
      BuildType currentTier = home.currentBuildingTier();
      float tier = Visit.indexOf(currentTier, type.upgradeTiers);
      tier /= Nums.max(1, type.upgradeTiers.length - 1);
      tier += type.homeSocialClass * 1f / CLASS_NOBLE;
      return tier / 2;
    }
    else {
      return actor.type().socialClass * 1f / CLASS_NOBLE;
    }
  }
  
  
  
  /**  Orchestrating actor behaviour-
    */
  public Task selectActorBehaviour(Actor actor) {
    //
    //  Non-adults don't do much-
    if (! actor.adult()) {
      return returnActorHere(actor);
    }
    //
    //  Non-nobles have work to do-
    if (actor.type().socialClass != CLASS_NOBLE) {
      //
      //  See if you can assist with building-projects:
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) {
        return building;
      }
      //
      //  Failing that, see if you can go shopping:
      BuildType tier = tierOffset(1);
      class Order { Building b; Good g; float amount; }
      Pick <Order> pickS = new Pick();
      
      for (Good cons : usedBy(tier)) {
        float need = 1 + maxTierStock(cons, tier);
        need -= inventory(cons);
        need -= totalFetchedFor(this, cons);
        if (need <= 0) continue;
        
        for (Building b : map.buildings) {
          if (! b.type().hasFeature(IS_VENDOR)) continue;
          
          float dist = distance(this, b);
          if (dist > 50) continue;
          
          float amount = b.inventory(cons);
          amount -= totalFetchedFrom(b, cons);
          if (amount < 1) continue;
          
          float rating = need * amount * distancePenalty(dist);
          Order o  = new Order();
          o.b      = b;
          o.g      = cons;
          o.amount = Nums.min(need, amount);
          pickS.compare(o, rating);
        }
      }
      if (! pickS.empty()) {
        Order o = pickS.result();
        TaskDelivery d = new TaskDelivery(actor);
        d.configDelivery(o.b, this, JOB.SHOPPING, o.g, o.amount, this);
        if (d != null) {
          return d;
        }
      }
    }
    //
    //  Failing that, select a leisure behaviour to perform:
    
    //  TODO:  Move this out to the basic AI for citizens!
    //  TODO:  Compare all nearby amenities!
    Pick <Building> pickV = new Pick();
    
    pickV.compare(this, 1.0f * Rand.num());
    Building goes = TaskDelivery.findNearestWithFeature(DIVERSION, 50, this);
    if (goes != null) {
      pickV.compare(goes, 1.0f * Rand.num());
    }
    goes = pickV.result();
    
    if (goes != this && goes != null) {
      Task visit = TaskResting.configResting(actor, goes);
      if (visit != null) return visit;
    }
    if (goes == this && Rand.yes()) {
      Task visit = TaskResting.configResting(actor, this);
      if (visit != null) return visit;
    }
    if (goes == this) {
      return TaskWander.configWandering(actor);
    }
    else {
      return super.selectActorBehaviour(actor);
    }
  }
  
  
  float totalFetchedFor(Building home, Good good) {
    float total = 0;
    
    List <Actor> all = home.residents.copy();
    for (Actor a : home.workers) all.include(a);
    
    for (Actor a : all) {
      if (! (a.task() instanceof TaskDelivery)) continue;
      TaskDelivery fetch = (TaskDelivery) a.task();
      if (fetch.carried == good) total += fetch.amount;
    }
    return total;
  }
  
  
  float totalFetchedFrom(Building store, Good good) {
    float total = 0;
    for (Active a : store.focused()) {
      if (! (a.task() instanceof TaskDelivery)) continue;
      TaskDelivery fetch = (TaskDelivery) a.task();
      if (fetch.from    != store) continue;
      if (fetch.carried == good ) total += fetch.amount;
    }
    return total;
  }
  
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    BuildType tier = currentBuildingTier();
    if (tier == null) return super.toString();
    return tier.name+" "+ID;
  }
}







