

package game;
import util.*;
import static game.GameConstants.*;



public class TestMilitary extends Test {
  
  
  public static void main(String args[]) {
    testMilitary(true);
  }
  
  
  static boolean testMilitary(boolean graphics) {
    Test test = new TestMilitary();
    
    World   world = GameConstants.setupDefaultWorld();
    City    homeC = world.cities.atIndex(0);
    City    awayC = world.cities.atIndex(1);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    homeC.name = "Home City";
    awayC.name = "Away City";
    awayC.council.typeAI = CityCouncil.AI_OFF;
    map.settings.toggleFog = false;
    
    City.setupRoute(homeC, awayC, 1);
    City.setPosture(homeC, awayC, City.POSTURE.ENEMY, true);
    
    
    BuildingForArmy fort = (BuildingForArmy) GARRISON.generate();
    fort.enterMap(map, 10, 10, 1);
    fillWorkVacancies(fort);
    CityMapPlanning.placeStructure(ROAD, map, true, 2, 9, 30, 1);
    
    for (int n = 8; n-- > 0;) {
      Building house = (Building) HOUSE.generate();
      house.enterMap(map, 2 + (n * 3), 7, 1);
      fillHomeVacancies(house, CITIZEN);
    }
    
    float initPrestige = homeC.prestige;
    float initLoyalty  = awayC.loyalty(homeC);
    
    
    Formation troops  = null;
    Formation enemies = new Formation(new ObjectiveConquer(), awayC);
    for (int n = 4; n-- > 0;) {
      Actor fights = (Actor) ((n == 0) ? SOLDIER : CITIZEN).generate();
      fights.assignHomeCity(awayC);
      enemies.toggleRecruit(fights, true);
    }
    awayC.armyPower = AVG_ARMY_POWER / 4;
    
    
    boolean recruited = false;
    boolean invaded   = false;
    boolean homeWin   = false;
    boolean invading  = false;
    boolean awayWin   = false;
    boolean backHome  = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_military.tlt");
      
      if (fort.recruits.size() >= 8 && ! recruited) {
        troops = new Formation(new ObjectiveConquer(), homeC);
        fort.deployInFormation(troops, true);
        troops.beginSecuring(map.tileAt(25, 25), TileConstants.E, null, map);
        recruited = true;
      }
      
      if (troops != null && troops.formationReady() && ! invaded) {
        enemies.beginSecuring(homeC);
        World.Journey j = world.journeyFor(enemies);
        world.completeJourney(j);
        enemies.beginSecuring(troops.securePoint, TileConstants.W, troops, map);
        invaded = true;
      }
      
      if (recruited && ! homeWin) {
        boolean survivors = false;
        for (Actor w : enemies.recruits) {
          if (w.state < Actor.STATE_DEAD) survivors = true;
        }
        homeWin = ! survivors;
        if (homeWin) {
          troops.disbandFormation();
          fillAllVacancies(map);
        }
      }
      
      if (homeWin && fort.recruits.size() >= 12 && ! invading) {
        troops = new Formation(new ObjectiveConquer(), homeC);
        fort.deployInFormation(troops, true);
        troops.beginSecuring(awayC);
        troops.assignDemands(City.POSTURE.VASSAL, null, null);
        invading = true;
      }
      
      if (invading && homeC.isLordOf(awayC)) {
        awayWin = true;
      }
      
      if (awayWin && ! backHome) {
        boolean someAway = false;
        for (Actor w : fort.recruits) {
          if (w.map != map) someAway = true;
        }
        backHome = fort.recruits.size() > 8 && ! someAway;
        
        if (homeC.prestige <= initPrestige) {
          I.say("\nPrestige should be boosted by conquest!");
          break;
        }
        
        if (awayC.loyalty(homeC) >= initLoyalty) {
          I.say("\nLoyalty should be reduced by conquest!");
          break;
        }
        
        if (backHome) {
          I.say("\nMILITARY TEST CONCLUDED SUCCESSFULLY!");
          if (! graphics) return true;
        }
      }
    }

    I.say("\nMILITARY TEST FAILED!");
    I.say("  Recruited: "+recruited);
    I.say("  Invaded:   "+invaded  );
    I.say("  Home win:  "+homeWin  );
    I.say("  Invading:  "+invading );
    I.say("  Away win:  "+awayWin  );
    I.say("  Back home: "+backHome );
    I.say("  Current recuits: "+fort.recruits().size());
    
    return false;
  }
  
}


