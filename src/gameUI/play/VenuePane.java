

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class VenuePane extends DetailPane {
  
  
  final Building venue;
  
  
  public VenuePane(HUD UI, Building venue) {
    super(UI, venue);
    this.venue = venue;
  }

  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    BuildType type = venue.type();
    
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
    
    for (final ActorType w : type.workerTypes.keys()) {
      int num = venue.numWorkers(w), max = venue.maxWorkers(w);
      int cost = venue.hireCost(w);
      boolean canHire = venue.homeCity().funds() >= cost;
      
      d.append("\n\n"+w.name+": ("+num+"/"+max+")");
      for (Actor a : venue.workers()) if (a.type() == w) {
        if (a.onMap()) {
          d.appendAll("\n  ", a, "  ", a.task());
        }
        else {
          d.appendAll("\n  ", a, " (pending arrival)");
        }
      }
      
      if (num < max && w.socialClass != CLASS_COMMON && canHire) {
        d.append("\n  ");
        d.append(new Description.Link("Hire "+w.name+" ("+cost+" Cr)") {
          public void whenClicked(Object context) {
            CityBorders.generateMigrant(w, venue, true);
          }
        });
      }
    }
    
    if (! venue.inventory().empty()) {
      d.append("\n\nInventory:");
      for (Good g : venue.inventory().keys()) {
        d.appendAll("\n  ", g, ": ", venue.inventory(g));
      }
    }
    
    
    BuildType upgrades[] = venue.type().allUpgrades;
    
    if (! Visit.empty(upgrades)) {
      int num = venue.numSideUpgrades(), max = venue.maxUpgrades();
      d.append("\n\nUpgrades: ("+num+"/"+max+")");
      
      for (final BuildType upgrade : upgrades) {
        
        boolean done    = venue.hasUpgrade(upgrade);
        boolean current = venue.currentUpgrade() == upgrade;
        boolean canDo   = venue.canBeginUpgrade(upgrade, false);
        String  name    = upgrade.name;
        
        if (done) {
          d.append("\n  "+name+" (complete)");
        }
        else if (current) {
          float progress = venue.upgradeProgress();
          d.append("\n  "+name+" ("+I.percent(progress)+"%)");
        }
        else if (canDo) {
          
          //  TODO:  List building materials as well...
          
          d.append("\n  ");
          d.append(new Description.Link(name) {
            public void whenClicked(Object context) {
              venue.beginUpgrade(upgrade);
            }
          });
        }
        else {
          Text.appendColour("\n  "+name, Colour.GREY, d);
        }
        
      }
    }
    
    
    d.append("\n\nVisitors:");
    for (Element i : venue.inside()) {
      d.appendAll("\n  ", i);
    }
    
    
    super.updateState();
  }
  
}















