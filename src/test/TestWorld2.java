

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
  
  
  static boolean testWorld(boolean graphics) {
    
    LogicTest test = new TestWorld2();
    boolean testOkay = true;
    
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      Base pair[] = configWeakStrongBasePair();
      float oldPower = pair[0].armyPower();
      Mission strike = runCompleteInvasion(pair[1], pair[0]);
      float newPower = pair[0].armyPower();
      
      if (newPower >= oldPower) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion inflicted no casualties!");
        return false;
      }
      if (! strike.success()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not succeed!");
        return false;
      }
      if (! pair[0].isVassalOf(pair[1])) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not impose vassal status!");
        return false;
      }
      if (pair[0].relations.bondLevel(pair[1].faction()) >= LOY_CIVIL) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion did not sour relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a security mission-
    {
      Base triple[] = configBases(FACTION_SETTLERS_A, FACTION_SETTLERS_A, FACTION_SETTLERS_B);
      Base a = triple[0], b = triple[1], c = triple[2];
      
      a.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
      b.initBuildLevels(HOLDING, 1f, TROOPER_LODGE, 0f);
      c.initBuildLevels(HOLDING, 4f, TROOPER_LODGE, 1f);
      a.setName("Ally Base");
      b.setName("Victim Base");
      c.setName("Invading Base");
      
      dispatchDefence(a, b);
      runCompleteInvasion(c, b);
      
      if (b.faction() == c.faction()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Base was not defended correctly!");
        return false;
      }
    }
    
    //  This tests for the outcome of a basic dialog attempt:
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      goes.federation().setTypeAI(Federation.AI_PACIFIST);
      runCompleteDialog(from, goes);
      
      if (! from.isAllyOf(goes)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Dialog did not create ally!");
        return false;
      }
    }
    
    // This tests for the outcome of a basic exploration attempt-
    {
      Base pair[] = configWeakStrongBasePair();
      Base goes = pair[0], from = pair[1];
      runCompleteExploration(from, goes);
      
      if (from.federation().exploreLevel(goes.locale) < 0.5f) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Exploration did not reveal area!");
        return false;
      }
    }
    
    
    //if (true) return false;
    
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    Base from[] = new Base[6];
    Base goes[] = new Base[4];
    float relations[][] = {
      {  0.5f, -0.5f, -0.5f, -1.0f },
      {  0.5f, -0.7f, -0.2f,  0.0f },
      {  0.5f, -0.7f, -0.2f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
      {  0.0f,  0.0f,  0.0f,  0.0f },
    };
    for (int i = from.length; i-- > 0;) {
      from[i] = new Base(world, world.addLocale(0, i), FACTION_SETTLERS_A, "F_"+i);
    }
    for (int i = goes.length; i-- > 0;) {
      goes[i] = new Base(world, world.addLocale(1, i), FACTION_SETTLERS_B, "G_"+i);
    }
    world.addBases(from);
    world.addBases(goes);
    
    
    for (int i = from.length; i-- > 0;) {
      for (int j = goes.length; j-- > 0;) {
        from[i].relations.incBond(goes[j], relations[i][j]);
        goes[j].relations.incBond(from[i], relations[i][j]);
      }
    }
    for (Base c : world.bases()) {
      c.federation().setExploreLevel(c.locale, 1);
      c.initBuildLevels(HOLDING, 2f, TROOPER_LODGE, 2f);
      for (Base o : world.bases()) if (c != o) {
        World.setupRoute(c.locale, o.locale, 1, Type.MOVE_LAND);
      }
    }
    Base main = from[0];
    
    Area map = new Area(world, main.locale, main);
    map.performSetup(32, new Terrain[0]);
    
    
    
    final int MAX_TIME  = LIFESPAN_LENGTH;
    final int NUM_YEARS = MAX_TIME / YEAR_LENGTH;
    
    int totalBattles = 0, timeWithEmpire = 0, timeWithAllies = 0;
    boolean empireExists = false, allianceExists = false;
    
    
    while (map.time() < MAX_TIME || graphics) {
      test.runLoop(main, 1, graphics, "saves/test_world.tlt");
      
      for (WorldEvents.Event e : world.events.history()) {
        I.say(world.events.descFor(e));
      }
      world.events.clearHistory();
      
      //
      //  We also check to see whether an empire and/or alliance has formed.
      int timeStep = world.settings.speedUp ? 100 : 10;
      empireExists = allianceExists = false;
      
      for (Base c : world.bases()) {
        boolean hasEmpire = true;
        boolean hasAllied = true;
        
        for (Base o : world.bases()) if (o != c) {
          if (o.federation().capital() != c) {
            hasEmpire = false;
          }
          if (o.isAllyOf(c)) {
            hasAllied = false;
          }
        }
        empireExists   |= hasEmpire;
        allianceExists |= hasAllied;
      }
      
      if (empireExists  ) timeWithEmpire += timeStep;
      if (allianceExists) timeWithAllies += timeStep;
    }
    

    //
    //  Finally, check to see whether total battles fall within reasonable
    //  bounds for the world in question.
    timeWithEmpire /= YEAR_LENGTH;
    timeWithAllies /= YEAR_LENGTH;
    int timeFree    = NUM_YEARS - Nums.max(timeWithEmpire, timeWithAllies);
    float popMult   = world.bases().size() * 1f / (AVG_RETIREMENT / 2);
    int minBattles  = (int) (timeFree  * popMult * 0.50f);
    int maxBattles  = (int) (NUM_YEARS * popMult * 2.00f);
    //boolean testOkay = true;
    
    if (! (empireExists || allianceExists)) {
      I.say("\nNo empire or alliance was formed!");
      reportOnWorld(world);
      testOkay = false;
    }
    /*
    if (totalBattles < minBattles) {
      I.say("\nToo few battles occurred: "+totalBattles+"/"+minBattles);
      reportOnWorld(world);
      testOkay = false;
    }
    if (totalBattles > maxBattles) {
      I.say("\nToo many battles occurred: "+totalBattles+"/"+maxBattles);
      reportOnWorld(world);
      testOkay = false;
    }
    //*/
    if (testOkay) {
      I.say("\nWORLD-EVENTS TESTING CONCLUDED SUCCESSFULLY!");
    }
    else {
      I.say("\nWORLD-EVENTS TESTING FAILED.");
    }
    
    I.say("  Total years simulated: "+NUM_YEARS);
    I.say("  Battles: "+totalBattles+", min/max "+minBattles+"/"+maxBattles);
    I.say("  Years of empire: "+timeWithEmpire);
    I.say("  Years of allies: "+timeWithAllies);
    
    if (graphics) reportOnWorld(world);
    return testOkay;
  }
  
  
  
  /**  Individual subtests-
    */
  static Base[] configWeakStrongBasePair() {
    Base bases[] = configBases(FACTION_SETTLERS_A, FACTION_SETTLERS_B);
    Base a = bases[0], b = bases[1];
    a.setName("Victim Base");
    b.setName("Invader Base");
    b.federation().assignCapital(b);
    a.initBuildLevels(HOLDING, 3f, TROOPER_LODGE, 1f);
    b.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
    return bases;
  }
  
  static Base[] configBases(Faction... belong) {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );

    Base bases[] = new Base[belong.length];
    for (int i = 0; i < bases.length; i++) {
      Base b = bases[i] = new Base(world, world.addLocale(0, i), belong[i]);
      b.setName("City No. "+i);
      b.federation().setTypeAI(Federation.AI_OFF);
      b.federation().relations.initPrestige(BaseRelations.PRESTIGE_MAX);
      
      for (int n = i; n-- > 0;) {
        World.setupRoute(b.locale, bases[n].locale, 1, Type.MOVE_LAND);
      }
    }
    
    world.addBases(bases);
    return bases;
  }
  
  
  static Mission runCompleteInvasion(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupStrikeMission(goes, from, from.armyPower(), false);
    MissionAIUtils.launchStrikeMission(force, world);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission dispatchDefence(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupDefendMission(goes, from, from.armyPower(), false);
    MissionAIUtils.launchDefendMission(force, world);

    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.arrived()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteDialog(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupDialogMission(goes, from, from.armyPower(), true);
    MissionAIUtils.launchDialogMission(force, world);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  static Mission runCompleteExploration(Base from, Base goes) {
    World world = from.world;
    
    Mission force = MissionAIUtils.setupExploreMission(goes, from, from.armyPower(), true);
    MissionAIUtils.launchExploreMission(force, world);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
    return force;
  }
  
  
  
  /**  Reporting on overall state of the world-
    */
  static void reportOnWorld(World world) {
    I.say("\nReporting world state:");
    for (Base c : world.bases()) {
      I.say("  "+c+":");
      I.say("    Pop:    "+c.population()+" / "+c.idealPopulation());
      I.say("    Arm:    "+c.armyPower ()+" / "+c.idealArmyPower ());
      I.say("    Prs:    "+c.federation().relations.prestige());
      I.say("    Need:   "+c.needLevels());
      I.say("    Accept: "+c.prodLevels());
      I.say("    Bld:    "+c.buildLevel());
      I.say("    Inv:    "+c.inventory());
      I.say("    Relations-");
      for (Focus o : c.relations.allBondedWith(0)) {
        I.add(" "+o+": "+c.relations.bondProperties(o)+" "+c.relations.bondLevel(o));
      }
    }
  }
}






