


package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.City.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;
import static game.GameContent.*;



public class TestWorld extends Test {
  
  
  public static void main(String args[]) {
    testWorld(false);
  }
  
  
  static boolean testWorld(boolean graphics) {
    Test test = new TestWorld();
    
    //  This tests for regeneration/consumption of goods, and normalisation of
    //  prestige and loyalty over time:
    {
      City pair[] = configWeakStrongCityPair();
      City vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      
      vassal.tradeLevel.setWith(MAIZE, 10f, COTTON, -5f);
      for (Good g : vassal.tradeLevel.keys()) {
        float demand = vassal.tradeLevel.valueFor(g);
        if (demand > 0) vassal.inventory.set(g, demand);
      }
      
      float AVG_P = City.PRESTIGE_AVG, AVG_L = City.LOY_CIVIL;
      float initPrestige = AVG_P + ((Rand.yes() ? 1 : -1) * 10);
      float initLoyalty  = AVG_L + ((Rand.yes() ? 1 : -1) / 2f);
      lord.prestige = initPrestige;
      City.incLoyalty(vassal, lord, initLoyalty);
      
      int time = 0;
      while (time < YEAR_LENGTH) {
        world.updateWithTime(time++);
      }
      
      for (Good g : vassal.tradeLevel.keys()) {
        float demand = vassal.tradeLevel.valueFor(g);
        if (demand > 0) {
          if (vassal.inventory.valueFor(g) > 1) {
            I.say("\nCity did not consume goods over time!");
            return false;
          }
        }
        if (demand < 0) {
          float supply = 0 - demand;
          if (vassal.inventory.valueFor(g) < supply - 1) {
            I.say("\nCity did not generate goods over time!");
            return false;
          }
        }
      }
      
      float endP = lord.prestige, endL = vassal.loyalty(lord);
      if (Nums.abs(endP - AVG_P) >= Nums.abs(initPrestige - AVG_P)) {
        I.say("\nCity prestige did not decay over time!");
        return false;
      }
      if (Nums.abs(endL - AVG_L) >= Nums.abs(initLoyalty  - AVG_L)) {
        I.say("\nCity loyalty did not decay over time!");
        return false;
      }
    }
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      City pair[] = configWeakStrongCityPair();
      float oldPower = pair[0].armyPower;
      runCompleteInvasion(pair[1], pair[0]);
      float newPower = pair[0].armyPower;
      
      if (newPower >= oldPower) {
        I.say("\nInvasion inflicted no casualties!");
        return false;
      }
      if (! pair[0].isVassalOf(pair[1])) {
        I.say("\nInvasion did not impose vassal status!");
        return false;
      }
      if (pair[0].loyalty(pair[1]) >= City.LOY_CIVIL) {
        I.say("\nInvasion did not sour relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a basic dialog attempt:
    {
      City pair[] = configWeakStrongCityPair();
      City goes = pair[0], from = pair[1];
      goes.council.typeAI = CityCouncil.AI_PACIFIST;
      runCompleteDialog(from, goes);
      
      if (from.posture(goes) != City.POSTURE.ALLY) {
        I.say("\nDialog did not create ally!");
        return false;
      }
    }
    
    //  This tests to ensure full life-cycles apply to the ruling house of
    //  foreign cities-
    {
      City pair[] = configWeakStrongCityPair();
      City mainC = pair[1];
      Actor monarch = (Actor) NOBLE  .generate();
      Actor queen   = (Actor) CONSORT.generate();
      monarch.sexData    = SEX_MALE;
      queen  .sexData    = SEX_FEMALE;
      monarch.ageSeconds = LIFESPAN_LENGTH / 4;
      queen  .ageSeconds = LIFESPAN_LENGTH / 4;
      
      ActorAsPerson.setBond(monarch, queen, BOND_MARRIED, BOND_MARRIED, 1);
      mainC.council.toggleMember(monarch, Role.MONARCH, true);
      mainC.council.toggleMember(queen  , Role.CONSORT, true);
      
      mainC.world.settings.toggleChildMort = false;
      
      int time = 0;
      while (time < LIFESPAN_LENGTH * 2) {
        mainC.world.updateWithTime(time++);
      }
      
      Series <Actor> heirs = mainC.council.allMembersWithRole(Role.HEIR);
      if (heirs.empty()) {
        I.say("\nNo heirs produced!");
        return false;
      }
      if (monarch.alive() || queen.alive()) {
        I.say("\nParents still alive!");
        return false;
      }
    }
    
    //  This tests for the effect of 'barbarian' invasions-
    {
      City pair[] = configWeakStrongCityPair();
      City goes = pair[0], from = pair[1];
      from.government = City.GOVERNMENT.BARBARIAN;
      runCompleteInvasion(from, goes);
      
      if (! goes.isEnemyOf(from)) {
        I.say("\nBarbarian invasion did not prompt correct posture!");
        return false;
      }
      if (City.suppliesDue(goes, from).size() > 0) {
        I.say("\nBarbarian invasion should not impose tribute!");
        return false;
      }
    }
    
    //  This tests for victory over another lord's vassal-
    {
      City pair[] = configWeakStrongCityPair();
      City weak = pair[0], strong = pair[1];
      World world = strong.world;
      
      City lord = new City(world);
      world.addCities(lord);
      City.setPosture(lord, weak, City.POSTURE.VASSAL, true);
      City capital = new City(world);
      world.addCities(capital);
      City.setPosture(capital, lord, City.POSTURE.VASSAL, true);
      
      if (capital != weak.capitalLord()) {
        I.say("\nDid not calculate capital correctly!");
        return false;
      }
      
      runCompleteInvasion(strong, weak);
      
      if (lord.isLordOf(weak)) {
        I.say("\nInvasion of vassal did not revoke lord's claim!");
        return false;
      }
      if (! weak.isVassalOf(strong)) {
        I.say("\nInvasion of vassal did not impose vassal status!");
        return false;
      }
      if (! capital.isEnemyOf(strong)) {
        I.say("\nInvasion of vassal did not provoke war!");
        return false;
      }
      if (capital.loyalty(strong) >= City.LOY_CIVIL) {
        I.say("\nInvasion of vassal did not sour relations with capital!");
        return false;
      }
    }
    
    //  This tests to ensure that rebellion has the expected effects upon a
    //  lord and vassal-
    {
      City pair[] = configWeakStrongCityPair();
      City vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      vassal.council.typeAI = CityCouncil.AI_DEFIANT;
      
      float initPrestige = lord.prestige;
      
      int time = 0;
      while (time < YEAR_LENGTH * (AVG_TRIBUTE_YEARS + 1)) {
        world.updateWithTime(time++);
      }
      
      if (vassal.isLoyalVassalOf(lord)) {
        I.say("\nDefiant vassal did not rebel!");
        return false;
      }
      if (vassal.isVassalOf(lord)) {
        I.say("\nCity in rebellion did not break relations!");
        return false;
      }
      if (lord.prestige >= initPrestige) {
        I.say("\nLord's prestige did not suffer from rebellion!");
      }
    }
    
    //  This tests for revolt-suppression-
    {
      City pair[] = configWeakStrongCityPair();
      City vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      vassal.council.typeAI = CityCouncil.AI_DEFIANT;
      
      int time = 0;
      while (vassal.isLoyalVassalOf(lord)) {
        world.updateWithTime(time++);
      }
      vassal.council.typeAI = CityCouncil.AI_OFF;

      runCompleteInvasion(lord, vassal);
      
      if (! vassal.isLoyalVassalOf(lord)) {
        I.say("\nRevolt suppression did not occur!");
        return false;
      }
    }
    
    //  And this tests to ensure that proper tribute is sent between foreign
    //  cities-
    {
      City pair[] = configWeakStrongCityPair();
      City vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      setSuppliesDue(vassal, lord, new Tally().setWith(STONE, 10));
      vassal.council.typeAI = CityCouncil.AI_COMPLIANT;
      
      int time = 0;
      while (time < YEAR_LENGTH * 0.8f) {
        world.updateWithTime(time++);
      }
      
      Relation r = vassal.relationWith(lord);
      float tributeSent = r.suppliesSent.valueFor(STONE);
      if (tributeSent < 5) {
        I.say("\nInsufficient tribute dispatched!");
        return false;
      }
    }
    
    //  TODO:  Test for synergy, marriage and trade-level effects on selecting
    //  for allies.
    
    {
      //final Good goods[] = { CLAY, MAIZE, WOOD, RAW_COTTON, POTTERY, COTTON };
      World world = new World(ALL_GOODS);
      world.assignCitizenTypes(ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
      
      City from[] = new City[6];
      City goes[] = new City[4];
      
      float relations[][] = {
        {  0.5f, -0.5f, -0.5f, -1.0f },
        {  0.5f, -0.7f, -0.2f,  0.0f },
        {  0.5f, -0.7f, -0.2f,  0.0f },
        {  0.0f,  0.0f,  0.0f,  0.0f },
        {  0.0f,  0.0f,  0.0f,  0.0f },
        {  0.0f,  0.0f,  0.0f,  0.0f },
      };
      
      for (int i = from.length; i-- > 0;) {
        from[i] = new City(world, "From_"+i);
      }
      for (int i = goes.length; i-- > 0;) {
        goes[i] = new City(world, "Goes_"+i);
      }
      world.addCities(from);
      world.addCities(goes);
      
      for (int i = from.length; i-- > 0;) {
        for (int j = goes.length; j-- > 0;) {
          incLoyalty(from[i], goes[j], relations[i][j]);
          incLoyalty(goes[j], from[i], relations[i][j]);
        }
      }
      
      for (City c : world.cities) {
        c.initBuildLevels(HOUSE, 2f, GARRISON, 2f);
        for (City o : world.cities) if (c != o) City.setupRoute(c, o, 1);
      }
      
      
      City main = from[0];
      
      //  Establish trade-options with city 3...
      main   .tradeLevel.setWith(RAW_COTTON,  5, POTTERY,  5);
      from[2].tradeLevel.setWith(RAW_COTTON, -5, POTTERY, -5);
      
      //  Establish enmity with city 5...
      incLoyalty(main, from[4], -0.5f);
      incLoyalty(from[4], main, -0.5f);
      
      //  TODO:  Establish a possible marriage with city 6 and test effects...
      
      CityCouncil.MissionAssessment
        D1 = main.council.dialogAssessment(main, from[1], false),
        D2 = main.council.dialogAssessment(main, from[2], false),
        D3 = main.council.dialogAssessment(main, from[3], false),
        D4 = main.council.dialogAssessment(main, from[4], false),
        allD[] = { D1, D2, D3, D4 };
      
      //*
      I.say("\nAppeal of alliances is: ");
      for (CityCouncil.MissionAssessment d : allD) {
        I.say("  "+d.goesC+": "+d.evaluatedAppeal);
      }
      //*/
      
      if (D1.evaluatedAppeal <= D3.evaluatedAppeal) {
        I.say("\nSimilar relations should boost appeal of alliance!");
        return false;
      }
      if (D2.evaluatedAppeal <= D1.evaluatedAppeal) {
        I.say("\nTrade-potential should boost appeal of alliance!");
        return false;
      }
      if (D4.evaluatedAppeal >= D3.evaluatedAppeal) {
        I.say("\nMutual enmity should lower appeal of alliance!");
        return false;
      }
      
      /*
      if (D5.evaluatedAppeal <= D3.evaluatedAppeal) {
        I.say("\nPotential marriage should boost appeal of alliance!");
      }
      //*/
    }
    
    //
    //  Now we set up random cities with random troops and resources, placed at
    //  each of the compass-points on the map, and run the simulation for a
    //  while to ensure that invasions take place at reasonable frequency-
    World world = new World(ALL_GOODS);
    world.assignCitizenTypes(ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
    
    final int NUM_CITIES = 4;
    final String names[] = { "Tollan", "Texcoco", "Tlacopan", "Tlaxcala" };
    final Good goods[] = { CLAY, MAIZE, WOOD, RAW_COTTON, POTTERY, COTTON };
    final int tints[] = {
      colour(9, 0, 0),
      colour(2, 2, 2),
      colour(0, 0, 9),
      colour(9, 9, 8)
    };
    
    for (int n = 0; n < 4; n++) {
      City city = new City(world);
      city.name = names[n];
      city.tint = tints[n];
      
      city.setWorldCoords(
        2 + (2 * TileConstants.T_X[n * 2]),
        2 + (2 * TileConstants.T_Y[n * 2])
      );
      for (Good g : goods) {
        float amount = (Rand.num() - 0.5f) * 10;
        amount = Nums.round(amount, 2, amount >= 0);
        city.tradeLevel.set(g, amount);
      }
      city.initBuildLevels(
        GARRISON, 2f + Rand.index(3),
        HOUSE   , 6f + Rand.index(10)
      );
      world.addCities(city);
    }
    world.mapHigh = 5;
    world.mapWide = 5;
    
    for (City c : world.cities) for (City o : world.cities) {
      if (c == o) continue;
      float dist = Nums.abs(c.mapX - o.mapX) + Nums.abs(c.mapY - o.mapY);
      City.setupRoute(c, o, (int) dist);
    }
    
    //
    //  Note:  This map is initialised purely to meet the requirements of the
    //  visual debugger...
    City mapCity = new City(world);
    CityMap map = new CityMap(mapCity);
    map.performSetup(8, new Terrain[0]);
    world.settings.worldView    = true;
    world.settings.speedUp      = true;
    world.settings.reportBattle = graphics;
    
    int MAX_TIME = LIFESPAN_LENGTH, NUM_YEARS = MAX_TIME / YEAR_LENGTH;
    boolean relationsOkay = true;
    int totalBattles = 0, timeWithEmpire = 0, timeWithAllies = 0;
    
    I.say("\nRunning world simulation...");
    while (map.time < MAX_TIME) {
      map = test.runLoop(map, 10, graphics, "saves/test_world.tlt");
      //
      //  Ensure that any forces being sent are of reasonable size:
      for (World.Journey j : world.journeys) {
        Object goes = j.going.first();
        if (goes instanceof Formation) {
          Formation force = (Formation) goes;
          City home = force.homeCity();
          if (j.goes == home) continue;
          
          if (force.powerSum() < AVG_ARMY_POWER / 4) {
            I.say("\n"+home+" is fighting with inadequate forces:");
            I.say("  Formation power: "+force.powerSum());
            I.say("  Average power:   "+AVG_ARMY_POWER);
            I.say("  Time: "+world.time+", going to: "+j.goes);
            return false;
          }
        }
      }
      //
      //  If anything big has happened, make sure relations stay consistent.
      if (! world.history.empty()) {
        for (World.Event e : world.history) {
          if (e.label.equals("attacked")) totalBattles += 1;
        }
        world.clearHistory();
        
        for (City c : world.cities) {
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
      
      for (City c : world.cities) {
        boolean hasEmpire = true;
        for (City o : world.cities) {
          if (o == mapCity || o == c) continue;
          if (o.capitalLord() != c) hasEmpire = false;
          if (o.isAllyOf(c)) timeWithAllies += timeStep;
        }
        empireExists |= hasEmpire;
      }
      
      if (empireExists) timeWithEmpire += timeStep;
    }
    
    //
    //  Finally, check to see whether total battles fall within reasonable
    //  bounds for the world in question:
    timeWithEmpire /= YEAR_LENGTH;
    timeWithAllies /= YEAR_LENGTH * NUM_CITIES * (NUM_CITIES - 1);
    int minBattles  = (NUM_YEARS - (timeWithEmpire + timeWithAllies)) / 2;
    int maxBattles  = NUM_YEARS * world.cities.size() * 2;
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
  
  
  static City[] configWeakStrongCityPair() {
    World world = new World(ALL_GOODS);
    world.assignCitizenTypes(ALL_CITIZENS, ALL_SOLDIERS, ALL_NOBLES);
    City a = new City(world);
    City b = new City(world);
    a.name = "Victim City" ;
    b.name = "Invader City";
    world.addCities(a, b);
    setupRoute(a, b, 1);
    a.initBuildLevels(HOUSE, 1f, GARRISON, 1f);
    b.initBuildLevels(HOUSE, 9f, GARRISON, 6f);
    a.council.typeAI = CityCouncil.AI_OFF;
    b.council.typeAI = CityCouncil.AI_OFF;
    return new City[] { a, b };
  }
  
  
  static void runCompleteInvasion(City from, City goes) {
    World world = from.world;
    
    CityCouncil.MissionAssessment IA = from.council.invasionAssessment(
      from, goes, 0.5f, false
    );
    Formation force = from.council.spawnFormation(IA);
    CityEvents.handleDeparture(force, from, goes);
    
    int time = 0;
    World.Journey j = world.journeyFor(force);
    while (! world.isComplete(j)) {
      world.updateWithTime(time++);
    }
  }
  
  
  static void runCompleteDialog(City from, City goes) {
    World world = from.world;
    
    CityCouncil.MissionAssessment DA = from.council.dialogAssessment(
      from, goes, false
    );
    Formation force = from.council.spawnFormation(DA);
    CityEvents.handleDeparture(force, from, goes);
    
    int time = 0;
    World.Journey j = world.journeyFor(force);
    while (! world.isComplete(j)) {
      world.updateWithTime(time++);
    }
    
    while (! (force.termsAccepted || force.termsRefused || force.complete)) {
      world.updateWithTime(time++);
    }
  }
  
  
  static boolean testRelationsOkay(City city) {
    int numLords = 0;
    
    for (City o : city.world.cities) {
      POSTURE p = city.posture(o);
      POSTURE i = o.posture(city);
      if (p == POSTURE.LORD) numLords++;
      if (p == POSTURE.VASSAL  && i != POSTURE.LORD   ) return false;
      if (p == POSTURE.LORD    && i != POSTURE.VASSAL ) return false;
      if (p == POSTURE.ENEMY   && i != POSTURE.ENEMY  ) return false;
      if (p == POSTURE.ALLY    && i != POSTURE.ALLY   ) return false;
      if (p == POSTURE.NEUTRAL && i != POSTURE.NEUTRAL) return false;
    }
    
    if (numLords > 1) return false;
    return true;
  }
  
  
  static void reportOnWorld(World world) {
    I.say("\nReporting world state:");
    for (City c : world.cities) {
      I.say("  "+c+":");
      I.say("    Pop: "+c.population);
      I.say("    Arm: "+c.armyPower );
      I.say("    Prs: "+c.prestige  );
      I.say("    Trd: "+c.tradeLevel);
      I.say("    Bld: "+c.buildLevel);
      I.say("    Inv: "+c.inventory );
      I.say("    Relations-");
      for (City o : world.cities) if (o != c) {
        City.Relation r = c.relationWith(o);
        I.add(" "+o+": "+r.posture+" "+r.loyalty);
      }
    }
  }
  
}




