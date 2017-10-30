

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
    //  First, set up random cities with random troops and resources, placed at
    //  each of the compass-points on the map, and 
    
    World world = new World();
    
    final String names[] = { "Tollan", "Texcoco", "Tlacopan", "Tlaxcala" };
    final Good goods[] = { CLAY, MAIZE, WOOD, RAW_COTTON, POTTERY, COTTON };
    final int tints[] = {
      colour(10, 0 , 0 ),
      colour(0 , 0 , 0 ),
      colour(0 , 0 , 10),
      colour(10, 10, 8 )
    };
    
    for (int n = 0; n < 4; n++) {
      City city = new City(world);
      city.name = names[n];
      city.tint = tints[n];
      //
      //I.say("Tint is "+city.tint);
      
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
      Test.runGameLoop(map, 10, graphics, "saves/test_city_events.tlt");
      //world.updateWithTime(world.time + 1);
      
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
    
    //
    //  And you'll want to test a variety of single-city interactions.
    //    Clash-and-victory
    //    Clash-and-defeat
    //    Clash-and-victory for barbarian
    //    Failure to pay tribute
    //    Revolt
    //    Victory over another lord's vassal
    //    Consumption of goods
    //    Regeneration of reputation & loyalty
    
    I.say("\nCITY EVENTS TESTING CONCLUDED...");
    I.say("  Total battles: "+totalBattles);
    reportOnWorld(world);
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




