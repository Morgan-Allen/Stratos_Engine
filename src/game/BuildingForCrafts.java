

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
    int orderID = -1;
  }
  
  List <ItemOrder> orders = new List();
  private int nextOrderID = 0;
  
  
  public BuildingForCrafts(BuildType type) {
    super(type);
  }
  
  
  public BuildingForCrafts(Session s) throws Exception {
    super(s);
    
    for (int n = s.loadInt(); n-- > 0;) {
      ItemOrder o = new ItemOrder();
      o.itemType   = (Good) s.loadObject();
      o.quality    = s.loadInt();
      o.client     = (Actor) s.loadObject();
      o.progress   = s.loadFloat();
      o.timePlaced = s.loadInt();
      o.orderID    = s.loadInt();
      orders.add(o);
    }
    nextOrderID = s.loadInt();
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
      s.saveInt(o.orderID);
    }
    s.saveInt(nextOrderID);
  }
  
  
  
  /**  Life-cycle, update and economic functions-
    */
  void updateOnPeriod(int period) {
    super.updateOnPeriod(period);
    for (ItemOrder order : orders) {
      int spent = Area.timeSince(order.timePlaced, map.time());
      if (spent > DAY_LENGTH * 2) orders.remove(order);
    }
  }
  
  
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
  
  
  ItemOrder orderWithID(int orderID) {
    for (ItemOrder order : orders) if (order.orderID == orderID) return order;
    return null;
  }
  
  
  public void addItemOrder(Good type, int quality, Actor client) {
    ItemOrder order = new ItemOrder();
    order.itemType   = type;
    order.quality    = quality;
    order.client     = client;
    order.timePlaced = map.time();
    order.orderID    = nextOrderID++;
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
    Task delivery = TaskDelivery.pickNextDelivery(actor, this, 1, produced());
    if (delivery != null) return delivery;
    //
    //  Failing all that, start crafting:
    Task crafting = TaskCrafting.nextCraftingTask(actor, this);
    if (crafting != null) return crafting;
    //
    //  See if any deliveries are required:
    delivery = TaskDelivery.pickNextDelivery(actor, this, 0, produced());
    if (delivery != null) return delivery;
    //
    //  Or just tend shop otherwise-
    Task tending = TaskWaiting.configWaiting(actor, this);
    if (tending != null) return tending;
    return null;
  }
  
  
  
  /**  Rendering, debug and graphics methods-
    */
  public String descOrder(Object order) {
    ItemOrder o = (ItemOrder) order;
    return o.itemType+" ("+o.client+"): "+I.shorten(o.progress, 1);
  }
}







