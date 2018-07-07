

package test;
import game.*;
import static game.GameConstants.*;
import static game.ActorBonds.*;
import static game.Base.*;
import static game.BaseCouncil.*;
import static content.GameContent.*;
import content.*;
import util.*;



public class TestDiplomacy extends LogicTest {
  
  
  public static void main(String args[]) {
    testDiplomacy(true);
  }
  
  
  static boolean testDiplomacy(boolean graphics) {
    LogicTest test = new TestDiplomacy();
    
    //
    //  Set up the structure of the world-
    
    World world = new World(ALL_GOODS);
    Base  baseC = new Base(world, world.addLocale(2, 2));
    Base  awayC = new Base(world, world.addLocale(2, 3));
    Base  neutC = new Base(world, world.addLocale(3, 2));
    Area  map   = AreaTerrain.generateTerrain(
      baseC, 32, 0, MEADOW, JUNGLE
    );
    world.assignTypes(
      ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES()
    );
    world.addBases(baseC, awayC, neutC);
    baseC.setName("Home City");
    awayC.setName("Away City");
    neutC.setName("Neutral City");
    awayC.council.setTypeAI(AI_OFF);
    neutC.council.setTypeAI(AI_OFF);
    
    World.setupRoute(baseC.locale, awayC.locale, 1, Type.MOVE_LAND);
    World.setupRoute(baseC.locale, neutC.locale, 1, Type.MOVE_LAND);
    
    world.settings.toggleFog     = false;
    world.settings.toggleMigrate = false;
    world.settings.toggleHunger  = false;
    world.settings.toggleFatigue = false;
    
    //
    //  Place local structures on the map-
    
    AreaPlanning.placeStructure(SHIELD_WALL, baseC, true, 7, 7, 12, 12);
    AreaPlanning.markDemolish(map, true, 8, 8, 10, 10);
    AreaPlanning.placeStructure(WALKWAY, baseC, true, 12, 20, 1, 12);
    
    Building gate = (Building) BLAST_DOOR.generate();
    gate.setFacing(TileConstants.N);
    gate.enterMap(map, 12, 18, 1, baseC);
    
    BuildingForGovern palace = (BuildingForGovern) BASTION.generate();
    BaseCouncil council = baseC.council;
    palace.enterMap(map, 10, 10, 1, baseC);
    
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
    
    //
    //  Set up trade dynamics-
    
    Good giftGood = PSALT;
    awayC.setInventory(PSALT, 10);
    palace.setNeedLevels(false, PSALT, 10);
    neutC.setTradeLevel(PSALT, 10, 0);
    
    //
    //  Begin the mission from the foreign base-
    
    Mission escort;
    escort = new MissionForContact(awayC);
    
    for (int n = 4; n-- > 0;) {
      Actor s = (Actor) Trooper.TROOPER.generate();
      escort.toggleRecruit(s, true);
    }
    Actor envoy = (Actor) Nobles.NOBLE.generate();
    escort.toggleEnvoy(envoy, true);
    Actor bride = (Actor) Nobles.CONSORT.generate();
    escort.toggleEnvoy(bride, true);
    
    for (Actor e : escort.recruits()) {
      e.assignBase(awayC);
    }
    
    escort.terms.assignTerms(Base.POSTURE.ALLY, null, bride, null);
    escort.setWorldFocus(baseC);
    escort.beginMission(awayC);
    
    //
    //  Begin the simulation-
    
    boolean escortArrived  = false;
    boolean giftGiven      = false;
    boolean offerGiven     = false;
    boolean offerAccepted  = false;
    boolean termsOkay      = false;
    boolean escortDeparted = false;
    boolean escortSent     = false;
    boolean giftAwayGiven  = false;
    boolean termsAwayGiven = false;
    boolean termsAwayOkay  = false;
    boolean escortReturned = false;
    boolean testOkay       = false;
    
    while (map.time() < 1000 || graphics) {
      test.runLoop(baseC, 1, graphics, "saves/test_diplomacy.tlt");
      
      if (! escortArrived) {
        escortArrived = escort.localMap() == map;
      }
      
      if (escortArrived && ! giftGiven) {
        giftGiven = palace.inventory(giftGood) > 0;
      }
      
      if (escortArrived && ! offerGiven) {
        offerGiven = council.petitions().includes(escort);
      }
      
      if (giftGiven && offerGiven && ! offerAccepted) {
        council.acceptTerms(escort);
        offerAccepted = true;
      }
      
      if (offerAccepted && ! termsOkay) {
        boolean termsFilled = true;
        termsFilled &= monarch.bonds.hasBondType(bride, BOND_MARRIED);
        termsFilled &= baseC.isAllyOf(awayC);
        termsOkay = termsFilled;
      }
      
      if (termsOkay && ! escortDeparted) {
        escortDeparted = escort.localMap() != map;
      }
      
      if (escortDeparted && ! escortSent) {
        escort = new MissionForContact(baseC);
        escort.terms.assignTerms(POSTURE.TRADING, null, null, null);
        for (Actor w : garrison.workers()) escort.toggleRecruit(w, true);
        escort.toggleEnvoy(minister, true);
        escort.setWorldFocus(neutC);
        escort.beginMission(baseC);
        escortSent = true;
      }
      
      if (escortSent && ! giftAwayGiven) {
        boolean gotGift = false;
        for (Actor a : neutC.council.members()) {
          if (a.outfit.carried(giftGood) > 0) gotGift = true;
        }
        giftAwayGiven = gotGift;
      }
      
      if (escortSent && ! termsAwayGiven) {
        termsAwayGiven = neutC.council.petitions().includes(escort);
      }
      
      if (giftAwayGiven && termsAwayGiven && ! termsAwayOkay) {
        neutC.council.acceptTerms(escort);
        termsAwayOkay = true;
      }
      
      if (termsAwayOkay && ! escortReturned) {
        boolean allBack = true;
        for (Actor a : escort.recruits()) if (a.map() != map) allBack = false;
        for (Actor a : escort.envoys  ()) if (a.map() != map) allBack = false;
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
    I.say("  Gift given:       "+giftGiven     );
    I.say("  Offer given:      "+offerGiven    );
    I.say("  Offer accepted:   "+offerAccepted );
    I.say("  Terms okay:       "+termsOkay     );
    I.say("  Escort departed:  "+escortDeparted);
    I.say("  Escort sent:      "+escortSent    );
    I.say("  Gift away given:  "+giftAwayGiven );
    I.say("  Terms away given: "+termsAwayGiven);
    I.say("  Terms away okay:  "+termsAwayOkay );
    I.say("  Escort returned:  "+escortReturned);
    return false;
  }
  
}












