


package test;
import util.*;
import content.*;
import game.*;
import static game.Actor.*;
import static game.ActorAsPerson.*;
import static game.Base.*;
import static game.BaseCouncil.*;
import static game.GameConstants.*;
import static game.World.*;
import static content.GameContent.*;



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
      
      vassal.setTradeLevel(CARBS   , 0, 10);
      vassal.setTradeLevel(MEDICINE, 5, 0 );
      //vassal.initTradeLevels(CARBS, 10f, MEDICINE, -5f);
      for (Good g : vassal.needLevels().keys()) {
        float demand = vassal.needLevel(g);
        vassal.setInventory(g, demand);
      }
      
      float AVG_P = Base.PRESTIGE_AVG, AVG_L = Base.LOY_CIVIL;
      float initPrestige = AVG_P + ((Rand.yes() ? 1 : -1) * 10);
      float initLoyalty  = AVG_L + ((Rand.yes() ? 1 : -1) / 2f);
      lord.initPrestige(initPrestige);
      Base.incLoyalty(vassal, lord, initLoyalty);
      
      int time = 0;
      while (time < YEAR_LENGTH) {
        world.updateWithTime(time++);
      }
      
      for (Good g : vassal.needLevels().keys()) {
        if (vassal.inventory(g) > 1) {
          I.say("\nCity did not consume goods over time!");
          return false;
        }
      }
      for (Good g : vassal.prodLevels().keys()) {
        float supply = vassal.prodLevel(g);
        if (vassal.inventory(g) < supply - 1) {
          I.say("\nCity did not generate goods over time!");
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
      
      float endP = lord.prestige(), endL = vassal.loyalty(lord);
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
      Base pair[] = configWeakStrongCityPair();
      float oldPower = pair[0].armyPower();
      runCompleteInvasion(pair[1], pair[0]);
      float newPower = pair[0].armyPower();
      
      if (newPower >= oldPower) {
        I.say("\nInvasion inflicted no casualties!");
        return false;
      }
      if (! pair[0].isVassalOf(pair[1])) {
        I.say("\nInvasion did not impose vassal status!");
        return false;
      }
      if (pair[0].loyalty(pair[1]) >= Base.LOY_CIVIL) {
        I.say("\nInvasion did not sour relations!");
        return false;
      }
    }
    
    //  This tests for the outcome of a basic dialog attempt:
    {
      Base pair[] = configWeakStrongCityPair();
      Base goes = pair[0], from = pair[1];
      goes.council.setTypeAI(BaseCouncil.AI_PACIFIST);
      runCompleteDialog(from, goes);
      
      if (from.posture(goes) != Base.POSTURE.ALLY) {
        I.say("\nDialog did not create ally!");
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
      if (monarch.health.alive() || queen.health.alive()) {
        I.say("\nParents still alive!");
        return false;
      }
    }
    
    //  This tests for the effect of 'barbarian' invasions-
    {
      Base pair[] = configWeakStrongCityPair();
      Base goes = pair[0], from = pair[1];
      from.setGovernment(Base.GOVERNMENT.BARBARIAN);
      runCompleteInvasion(from, goes);
      
      if (! goes.isEnemyOf(from)) {
        I.say("\nBarbarian invasion did not prompt correct posture!");
        return false;
      }
      if (Base.suppliesDue(goes, from).size() > 0) {
        I.say("\nBarbarian invasion should not impose tribute!");
        return false;
      }
    }
    
    //  This tests for victory over another lord's vassal-
    {
      Base pair[] = configWeakStrongCityPair();
      Base weak = pair[0], strong = pair[1];
      World world = strong.world;
      
      Base lord = new Base(world, world.addLocale(0, 1));
      world.addBases(lord);
      Base.setPosture(lord, weak, Base.POSTURE.VASSAL, true);
      Base capital = new Base(world, world.addLocale(1, 1));
      world.addBases(capital);
      Base.setPosture(capital, lord, Base.POSTURE.VASSAL, true);
      
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
      if (capital.loyalty(strong) >= Base.LOY_CIVIL) {
        I.say("\nInvasion of vassal did not sour relations with capital!");
        return false;
      }
    }
    
    //  This tests to ensure that rebellion has the expected effects upon a
    //  lord and vassal-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      vassal.council.setTypeAI(BaseCouncil.AI_DEFIANT);
      
      float initPrestige = lord.prestige();
      
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
      if (lord.prestige() >= initPrestige) {
        I.say("\nLord's prestige did not suffer from rebellion!");
      }
    }
    
    //  This tests for revolt-suppression-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      vassal.council.setTypeAI(BaseCouncil.AI_DEFIANT);
      
      int time = 0;
      while (vassal.isLoyalVassalOf(lord)) {
        world.updateWithTime(time++);
      }
      vassal.council.setTypeAI(BaseCouncil.AI_OFF);

      runCompleteInvasion(lord, vassal);
      
      if (! vassal.isLoyalVassalOf(lord)) {
        I.say("\nRevolt suppression did not occur!");
        return false;
      }
    }
    
    //  And this tests to ensure that proper tribute is sent between foreign
    //  cities-
    {
      Base pair[] = configWeakStrongCityPair();
      Base vassal = pair[0], lord = pair[1];
      World world = vassal.world;
      setPosture(vassal, lord, POSTURE.LORD, true);
      setSuppliesDue(vassal, lord, new Tally().setWith(SPYCE, 10));
      vassal.council.setTypeAI(BaseCouncil.AI_COMPLIANT);
      
      int time = 0;
      while (time < YEAR_LENGTH * 0.8f) {
        world.updateWithTime(time++);
      }
      
      float tributeSent = Base.suppliesDue(vassal, lord, SPYCE);
      if (tributeSent < 5) {
        I.say("\nInsufficient tribute dispatched!");
        return false;
      }
    }
    
    //  TODO:  Test for synergy, marriage and trade-level effects on selecting
    //  for allies.
    
    {
      World world = new World(ALL_GOODS);
      world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
      
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
        from[i] = new Base(world, world.addLocale(0, i), "From_"+i);
      }
      for (int i = goes.length; i-- > 0;) {
        goes[i] = new Base(world, world.addLocale(1, i), "Goes_"+i);
      }
      world.addBases(from);
      world.addBases(goes);
      
      for (int i = from.length; i-- > 0;) {
        for (int j = goes.length; j-- > 0;) {
          incLoyalty(from[i], goes[j], relations[i][j]);
          incLoyalty(goes[j], from[i], relations[i][j]);
        }
      }
      
      for (Base c : world.bases()) {
        c.initBuildLevels(HOLDING, 2f, TROOPER_LODGE, 2f);
        for (Base o : world.bases()) if (c != o) {
          World.setupRoute(c.locale, o.locale, 1);
        }
      }
      
      
      Base main = from[0];
      
      //  Establish trade-options with city 3...
      main   .setTradeLevel(GREENS, 0, 5);
      main   .setTradeLevel(PARTS , 0, 5);
      from[2].setTradeLevel(GREENS, 5, 0);
      from[2].setTradeLevel(PARTS , 5, 0);
      //main   .initTradeLevels(GREENS,  5, PARTS,  5);
      //from[2].initTradeLevels(GREENS, -5, PARTS, -5);
      
      //  Establish enmity with city 5...
      incLoyalty(main, from[4], -0.5f);
      incLoyalty(from[4], main, -0.5f);
      
      //  TODO:  Establish a possible marriage with city 6 and test effects...
      
      BaseCouncil.MissionAssessment
        D1 = main.council.dialogAssessment(main, from[1], false),
        D2 = main.council.dialogAssessment(main, from[2], false),
        D3 = main.council.dialogAssessment(main, from[3], false),
        D4 = main.council.dialogAssessment(main, from[4], false),
        allD[] = { D1, D2, D3, D4 };
      
      I.say("\nAppeal of alliances is: ");
      for (BaseCouncil.MissionAssessment d : allD) {
        I.say("  "+d.goes()+": "+d.appeal());
      }
      if (D1.appeal() <= D3.appeal()) {
        I.say("\nSimilar relations should boost appeal of alliance!");
        return false;
      }
      if (D2.appeal() <= D1.appeal()) {
        I.say("\nTrade-potential should boost appeal of alliance!");
        return false;
      }
      if (D4.appeal() >= D3.appeal()) {
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
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    
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
      Locale l = world.addLocale(
        2 + (2 * TileConstants.T_X[n * 2]),
        2 + (2 * TileConstants.T_Y[n * 2])
      );
      Base city = new Base(world, l);
      city.setName(names[n]);
      city.setTint(tints[n]);
      
      for (Good g : goods) {
        float amount = (Rand.num() - 0.5f) * 10;
        amount = Nums.round(amount, 2, amount >= 0);
        if (amount > 0) city.setTradeLevel(g, amount, 0);
        else city.setTradeLevel(g, 0, 0 - amount);
      }
      city.initBuildLevels(
        TROOPER_LODGE, 2f + Rand.index(3),
        HOLDING      , 6f + Rand.index(10)
      );
      if (Rand.yes()) city.council.setTypeAI(BaseCouncil.AI_WARLIKE);
      world.addBases(city);
    }
    world.setMapSize(5, 5);
    
    for (Base c : world.bases()) for (Base o : world.bases()) {
      if (c == o) continue;
      float dist = World.mapCoords(c).lineDist(World.mapCoords(o));
      World.setupRoute(c.locale, o.locale, (int) dist);
    }
    
    //
    //  Note:  This map is initialised purely to meet the requirements of the
    //  visual debugger...
    Base mapCity = new Base(world, world.addLocale(0, 0));
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
          
          float power = MissionStrike.powerSum(force.recruits(), null);
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
          if (o.capitalLord() != c) hasEmpire = false;
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
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    Base a = new Base(world, world.addLocale(0, 0));
    Base b = new Base(world, world.addLocale(1, 0));
    a.setName("Victim City" );
    b.setName("Invader City");
    world.addBases(a, b);
    setupRoute(a.locale, b.locale, 1);
    a.initBuildLevels(HOLDING, 1f, TROOPER_LODGE, 1f);
    b.initBuildLevels(HOLDING, 9f, TROOPER_LODGE, 6f);
    a.council.setTypeAI(BaseCouncil.AI_OFF);
    b.council.setTypeAI(BaseCouncil.AI_OFF);
    return new Base[] { a, b };
  }
  
  
  static void runCompleteInvasion(Base from, Base goes) {
    World world = from.world;
    
    BaseCouncil.MissionAssessment IA = from.council.invasionAssessment(
      from, goes, 0.5f, false
    );
    Mission force = from.council.spawnFormation(IA);
    force.beginMission(from);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
  }
  
  
  static void runCompleteDialog(Base from, Base goes) {
    World world = from.world;
    
    BaseCouncil.MissionAssessment DA = from.council.dialogAssessment(
      from, goes, false
    );
    Mission force = from.council.spawnFormation(DA);
    force.beginMission(from);
    
    int time = 0;
    while (time < YEAR_LENGTH && force.active() && ! force.complete()) {
      world.updateWithTime(time++);
    }
  }
  
  
  static boolean testRelationsOkay(Base city) {
    int numLords = 0;
    
    for (Base o : city.world.bases()) {
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
    for (Base c : world.bases()) {
      I.say("  "+c+":");
      I.say("    Pop:    "+c.population()+" / "+c.idealPopulation());
      I.say("    Arm:    "+c.armyPower ()+" / "+c.idealArmyPower ());
      I.say("    Prs:    "+c.prestige());
      I.say("    Need:   "+c.needLevels());
      I.say("    Accept: "+c.prodLevels());
      I.say("    Bld:    "+c.buildLevel());
      I.say("    Inv:    "+c.inventory());
      I.say("    Relations-");
      for (Base o : world.bases()) if (o != c) {
        I.add(" "+o+": "+c.posture(o)+" "+c.loyalty(o));
      }
    }
  }
  
}







