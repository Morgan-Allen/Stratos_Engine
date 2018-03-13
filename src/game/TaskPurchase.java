

package game;
import util.*;
import static game.GameConstants.*;




public class TaskPurchase extends Task {
  
  
  BuildingForCrafts shop;
  Good itemType;
  int quality;
  int pricePays;
  
  
  public TaskPurchase(
    Actor actor, BuildingForCrafts shop,
    Good type, int quality, int pricePays
  ) {
    super(actor);
    this.shop      = shop;
    this.itemType  = type;
    this.quality   = quality;
    this.pricePays = pricePays;
  }
  
  
  public TaskPurchase(Session s) throws Exception {
    super(s);
    this.shop      = (BuildingForCrafts) s.loadObject();
    this.itemType  = (Good) s.loadObject();
    this.quality   = s.loadInt();
    this.pricePays = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(shop);
    s.saveObject(itemType);
    s.saveInt(quality);
    s.saveInt(pricePays);
  }
  
  
  static TaskPurchase nextPurchase(ActorAsPerson actor) {
    //  TODO:  Base the chance of this on intelligence, etc.?
    
    Pick <TaskPurchase> pick = new Pick(0);
    boolean hasPurchase = false;
    
    for (Task t : actor.todo) if (t instanceof TaskPurchase) {
      TaskPurchase p = (TaskPurchase) t;
      if (! p.shop.hasItemOrder(p.itemType, actor)) {
        actor.todo.remove(t);
      }
      else {
        hasPurchase = true;
        p = resumePurchase(p);
        if (p != null) pick.compare(p, p.priority() * Rand.num());
      }
    }
    
    if (! hasPurchase) for (Building b : actor.map().buildings()) {
      if (b.base() != actor.base()) continue;
      if (! (b instanceof BuildingForCrafts)) continue;
      BuildingForCrafts shop = (BuildingForCrafts) b;
      for (TaskPurchase p : configPurchases(actor, shop)) {
        if (p == null) continue;
        pick.compare(p, p.priority() * Rand.num());
      }
    }
    
    return pick.result();
  }
  
  
  static Series <TaskPurchase> configPurchases(
    Actor actor, BuildingForCrafts shop
  ) {
    //  TODO:  Consider just ordering the cheapest of these?
    Batch <TaskPurchase> purchases = new Batch();
    if (! actor.map().world.settings.togglePurchases) return purchases;
    
    for (Good g : shop.shopItems()) {
      if (shop.hasItemOrder(g, actor)) continue;
      
      if (g == actor.type().weaponType) {
        purchases.add(configNextPurchase(actor, g, shop));
      }
      if (g == actor.type().armourType) {
        purchases.add(configNextPurchase(actor, g, shop));
      }
      if (Visit.arrayIncludes(actor.type().useItemTypes, g)) {
        purchases.add(configNextPurchase(actor, g, shop));
      }
    }
    return purchases;
  }
  
  
  static TaskPurchase configNextPurchase(
    Actor actor, Good good, BuildingForCrafts shop
  ) {
    if (! actor.map().world.settings.togglePurchases) {
      return null;
    }
    if (shop.hasItemOrder(good, actor)) {
      return null;
    }
    
    int cash     = (int) actor.carried(CASH);
    int oldLevel = (int) actor.carried(good);
    int levelCap = good.isWeapon || good.isArmour ? good.maxQuality : good.maxCarried;
    
    for (int bonus = levelCap; bonus > 0; bonus--) {
      int level = oldLevel + bonus;
      if (level > levelCap) continue;
      
      int price = good.isUsable ? good.price : good.priceLevels[level - 1];
      if (price > cash) continue;
      
      TaskPurchase task = new TaskPurchase(actor, shop, good, level, price);
      return (TaskPurchase) task.configTask(null, shop, null, JOB.SHOPPING, 0);
    }
    
    return null;
  }
  
  
  static TaskPurchase resumePurchase(TaskPurchase p) {
    if (! p.shop.orderComplete(p.itemType, (Actor) p.active)) return null;
    return (TaskPurchase) p.configTask(null, p.shop, null, JOB.COLLECTING, 0);
  }
  
  
  
  protected float successChance() {
    Actor actor = (Actor) this.active;
    float priority = ROUTINE;
    
    if (type == JOB.SHOPPING) {
      //priority *= 1 + (actor.levelOf(TRAIT_INTELLECT) / MAX_SKILL_LEVEL);
      priority = URGENT;
    }
    if (type == JOB.COLLECTING) {
      priority = PARAMOUNT;
    }
    
    return priority;
  }


  protected void onVisit(Building visits) {
    Actor actor = (Actor) this.active;
    
    if (type == JOB.SHOPPING && visits == shop) {
      shop.addItemOrder(itemType, quality, actor);
      ((ActorAsPerson) actor).todo.add(this);
      
      actor.incCarried(CASH, 0 - pricePays);
      shop.addInventory(pricePays, CASH);
    }
    
    if (type == JOB.COLLECTING) {
      shop.removeOrder(itemType, actor);
      ((ActorAsPerson) actor).todo.remove(this);
      
      if (itemType.isUsable) actor.incCarried(itemType, 1);
      else actor.setCarried(itemType, quality);
    }
  }
  
}






