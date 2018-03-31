

package game;
import static game.GameConstants.*;
import util.*;



public class ActorOutfit {
  
  
  final Actor actor;
  
  Tally <Good> carried = new Tally();
  
  
  
  ActorOutfit(Actor actor) {
    this.actor = actor;
  }
  
  
  void loadState(Session s) throws Exception {
    s.loadTally(carried);
  }
  
  
  void saveState(Session s) throws Exception {
    s.saveTally(carried);
  }
  
  
  
  public void pickupGood(Good carried, float amount, Building store) {
    if (store == null || carried == null || amount <= 0) return;
    
    store.addInventory(0 - amount, carried);
    incCarried(carried, amount);
  }
  
  
  public void offloadGood(Good good, Building store) {
    float amount = carried.valueFor(good);
    if (store == null || amount == 0) return;
    
    store.addInventory(amount, good);
    carried.set(good, 0);
  }
  
  
  public void incCarried(Good good, float amount) {
    float newAmount = carried.valueFor(good) + amount;
    if (newAmount < 0) newAmount = 0;
    carried.set(good, newAmount);
  }
  
  
  public void setCarried(Good good, float amount) {
    carried.set(good, amount);
  }
  
  
  public void clearCarried() {
    carried.clear();
  }
  
  
  public void setCarried(Tally <Good> cargo) {
    if (cargo == null || cargo == this.carried) return;
    clearCarried();
    carried.add(cargo);
  }
  
  
  public Tally <Good> carried() {
    return carried;
  }
  
  
  public float carried(Good g) {
    return carried.valueFor(g);
  }
  
  
  public Tally <Good> inventory() {
    return carried;
  }
  
}
