

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class MissionPane extends DetailPane {
  
  
  final Formation mission;
  
  
  public MissionPane(HUD UI, Formation subject) {
    super(UI, subject);
    this.mission = subject;
  }
  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    //  TODO:  RESTORE THIS!
    /*
    mission.describeMission(d);
    d.append("\n");
    
    final int credits = mission.baseReward();
    d.append("\nReward: "+credits+" credits");
    
    d.append(" ");
    if (mission.canIncreaseReward(100)) {
      d.append(new Description.Link("MORE") {
        public void whenClicked(Object context) {
          mission.incBaseReward(100);
        }
      });
    }
    else Text.appendColour("MORE", Colour.LITE_GREY, d);
    
    final Series <Actor> applied = mission.applicants();
    d.append("\n\nApplied: ");
    for (Actor a : applied) {
      d.appendAll("\n  ", a);
    }
    //*/
    
    super.updateState();
  }
  
  
  
}







