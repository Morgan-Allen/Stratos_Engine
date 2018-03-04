

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
    boolean controls = venue.base() == PlayUI.playerBase();
    
    d.append(""+venue.toString());
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
      boolean canHire = venue.base().funds() >= cost;
      
      d.append("\n\n"+w.name+": ("+num+"/"+max+")");
      for (Actor a : venue.workers()) if (a.type() == w) {
        if (a.onMap()) {
          d.appendAll("\n  ", a);//, "  ", a.task());
        }
        else {
          d.appendAll("\n  ", a, " (pending arrival)");
        }
      }
      
      if (num < max && w.socialClass >= CLASS_SOLDIER && canHire) {
        d.append("\n  ");
        d.append(new Description.Link("Hire "+w.name+" ("+cost+" Cr)") {
          public void whenClicked(Object context) {
            ActorUtils.generateMigrant(w, venue, true);
          }
        });
      }
    }
    
    if (! venue.residents().empty()) {
      int num = venue.residents().size(), max = venue.maxResidents(-1);
      d.append("\n\nResidents: ("+num+"/"+max+")");
      for (Actor a : venue.residents()) {
        if (a.onMap()) {
          d.appendAll("\n  ", a);//, "  ", a.task());
        }
        else {
          d.appendAll("\n  ", a, " (pending arrival)");
        }
      }
    }
    
    
    if (venue instanceof Trader) {
      final Trader t = (Trader) venue;
      d.append("\n\nTrade Levels:");
      
      for (final Good g : venue.map().world.goodTypes()) {
        if (g == CASH) continue;
        
        final int have = (int) t.inventory ().valueFor(g);
        final int need = (int) t.needLevels().valueFor(g);
        final int prod = (int) t.prodLevels().valueFor(g);
        
        d.append("\n  "+I.padToLength(g.name+":", 10));
        d.append(I.padToLength(""+have, 2)+" Get ");
        d.append(new Description.Link(""+need) {
          public void whenClicked(Object context) {
            t.needLevels().set(g, (need + 5) % 25);
          }
        });
        d.append(" Allow ");
        d.append(new Description.Link(""+(need + prod)) {
          public void whenClicked(Object context) {
            t.prodLevels().set(g, (prod + 5) % 25);
          }
        });
      }
      d.append("\n  Cash: "+((int) t.inventory().valueFor(CASH)));
    }
    else if (! venue.inventory().empty()) {
      d.append("\n\nInventory:");
      
      Batch <Good> goods = new Batch();
      Visit.appendTo(goods, venue.map().world.goodTypes());
      for (Good g : venue.inventory().keys()) goods.include(g);
      
      for (Good g : goods) {
        float have = venue.inventory(g);
        int demand = (int) venue.stockLimit(g);
        if (have == 0 && demand == 0) continue;
        
        d.appendAll("\n  ", g, ": ", I.shorten(have, 1));
        if (demand > 0) d.append("/"+demand);
      }
    }
    
    
    BuildType upgrades[] = venue.type().allUpgrades;
    if (controls && ! Visit.empty(upgrades)) {
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
    
    if (controls) {
      d.append("\n\n");
      d.append(new Description.Link("DEMOLISH") {
        public void whenClicked(Object context) {
          AreaMap map = venue.map();
          if (map == null) return;
          map.planning.unplaceObject(venue);
          venue.exitMap(venue.map());
        }
      });
    }
    
    super.updateState();
  }
  
  
}




