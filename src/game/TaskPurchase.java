

package game;
import util.*;
import static game.GameConstants.*;




public class TaskPurchase extends Task {
  
  
  /**  Data fields, construction and save/load methods-
    */
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
  
  
  
  /**  External factory methods-
    */
  static TaskPurchase nextPurchase(ActorAsPerson actor) {
    Pick <TaskPurchase> pick = new Pick(0);
    boolean hasPurchase = false;
    
    for (Task t : actor.todo()) if (t instanceof TaskPurchase) {
      hasPurchase = true;
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
    
    int cash     = (int) actor.outfit.carried(CASH);
    int oldLevel = (int) actor.outfit.carried(good);
    int levelCap = good.isWeapon || good.isArmour ? good.maxQuality : good.maxCarried;
    
    for (int bonus = levelCap; bonus > 0; bonus--) {
      int level = oldLevel + bonus;
      if (level > levelCap) continue;
      
      int price = good.isUsable ? (good.price * bonus) : good.priceLevels[level - 1];
      if (price > cash) continue;
      
      TaskPurchase task = new TaskPurchase(actor, shop, good, level, price);
      return (TaskPurchase) task.configTask(null, shop, null, JOB.SHOPPING, 0);
    }
    
    return null;
  }
  
  
  
  /**  Priority-evaluation-
    */
  protected float successPriority() {
    //  TODO:  Base the chance of this on intelligence, etc.?
    
    //Actor actor = (Actor) active;
    float priority = ROUTINE;
    
    if (type == JOB.SHOPPING) {
      //priority *= 1 + (actor.traits.levelOf(TRAIT_INTELLECT) / MAX_SKILL_LEVEL);
      priority = URGENT;
    }
    if (type == JOB.COLLECTING) {
      priority = PARAMOUNT;
    }
    
    return priority;
  }
  
  
  
  /**  Actual behaviour-execution-
    */
  protected int checkResume() {
    Actor client = (Actor) active;
    
    if (! shop.hasItemOrder (itemType, client)) return RESUME_NO;
    if (! shop.orderComplete(itemType, client)) return RESUME_WAIT;
    
    configTask(null, shop, null, JOB.COLLECTING, 0);
    if (! pathValid()) return RESUME_NO;
    
    return RESUME_YES;
  }


  protected void onVisit(Building visits) {
    ActorAsPerson actor = (ActorAsPerson) this.active;
    
    if (type == JOB.SHOPPING && visits == shop) {
      shop.addItemOrder(itemType, quality, actor);
      actor.addTodo(this);
      
      actor.outfit.incCarried(CASH, 0 - pricePays);
      shop.addInventory(pricePays, CASH);
    }
    
    if (type == JOB.COLLECTING) {
      shop.removeOrder(itemType, actor);
      
      if (itemType.isUsable) actor.outfit.incCarried(itemType, 1);
      else actor.outfit.setCarried(itemType, quality);
    }
  }
  
}






