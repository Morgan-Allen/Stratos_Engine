

package test;
import game.*;
import content.*;
import util.*;
import static game.ActorAsPerson.*;
import static game.Base.*;
import static game.CityCouncil.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class TestDiplomacy extends LogicTest {
  
  
  public static void main(String args[]) {
    testDiplomacy(true);
  }
  
  
  static boolean testDiplomacy(boolean graphics) {
    LogicTest test = new TestDiplomacy();
    
    World   world = new World(ALL_GOODS);
    Base    baseC = new Base(world, world.addLocale(2, 2));
    Base    awayC = new Base(world, world.addLocale(2, 3));
    Base    neutC = new Base(world, world.addLocale(3, 2));
    AreaMap map   = CityMapTerrain.generateTerrain(
      baseC, 32, 0, MEADOW, JUNGLE
    );
    world.assignTypes(ALL_BUILDINGS, ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    world.addBases(baseC, awayC, neutC);
    baseC.setName("Home City");
    awayC.setName("Away City");
    neutC.setName("Neutral City");
    awayC.council.setTypeAI(AI_OFF);
    neutC.council.setTypeAI(AI_OFF);
    world.settings.toggleFog     = false;
    world.settings.toggleMigrate = false;
    
    
    CityMapPlanning.placeStructure(SHIELD_WALL, baseC, true, 7, 7, 12, 12);
    CityMapPlanning.markDemolish(map, true, 8, 8, 10, 10);
    
    Building gate = (Building) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 12, 17, 1, baseC);
    
    Building palace = (Building) BASTION.generate();
    CityCouncil council = baseC.council;
    palace.enterMap(map, 10, 10, 1, baseC);
    CityMapPlanning.placeStructure(WALKWAY, baseC, true, 12, 19, 1, 13);
    
    ActorAsPerson monarch = (ActorAsPerson) Nobles.NOBLE.generate();
    council.toggleMember(monarch, Role.MONARCH, true);
    palace.setResident(monarch, true);
    monarch.enterMap(map, 12, 9, 1, baseC);
    
    ActorAsPerson minister = (ActorAsPerson) Nobles.NOBLE.generate();
    council.toggleMember(minister, Role.PRIME_MINISTER, true);
    palace.setResident(minister, true);
    minister.enterMap(map, 12, 9, 1, baseC);
    
    Building garrison = (Building) TROOPER_LODGE.generate();
    garrison.enterMap(map, 12, 1, 1, baseC);
    
    ActorUtils.fillAllWorkVacancies(map);
    
    
    Mission escort;
    escort = new Mission(Mission.OBJECTIVE_DIALOG, awayC, true);
    for (int n = 4; n-- > 0;) {
      Actor s = (Actor) Trooper.TROOPER.generate();
      s.assignBase(awayC);
      escort.toggleRecruit(s, true);
    }
    
    Actor envoy = (Actor) Nobles.NOBLE.generate();
    escort.toggleEscorted(envoy, true);
    Actor bride = (Actor) Nobles.CONSORT.generate();
    escort.toggleEscorted(bride, true);
    
    for (Actor e : escort.escorted()) e.assignBase(awayC);
    
    escort.assignTerms(Base.POSTURE.ALLY, null, bride, null);
    escort.setFocus(baseC);
    
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
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(baseC, 1, graphics, "saves/test_diplomacy.tlt");
      
      if (! escortArrived) {
        escortArrived = escort.onMap(map);
      }
      
      if (escortArrived && ! offerGiven) {
        offerGiven = council.petitions().includes(escort);
      }
      
      if (offerGiven && ! offerAccepted) {
        council.acceptTerms(escort);
        offerAccepted = true;
      }
      
      if (offerAccepted && ! termsOkay) {
        boolean termsFilled = true;
        termsFilled &= monarch.hasBondType(bride, BOND_MARRIED);
        termsFilled &= baseC.isAllyOf(awayC);
        termsOkay = termsFilled;
      }
      
      if (termsOkay && ! escortDeparted) {
        escortDeparted = ! escort.onMap();
      }
      
      if (escortDeparted && ! escortSent) {
        escort = new Mission(Mission.OBJECTIVE_DIALOG, baseC, true);
        escort.assignTerms(POSTURE.TRADING, null, null, null);
        for (Actor w : garrison.workers()) escort.toggleRecruit(w, true);
        escort.toggleEscorted(minister, true);
        escort.setFocus(neutC);
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
        for (Actor a : escort.recruits()) if (a.map() != map) allBack = false;
        for (Actor a : escort.escorted()) if (a.map() != map) allBack = false;
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












