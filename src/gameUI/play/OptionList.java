/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import gameUI.misc.*;
import util.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;




public class OptionList extends UIGroup implements UIConstants {
  
  
  final static ImageAsset
    STRIKE_BUTTON_IMG = ImageAsset.fromImage(
      OptionList.class, "strike_btn_img",
      "media/GUI/Missions/button_strike.png"
    ),
    RECON_BUTTON_IMG = ImageAsset.fromImage(
      OptionList.class, "recon_btn_img",
      "media/GUI/Missions/button_recon.png"
    );
  
  
  final PlayUI BUI;
  //final Element subject;
  
  
  //  TODO:  Just have this auto-renew automagically, rather than being
  //  dependent on a particular subject.
  
  public OptionList(PlayUI UI, Element subject) {
    super(UI);
    this.BUI = UI;
    //this.subject = subject;
    this.relAlpha = 0;
    setup();
  }
  
  
  protected String info() {
    return "Options";
    //return "Options for "+subject;
  }
  
  
  private class MissionButton extends Button {
    
    final City base;
    final Mission mission;
    
    
    MissionButton(PlayUI UI, String ID, ImageAsset button, String info, Mission m) {
      super(UI, ID, button, Button.CIRCLE_LIT, info);
      this.base = UI.base;
      this.mission = m;
    }
    
    protected void whenClicked() {
      final PlayTask task = new PlayTask() {
        public void doTask(PlayUI UI) {
          
          Selection.Focus hovered = UI.selection.hovered();
          Mission match = base.matchingMission(mission.objective, hovered);
          
          if (match != null && UI.mouseClicked()) {
            PlayUI.pushSelection(match);
          }
          else if (mission.allowsFocus(hovered)) {
            if (UI.mouseClicked()) {
              mission.setFocus(hovered, 0, base.activeMap());
              mission.setAsBounty(0);
              PlayUI.pushSelection(mission);
            }
            else {
              //mission.renderFlag(rendering);
            }
          }
          
          if (KeyInput.wasTyped(Keys.ESCAPE)) {
            UI.assignTask(null);
          }
        }
      };
      BUI.assignTask(task);
    }
  }
  
  
  //  TODO:  Restore this!
  
  /*
  private class PowerButton extends Button {
    final Technique power;
    final Base base;
    final Element subject;
    
    PowerButton(PlayUI UI, Technique power, Base base, Element subject) {
      super(
        UI, power.uniqueID()+"_button",
        power.icon, Button.CIRCLE_LIT, power.info
      );
      this.power   = power  ;
      this.base    = base   ;
      this.subject = subject;
    }
    
    
    protected void updateState() {
      this.enabled = base.credits() >= power.costCash;
      super.updateState();
    }
    
    
    protected void whenClicked() {
      power.applyAsPower(base, subject);
    }
  }
  //*/
  
  
  private void setup() {
    
    final CityMap stage = BUI.stage;
    final City    base  = BUI.base ;
    final List <UINode> options = new List();
    
    options.add(new MissionButton(
      BUI, STRIKE_BUTTON_ID, STRIKE_BUTTON_IMG,
      "Destroy or raze subject",
      new Mission(Mission.OBJECTIVE_CONQUER, base, false)
    ));
    
    options.add(new MissionButton(
      BUI, RECON_BUTTON_ID, RECON_BUTTON_IMG,
      "Explore area",
      new Mission(Mission.OBJECTIVE_RECON, base, false)
    ));
    
    /*
    for (Venue v : stage.allVenues()) if (v instanceof Technique.Source) {
      Technique.Source source = (Technique.Source) v;
      for (Technique t : source.techniquesAvailable()) {
        if (! t.canUsePower(base, subject)) continue;
        options.add(new PowerButton(BUI, t, base, subject));
      }
    }
    
    //*/
    
    final int sizeB = OPT_BUTTON_SIZE, spaceB = sizeB + OPT_MARGIN;
    int sumWide = options.size() * spaceB, across = 0;
    for (UINode option : options) {
      option.alignToArea(across - (sumWide / 2), 0, sizeB, sizeB);
      option.attachTo(this);
      across += spaceB;
    }
  }
  
  
  protected void updateState() {
    this.alignBottom(0, 0);
    this.alignHorizontal(0.5f, 0, 0);
    
    final float fadeInc = 1f / (DEFAULT_FADE_TIME * UI.rendering.frameRate());
    if (fadeout) {
      this.relAlpha -= fadeInc;
      if (relAlpha <= 0) detach();
    }
    else {
      this.relAlpha += fadeInc;
      if (relAlpha > 1) relAlpha = 1;
    }
    super.updateState();
  }
  
  
  protected UINode selectionAt(Vector2 mousePos) {
    if (fadeout || relAlpha < 1) return null;
    return super.selectionAt(mousePos);
  }
}








