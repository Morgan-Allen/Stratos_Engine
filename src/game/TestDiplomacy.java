

package game;
import util.*;
import static game.ActorAsPerson.*;
import static game.City.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;



public class TestDiplomacy extends Test {
  
  
  public static void main(String args[]) {
    testDiplomacy(true);
  }
  
  
  static boolean testDiplomacy(boolean graphics) {
    Test test = new TestDiplomacy();
    
    World   world = new World();
    City    homeC = new City(world);
    City    awayC = new City(world);
    City    neutC = new City(world);
    CityMap map   = CityMapTerrain.generateTerrain(
      homeC, 32, 0, MEADOW, JUNGLE
    );
    world.addCities(homeC, awayC, neutC);
    homeC.name = "Home City";
    awayC.name = "Away City";
    neutC.name = "Neutral City";
    awayC.council.typeAI = AI_OFF;
    neutC.council.typeAI = AI_OFF;
    world.settings.toggleFog     = false;
    world.settings.toggleMigrate = false;
    
    
    CityMapPlanning.placeStructure(WALL, map, true, 7, 7, 12, 12);
    CityMapPlanning.markDemolish(map, true, 8, 8, 10, 10);
    
    Building gate = (Building) GATE.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 12, 17, 1);
    
    Building palace = (Building) PALACE.generate();
    CityCouncil council = map.city.council;
    palace.enterMap(map, 10, 10, 1);
    CityMapPlanning.placeStructure(ROAD, map, true, 12, 19, 1, 13);
    
    ActorAsPerson monarch = (ActorAsPerson) NOBLE.generate();
    council.toggleMember(monarch, Role.MONARCH, true);
    palace.setResident(monarch, true);
    monarch.enterMap(map, 12, 9, 1);
    
    ActorAsPerson minister = (ActorAsPerson) NOBLE.generate();
    council.toggleMember(minister, Role.PRIME_MINISTER, true);
    palace.setResident(minister, true);
    minister.enterMap(map, 12, 9, 1);
    
    Building garrison = (Building) GARRISON.generate();
    garrison.enterMap(map, 12, 1, 1);
    Test.fillAllVacancies(map);
    
    
    Formation escort;
    escort = new Formation(Formation.OBJECTIVE_DIALOG, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor s = (Actor) SOLDIER.generate();
      s.assignHomeCity(awayC);
      escort.toggleRecruit(s, true);
    }
    
    Actor envoy = (Actor) NOBLE.generate();
    escort.toggleEscorted(envoy, true);
    Actor bride = (Actor) CONSORT.generate();
    escort.toggleEscorted(bride, true);
    
    for (Actor e : escort.escorted) e.assignHomeCity(awayC);
    
    escort.assignTerms(City.POSTURE.ALLY, null, bride, null);
    escort.beginSecuring(homeC);
    
    boolean escortArrived  = false;
    boolean offerGiven     = false;
    boolean offerAccepted  = false;
    boolean termsOkay      = false;
    boolean escortDeparted = false;
    boolean escortSent     = false;
    boolean termsAwayGiven = false;
    boolean termsAwayOkay  = false;
    boolean escortReturned = false;
    boolean testOkay       = false;
    
    while (map.time < 1000 || graphics) {
      map = test.runLoop(map, 1, graphics, "saves/test_diplomacy.tlt");
      
      if (! escortArrived) {
        escortArrived = escort.map == map;
      }
      
      if (escortArrived && ! offerGiven) {
        offerGiven = council.petitions.includes(escort);
      }
      
      if (offerGiven && ! offerAccepted) {
        council.acceptTerms(escort);
        offerAccepted = true;
      }
      
      if (offerAccepted && ! termsOkay) {
        boolean termsFilled = true;
        termsFilled &= monarch.hasBondType(bride, BOND_MARRIED);
        termsFilled &= homeC.isAllyOf(awayC);
        termsOkay = termsFilled;
      }
      
      if (termsOkay && ! escortDeparted) {
        escortDeparted = escort.map == null;
      }
      
      if (escortDeparted && ! escortSent) {
        escort = new Formation(Formation.OBJECTIVE_DIALOG, homeC, true);
        escort.assignTerms(POSTURE.TRADING, null, null, null);
        garrison.deployInFormation(escort, true);
        escort.toggleEscorted(minister, true);
        escort.beginSecuring(neutC);
        escortSent = true;
      }
      
      if (escortSent && ! termsAwayGiven) {
        termsAwayGiven = neutC.council.petitions().includes(escort);
      }
      
      if (termsAwayGiven && ! termsAwayOkay) {
        neutC.council.acceptTerms(escort);
        termsAwayOkay = true;
      }
      
      if (termsAwayOkay && ! escortReturned) {
        boolean allBack = true;
        for (Actor a : escort.recruits) if (a.map() != map) allBack = false;
        for (Actor a : escort.escorted) if (a.map() != map) allBack = false;
        escortReturned = allBack;
      }
      
      if (escortReturned && ! testOkay) {
        I.say("\nDIPLOMACY TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nDIPLOMACY TEST FAILED!");
    I.say("  Escort arrived:   "+escortArrived );
    I.say("  Offer given:      "+offerGiven    );
    I.say("  Offer accepted:   "+offerAccepted );
    I.say("  Terms okay:       "+termsOkay     );
    I.say("  Escort departed:  "+escortDeparted);
    I.say("  Escort sent:      "+escortSent    );
    I.say("  Terms away given: "+termsAwayGiven);
    I.say("  Terms away okay:  "+termsAwayOkay );
    I.say("  Escort returned:  "+escortReturned);
    return false;
  }
  
}












