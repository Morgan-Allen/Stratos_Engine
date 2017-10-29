

package game;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.*;



public class TestCityEvents extends Test {
  
  
  public static void main(String args[]) {
    testCityEvents(true);
  }
  
  static void testCityEvents(boolean graphics) {
    
    World world = new World();
    
    final Good goods[] = { CLAY, MAIZE, WOOD, RAW_COTTON, POTTERY, COTTON };
    
    for (int n = 5; n-- > 0;) {
      City city = new City(world);
      city.name = "City "+n;
      world.addCity(city);
      city.setWorldCoords(n, 0);
      
      for (Good g : goods) {
        city.tradeLevel.set(g, (Rand.num() - 0.5f) * 10);
      }
      city.initBuildLevels(
        GARRISON, 1f + Rand.index(3),
        HOUSE   , 5f + Rand.index(10)
      );
    }
    world.mapHigh = 5;
    world.mapWide = 5;
    
    for (City c : world.cities) for (City o : world.cities) {
      if (c == o) continue;
      float dist = Nums.abs(c.mapX - o.mapX) + Nums.abs(c.mapY - o.mapY);
      City.setupRoute(c, o, (int) dist);
    }
    
    
    
    
    //  Set up random cities with random troops and resources at various
    //  distances and sample/record the events that take place.
    
    int MAX_TIME = LIFESPAN_LENGTH;
    
    I.say("\nRunning world simulation...");
    
    while (world.time < MAX_TIME) {
      world.updateWithTime(world.time + 1);
      
      if (! world.history.empty()) {
        I.say("\nEvents:");
        for (World.Event e : world.history) {
          I.say("  "+world.descFor(e));
        }
        world.clearHistory();
      }
    }

    
    //  You'll also need a way to test that failure-to-pay-tribute results
    //  in retaliation, and so forth.  For that, you'll need flags to force
    //  attitudes.
    
    //  And you'll want to test a variety of single-city interactions.
    
    
    /*
    for (City city : world.cities) {
      List <CityEvents.InvasionAssessment> choices;
      choices = city.events.updateInvasionChoices();
      
      for (CityEvents.InvasionAssessment a : choices) {
        I.say("\n  "+a.attackC+" -> "+a.defendC);
        for (Field f : a.getClass().getDeclaredFields()) try {
          I.say("    "+f.getName()+": "+f.get(a));
        }
        catch (IllegalAccessException e) {
          I.say("    Could not access "+f.getName());
        }
      }
    }
    //*/
  }
  
}




