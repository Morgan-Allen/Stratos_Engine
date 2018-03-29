

package game;
import static game.GameConstants.*;



public class TaskShipTravel extends Task {
  
  
  public TaskShipTravel(Active actor) {
    super(actor);
  }
  
  
  public TaskShipTravel(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  
  //  TODO:  Wait.  If ships are going to serve as depots, you need to be able
  //  to either set their import/store levels, or have those borrowed from
  //  their home dock-point.
  
  //  Either that... or those get set between the two bases.
  
  
  
  static TaskShipTravel nextTradeVisit(ActorAsVessel ship, Trader from) {
    //TaskTrading trade = BuildingForTrade.selectTraderBehaviour(from, ship, tradePartner, map)
    return null;
  }
  
  
  
  
  
}









