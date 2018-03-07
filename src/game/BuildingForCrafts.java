

package game;
import util.*;
import static game.GameConstants.*;



public class BuildingForCrafts extends Building {
  
  
  /**  Data fields, construction and save/load methods-
    */
  static class ItemOrder {
    Good itemType;
    int quality;
    Actor client;
    float progress;
    int timePlaced = -1;
  }
  
  List <ItemOrder> orders = new List();
  
  
  public BuildingForCrafts(BuildType type) {
    super(type);
  }
  
  
  public BuildingForCrafts(Session s) throws Exception {
    super(s);
    
    for (int n = s.loadInt(); n-- > 0;) {
      ItemOrder o = new ItemOrder();
      o.itemType = (Good) s.loadObject();
      o.quality  = s.loadInt();
      o.client   = (Actor) s.loadObject();
      o.progress = s.loadFloat();
      o.timePlaced = s.loadInt();
      orders.add(o);
    }
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveInt(orders.size());
    for (ItemOrder o : orders) {
      s.saveObject(o.itemType);
      s.saveInt(o.quality);
      s.saveObject(o.client);
      s.saveFloat(o.progress);
      s.saveInt(o.timePlaced);
    }
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    for (ItemOrder order : orders) {
      int spent = AreaMap.timeSince(order.timePlaced, map.time());
      if (spent > MONTH_LENGTH * 2) orders.remove(order);
    }
  }
  
  
  /*
  boolean canAdvanceCrafting(Recipe recipe) {
    boolean anyRoom = false, allMaterials = true;
    
    for (Good made : produced()) {
      if (inventory(made) < stockLimit(made)) anyRoom = true;
    }
    for (ItemOrder order : orders()) {
      if (order.progress < 1) anyRoom = true;
    }
    for (Good need : needed()) {
      if (inventory(need) <= 0) allMaterials = false;
    }
    
    return anyRoom && allMaterials;
  }
  //*/
  
  
  public ItemOrder nextUnfinishedOrder() {
    for (ItemOrder order : orders) if (order.progress < 1) return order;
    return null;
  }
  
  
  ItemOrder orderMatching(Good type, Actor client) {
    for (ItemOrder order : orders) {
      if (order.itemType != type  ) continue;
      if (order.client   != client) continue;
      return order;
    }
    return null;
  }
  
  
  public void addItemOrder(Good type, int quality, Actor client) {
    ItemOrder order = new ItemOrder();
    order.itemType   = type;
    order.quality    = quality;
    order.client     = client;
    order.timePlaced = map.time();
    orders.add(order);
  }
  
  
  public void removeOrder(Good type, Actor client) {
    ItemOrder order = orderMatching(type, client);
    if (order != null) orders.remove(order);
  }
  
  
  public float orderProgress(Good type, Actor client) {
    ItemOrder order = orderMatching(type, client);
    return order == null ? -1 : order.progress;
  }
  
  
  public boolean hasItemOrder(Good type, Actor client) {
    return orderProgress(type, client) != -1;
  }
  
  
  public boolean orderComplete(Good type, Actor client) {
    return orderProgress(type, client) >= 1;
  }
  
  
  public Series <ItemOrder> orders() {
    return orders;
  }
  
  
  
  /**  Handling actor behaviours:
    */
  public Task selectActorBehaviour(Actor actor) {
    //
    //  See if construction is possible-
    Task building = TaskBuilding.nextBuildingTask(this, actor);
    if (building != null) return building;
    //
    //  See if any deliveries are required:
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, produced());
    if (delivery != null) return delivery;
    //
    //  Failing all that, start crafting:
    Task crafting = TaskCrafting.nextCraftingTask(actor, this);
    if (crafting != null) return crafting;
    //
    //  Or just tend shop otherwise-
    Task tending = TaskSupervise.configSupervision(actor, this);
    if (tending != null) return tending;
    return null;
  }
  
  
}







