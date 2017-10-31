

package game;
import static game.GameConstants.*;
import util.*;



public class TestSieging extends Test {
  
  
  public static void main(String args[]) {
    testSieging(true);
  }
  
  
  static boolean testSieging(boolean graphics) {
    
    World   world = GameConstants.setupDefaultWorld();
    City    cityA = world.cities.atIndex(0);
    City    cityB = world.cities.atIndex(1);
    CityMap map   = CityMapTerrain.generateTerrain(
      cityA, 32, MEADOW, JUNGLE
    );
    map.settings.toggleFog = false;
    cityA.name = "Home City";
    cityB.name = "Away City";
    cityB.council.typeAI = CityCouncil.AI_OFF;
    
    City.setupRoute(cityA, cityB, 1);
    City.setPosture(cityA, cityB, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    CityMap.applyPaving(map, 10, 9, 40, 1, true);
    
    Building store = (Building) PORTER_HOUSE.generate();
    store.enterMap(map, 10, 6, 1);
    
    
    Formation enemies = new Formation();
    enemies.setupFormation(GARRISON, cityB);
    
    for (int n = 8; n-- > 0;) {
      Actor fights = (Actor) ((n < 3) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(cityB);
      enemies.toggleRecruit(fights, true);
    }
    enemies.beginSecuring(cityA);
    Tally <Good> tribute = new Tally().setWith(COTTON, 10);
    enemies.assignDemands(City.POSTURE.VASSAL, null, tribute);
    
    float initPrestige = cityB.prestige;
    float initLoyalty  = cityA.loyalty(cityB);
    
    
    boolean victorious  = false;
    boolean tributePaid = false;
    
    while (map.time < 1000 || graphics) {
      map = runGameLoop(map, 10, graphics, "saves/test_sieging.tlt");
      
      if (cityA.isVassalOf(cityB) && (! enemies.away) && ! victorious) {
        victorious = true;
        store.inventory.add(tribute);
        fillWorkVacancies(store);
      }
      
      if (victorious && ! tributePaid) {
        
        City.Relation r = cityA.relationWith(cityB);
        boolean allSent = true;
        for (Good g : tribute.keys()) {
          float need = tribute.valueFor(g);
          float sent = r.suppliesSent.valueFor(g);
          if (need < sent) allSent = false;
        }
        tributePaid = allSent;
        
        if (cityA.currentFunds > 0) {
          I.say("\nShould not receive payment for tribute!");
          break;
        }
        
        if (cityB.prestige <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (cityA.loyalty(cityB) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
        
        if (tributePaid) {
          I.say("\nSIEGING TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }
    
    I.say("\nSIEGE TEST FAILED!");
    I.say("  Victorious:   "+victorious);
    I.say("  Tribute paid: "+tributePaid);
    return false;
  }
  
  
  
}









