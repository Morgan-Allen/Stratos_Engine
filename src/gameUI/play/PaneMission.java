

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class PaneMission extends DetailPane {
  
  
  final Mission mission;
  
  
  public PaneMission(HUD UI, Mission subject) {
    super(UI, subject);
    this.mission = subject;
  }
  

  protected void updateText(Text text) {
    
    text.setText("");
    final Description d = text;
    
    Base base = mission.base();
    
    d.append(mission.fullName());
    
    Base declares = mission.base();
    d.append("\nDeclared by: "+declares);
    
    int credits = mission.rewards.cashReward();
    d.append("\nReward: "+credits+" credits");
    
    d.append(" ");
    if (base.funds() >= 100) {
      d.append(new Description.Link("MORE") {
        public void whenClicked(Object context) {
          mission.rewards.incReward(100);
        }
      });
    }
    else Text.appendColour("MORE", Colour.LITE_GREY, d);
    
    final Series <Actor> applied = mission.recruits();
    d.append("\n\nApplied: ");
    for (Actor a : applied) {
      d.appendAll("\n  ", a);
    }
    
    
    if (declares == PlayUI.playerBase()) {
      d.append("\n\n");
      d.append(new Description.Link("CANCEL") {
        public void whenClicked(Object context) {
          mission.disbandMission();
          PlayUI.pushSelection(null);
        }
      });
    }
  }
  
  
  
}







