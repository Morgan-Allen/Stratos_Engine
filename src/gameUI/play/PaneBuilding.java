

package gameUI.play;
import game.*;
import static game.GameConstants.*;
import static game.ActorUtils.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class PaneBuilding extends DetailPane {
  
  
  final Building built;
  
  
  public PaneBuilding(HUD UI, Building venue) {
    super(UI, venue, "(STAFF)", "(GOODS)", "(UPGRADE)");
    this.built = venue;
  }
  
  
  protected void updateText(Text text) {
    
    text.setText("");
    final Description d = text;
    
    d.append("\nHP: ");
    final int
      maxHP = built.type().maxHealth,
      HP    = (int) (maxHP * built.buildLevel())
    ;
    d.append(HP+"/"+maxHP);
    
    if (inCategory("(STAFF)"  )) describeStaff  (d);
    if (inCategory("(GOODS)"  )) describeGoods  (d);
    if (inCategory("(UPGRADE)")) describeUpgrade(d);
  }
  
  
  void describeStaff(Description d) {
    
    boolean controls = built.base() == PlayUI.playerBase();
    BuildType type = built.type();
    World world = built.map().world;
    Base base = built.base();
    
    for (final ActorType w : type.workerTypes.keys()) {
      int num = built.numWorkers(w), max = built.maxWorkers(w);
      int cost = base.trading.hireCost(w);
      
      d.append("\n\n"+w.name+": ("+num+"/"+max+")");
      
      if (num < max && w.socialClass >= CLASS_SOLDIER && controls) {
        d.append("\n  ");
        Object hireCheck = ActorUtils.hireCheck(w, built, true);
        if (hireCheck == MIGRATE.OKAY) {
          d.append(new Description.Link("Hire "+w.name+" ("+cost+" Cr)") {
            public void whenClicked(Object context) {
              ActorUtils.generateMigrant(w, built, true);
            }
          });
        }
        else if (hireCheck == MIGRATE.NO_FUNDS) {
          Text.appendColour("Hire "+w.name+" ("+cost+" Cr)", Colour.RED, d);
        }
        else if (hireCheck == MIGRATE.NOT_COMPLETE) {
          d.append("Construction incomplete.");
        }
        else if (hireCheck == MIGRATE.NO_HOMELAND) {
          d.append("No homeland.");
        }
        else if (hireCheck == MIGRATE.NO_TRANSPORT) {
          d.append("No transport.");
        }
        else {
          d.append("Settings do not allow recruitment.");
        }
        d.append("\n");
      }
      
      for (Actor a : built.workers()) if (a.type() == w) {
        if (a.onMap()) {
          d.appendAll("\n  ", a, "\n    ", a.jobDesc());
        }
        else {
          WorldLocale offmap = a.offmap();
          int ETA = world.arriveTime(a, base);
          if (ETA < 0) {
            d.appendAll("\n  ", a, "\n    (On "+offmap+")");
          }
          else {
            d.appendAll("\n  ", a, "\n    (ETA "+ETA+")");
          }
        }
      }
    }
    
    if (! built.residents().empty()) {
      d.append("\n\nResidents:");
      for (Actor a : built.residents()) {
        if (a.onMap()) {
          d.appendAll("\n  ", a, "\n    ", a.jobDesc());
        }
        else {
          WorldLocale offmap = a.offmap();
          int ETA = world.arriveTime(a, base);
          if (ETA < 0) {
            d.appendAll("\n  ", a, "\n    (On "+offmap+")");
          }
          else {
            d.appendAll("\n  ", a, "\n    (ETA "+ETA+")");
          }
        }
      }
    }
    
    Batch <Element> visiting = new Batch();
    for (Actor i : built.allInside()) {
      if (i.work() == built || i.home() == built) continue;
      visiting.include(i);
    }
    if (! visiting.empty()) {
      d.append("\n\nVisitors:");
      for (Element i : visiting) {
        d.appendAll("\n  ", i);
      }
    }
    
  }
  
  
  void describeGoods(Description d) {
    
    if (built instanceof BuildingForTrade) {
      final BuildingForTrade t = (BuildingForTrade) built;
      final boolean exports = t.allowExports();
      
      d.append("\n\nTrade Levels:");
      
      for (final Good g : built.map().world.goodTypes()) {
        if (g == CASH) continue;
        
        final int have = (int) t.inventory ().valueFor(g);
        final int need = (int) t.needLevels().valueFor(g);
        final int prod = (int) t.prodLevels().valueFor(g);
        
        d.append("\n  "+I.padToLength(g.name+":", 10));
        d.append(I.padToLength(""+have, 2)+" Get ");
        
        d.append(new Description.Link(I.padToLength(""+need, 2)) {
          public void whenClicked(Object context) {
            t.needLevels().set(g, (need + 5) % 35);
          }
        });
        d.append(" Store ");
        d.append(new Description.Link(I.padToLength(""+(need + prod), 2)) {
          public void whenClicked(Object context) {
            t.prodLevels().set(g, (prod + 5) % 35);
          }
        });
      }
      d.append("\n\n  Cash: "+((int) t.inventory().valueFor(CASH)));
      d.append("\n  Export Surplus? ");
      d.append(new Description.Link(exports ? "Yes" : "No") {
        public void whenClicked(Object context) {
          t.toggleExports(! exports);
        }
      });
    }
    else if (! built.inventory().empty()) {
      d.append("\n\nInventory:");
      
      Batch <Good> goods = new Batch();
      Visit.appendTo(goods, built.map().world.goodTypes());
      for (Good g : built.inventory().keys()) goods.include(g);
      
      for (Good g : goods) {
        float have = built.inventory(g);
        int demand = (int) built.stockLimit(g);
        if (have == 0 && demand == 0) continue;
        
        d.appendAll("\n  ", g, ": ", I.shorten(have, 1));
        if (demand > 0) d.append("/"+demand);
      }
      
      if (built instanceof BuildingForCrafts) {
        BuildingForCrafts BC = (BuildingForCrafts) built;
        for (Object order : BC.orders()) {
          d.append("\n  "+BC.descOrder(order));
        }
      }
    }
  }
  
  
  void describeUpgrade(Description d) {
    boolean controls = built.base() == PlayUI.playerBase();
    
    BuildType upgrades[] = built.type().allUpgrades;
    if (controls && ! Visit.empty(upgrades)) {
      int num = built.numSideUpgrades(), max = built.maxUpgrades();
      d.append("\n\nUpgrades: ("+num+"/"+max+")");
      
      for (final BuildType upgrade : upgrades) {
        
        boolean done    = built.upgradeComplete(upgrade);
        boolean current = built.currentUpgrade() == upgrade;
        boolean canDo   = built.canBeginUpgrade(upgrade, false);
        String  name    = upgrade.name;
        
        if (done) {
          d.append("\n  "+name+" (complete)");
        }
        else if (current) {
          float progress = built.upgradeProgress();
          d.append("\n  "+name+" ("+I.percent(progress)+"%)");
        }
        else if (canDo) {
          //  TODO:  List building materials as well...
          
          d.append("\n  ");
          d.append(new Description.Link(name) {
            public void whenClicked(Object context) {
              built.beginUpgrade(upgrade);
            }
          });
        }
        else {
          Text.appendColour("\n  "+name, Colour.GREY, d);
        }
      }
    }
    
    if (controls) {
      d.append("\n\n");
      d.append(new Description.Link("DEMOLISH") {
        public void whenClicked(Object context) {
          Area map = built.map();
          if (map == null) return;
          map.planning.unplaceObject(built);
          built.exitMap(built.map());
        }
      });
    }
  }
  
  
}












