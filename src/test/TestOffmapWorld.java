

package test;
import game.*;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestOffmapWorld extends LogicTest {
  
  
  
  public static void main(String args[]) {
    testWorld(true);
  }
  
  
  final static AreaType AREA_GRID[][] = new AreaType[3][3];
  
  static {
    for (Coord c : Visit.grid(0, 0, 3, 3, 1)) {
      AreaType a = areaType(c.x, c.y, false, "A_"+c);
      AREA_GRID[c.x][c.y] = a;
    }
    for (Coord c : Visit.grid(0, 0, 3, 2, 1)) {
      AreaType a1 = AREA_GRID[c.x][c.y];
      AreaType a2 = AREA_GRID[c.x][c.y + 1];
      AreaType a3 = AREA_GRID[c.y][c.x];
      AreaType a4 = AREA_GRID[c.y + 1][c.x];
      
      AreaType.setupRoute(a1, a2, 1, Type.MOVE_LAND);
      AreaType.setupRoute(a3, a4, 1, Type.MOVE_LAND);
    }
  }
  
  
  static boolean testWorld(boolean graphics) {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    for (Coord c : Visit.grid(0, 0, 3, 3, 1)) {
      world.addArea(AREA_GRID[c.x][c.y]);
    }
    
    Area landsA = world.areaAt(AREA_GRID[0][Rand.index(3)]);
    Area landsB = world.areaAt(AREA_GRID[2][Rand.index(3)]);
    Base baseA = new Base(world, landsA, FACTION_SETTLERS_A, "Landing A");
    Base baseB = new Base(world, landsB, FACTION_SETTLERS_B, "Landing B");
    
    world.addBases(baseA, baseB);
    baseA.federation().assignCapital(baseA);
    baseB.federation().assignCapital(baseB);
    
    for (Base c : world.bases()) {
      c.federation().setExploreLevel(c.area, 1);
      c.assignTechTypes(c.faction().buildTypes());
      c.growth.initBuildLevels(BASTION, 1F, HOLDING, 2f, TROOPER_LODGE, 2f);
      c.relations.setBond(c.faction(), 0.5f);
    }
    
    final int MAX_TIME = LIFESPAN_LENGTH;
    final Faction ACTIVE[] = { FACTION_SETTLERS_A, FACTION_SETTLERS_B };
    final int MAX_EPOCHS = 10;
    
    Base withEmpire = null, withAllies = null;
    int time = 0, epoch = 0;
    boolean missionsActive = false;
    boolean testOkay = true;
    
    world.settings.reportMissionEval = true;
    

    while (world.time() < MAX_TIME) {
      
      int timeStep = 10;
      world.updateWithTime(time += timeStep);
      
      //
      //  Report any events that occurred-
      
      if (graphics) for (WorldEvents.Event e : world.events.history()) {
        I.say(world.events.descFor(e));
      }
      world.events.clearHistory();
      //
      //  Check to see if every federation has completed their missions (in
      //  which case enable their AI), or has at least one mission active (in
      //  which case, disable their AI.)
      
      if (! missionsActive) {
        boolean allLaunched = true;
        
        for (Faction f : ACTIVE) {
          boolean launched = false;
          
          for (Base b : world.bases()) if (b.faction() == f) {
            if (b.missions().size() > 0) {
              launched = true;
            }
          }
          
          if (launched) world.federation(f).setTypeAI(Federation.AI_OFF);
          allLaunched &= launched;
        }
        
        if (allLaunched) {
          missionsActive = true;
        }
      }
      else {
        boolean allDone = true;
        
        for (Faction f : ACTIVE) {
          int numActive = 0;
          
          for (Base b : world.bases()) if (b.faction() == f) {
            for (Mission m : b.missions()) if (m.active()) numActive += 1;
          }
          if (numActive > 0) allDone = false;
        }
        
        if (allDone) {
          for (Federation fed : world.federations()) {
            fed.setTypeAI(Federation.AI_NORMAL);
          }
          epoch += 1;
          missionsActive = false;
          
          if (graphics) {
            I.say("\n\nNEXT MISSION CYCLE, EPOCH "+epoch+" TIME "+time+"/"+MAX_TIME);
            reportOnWorld(world, ACTIVE);
          }
          
          if (MAX_EPOCHS > 0 && epoch >= MAX_EPOCHS) break;
        }
      }
      
      //
      //  Finally, check to see whether game victory conditions are met-
      for (Base c : world.bases()) {
        boolean hasEmpire = true;
        boolean hasAllied = true;
        
        for (Base o : world.bases()) if (o != c) {
          if (! o.isLoyalVassalOf(c)) {
            hasEmpire = false;
          }
          if (! o.isAllyOrFaction(c)) {
            hasAllied = false;
          }
        }
        
        if (hasEmpire) withEmpire = c;
        if (hasAllied) withAllies = c;
      }
      
      if (withEmpire != null || withAllies != null) {
        I.say("Found empire or alliance...");
        //  TODO:  Report which!
        break;
      }
    }
    
    if (testOkay) {
      I.say("\nWORLD-EVENTS TESTING CONCLUDED SUCCESSFULLY!");
    }
    else {
      I.say("\nWORLD-EVENTS TESTING FAILED.");
    }
    
    I.say("  Total years simulated: "+(world.time() / YEAR_LENGTH));
    
    ///if (graphics) reportOnWorld(world);
    return testOkay;
  }
  
  
  
  /**  Reporting on overall state of the world-
    */
  static void reportOnWorld(World world, Faction factions[]) {

    I.say("\n  WORLD STATE:");
    
    int colours[][] = new int[3][3];
    
    for (Coord c : Visit.grid(0, 0, 3, 3, 1)) {
      Area a = world.areaAt(AREA_GRID[c.x][c.y]);
      colours[c.x][c.y] = BLANK_COLOR;
      
      I.say("    "+a);
      for (Base b : a.bases()) if (b != a.locals) {
        I.say("      "+b+" ("+b.faction()+")");
        I.add(" (pop "+b.growth.population()+") (pwr "+b.growth.armyPower()+")");

        I.say("      Bld: "+b.growth.buildLevel());
        I.say("      Rel:");
        for (RelationSet.Focus f : b.relations.allBondedWith(0)) {
          if (! (f instanceof Faction)) continue;
          I.add(" "+f+": "+b.relations.bondLevel(f));
        }
        
        colours[c.x][c.y] = b.faction().tint();
      }
    }
    
    I.say("\n  FACTIONS:");
    for (Faction a : factions) {
      I.say("    "+a);
      I.say("      Rel:");
      Federation r = world.federation(a);
      for (RelationSet.Focus f : r.relations.allBondedWith(0)) {
        if (! (f instanceof Faction)) continue;
        I.add(" "+f+": "+r.relations.bondLevel(f));
      }
    }
    
    I.present("WORLD_MAP", 300, 300, colours);
    
    /*
    I.say("\nReporting world state:");
    for (Base c : world.bases()) {
      BaseGrowth g = c.growth;
      I.say("  "+c+":");
      I.say("    Pop:    "+g.population()+" / "+g.maxPopulation());
      I.say("    Arm:    "+g.armyPower ()+" / "+g.maxArmyPower ());
      I.say("    Prs:    "+c.federation().relations.prestige());
      I.say("    Need:   "+c.needLevels());
      I.say("    Accept: "+c.prodLevels());
      I.say("    Bld:    "+g.buildLevel());
      I.say("    Inv:    "+c.inventory());
      I.say("    Relations-");
      for (Focus o : c.relations.allBondedWith(0)) {
        I.add(" "+o+": "+c.relations.bondProperties(o)+" "+c.relations.bondLevel(o));
      }
    }
    //*/
  }
}




