

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
  

  //  TODO-
  //  ...You'll need to have blank spaces on the map for this.  A square map
  //  in grid-format, 4x4.  Plonk down some starting locations and have the
  //  colonies grow over time.
  
  
  //
  //  Set up the geographical parameters of the world first-
  final static AreaType
    AREAS_A[] = new AreaType[4],
    AREAS_B[] = new AreaType[4]
  ;
  static {
    for (int i = 4; i-- > 0;) {
      AREAS_A[i] = areaType(3, 2 + (i * 2), false, "F_"+i);
      AREAS_B[i] = areaType(7, 2 + (i * 2), false, "F_"+i);
    }
    
    Batch <AreaType> areas = new Batch();
    Visit.appendTo(areas, AREAS_A);
    Visit.appendTo(areas, AREAS_B);
    for (AreaType a : areas) for (AreaType b : areas) if (a != b) {
      AreaType.setupRoute(a, b, 1, Type.MOVE_LAND);
    }
  }
  
  
  static boolean testWorld(boolean graphics) {
    
    //LogicTest test = new TestWorld2();
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    
    Base from[] = new Base[4];
    Base goes[] = new Base[4];
    for (int i = 4; i-- > 0;) {
      from[i] = new Base(world, world.addArea(AREAS_A[i]), FACTION_SETTLERS_A, "F_"+i);
      goes[i] = new Base(world, world.addArea(AREAS_B[i]), FACTION_SETTLERS_B, "G_"+i);
    }
    world.addBases(from);
    world.addBases(goes);
    
    for (Base c : world.bases()) {
      c.federation().setExploreLevel(c.area, 1);
      c.growth.initBuildLevels(HOLDING, 2f, TROOPER_LODGE, 2f);
    }
    
    
    final int MAX_TIME  = LIFESPAN_LENGTH / 10;
    final int NUM_YEARS = MAX_TIME / YEAR_LENGTH;
    
    Base withEmpire = null, withAllies = null;
    int time = 0, epoch = 0;
    boolean testOkay = true;
    
    world.settings.reportMissionEval = true;
    
    
    //  Okay.  New items to implement-
    
    //  Gradual contact effects (hostile/neutral/trading/allied.)
    //  Boost fondness, chance to defect based on intimidation.
    
    

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






