

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;
import java.lang.reflect.*;



public class TestCityEvents extends Test {
  
  
  public static void main(String args[]) {
    testCityEvents(true);
  }
  
  
  static void testCityEvents(boolean graphics) {
    

    //
    //  And you'll want to test a variety of single-city interactions.
    //    Failure to pay tribute
    //    Revolt
    //    Consumption of goods
    //    Regeneration of reputation & loyalty
    
    //  This tests for the basic outcomes of a single invasion attempt:
    {
      City pair[] = configWeakStrongCityPair();
      float oldPower = pair[0].armyPower;
      runCompleteInvasion(pair);
      float newPower = pair[0].armyPower;
      
      if (newPower >= oldPower) {
        I.say("\nInvasion inflicted no casualties!");
        return;
      }
      if (! pair[0].isVassalOf(pair[1])) {
        I.say("\nInvasion did not impose vassal status!");
        return;
      }
    }
    
    //  This tests for the effect of 'barbarian' invasions-
    {
      City pair[] = configWeakStrongCityPair();
      pair[1].government = City.GOVERNMENT.BARBARIAN;
      runCompleteInvasion(pair);
      
      if (! pair[0].isEnemyOf(pair[1])) {
        I.say("\nBarbarian invasion did not prompt correct posture!");
        return;
      }
    }
    
    //  This tests for victory over another lord's vassal-
    {
      City pair[] = configWeakStrongCityPair();
      World world = pair[0].world;
      City lord = new City(world);
      world.addCity(lord);
      City.setPosture(lord, pair[0], City.POSTURE.VASSAL, true);
      runCompleteInvasion(pair);
      
      if (lord.isLordOf(pair[0])) {
        I.say("\nInvasion of vassal did not revoke lord's claim!");
        return;
      }
      if (! pair[0].isVassalOf(pair[1])) {
        I.say("\nInvasion of vassal did not impose vassal status!");
        return;
      }
    }
    
    
    //
    //  Now we set up random cities with random troops and resources, placed at
    //  each of the compass-points on the map, and run the simulation for a
    //  while to ensure that invasions do take place.
    
    World world = new World();
    
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
      world.addCity(city);
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
    map.performSetup(8);
    map.settings.worldView = true;
    map.settings.speedUp   = true;
    
    
    int MAX_TIME = LIFESPAN_LENGTH;
    boolean relationsOkay = true;
    int totalBattles = 0;
    
    I.say("\nRunning world simulation...");
    while (map.time < MAX_TIME) {
      runGameLoop(map, 1, graphics, "saves/test_city_events.tlt");
      
      if (! world.history.empty()) {
        I.say("\nEvents:");
        for (World.Event e : world.history) {
          I.say("  "+world.descFor(e));
          if (e.label.equals("attacked")) totalBattles += 1;
        }
        world.clearHistory();
        
        reportOnWorld(world);
        
        for (City c : world.cities) {
          if (! testRelationsOkay(c)) relationsOkay = false;
        }
        
        if (! relationsOkay) {
          I.say("\nINCONSISTENT CITY RELATIONS, WILL QUIT");
          break;
        }
      }
    }
    
    //  Note:  There's a problem where the cities wind up completely exhausting
    //  eachother's armies in skirmishes, which isn't completely realistic.
    //  Try to add a 'war weariness' factor or something to balance that?
    
    
    
    
    I.say("\nCITY EVENTS TESTING CONCLUDED...");
    I.say("  Total battles: "+totalBattles);
    reportOnWorld(world);
  }
  
  
  static City[] configWeakStrongCityPair() {
    World world = new World();
    City a = new City(world);
    City b = new City(world);
    world.cities.add(a);
    world.cities.add(b);
    setupRoute(a, b, 1);
    a.initBuildLevels(HOUSE, 1f, GARRISON, 1f);
    b.initBuildLevels(HOUSE, 9f, GARRISON, 4f);
    a.council.toggleAI = false;
    b.council.toggleAI = false;
    return new City[] { a, b };
  }
  
  
  static void runCompleteInvasion(City... pair) {
    City goes = pair[0], from = pair[1];
    World world = from.world;
    
    CityCouncil.InvasionAssessment IA = from.council.performAssessment(
      from, goes, 0.5f, false
    );
    Formation force = from.council.spawnInvasion(IA);
    CityEvents.handleDeparture(force, from, goes);
    
    int time = 0;
    World.Journey j = world.journeyFor(force);
    while (! world.isComplete(j)) {
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




