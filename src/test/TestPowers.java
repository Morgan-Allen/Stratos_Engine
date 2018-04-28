

package test;
import game.*;
import static game.GameConstants.*;
//import content.*;
import static content.GameContent.*;
import util.*;




public class TestPowers extends LogicTest {
  
  
  
  boolean powerTest(boolean graphics, String title, ActorTechnique technique) {
    
    Base base = LogicTest.setupTestBase(32, ALL_GOODS, false);
    base.setName("Client Base");
    Area map = base.activeMap();
    
    Building guild      = createGuild(map, base);
    Actor    caster     = guild.workers().first();
    Target   subject    = createSubject(map, guild);
    boolean  guildCasts = guildCasts();
    
    boolean castOkay = false;
    boolean testOkay = false;
    
    while (map.time() < 100 || graphics) {
      runLoop(base, 1, graphics, "saves/test_bounties.str");
      
      if (! castOkay) {
        castOkay = verifyEffect(subject, caster);
      }
      
      if (castOkay && ! testOkay) {
        testOkay = true;
        I.say("\n"+title+" TEST CONCLUDED SUCCESSFULLY!");
        if (! graphics) return true;
      }
    }
    
    I.say("\n"+title+" TEST FAILED!");
    return false;
  }
  
  
  Building createGuild(Area map, Base base) {
    return null;
  }
  
  
  Target createSubject(Area map, Building guild) {
    return null;
  }
  
  
  boolean guildCasts() {
    return false;
  }
  
  
  boolean verifyEffect(Target subject, Actor caster) {
    return false;
  }
  
}



