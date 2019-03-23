

package test;
import game.*;
import static game.GameConstants.*;
import static game.BaseRelations.*;
import static content.GameContent.*;
import static content.GameWorld.*;
import util.*;



public class TestWorld2 extends LogicTest {
  
  
  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  final static AreaType AREA_GRID[][] = new AreaType[4][4];
  
  static {
    for (Coord c : Visit.grid(0, 0, 4, 4, 1)) {
      AreaType a = areaType(c.x, c.y, false, "A_"+c);
      AREA_GRID[c.x][c.y] = a;
    }
    for (Coord c : Visit.grid(0, 0, 4, 3, 1)) {
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
    for (Coord c : Visit.grid(0, 0, 4, 4, 1)) {
      world.addArea(AREA_GRID[c.x][c.y]);
    }
    
    Area landsA = world.areas().atIndex(2  + Rand.index(4));
    Area landsB = world.areas().atIndex(10 + Rand.index(4));
    Base baseA = new Base(world, landsA, FACTION_SETTLERS_A);
    Base baseB = new Base(world, landsB, FACTION_SETTLERS_B);
    world.addBases(baseA, baseB);
    
    for (Base c : world.bases()) {
      c.federation().setExploreLevel(c.area, 1);
      c.growth.initBuildLevels(BASTION, 1F, HOLDING, 2f, TROOPER_LODGE, 2f);
    }
    
    
    final int MAX_TIME  = LIFESPAN_LENGTH / 10;
    final int NUM_YEARS = MAX_TIME / YEAR_LENGTH;
    
    Base withEmpire = null, withAllies = null;
    int time = 0, epoch = 0;
    boolean testOkay = true;
    
    world.settings.reportMissionEval = true;
    

    while (world.time() < MAX_TIME) {
      
      int timeStep = 10;
      world.updateWithTime(time += timeStep);
      
      //
      //  Report any events that occurred-
      for (WorldEvents.Event e : world.events.history()) {
        I.say(world.events.descFor(e));
      }
      world.events.clearHistory();
      //
      //  Check to see if every federation has completed their missions (in
      //  which case enable their AI), or has at least one mission active (in
      //  which case, disable their AI.)
      boolean allDone = true;
      
      for (Federation fed : world.federations()) {
        boolean launched = false;
        
        for (Base b : world.bases()) if (b.federation() == fed) {
          if (b.missions().size() > 0) launched = true;
          for (Mission m : b.missions()) allDone &= m.disbanded();
        }
        
        if (launched) fed.setTypeAI(Federation.AI_OFF);
      }
      //
      //  If a given epoch is complete, also give a report on the state of the
      //  world.
      if (allDone) {
        for (Federation fed : world.federations()) {
          fed.setTypeAI(Federation.AI_NORMAL);
        }
        epoch += 1;
        I.say("\n\nNEXT MISSION CYCLE, EPOCH "+epoch+" TIME "+time);
        
        /*
        I.say("\nBASES ARE: ");
        for (Base base : world.bases()) {
          float e1 = world.federation(FACTION_SETTLERS_A).exploreLevel(base.locale);
          float e2 = world.federation(FACTION_SETTLERS_B).exploreLevel(base.locale);
          I.say("  "+base+", offmap: "+base.isOffmap()+" | "+e1+" "+e2);
        }
        //*/
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
          if (! (o.isAllyOf(c) || o.isLoyalVassalOf(c))) {
            hasAllied = false;
          }
        }
        
        if (hasEmpire) withEmpire = c;
        if (hasAllied) withAllies = c;
      }
      
      if (withEmpire != null || withAllies != null) {
        I.say("Found empire or alliance...");
        break;
      }
    }
    
    if (testOkay) {
      I.say("\nWORLD-EVENTS TESTING CONCLUDED SUCCESSFULLY!");
    }
    else {
      I.say("\nWORLD-EVENTS TESTING FAILED.");
    }
    
    I.say("  Total years simulated: "+NUM_YEARS);
    
    if (graphics) reportOnWorld(world);
    return testOkay;
  }
  
  
  
  /**  Reporting on overall state of the world-
    */
  static void reportOnWorld(World world) {
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
  }
}






