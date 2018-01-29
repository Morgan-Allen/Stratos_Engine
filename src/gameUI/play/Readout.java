/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class Readout extends UIGroup {
  
  
  final public static ImageAsset READOUT_FRAME = ImageAsset.fromImage(
    Readout.class, "readout_frame", "media/GUI/tips_frame.png"
  );
  
  
  final PlayUI UI;
  Bordering border;
  Text read;
  
  
  protected Readout(PlayUI UI) {
    super(UI);
    this.UI = UI;
    
    this.border = new Bordering(UI, READOUT_FRAME);
    border.left   = 2;
    border.right  = 2;
    border.bottom = 2;
    border.top    = 2;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.read = new Text(UI, DetailPane.DETAIL_FONT);
    read.alignToFill();
    read.scale = 0.75f;
    read.attachTo(this);
  }
  
  
  protected void updateState() {
    super.updateState();
    
    final City played = UI.base;
    read.setText("");
    //
    //  Credits first-
    final int credits = played.funds();
    read.append("    ");
    read.append(credits+" Credits", Colour.WHITE);
    
    /*
    //
    //  Then psy points-
    final boolean ruled = played.ruler() != null;
    final ActorHealth RH = ruled ? played.ruler().health : null;
    int psyPoints = 0, maxPsy = 0;
    if (played.ruler() != null) {
      maxPsy    += RH.maxHealth();
      psyPoints += maxPsy - RH.fatigue();
      read.append("   Psy Points: ");
      read.append(    I.lengthen(psyPoints, 2, true));
      read.append("/"+I.lengthen(maxPsy   , 2, true));
      read.append("   ");
    }
    
    //
    //  Then time and date-
    final String timeStamp = SaveUtils.timeStamp(world.currentTime());
    read.append(timeStamp);
    
    //
    //  Finally, include the set of provisions and their supply/demand:
    final BaseTransport p = played.transport;
    final Traded provs[] = { Economy.POWER, Economy.WATER };
    
    for (Traded type : provs) {
      read.append("  ("+type.name+": ");
      int supply = (int) p.allSupply(type);
      int demand = (int) p.allDemand(type);
      read.append(supply+"/"+demand+")");
    }
    //*/
  }
  
}





