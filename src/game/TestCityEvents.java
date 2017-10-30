

package game;
import util.*;
import static game.City.*;
import static game.GameConstants.*;
import java.lang.reflect.*;



public class TestCityEvents extends Test {
  
  
  public static void main(String args[]) {
    testCityEvents(true);
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
  
  
  static void testCityEvents(boolean graphics) {
    
    World world = new World();
    
    final String names[] = { "Tollan", "Texcoco", "Tlacopan", "Tlaxcala" };
    final Good goods[] = { CLAY, MAIZE, WOOD, RAW_COTTON, POTTERY, COTTON };
    
    for (int n = 0; n < 4; n++) {
      City city = new City(world);
      city.name = names[n];
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
    boolean relationsOkay = true;
    
    
    I.say("\nRunning world simulation...");
    while (world.time < MAX_TIME) {
      world.updateWithTime(world.time + 1);
      
      if (! world.history.empty()) {
        I.say("\nEvents:");
        for (World.Event e : world.history) {
          I.say("  "+world.descFor(e));
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
    
    
    //  And you'll want to test a variety of single-city interactions.
    //    Clash-and-victory
    //    Clash-and-defeat
    //    Clash-and-victory for barbarian
    //    Failure to pay tribute
    //    Revolt
    //    Victory over another lord's vassal
    //    Consumption of goods
    //    Regeneration of reputation & loyalty
    
    
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




