


package game;
import util.*;
import static game.GameConstants.*;
import static game.CityBorders.*;



public class BuildingForTrade extends Building implements Trader {
  
  
  /**  Data fields, setup and save/load methods-
    */
  Tally <Good> tradeLevel = new Tally();
  City tradePartner = null;
  List <Good> tradeFixed = new List();
  Good needed[] = NO_GOODS, produced[] = NO_GOODS;
  
  
  public BuildingForTrade(Type type) {
    super(type);
  }
  
  
  public BuildingForTrade(Session s) throws Exception {
    super(s);
    s.loadTally(tradeLevel);
    s.loadObjects(tradeFixed);
    tradePartner = (City) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(tradeLevel);
    s.saveObjects(tradeFixed);
    s.saveObject(tradePartner);
  }

  
  
  /**  Updating demands-
    */
  public void setTradeLevels(boolean matchStock, Object... args) {
    tradeLevel.setWith(args);
    if (matchStock) for (Good g : tradeLevel.keys()) {
      setInventory(g, Nums.abs(tradeLevel.valueFor(g)));
    }
  }
  

  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    
    Batch <Good> impB = new Batch(), expB = new Batch();
    for (Good g : tradeLevel.keys()) {
      float level = tradeLevel.valueFor(g);
      if (level != 0) impB.add(g);
      if (level != 0) expB.add(g);
    }
    produced = impB.toArray(Good.class);
    needed   = expB.toArray(Good.class);
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return super.inventory(); }
  
  public Good[] needed  () { return needed  ; }
  public Good[] produced() { return produced; }
  
  public float stockNeeded(Good need) {
    return Nums.abs(tradeLevel.valueFor(need));
  }
  
  public float stockLimit (Good made) {
    return Nums.abs(tradeLevel.valueFor(made));
  }
  
  public float demandFor(Good g) {
    boolean consumes = accessible() && Visit.arrayIncludes(needed(), g);
    float need = consumes ? stockNeeded(g) : 0;
    return super.demandFor(g) + need;
  }
  
  
  public City homeCity() {
    return map.city;
  }
  
  
  
  /**  Selecting behaviour for walkers-
    */
  public void selectActorBehaviour(Actor actor) {
    
    if (actor == workers.first()) {
      selectTraderBehaviour(actor);
    }
    else {
      if (! actorIsHereWithPrompt(actor)) return;
      
      Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
      if (delivery != null) {
        actor.assignTask(delivery);
        return;
      }
      
      Task building = TaskBuilding.nextBuildingTask(this, actor);
      if (building != null) {
        actor.assignTask(building);
        return;
      }
    }
  }
  
  
  void selectTraderBehaviour(Actor trader) {
    
    class Order { Tally <Good> cargo; Trader goes; float rating; }
    List <Trader> targets = new List();
    List <Order> orders = new List();
    City homeCity = map.city;
    World world = homeCity.world;
    
    for (Building b : map.buildings) {
      if (b == this || ! (b instanceof Trader)) continue;
      targets.add((Trader) b);
    }
    
    if (tradePartner != null) {
      targets.add(tradePartner);
    }
    else for (City c : world.cities) {
      if (c == homeCity              ) continue;
      if (c.isEnemyOf(homeCity)      ) continue;
      if (c.distance (homeCity) == -1) continue;
      targets.add(c);
    }
    
    for (Trader t : targets) {
      World w = map.city.world;
      City c = (t == t.homeCity()) ? ((City) t) : null;
      Tally <Good> cargoAway = configureCargo(this, t, false, w);
      Tally <Good> cargoBack = configureCargo(t, this, true , w);
      
      float distRating = distanceRating(this, t);
      float rating = 0;
      
      if (cargoAway.size() > 0) {
        for (Good good : cargoAway.keys()) {
          rating += cargoAway.valueFor(good) * distRating;
        }
      }
      if (cargoBack.size() > 0) {
        for (Good good : cargoBack.keys()) {
          rating += cargoBack.valueFor(good) * distRating;
        }
      }
      
      if (homeCity.isVassalOf(c)) {
        rating *= 2.5f;
      }
      if (homeCity.isLordOf(c)) {
        rating *= 1.5f;
      }
      if (rating > 0) {
        Order order  = new Order();
        order.cargo  = cargoAway;
        order.goes   = t;
        order.rating = rating;
        orders.add(order);
      }
    }
    
    Pick <Order> pick = new Pick(0);
    for (Order o : orders) {
      pick.compare(o, o.rating);
    }
    
    if (! pick.empty()) {
      Order o = pick.result();
      
      if (reports()) {
        I.say("\n"+this+" assigning delivery to "+trader);
        I.say("  Cargo is: "+o.cargo);
        I.say("  Destination is: "+o.goes);
      }
      
      TaskTrading t = new TaskTrading(trader);
      t.configTrading(this, o.goes, o.cargo);
      trader.assignTask(t);
    }
    
  }
  
  
  
  
  
}














