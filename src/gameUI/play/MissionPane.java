

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class MissionPane extends DetailPane {
  
  
  final Mission mission;
  
  
  public MissionPane(HUD UI, Mission subject) {
    super(UI, subject);
    this.mission = subject;
  }
  
  
  protected void updateState() {
    
    City base = mission.homeCity();
    
    this.text.setText("");
    final Description d = this.text;
    
    d.append(mission.fullName());
    
    int credits = mission.cashReward();
    d.append("\nReward: "+credits+" credits");
    
    d.append(" ");
    if (base.funds() >= 100) {
      d.append(new Description.Link("MORE") {
        public void whenClicked(Object context) {
          mission.incReward(100);
        }
      });
    }
    else Text.appendColour("MORE", Colour.LITE_GREY, d);
    
    final Series <Actor> applied = mission.recruits();
    d.append("\n\nApplied: ");
    for (Actor a : applied) {
      d.appendAll("\n  ", a);
    }
    
    super.updateState();
  }
  
  
  
}







