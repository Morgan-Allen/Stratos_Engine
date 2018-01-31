

package gameUI.play;
import game.*;
import graphics.widgets.*;
import util.*;
import static game.GameConstants.*;



public class VenuePane extends DetailPane {
  
  
  final Building venue;
  
  
  public VenuePane(HUD UI, Building venue) {
    super(UI, venue);
    this.venue = venue;
  }

  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    Type type = venue.type();
    
    d.append(""+venue.toString());//+" (Level "+venue.structure.venueLevel()+")");
    d.append("\n");
    
    d.append("\nHP: ");
    final int
      maxHP = venue.type().maxHealth,
      HP    = (int) (maxHP * venue.buildLevel())
    ;
    d.append(HP+"/"+maxHP);
    //d.append("\nDamage: "+venue.structure.damage());
    //d.append("\nArmour: "+venue.structure.armour());
    //d.append("\nCredits: "+venue.stocks.credits());
    
    for (Type w : type.workerTypes.keys()) {
      int num = venue.numWorkers(w), max = (int) type.workerTypes.valueFor(w);
      d.append("\n\n"+w.name+": ("+num+"/"+max+")");
      for (Actor a : venue.workers()) if (a.type() == w) {
        d.appendAll("\n  ", a, "  ", a.task());
      }
      
      /*
      if (venue.canHire(w) && ! venue.isBusyHiring()) {
        final int cost = w.hireCost;
        d.append("\n  ");
        d.append(new Description.Link("Hire "+w.name+" ("+cost+" Cr)") {
          public void whenClicked(Object context) {
            venue.startHiring((Actor) w.generate(), w);
          }
        });
      }
      ///*/
    }
    
    if (! venue.inventory().empty()) {
      d.append("\n\nInventory:");
      for (Good g : venue.inventory().keys()) {
        d.appendAll("\n  ", g, ": ", venue.inventory(g));
      }
    }
    
    
    /*
    Series <Upgrade> upgrades = venue.blueprint().upgradesAvailable();
    if (! upgrades.empty()) {
      int num = venue.structure.numUpgrades(), max = venue.structure.maxUpgrades();
      d.append("\n\nUpgrades: ("+num+"/"+max+")");
      
      for (final Upgrade u : upgrades) {
        if (venue.structure.canUpgrade(u) && ! venue.structure.busyUpgrading()) {
          d.append("\n  ");
          d.append(new Description.Link(u.name+" ("+u.upgradeCost+")") {
            public void whenClicked(Object context) {
              venue.structure.startUpgrade(u);
            }
          });
        }
        else {
          continue;
          //d.append("\n  ");
          //d.append(u.name);
        }
      }
      for (Upgrade u : venue.structure.upgradesQueued()) {
        d.append("\n  Upgrading: "+u.name);
        int prog = (int) (venue.structure.upgradeProgress(u) * 100);
        d.append(" ("+prog+"%)");
      }
      for (Upgrade u : venue.structure.allUpgrades()) {
        d.append("\n  "+u.name+" (installed)");
      }
    }
    
    final Blueprint nextLevel = venue.structure.nextVenueLevel();
    if (nextLevel != null && venue.structure.canUpgradeVenue(nextLevel)) {
      d.append("\n  Upgrade to ");
      d.append(new Description.Link(nextLevel.name) {
        public void whenClicked(Object context) {
          venue.structure.startVenueUpgrade(nextLevel);
        }
      });
    }
    
    for (Actor a : venue.staff.hiring()) {
      d.append("\n  Hiring: "+a);
      int prog = (int) (venue.staff.hiringProgress(a) * 100);
      d.append(" ("+prog+"%)");
    }
    
    d.append("\n\nVisitors:");
    for (Element i : venue.inside()) {
      d.appendAll("\n  ", i);
    }
    //*/
    
    super.updateState();
  }
  
}















