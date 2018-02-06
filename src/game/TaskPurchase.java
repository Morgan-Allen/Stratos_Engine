

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
  
  
  
  static Series <TaskPurchase> configPurchases(
    Actor actor, BuildingForCrafts shop
  ) {
    //  TODO:  Consider just ordering the cheapest of these?
    Batch <TaskPurchase> purchases = new Batch();
    for (Good g : shop.type().canOrder) {
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
    if (shop.hasItemOrder(good, actor)) {
      return null;
    }
    
    int oldLevel = (int) actor.carried(good);
    int levelCap = good.isWeapon || good.isArmour ? good.maxQuality : good.maxCarried;
    
    for (int bonus = levelCap; bonus > 0; bonus--) {
      int level = oldLevel + bonus;
      if (level > levelCap) continue;
      
      int price = good.isUsable ? good.price : good.priceLevels[level - 1];
      if (price > actor.carried(CASH)) return null;
      
      TaskPurchase task = new TaskPurchase(actor, shop, good, level, price);
      return (TaskPurchase) task.configTask(null, shop, null, JOB.SHOPPING, 0);
    }
    
    return null;
  }
  
  
  static TaskPurchase resumePurchase(TaskPurchase p) {
    if (! p.shop.orderComplete(p.itemType, p.actor)) return null;
    return (TaskPurchase) p.configTask(null, p.shop, null, JOB.COLLECTING, 0);
  }
  
  
  protected void onVisit(Building visits) {
    if (actor.jobType() == JOB.SHOPPING && visits == shop) {
      shop.addItemOrder(itemType, quality, actor);
      ((ActorAsPerson) actor).todo.add(this);
      
      actor.incCarried(CASH, pricePays);
      shop.addInventory(pricePays, CASH);
    }
    if (actor.jobType() == JOB.COLLECTING) {
      shop.removeOrder(itemType, actor);
      ((ActorAsPerson) actor).todo.remove(this);
      
      if (itemType.isUsable) actor.incCarried(itemType, 1);
      else actor.setCarried(itemType, quality);
    }
  }
  
  
}






