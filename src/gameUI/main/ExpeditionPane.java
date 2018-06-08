/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.main;
import game.*;
import gameUI.play.*;
import gameUI.misc.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class ExpeditionPane extends PaneMission {
  
  
  //  TODO:  You need to include these as well!
  final static String
    CAT_COLONISTS = "Colonists",
    CAT_SUPPLIES  = "Supplies",
    CATEGORIES[]  = {};
  
  
  final Expedition expedition;
  
  
  public ExpeditionPane(HUD UI, Expedition expedition, Mission mission) {
    //super(UI, null, null, true, CATEGORIES);
    super(UI, mission);
    this.expedition = expedition;
  }
  
  
  protected void describeStatus(
    final Mission mission, boolean canChange, Description d
  ) {
    //  TODO:  Restore this...
    /*
    final int
      funding = expedition.funding(),
      tribute = expedition.tribute(),
      title   = expedition.titleGranted();
    
    d.append("\n  Starting Capital: "+expedition.funding()+" Credits");
    d.append(new Description.Link(" (+)") {
      public void whenClicked(Object context) {
        if (funding >= Expedition.MAX_FUNDING) return;
        expedition.setFunding(funding + 1000, tribute);
      }
    });
    d.append(new Description.Link(" (-)") {
      public void whenClicked(Object context) {
        if (funding <= Expedition.MIN_FUNDING) return;
        expedition.setFunding(funding - 1000, tribute);
      }
    });
    
    d.append("\n  Tribute Returned: "+expedition.tribute()+"%");
    d.append(new Description.Link(" (+)") {
      public void whenClicked(Object context) {
        if (tribute >= Expedition.MAX_TRIBUTE) return;
        expedition.setFunding(funding, tribute + 5);
      }
    });
    d.append(new Description.Link(" (-)") {
      public void whenClicked(Object context) {
        if (tribute <= Expedition.MIN_TRIBUTE) return;
        expedition.setFunding(funding, tribute - 5);
      }
    });
    
    d.append("\n  Title: "+expedition.titleDesc());
    d.appendAll("\n  Faction: ", expedition.backing());
    d.appendAll("\n  Point of origin: ", expedition.origin());

    final Base declares = mission.base();
    if (declares == viewing) super.describeOrders(canChange, d);
    //*/
  }
  
}















