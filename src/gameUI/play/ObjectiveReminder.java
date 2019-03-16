/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import gameUI.misc.*;
import graphics.widgets.*;
import start.*;
import game.*;
import static game.WorldScenario.*;
import util.*;



public class ObjectiveReminder extends ReminderListing.Entry {
  
  
  final Objective m;
  final BorderedLabel label;
  
  
  ObjectiveReminder(final PlayUI BUI, final Objective m) {
    super(BUI, m, 40, 20);
    this.m = m;
    
    label = new BorderedLabel(BUI);
    label.alignLeft(0, 0);
    label.alignVertical(DEFAULT_MARGIN, DEFAULT_MARGIN);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage("<objective>", false, 0);
    label.attachTo(this);
  }
  
  
  protected void updateState() {
    
    label.text.setText("");
    label.text.append("OBJECTIVE: "+m.description());
    label.setToFitText(false, 0);
    
    super.updateState();
  }
}




