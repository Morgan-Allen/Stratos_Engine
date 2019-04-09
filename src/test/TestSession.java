

package test;
import static content.GameContent.*;
import game.*;
import util.*;




public class TestSession extends LogicTest {
  
  
  public static void main(String s[]) {
    Actor saveSample = (Actor) ECOLOGIST.generate();
    testSession(saveSample);
  }
  
  
  public static boolean testSession(Session.Saveable saveSample) {
    try {
      Session.checkType = true;
      
      Session.saveSession("saves/test_save.tlt", saveSample);
      Session session = Session.loadSession("saves/test_save.tlt", true);
      Session.Saveable loadSample = session.loaded()[0];
      
      if (loadSample == null) I.say("FAILED TO SAVE/LOAD ITEM!");
      else I.say("Successfully saved/loaded: "+loadSample);
      
      Session.checkType = false;
      return true;
    }
    catch(Exception e) {
      I.report(e);
      return false;
    }
  }
  
}


