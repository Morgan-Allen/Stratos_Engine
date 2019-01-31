


package test;
import util.*;
import content.*;
import game.*;
import static game.ActorBonds.*;
import static game.BaseRelations.*;
import static game.FederationRelations.*;
import static game.Federation.*;
import static game.GameConstants.*;
import static game.World.*;
import static content.GameContent.*;
import static content.GameWorld.*;



/*

public class TestWorld extends LogicTest {
  
  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  static boolean testWorld(boolean graphics) {
    LogicTest test = new TestWorld();
    
    //  This tests for regeneration/consumption of goods, and normalisation of
    //  prestige and loyalty over time:
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      
      vassal.trading.setTradeLevel(CARBS   , 0, 10);
      vassal.trading.setTradeLevel(MEDICINE, 5, 0 );
      //vassal.initTradeLevels(CARBS, 10f, MEDICINE, -5f);
      for (Good g : vassal.needLevels().keys()) {
        float demand = vassal.trading.needLevel(g);
        vassal.trading.setInventory(g, demand);
      }
      
      float AVG_P = PRESTIGE_AVG, AVG_L = LOY_CIVIL;
      float initPrestige = AVG_P + ((Rand.yes() ? 1 : -1) * 10);
      float initLoyalty  = AVG_L + ((Rand.yes() ? 1 : -1) / 2f);
      lord.council().relations.initPrestige(initPrestige);
      vassal.relations.incBond(lord.faction(), initLoyalty);
      
      int time = 0;
      while (time < YEAR_LENGTH) {
        world.updateWithTime(time++);
      }
      
      for (Good g : vassal.needLevels().keys()) {
        if (vassal.trading.inventory(g) > 1) {
          I.say("\nWORLD-EVENTS TESTING FAILED- City did not consume goods over time!");
          return false;
        }
      }
      for (Good g : vassal.prodLevels().keys()) {
        float supply = vassal.trading.prodLevel(g);
        if (vassal.trading.inventory(g) < supply - 1) {
          I.say("\nWORLD-EVENTS TESTING FAILED- City did not generate goods over time!");
          return false;
        }
      }
      
      /*
      for (Good g : vassal.tradeLevel().keys()) {
        float demand = vassal.tradeLevel(g);
        if (demand > 0) {
          if (vassal.inventory(g) > 1) {
            I.say("\nCity did not consume goods over time!");
            return false;
          }
        }
        if (demand < 0) {
          float supply = 0 - demand;
          if (vassal.inventory(g) < supply - 1) {
            I.say("\nCity did not generate goods over time!");
            return false;
          }
        }
      }
      //*/
/*
      
      float endP = lord.council().relations.prestige();
      float endL = vassal.relations.bondLevel(lord.faction());
      if (Nums.abs(endP - AVG_P) >= Nums.abs(initPrestige - AVG_P)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- City prestige did not decay over time!");
        return false;
      }
      if (Nums.abs(endL - AVG_L) >= Nums.abs(initLoyalty  - AVG_L)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- City loyalty did not decay over time!");
        return false;
      }
    }
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      Base pair[] = configWeakStrongCityPair();
      float oldPower = pair[0].armyPower();
      runCompleteInvasion(pair[1], pair[0]);
      float newPower = pair[0].armyPower();
      
      if (newPower >= oldPower) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion inflicted no casualties!");
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
    
    //  This tests for the outcome of a basic dialog attempt:
    {
      Base pair[] = configWeakStrongCityPair();
      Base goes = pair[0], from = pair[1];
      goes.council().setTypeAI(BaseCouncil.AI_PACIFIST);
      runCompleteDialog(from, goes);
      
      if (! from.relations.hasBondType(goes, BOND_ALLY)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Dialog did not create ally!");
        return false;
      }
    }
    
    //  This tests to ensure full life-cycles apply to the ruling house of
    //  foreign cities-
    {
      Base pair[] = configWeakStrongCityPair();
      Base mainC = pair[1];
      Actor monarch = (Actor) Nobles.NOBLE  .generate();
      Actor queen   = (Actor) Nobles.CONSORT.generate();
      monarch.health.setSexData(SEX_MALE  );
      queen  .health.setSexData(SEX_FEMALE);
      monarch.health.setAgeYears(AVG_RETIREMENT / 4);
      queen  .health.setAgeYears(AVG_RETIREMENT / 4);
      
      ActorBonds.setBond(monarch, queen, BOND_MARRIED, BOND_MARRIED, 1);
      mainC.council().toggleMember(monarch, Role.MONARCH, true);
      mainC.council().toggleMember(queen  , Role.CONSORT, true);
      
      mainC.world.settings.toggleChildMort = false;
      
      int time = 0;
      while (time < LIFESPAN_LENGTH * 2) {
        mainC.world.updateWithTime(time++);
      }
      
      Series <Actor> heirs = mainC.council().allMembersWithRole(Role.HEIR);
      if (heirs.empty()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- No heirs produced!");
        return false;
      }
      if (monarch.health.alive() || queen.health.alive()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Parents still alive!");
        return false;
      }
    }
    
    //  This tests for the effect of 'barbarian' invasions-
    {
      Base pair[] = configWeakStrongCityPair();
      Base goes = pair[0], from = pair[1];
      from.council().setGovernment(GOVERNMENT.BARBARIAN);
      runCompleteInvasion(from, goes);
      
      if (! goes.isEnemyOf(from)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Barbarian invasion did not prompt correct posture!");
        return false;
      }
      if (goes.relations.suppliesDue(from.faction()).size() > 0) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Barbarian invasion should not impose tribute!");
        return false;
      }
    }
    
    //  This tests for victory over another lord's vassal-
    {
      Base pair[] = configWeakStrongCityPair();
      Base weak = pair[0], strong = pair[1];
      World world = strong.world;
      
      Base lord = new Base(world, world.addLocale(0, 1), FACTION_SETTLERS_A);
      world.addBases(lord);
      setPosture(lord.faction(), weak.faction(), BOND_VASSAL, world);
      Base capital = new Base(world, world.addLocale(1, 1), FACTION_SETTLERS_B);
      world.addBases(capital);
      setPosture(capital.faction(), lord.faction(), BOND_VASSAL, world);
      
      if (capital != weak.council().capital()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Did not calculate capital correctly!");
        return false;
      }
      
      runCompleteInvasion(strong, weak);
      
      if (lord.isLordOf(weak)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion of vassal did not revoke lord's claim!");
        return false;
      }
      if (! weak.isVassalOf(strong)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion of vassal did not impose vassal status!");
        return false;
      }
      if (! capital.isEnemyOf(strong)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion of vassal did not provoke war!");
        return false;
      }
      if (capital.relations.bondLevel(strong.faction()) >= LOY_CIVIL) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Invasion of vassal did not sour relations with capital!");
        return false;
      }
    }
    
    //  This tests to ensure that rebellion has the expected effects upon a
    //  lord and vassal-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal.faction(), lord.faction(), BOND_LORD, world);
      vassal.council().setTypeAI(BaseCouncil.AI_DEFIANT);
      
      float initPrestige = lord.council().relations.prestige();
      
      int time = 0;
      while (time < YEAR_LENGTH * (AVG_TRIBUTE_YEARS + 1)) {
        world.updateWithTime(time++);
      }
      
      if (vassal.relations.isLoyalVassalOf(lord.faction())) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Defiant vassal did not rebel!");
        return false;
      }
      if (vassal.isVassalOf(lord)) {
        I.say("\nWORLD-EVENTS TESTING FAILED- City in rebellion did not break relations!");
        return false;
      }
      if (lord.council().relations.prestige() >= initPrestige) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Lord's prestige did not suffer from rebellion!");
      }
    }
    
    //  This tests for revolt-suppression-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal.faction(), lord.faction(), BOND_LORD, world);
      vassal.council().setTypeAI(BaseCouncil.AI_DEFIANT);
      
      int time = 0;
      while (vassal.relations.isLoyalVassalOf(lord.faction())) {
        world.updateWithTime(time++);
      }
      vassal.council().setTypeAI(BaseCouncil.AI_OFF);

      runCompleteInvasion(lord, vassal);
      
      if (! vassal.relations.isLoyalVassalOf(lord.faction())) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Revolt suppression did not occur!");
        return false;
      }
    }
    
    //  And this tests to ensure that proper tribute is sent between foreign
    //  cities-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal.faction(), lord.faction(), BOND_LORD, world);
      vassal.relations.setSuppliesDue(lord.faction(), new Tally().setWith(PSALT, 10));
      vassal.council().setTypeAI(BaseCouncil.AI_COMPLIANT);
      
      int time = 0;
      while (time < YEAR_LENGTH * 0.8f) {
        world.updateWithTime(time++);
      }
      
      float tributeSent = vassal.relations.suppliesDue(lord, PSALT);
      if (tributeSent < 5) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Insufficient tribute dispatched!");
        return false;
      }
    }
    
    //  TODO:  Test for synergy, marriage and trade-level effects on selecting
    //  for allies.
    
    {
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
        from[i] = new Base(world, world.addLocale(0, i), FACTION_SETTLERS_A, "From_"+i);
      }
      for (int i = goes.length; i-- > 0;) {
        goes[i] = new Base(world, world.addLocale(1, i), FACTION_SETTLERS_B, "Goes_"+i);
      }
      world.addBases(from);
      world.addBases(goes);
      
      for (int i = from.length; i-- > 0;) {
        for (int j = goes.length; j-- > 0;) {
          from[i].relations.incBond(goes[j].faction(), relations[i][j]);
          goes[j].relations.incBond(from[i].faction(), relations[i][j]);
        }
      }
      for (Base c : world.bases()) {
        c.initBuildLevels(HOLDING, 2f, TROOPER_LODGE, 2f);
        for (Base o : world.bases()) if (c != o) {
          World.setupRoute(c.locale, o.locale, 1, Type.MOVE_LAND);
        }
      }
      Base main = from[0];
      
      //  Establish trade-options with city 3...
      main   .trading.setTradeLevel(GREENS, 0, 5);
      main   .trading.setTradeLevel(PARTS , 0, 5);
      from[2].trading.setTradeLevel(GREENS, 5, 0);
      from[2].trading.setTradeLevel(PARTS , 5, 0);
      //main   .initTradeLevels(GREENS,  5, PARTS,  5);
      //from[2].initTradeLevels(GREENS, -5, PARTS, -5);
      
      //  Establish enmity with city 5...
      main   .relations.incBond(from[4].faction(), -0.5f);
      from[4].relations.incBond(main   .faction(), -0.5f);
      
      //  TODO:  Establish a possible marriage with city 6 and test effects...
      
      BaseCouncil.MissionAssessment
        D1 = main.council().dialogAssessment(main, from[1], false),
        D2 = main.council().dialogAssessment(main, from[2], false),
        D3 = main.council().dialogAssessment(main, from[3], false),
        D4 = main.council().dialogAssessment(main, from[4], false),
        allD[] = { D1, D2, D3, D4 }
      ;
      
      I.say("\nAppeal of alliances is: ");
      for (BaseCouncil.MissionAssessment d : allD) {
        I.say("  "+d.goes()+": "+d.appeal());
      }
      if (D1.appeal() <= D3.appeal()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Similar relations should boost appeal of alliance!");
        return false;
      }
      if (D2.appeal() <= D1.appeal()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Trade-potential should boost appeal of alliance!");
        return false;
      }
      if (D4.appeal() >= D3.appeal()) {
        I.say("\nWORLD-EVENTS TESTING FAILED- Mutual enmity should lower appeal of alliance!");
        return false;
      }
      /*
      if (D5.evaluatedAppeal <= D3.evaluatedAppeal) {
        I.say("\nPotential marriage should boost appeal of alliance!");
      }
      //*/
/*
    }
    
    //
    //  Now we set up random cities with random troops and resources, placed at
    //  each of the compass-points on the map, and run the simulation for a
    //  while to ensure that invasions take place at reasonable frequency-
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    
    final int NUM_CITIES = 4;
    final String names[] = { "Base A", "Base B", "Base C", "Base D" };
    final Good goods[] = { ORES, CARBS, CARBONS, GREENS, PARTS, MEDICINE };
    final int tints[] = {
      colour(9, 0, 0),
      colour(2, 2, 2),
      colour(0, 0, 9),
      colour(9, 9, 8)
    };
    
    for (int n = 0; n < 4; n++) {
      WorldLocale l = world.addLocale(
        2 + (2 * TileConstants.T_X[n * 2]),
        2 + (2 * TileConstants.T_Y[n * 2])
      );
      Base city = new Base(world, l, FACTION_SETTLERS_C);
      city.setName(names[n]);
      ///city.setTint(tints[n]);
      
      for (Good g : goods) {
        float amount = (Rand.num() - 0.5f) * 10;
        amount = Nums.round(amount, 2, amount >= 0);
        if (amount > 0) city.trading.setTradeLevel(g, amount, 0);
        else city.trading.setTradeLevel(g, 0, 0 - amount);
      }
      city.initBuildLevels(
        TROOPER_LODGE, 2f + Rand.index(3),
        HOLDING      , 6f + Rand.index(10)
      );
      if (Rand.yes()) city.council().setTypeAI(BaseCouncil.AI_WARLIKE);
      world.addBases(city);
    }
    world.setMapSize(5, 5);
    
    for (Base c : world.bases()) for (Base o : world.bases()) {
      if (c == o) continue;
      float dist = World.mapCoords(c).lineDist(World.mapCoords(o));
      World.setupRoute(c.locale, o.locale, (int) dist, Type.MOVE_LAND);
    }
    
    //
    //  Note:  This map is initialised purely to meet the requirements of the
    //  visual debugger...
    Base mapCity = new Base(world, world.addLocale(0, 0), FACTION_SETTLERS_A);
    Area map = new Area(world, mapCity.locale, mapCity);
    map.performSetup(8, new Terrain[0]);
    world.settings.worldView    = true;
    world.settings.speedUp      = true;
    world.settings.reportBattle = graphics;
    
    int MAX_TIME = LIFESPAN_LENGTH, NUM_YEARS = MAX_TIME / YEAR_LENGTH;
    boolean relationsOkay = true;
    int totalBattles = 0, timeWithEmpire = 0, timeWithAllies = 0;
    
    I.say("\nRunning world simulation...");
    while (map.time() < MAX_TIME) {
      test.runLoop(mapCity, 10, graphics, "saves/test_world.tlt");
      //
      //  Ensure that any forces being sent are of reasonable size:
      for (World.Journey j : world.journeys()) {
        for (Journeys g : j.going()) if (g instanceof Mission) {
          Mission force = (Mission) g;
          Base home = force.base();
          if (j.goes() == home) continue;
          
          float power = MissionForStrike.powerSum(force.recruits(), null);
          if (power < home.idealArmyPower() / 6) {
            I.say("\n"+home+" is fighting with inadequate forces:");
            I.say("  Formation power: "+power);
            I.say("  Average power:   "+AVG_ARMY_POWER);
            I.say("  Time: "+world.time()+", going to: "+j.goes());
            return false;
          }
        }
      }
      //
      //  If anything big has happened, make sure relations stay consistent.
      if (! world.history().empty()) {
        for (World.Event e : world.eventsWithLabel("attacked")) {
          totalBattles += 1;
        }
        world.clearHistory();
        
        for (Base c : world.bases()) {
          if (! testRelationsOkay(c)) relationsOkay = false;
        }
        
        if (! relationsOkay) {
          I.say("\nINCONSISTENT CITY RELATIONS, WILL QUIT");
          break;
        }
      }
      //
      //  We also check to see whether an empire has formed.  (This is isn't
      //  actually required, but it might account for a lull in hostilities.)
      int timeStep = world.settings.speedUp ? 100 : 10;
      boolean empireExists = false;
      
      for (Base c : world.bases()) {
        boolean hasEmpire = true;
        for (Base o : world.bases()) {
          if (o == mapCity || o == c) continue;
          if (o.council().capital() != c) hasEmpire = false;
          if (o.isAllyOf(c)) timeWithAllies += timeStep;
        }
        empireExists |= hasEmpire;
      }
      
      if (empireExists) timeWithEmpire += timeStep;
    }
    
    //
    //  Finally, check to see whether total battles fall within reasonable
    //  bounds for the world in question.
    timeWithEmpire /= YEAR_LENGTH;
    timeWithAllies /= YEAR_LENGTH * NUM_CITIES * (NUM_CITIES - 1);
    int timeFree    = NUM_YEARS - Nums.max(timeWithEmpire, timeWithAllies);
    float popMult   = world.bases().size() * 1f / (AVG_RETIREMENT / 2);
    int minBattles  = (int) (timeFree  * popMult * 0.50f);
    int maxBattles  = (int) (NUM_YEARS * popMult * 2.00f);
    boolean testOkay = true;
    
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
  
  
  static Base[] configWeakStrongCityPair() {
    World world = new World(ALL_GOODS);
    
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    Base a = new Base(world, world.addLocale(0, 0), FACTION_SETTLERS_A);
    Base b = new Base(world, world.addLocale(1, 0), FACTION_SETTLERS_B);
    a.setName("Victim City" );
    b.setName("Invader City");
    world.addBases(a, b);
    
    setupRoute(a.locale, b.locale, 1, Type.MOVE_LAND);
    a.initBuildLevels(HOLDING, 1f, TROOPER_LODGE, 1f);
    b.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
    a.council().setTypeAI(BaseCouncil.AI_OFF);
    b.council().setTypeAI(BaseCouncil.AI_OFF);
    
    return new Base[] { a, b };
  }
  
  
  static void runCompleteInvasion(Base from, Base goes) {
    World world = from.world;
    
    BaseCouncil.MissionAssessment IA = from.council().invasionAssessment(
      from.armyPower(), from, goes, false
    );
    Mission force = from.council().spawnFormation(IA, from);
    force.beginMission(from);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
  }
  
  
  static void runCompleteDialog(Base from, Base goes) {
    World world = from.world;
    
    BaseCouncil.MissionAssessment DA = from.council().dialogAssessment(
      from, goes, false
    );
    Mission force = from.council().spawnFormation(DA, from);
    force.beginMission(from);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
  }
  
  
  static boolean testRelationsOkay(Base city) {
    int numLords = 0;
    
    for (Base o : city.world.bases()) {
      int p = city.posture(o.faction());
      int i = o.posture(city.faction());
      if (p == BOND_LORD) numLords++;
      if (p == BOND_VASSAL  && i != BOND_LORD   ) return false;
      if (p == BOND_LORD    && i != BOND_VASSAL ) return false;
      if (p == BOND_ENEMY   && i != BOND_ENEMY  ) return false;
      if (p == BOND_ALLY    && i != BOND_ALLY   ) return false;
      if (p == BOND_NEUTRAL && i != BOND_NEUTRAL) return false;
    }
    
    if (numLords > 1) return false;
    return true;
  }
  
  
  static void reportOnWorld(World world) {
    I.say("\nReporting world state:");
    for (Base c : world.bases()) {
      I.say("  "+c+":");
      I.say("    Pop:    "+c.population()+" / "+c.idealPopulation());
      I.say("    Arm:    "+c.armyPower ()+" / "+c.idealArmyPower ());
      I.say("    Prs:    "+c.council().relations.prestige());
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
//*/








