

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
    AreaTile entrance = mainEntrance();
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
      AreaTile tile = (AreaTile) p;
      float cost = costs.valueFor(tile);
      for (int dir : AMB_DIRS) {
        AreaTile n = map.tileAt(tile.x + T_X[dir], tile.y + T_Y[dir]);
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
      if (g.isEdible) continue;
      float amount = inventory(g);
      if (amount < maxTierStock(g, tier)) return false;
    }
    return true;
  }
  
  
  public float maxStock(Good g) {
    return maxTierStock(g, currentBuildingTier());
  }
  
  
  float maxTierStock(Good g, BuildType tier) {
    return tier.homeUseGoods.valueFor(g);
  }
  
  
  public Batch <Good> usedBy(BuildType tier) {
    Batch <Good> consumes = new Batch();
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
      canBeginUpgrade(nextTier, false) &&
      (nextAmbOK && nextSerOK && nextConOK)
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
      base().trading.usedTotals.add(oldAmount - amount, cons);
    }
  }
  
  
  void generateOutputs(BuildType tier) {
    float taxLevel = 0;
    int tierID = Visit.indexOf(tier, type().upgradeTiers);
    for (Actor a : residents) taxLevel += TAX_VALUES[a.type().socialClass];
    taxLevel *= TIER_VALUES[tierID] / 100f;
    taxLevel *= type().updateTime * 1f / TAX_INTERVAL;
    addInventory(taxLevel, CASH);
    base().trading.makeTotals.add(taxLevel, CASH);
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
      tier += type.homeComfortLevel * 1f / AVG_HOME_COMFORT;
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
    if (! actor.health.adult()) {
      return TaskWaiting.configWaiting(
        actor, this, TaskWaiting.TYPE_DOMESTIC, this
      );
    }
    //
    //  But non-nobles have work to do-
    if (actor.type().socialClass != CLASS_NOBLE) {
      TaskDelivery shopping = TaskDelivery.pickNextShopping(actor, this, homeUsed());
      if (shopping != null) return shopping;
    }
    return null;
  }
  
  
  
  /**  Rendering, debug and interface methods-
    */
  public String toString() {
    BuildType tier = currentBuildingTier();
    if (tier == null) return super.toString();
    return tier.name+" "+ID;
  }
}







