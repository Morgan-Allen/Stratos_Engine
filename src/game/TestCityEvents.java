

package game;
import util.*;
import static game.GameConstants.*;
import java.lang.reflect.*;



public class TestCityEvents extends Test {
  
  
  public static void main(String args[]) {
    testCityEvents(true);
  }
  
  static void testCityEvents(boolean graphics) {
    
    World world = setupDefaultWorld();
    I.say("\nUpdating invasion choices:");
    
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
  }
  
}













