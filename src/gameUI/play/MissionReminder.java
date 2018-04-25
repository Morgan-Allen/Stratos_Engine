/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import graphics.widgets.*;
import game.*;
import util.*;



//  TODO:  List a red-green-amber indicator for approval status?
public class MissionReminder extends ReminderListing.Entry {
  
  
  final Mission m;
  private Batch <Actor> applied = new Batch <Actor> ();
  
  final BorderedLabel label;
  final List <Image> appImgs = new List <Image> ();
  
  
  MissionReminder(final PlayUI BUI, final Mission m) {
    super(BUI, m, 40, 40);
    this.m = m;
    
    final Composite portrait = m.portrait();
    final Button button = new Button(
      BUI, m.fullName(),
      portrait.texture(), Button.CIRCLE_LIT.asTexture(), m.fullName()
    ) {
      protected void whenClicked() {
        PlayUI.pushSelection(m);
      }
    };
    button.alignVertical  (0, 0);
    button.alignHorizontal(0, 0);
    button.attachTo(this);
    
    label = new BorderedLabel(BUI);
    label.alignLeft  ( 0 , 0);
    label.alignBottom(-DEFAULT_MARGIN, 0);
    label.text.scale = SMALL_FONT_SIZE;
    label.setMessage(m.fullName(), false, 0);
    label.attachTo(this);
  }
  
  
  private void updateApplicantsShown() {
    //
    //  TODO:  Restore these functions!
    /*
    final PlayUI BUI = (PlayUI) UI;
    
    for (Image i : appImgs) i.detach();
    appImgs.clear();
    
    int n = 0; for (final Actor a : applied) {
      final Composite AP = a.portrait(BUI);
      if (AP == null) continue;
      final Image AI = new Image(BUI, AP.texture()) {
        protected String info() { return a.fullName(); }
      };
      AI.blocksSelect = true;
      final int size = MIN_WIDGET_SIZE;
      AI.alignTop(0, size);
      AI.alignRight(++n * -size, size);
      appImgs.add(AI);
      AI.attachTo(this);
    }
    //*/
  }
  
  
  protected void updateState() {
    
    /*
    if (! m.recruits().contentsMatch(applied)) {
      applied.clear();
      Visit.appendTo(applied, m.recruits());
      updateApplicantsShown();
    }
    //*/
    
    label.text.setText("");
    label.text.append(m.toString());
    //m.describeMission(label.text);
    label.setToFitText(false, 0);
    
    super.updateState();
  }
}










