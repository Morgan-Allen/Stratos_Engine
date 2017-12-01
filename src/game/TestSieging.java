

package game;
import static game.GameConstants.*;
import util.*;



public class TestSieging extends Test {
  
  
  public static void main(String args[]) {
    testSieging(true);
  }
  
  
  static boolean testSieging(boolean graphics) {
    Test test = new TestSieging();
    
    World   world = new World();
    City    homeC = new City(world);
    City    awayC = new City(world);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    map.settings.toggleFog = false;
    homeC.name = "Home City";
    awayC.name = "Away City";
    world.addCities(homeC, awayC);
    
    awayC.initBuildLevels(GARRISON, 5, HOUSE, 1);
    awayC.council.typeAI = CityCouncil.AI_OFF;
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(ROAD, map, true, 10, 9, 40, 1);
    
    Building store = (Building) PORTER_POST.generate();
    store.enterMap(map, 10, 6, 1);
    store.inventory.setWith(COTTON, 10);
    
    
    float initPrestige = awayC.prestige;
    float initLoyalty  = homeC.loyalty(awayC);
    Formation enemy = null;
    Tally <Good> tribute = null;
    
    boolean evalDone    = false;
    boolean siegeComing = false;
    boolean victorious  = false;
    boolean tributePaid = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_sieging.tlt");
      
      if (! evalDone) {
        boolean checked = true;
        if (homeC.armyPower <= 0   ) checked = false;
        if (homeC.inventory.empty()) checked = false;
        
        if (checked) {
          evalDone = true;
          awayC.council.typeAI = CityCouncil.AI_WARLIKE;
        }
      }
      
      if (evalDone && ! siegeComing) {
        for (World.Journey j : map.city.world.journeys) {
          Object goes = j.going.first();
          if (goes instanceof Formation) {
            enemy = (Formation) goes;
            tribute = enemy.tributeDemand;
            siegeComing = true;
          }
        }
      }
      
      if (siegeComing && homeC.isVassalOf(awayC) && ! victorious) {
        victorious = true;
        store.inventory.add(tribute);
        fillWorkVacancies(store);
      }
      
      if (victorious && ! tributePaid) {
        
        City.Relation r = homeC.relationWith(awayC);
        boolean allSent = true;
        for (Good g : tribute.keys()) {
          float need = tribute.valueFor(g);
          float sent = r.suppliesSent.valueFor(g);
          if (sent < need) allSent = false;
        }
        tributePaid = allSent;
        
        if (homeC.currentFunds > 0) {
          I.say("\nShould not receive payment for tribute!");
          break;
        }
        
        if (awayC.prestige <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (homeC.loyalty(awayC) >= initLoyalty) {
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
    I.say("  Siege coming: "+siegeComing);
    I.say("  Victorious:   "+victorious );
    I.say("  Tribute paid: "+tributePaid);
    return false;
  }
  
  
  
  //  Older invasion-setup code, kept for posterity.
  /*
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

  //*/
  
}









