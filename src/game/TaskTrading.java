

package game;
import util.*;
import static game.ActorUtils.*;
import static game.GameConstants.*;

import game.GameConstants.Pathing;



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
  
  
  static TaskTrading configTrading(
    Trader from, Trader goes, Actor trading, Tally <Good> taken
  ) {
    TaskTrading task = new TaskTrading(trading);
    
    task.taken     = taken;
    task.tradeFrom = from ;
    task.tradeGoes = goes ;
    task.homeCity  = from.base();
    
    Employer origin = null;
    if (from instanceof Employer) origin = (Employer) from;
    
    task.configTravel(from, from, JOB.TRADING, origin);
    return task;
  }
  
  
  
  /**  Handling actor events-
    */
  void configTravel(Trader from, Trader goes, Task.JOB jobType, Employer e) {
    Actor actor = (Actor) active;
    Area fromA = from.base().activeMap(), goesA = goes.base().activeMap();
    //
    //  If your're headed to a building on the same map, either proceed to the
    //  entrance or head to the corner tile.
    if (fromA != null && fromA == goesA) {
      Building goesB = (Building) goes;
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
        AreaTile exits = findTransitPoint(actor.map(), from.base(), goes.base());
        configTask(origin, null, exits, Task.JOB.TRADING, 0);
      }
      else {
        World world = homeCity.world;
        int moveMode = actor.type().moveMode;
        world.beginJourney(from.base(), goes.base(), moveMode, actor);
      }
    }
  }
  
  
  protected boolean doingLanding(Base local) {
    boolean mapVisitor = local != homeCity && local.activeMap() != null;
    return mapVisitor && active.type().isAirship();
  }
  
  
  protected boolean shouldTakeoff(Area map) {
    return map.time() - waitInitTime > SHIP_WAIT_TIME;
  }
  
  
  protected void onArrival(Base goes, World.Journey journey) {
    //
    //  
    if (doingLanding(goes)) {
      ActorAsVessel ship = (ActorAsVessel) active;
      AreaTile docks = ship.findLandingPoint(goes.activeMap(), this);
      configTask(origin, null, docks, JOB.DOCKING, 10);
      return;
    }
    //
    //  If you've arrived at your destination city, offload your cargo, take on
    //  fresh goods, and record any profits in the process:
    if (goes != homeCity && ! didImport) {
      onVisit(goes);
    }
    //
    //  If you've arrived back on your home map, return to your post-
    else if (goes == homeCity) {
      configTravel(tradeFrom, tradeFrom, JOB.TRADING, origin);
    }
    //
    //  ...Fwaaah?
    else I.complain("THIS SHOULDN'T HAPPEN");
  }
  
  
  protected void onTarget(Target target) {
    Actor actor = (Actor) this.active;
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
    //  If this is a place you're waiting, stay there for a while.
    if (type == JOB.DOCKING && actor.type().isVessel() && target.isTile()) {
      ActorAsVessel ship = (ActorAsVessel) actor;
      Area map = ship.map();
      
      if (! ship.landed()) {
        ship.doLanding((AreaTile) target);
        waitInitTime = map.time();
      }
      if (shouldTakeoff(map) && ship.readyForTakeoff()) {
        ship.doTakeoff((AreaTile) target);
        configTravel(tradeGoes, tradeFrom, JOB.DEPARTING, origin);
      }
      else {
        configTask(origin, null, target, JOB.DOCKING, 10);
      }
      return;
    }
    //
    //  If you've arrived at the edge of the map, begin your journey to a
    //  foreign city-
    else if (tradeGoes.base() == tradeGoes) {
      Base city = tradeFrom.base();
      int moveMode = actor.type().moveMode;
      city.world.beginJourney(city, (Base) tradeGoes, moveMode, actor);
      actor.exitMap(actor.map);
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
    //
    //  If you haven't done your export, and this is your 'from' point, take on
    //  goods, then head to your 'goes' point.
    //
    if (visits == tradeFrom && ! didExport) {
      if (taken == null) {
        taken = configureCargo(tradeFrom, tradeGoes, false, world);
      }
      transferGoods(tradeFrom, actor, taken);
      configTravel(tradeFrom, tradeGoes, Task.JOB.TRADING, origin);
      didExport = true;
    }
    //
    //  If this is your 'goes' point, and you haven't done your import, take on
    //  goods, and return to your 'from' point.
    //
    else if (visits == tradeGoes && ! didImport) {
      transferGoods(actor, tradeGoes, taken);
      taken = configureCargo(tradeGoes, tradeFrom, false, world);
      transferGoods(tradeGoes, actor, taken);
      configTravel(tradeGoes, tradeFrom, Task.JOB.TRADING, origin);
      didImport = true;
    }
    //
    //  If you *have* done your export, and this is your 'from' point, offload
    //  goods, and end the task.
    //
    else if (visits == tradeFrom) {
      transferGoods(actor, tradeFrom, taken);
      int profit = (int) actor.carried(CASH);
      actor.setCarried(CASH, 0);
      incFunds(tradeFrom, profit);
      if (reports() && profit != 0) {
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
    boolean fromFlex   = from == goes.base().homeland();
    Base.Relation relation = fromB.relationWith(goesB);
    
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
        paysFor -= tributeQuantityRemaining(relation, g);
        paysFor = Nums.max(0, paysFor);
      }
      
      totalPays += paysFor * priceP;
      totalGets += paysFor * priceG;
      relation.suppliesSent.add(amount, g);
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
  void incFunds(Carrier gets, float payment) {
    if (gets.base() == gets) {
      gets.base().incFunds((int) payment);
    }
    else {
      gets.inventory().add(payment, CASH);
    }
  }
  
  
  float tributeQuantityRemaining(Base.Relation r, Good good) {
    if (r == null) return 0;
    float demand = r.suppliesDue .valueFor(good);
    float paid   = r.suppliesSent.valueFor(good);
    return Nums.max(0, demand - paid);
  }
  
  
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
    boolean fromFlex = from == goes.base().homeland();
    boolean goesFlex = goes == from.base().homeland();
    if (cityOnly && ! (fromCity || goesCity)) return cargo;
    
    Base.Relation fromR = goes.base().relationWith(from.base());
    Base.Relation goesR = from.base().relationWith(goes.base());
    
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
        needFrom = Nums.max(needFrom, fromR.suppliesDue.valueFor(good));
      }
      if (goesCity) {
        needGoes = Nums.max(needGoes, goesR.suppliesDue.valueFor(good));
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
















