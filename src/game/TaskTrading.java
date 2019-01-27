

package game;
import static game.GameConstants.*;
import static game.ActorUtils.*;
import static game.BaseRelations.*;
import util.*;



public class TaskTrading extends Task {
  
  
  /**  Data fields, construction/setup and save/load methods-
    */
  Tally <Good> taken;
  Base    homeCity ;
  Trader  tradeFrom;
  Trader  tradeGoes;
  boolean didExport;
  boolean didImport;
  int waitInitTime = -1;
  boolean takeoff = false;
  
  
  public TaskTrading(Actor actor) {
    super(actor);
  }
  
  
  public TaskTrading(Session s) throws Exception {
    super(s);
    s.loadTally(taken = new Tally());
    homeCity  = (Base  ) s.loadObject();
    tradeFrom = (Trader) s.loadObject();
    tradeGoes = (Trader) s.loadObject();
    didExport = s.loadBool();
    didImport = s.loadBool();
    waitInitTime = s.loadInt();
    takeoff      = s.loadBool();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveTally(taken);
    s.saveObject(homeCity );
    s.saveObject(tradeFrom);
    s.saveObject(tradeGoes);
    s.saveBool(didExport);
    s.saveBool(didImport);
    s.saveInt(waitInitTime);
    s.saveBool(takeoff);
  }
  
  
  public static TaskTrading configTrading(
    Trader from, Trader goes, Actor trading, Tally <Good> taken
  ) {
    TaskTrading task = new TaskTrading(trading);
    
    task.taken     = taken;
    task.tradeFrom = from ;
    task.tradeGoes = goes ;
    task.homeCity  = from.base();
    
    Employer origin = null;
    if (from instanceof Employer) origin = (Employer) from;
    
    if (task.doingLanding(from.base())) {
      ActorAsVessel ship = (ActorAsVessel) trading;
      AreaTile docks = ship.landsAt();
      task.configTask(origin, null, docks, JOB.DOCKING, 10);
      if (! task.pathValid()) return null;
    }
    else {
      task.configTravel(from, from, JOB.TRADING, origin, false);
      if (trading.onMap() && task.path == null) return null;
    }
    
    return task;
  }
  
  
  
  /**  Additional utility methods for off-map ships and visitors...
    */
  public void beginFromOffmap(Base from) {
    onVisit(from);
  }
  
  
  boolean doingLanding(Base local) {
    return local.activeMap() != null && active.type().isAirship();
  }
  
  
  boolean shouldTakeoff(Area map) {
    return map.time() - waitInitTime > SHIP_WAIT_TIME;
  }
  
  
  Area activeMap(Trader t) {
    if (t instanceof Element) return ((Element) t).map();
    else return t.base().activeMap();
  }
  
  
  
  /**  Handling actor events-
    */
  void configTravel(
    Trader from, Trader goes, Task.JOB jobType, Employer e, boolean duringTask
  ) {
    Actor actor = (Actor) active;
    Area fromA = activeMap(from), goesA = activeMap(goes);
    //
    //  If your're headed to a building on the same map, either proceed to the
    //  entrance or head to the corner tile.
    if (fromA != null && fromA == goesA) {
      Pathing goesB = (Pathing) goes;
      if (goesB.complete()) {
        configTask(e, goesB, null, jobType, 0);
      }
      else {
        configTask(e, null, goesB.at(), jobType, 0);
      }
    }
    //
    //  If you're headed to a foreign base, either head to the nearest exit
    //  point or begin your journey directly.
    else {
      if (actor.onMap()) {
        AreaTile exits = findTransitPoint(
          actor.map(), from.base(), goes.base(), actor
        );
        configTask(origin, null, exits, Task.JOB.DEPARTING, 0);
      }
      else if (duringTask && goes != actor.offmapBase()) {
        World world = homeCity.world;
        int moveMode = actor.type().moveMode;
        world.beginJourney(from.base(), goes.base(), moveMode, actor);
      }
    }
  }
  
  
  protected boolean updateOnArrival(Base goes, World.Journey journey) {
    //
    //  If you're landing as a ship, proceed to whichever docking site is
    //  available-
    if (doingLanding(goes)) {
      ActorAsVessel ship = (ActorAsVessel) active;
      AreaTile docks = ship.landsAt();
      configTask(origin, null, docks, JOB.DOCKING, 10);
      return true;
    }
    //
    //  If you've arrived at your destination city, offload your cargo, take on
    //  fresh goods, and record any profits in the process:
    if (goes != homeCity && ! didImport) {
      onVisit(goes);
      return true;
    }
    //
    //  If you've arrived back on your home map, return to your post-
    else if (goes == homeCity && goes.activeMap() != null) {
      configTravel(tradeFrom, tradeFrom, JOB.RETURNING, origin, true);
      return true;
    }
    //
    //  Otherwise, you've arrived back on your home map, which is a foreign
    //  base, and you're done:
    else {
      return false;
    }
  }
  
  
  protected void onTarget(Target target) {
    //
    //  We might be visiting an incomplete structure:
    if (target.at().above == tradeFrom) {
      onVisit(tradeFrom);
      return;
    }
    if (target.at().above == tradeGoes) {
      onVisit(tradeGoes);
      return;
    }
    //
    //  Otherwise, assemble some basic facts...
    Actor actor = (Actor) this.active;
    Base from = tradeFrom.base(), goes = tradeGoes.base();
    int moveMode = actor.type().moveMode;
    
    
    //  TODO:  Move this into a separate task...?
    //
    //  If this is a place you're waiting for trade, stay there for a while.
    if (type == JOB.DOCKING) {
      ActorAsVessel ship = (ActorAsVessel) actor;
      Area map = ship.map();
      
      if (! ship.landed()) {
        ship.doLanding((AreaTile) target);
      }
      if (waitInitTime == -1) {
        World world = map.world;
        waitInitTime = map.time();
        
        if (! didExport) {
          taken = configureCargo(tradeFrom, tradeGoes, false, world);
        }
        else if (! didImport) {
          taken = configureCargo(tradeGoes, tradeFrom, false, world);
        }
        else {
          taken = new Tally();
        }
      }
      
      if (shouldTakeoff(map) && ship.readyForTakeoff()) {
        ship.doTakeoff((AreaTile) target);
        waitInitTime = -1;
        
        if (! didExport) {
          didExport = true;
          configTravel(tradeFrom, tradeGoes, JOB.DEPARTING, origin, true);
        }
        else {
          didImport = true;
          configTravel(tradeGoes, tradeFrom, JOB.DEPARTING, origin, true);
        }
      }
      else {
        configTask(origin, null, target, JOB.DOCKING, 10);
      }
      return;
    }
    //
    //  But if you've arrived at the edge of the map, begin your journey to a
    //  foreign city:
    else if (type == JOB.DEPARTING && ! didImport) {
      from.world.beginJourney(from, goes, moveMode, actor);
      actor.exitMap(actor.map());
    }
    else if (type == JOB.DEPARTING && didImport) {
      from.world.beginJourney(goes, from, moveMode, actor);
      actor.exitMap(actor.map());
    }
  }
  
  
  protected void onVisit(Pathing visits) {
    //
    //  Any visited building is assumed to be one of the depots-
    onVisit((Trader) visits);
  }
  
    
  protected void onVisit(Trader visits) {
    Actor actor = (Actor) active;
    World world = homeCity.world;
    boolean report = actor.reports();
    //
    //  If migrants are waiting at a foreign base, take them aboard-
    if (visits == visits.base() && actor.type().isVessel()) {
      Base goes = visits.base();
      //  TODO:  Screen migrants based on base of destination?  To be safe?...
      ActorAsVessel ship = (ActorAsVessel) active;
      for (Actor a : goes.trading.migrants()) {
        goes.trading.removeMigrant(a);
        a.setInside(ship, true);
      }
    }
    //
    //  If you haven't done your export, and this is your 'from' point, take on
    //  goods, then head to your 'goes' point.
    //
    if (visits == tradeFrom && ! didExport) {
      if (taken == null) {
        taken = configureCargo(tradeFrom, tradeGoes, false, world);
      }
      transferGoods(tradeFrom, actor, taken);
      
      if (report) {
        I.say("\n"+actor+" starts with goods: "+taken);
        I.say("  Profit: "+actor.outfit.carried(CASH));
      }
      
      configTravel(tradeFrom, tradeGoes, Task.JOB.TRADING, origin, true);
      didExport = true;
    }
    //
    //  If this is your 'goes' point, and you haven't done your import, take on
    //  goods, and return to your 'from' point.
    //
    else if (visits == tradeGoes && ! didImport) {
      
      transferGoods(actor, tradeGoes, taken);

      if (report) {
        I.say("\n"+actor+" dropped off goods: "+taken);
        I.say("  Profit: "+actor.outfit.carried(CASH));
      }
      
      taken = configureCargo(tradeGoes, tradeFrom, false, world);
      transferGoods(tradeGoes, actor, taken);
      
      if (report) {
        I.say("\n"+actor+" picked up goods: "+taken);
        I.say("  Profit: "+actor.outfit.carried(CASH));
      }
      
      configTravel(tradeGoes, tradeFrom, Task.JOB.TRADING, origin, true);
      didImport = true;
    }
    //
    //  If you *have* done your export, and this is your 'from' point, offload
    //  goods, and end the task.
    //
    else if (visits == tradeFrom) {
      transferGoods(actor, tradeFrom, taken);
      taken = new Tally();
      int profit = (int) actor.outfit.carried(CASH);
      actor.outfit.setCarried(CASH, 0);
      incFunds(tradeFrom, profit);
      
      if (report) {
        I.say("\n"+actor+" returned profit: "+profit);
      }
    }
  }
  
  
  void transferGoods(Carrier from, Carrier goes, Tally <Good> cargo) {
    
    boolean report = reports() && ! cargo.empty();
    if (report) {
      I.say("\nTransferring goods from "+from+" to "+goes);
      I.say("  Cargo: "+cargo);
    }
    
    Base fromB = from.base(), goesB = goes.base();
    boolean paymentDue = fromB != goesB;
    boolean tributeDue = fromB.isLoyalVassalOf(goesB);
    boolean fromFlex   = flexibleGoods(from, tradeFrom, tradeGoes);
    
    //  TODO:  Yeah, it's not clear how this is supposed to work.
    //Relation relation  = fromB.relations.relationWith(goesB);
    
    Tally <Good> stock = from.inventory();
    float totalGets = 0, totalPays = 0;
    
    for (Good g : cargo.keys()) {
      if (g == CASH) continue;
      
      float priceP = goesB.importPrice(g, fromB);
      float priceG = fromB.exportPrice(g, goesB);
      float amount = cargo.valueFor(g);
      
      if (! fromFlex) {
        amount = Nums.min(amount, stock.valueFor(g));
        from.inventory().add(0 - amount, g);
      }
      goes.inventory().add(amount + 0, g);
      
      float paysFor = amount;
      if (tributeDue) {
        paysFor -= tributeQuantityRemaining(fromB, goesB, g);
        paysFor = Nums.max(0, paysFor);
      }
      
      totalPays += paysFor * priceP;
      totalGets += paysFor * priceG;
      fromB.trading.recordGoodsSent(goesB, g, amount);
    }
    
    if (report && paymentDue) {
      I.say("  Price pays: "+totalPays);
      I.say("  Price gets: "+totalGets);
    }
    
    if (paymentDue) {
      incFunds(from, totalGets + 0);
      incFunds(goes, 0 - totalPays);
    }
  }
  
  
  
  /**  Other utility methods:
    */
  static boolean flexibleGoods(Carrier c, Trader from, Trader goes) {
    //
    //  The homeland is more flexible about the goods it will import and
    //  export.
    
    //  TODO:  Move this into the same code-region that deals with pricing.
    
    if (c != c.base()) return false;
    Base land = c.base();
    if (land == from) return land == goes.base().council().homeland();
    if (land == goes) return land == from.base().council().homeland();
    return false;
  }
  
  
  void incFunds(Carrier gets, float payment) {
    if (gets.base() == gets) {
      gets.base().incFunds((int) payment);
    }
    else {
      gets.inventory().add(payment, CASH);
    }
  }
  
  
  static float tributeQuantityRemaining(Base from, Base goes, Good good) {
    float demand = from.relations.suppliesDue(goes, good);
    float paid   = BaseTrading.goodsSent(from, goes, good);
    return Nums.max(0, demand - paid);
  }
  
  
  static float tributeQuantityRemaining(Trader from, Trader goes, Good good) {
    return tributeQuantityRemaining(from.base(), goes.base(), good);
  }
  
  
  /*
  float tributeQuantityRemaining(Relation r, Good good) {
    if (r == null) return 0;
    float demand = r.suppliesDue .valueFor(good);
    float paid   = r.suppliesSent.valueFor(good);
    return Nums.max(0, demand - paid);
  }
  //*/
  
  
  static Tally <Good> configureCargo(
    Trader from, Trader goes, boolean cityOnly, World world
  ) {
    //  TODO:  You need to cap the total sum of goods transported based on
    //  cargo limits!
    
    Tally <Good> cargo = new Tally();
    if (from == null || goes == null) return cargo;
    
    boolean report = false;
    if (report) I.say("\nDoing cargo config for "+from+" -> "+goes);
    
    boolean fromCity = from.base() == from;
    boolean goesCity = goes.base() == goes;
    boolean fromFlex = flexibleGoods(from, from, goes);
    boolean goesFlex = flexibleGoods(goes, from, goes);
    if (cityOnly && ! (fromCity || goesCity)) return cargo;
    
    //Relation fromR = goes.base().relations.relationWith(from.base());
    //Relation goesR = from.base().relations.relationWith(goes.base());
    
    for (Good good : world.goodTypes) {
      if (good == CASH || ! from.allowExport(good, goes)) continue;
      
      float amountFrom = from.inventory ().valueFor(good);
      float amountGoes = goes.inventory ().valueFor(good);
      float needFrom   = from.needLevels().valueFor(good);
      float needGoes   = goes.needLevels().valueFor(good);
      
      if (report) {
        I.say("  "+I.padToLength(good.name, 12)+": ");
        I.add(amountFrom+"/"+needFrom+" -> "+amountGoes+"/"+needGoes);
      }
      
      if (fromCity) {
        needFrom = Nums.max(needFrom, tributeQuantityRemaining(goes, from, good));
        //needFrom = Nums.max(needFrom, goes.base().relations.suppliesDue(from.base().faction(), good));
        //needFrom = Nums.max(needFrom, fromR.suppliesDue.valueFor(good));
      }
      if (goesCity) {
        needGoes = Nums.max(needGoes, tributeQuantityRemaining(from, goes, good));
        //needGoes = Nums.max(needGoes, from.base().relations.suppliesDue(goes.base().faction(), good));
        //needGoes = Nums.max(needGoes, goesR.suppliesDue.valueFor(good));
      }
      
      float surplus  = amountFrom - needFrom;
      float shortage = needGoes - amountGoes;
      
      if (fromFlex) surplus  = Nums.max(surplus , shortage);
      if (goesFlex) shortage = Nums.max(shortage, surplus );
      
      if (surplus > 0 && shortage > 0) {
        float size = Nums.min(surplus, shortage);
        cargo.set(good, size);
      }
    }
    
    if (report) {
      I.say("  Cargo: "+cargo);
      I.say("");
    }
    
    return cargo;
  }
  
  
  static float distanceRating(Trader from, Trader goes) {
    final Base fromB = from.base(), goesB = goes.base();
    if (
      from instanceof Building &&
      goes instanceof Building &&
      fromB == goesB
    ) {
      float mapDist = Area.distance(
        ((Building) from).mainEntrance(),
        ((Building) goes).mainEntrance()
      );
      return 1f / (1 + (mapDist / MAX_TRADER_RANGE));
    }
    else {
      return ActorUtils.distanceRating(fromB, goesB);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  boolean reports() {
    return super.reports();
  }
  
  
  public String toString() {
    Actor actor = (Actor) active;
    int arriveTime = homeCity.world.arriveTime(actor);
    String arriveDesc = "";
    if (arriveTime != -1) arriveDesc = " (ETA "+arriveTime+")";
    
    if (! didExport) {
      return "Collecting goods from "+tradeFrom+arriveDesc;
    }
    else if (! didImport) {
      return "Travelling to "+tradeGoes+arriveDesc;
    }
    else {
      return "Returning to "+tradeFrom+arriveDesc;
    }
  }
  
}
















