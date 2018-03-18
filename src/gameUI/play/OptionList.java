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
import static game.GameConstants.*;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Texture;




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
  List <UINode> options = new List();
  
  
  //  TODO:  Just have this auto-renew automagically, rather than being
  //  dependent on a particular subject.
  
  public OptionList(PlayUI UI) {
    super(UI);
    this.BUI = UI;
    this.relAlpha = 0;
  }
  
  
  protected String info() {
    return "Options";
    //return "Options for "+subject;
  }
  
  
  protected void setupFrom(Area area, Base base) {
    
    for (UINode option : options) {
      option.detach();
    }
    
    options.add(new MissionButton(
      BUI, STRIKE_BUTTON_ID, STRIKE_BUTTON_IMG,
      "Destroy or raze subject"
    ) {
      Mission initMission() {
        Mission m = new Mission(Mission.OBJECTIVE_CONQUER, base, false);
        return m;
      }
    });
    
    options.add(new MissionButton(
      BUI, RECON_BUTTON_ID, RECON_BUTTON_IMG,
      "Explore area"
    ) {
      Mission initMission() {
        Mission m = new Mission(Mission.OBJECTIVE_RECON, base, false);
        return m;
      }
    });
    
    for (ActorTechnique t : base.rulerPowers()) {
      options.add(new PowerButton(BUI, t, base));
    }
    
    final int sizeB = OPT_BUTTON_SIZE, spaceB = sizeB + OPT_MARGIN;
    int sumWide = options.size() * spaceB, across = 0;
    for (UINode option : options) {
      option.alignToArea(across - (sumWide / 2), 0, sizeB, sizeB);
      option.attachTo(this);
      across += spaceB;
    }
  }
  
  
  
  private abstract class MissionButton extends Button {
    
    final Base base;
    private Mission mission = null;
    
    
    MissionButton(PlayUI UI, String ID, ImageAsset button, String info) {
      super(UI, ID, button, Button.CIRCLE_LIT, info);
      this.base = UI.base;
    }
    
    abstract Mission initMission();
    
    protected void whenClicked() {
      final PlayTask task = new PlayTask() {
        public void doTask(PlayUI UI) {
          if (mission == null) mission = initMission();
          
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
              mission = null;
              UI.assignTask(null);
            }
            else {
              //mission.renderFlag(rendering);
            }
          }
          
          if (KeyInput.wasTyped(Keys.ESCAPE)) {
            UI.assignTask(null);
          }
        }

        public Texture cursor() {
          return texture;
        }
      };
      BUI.assignTask(task);
    }
  }
  
  
  private class PowerButton extends Button {
    
    final ActorTechnique power;
    final Base base;
    
    
    PowerButton(PlayUI UI, ActorTechnique power, Base base) {
      super(
        UI, power.uniqueID()+"_button",
        power.icon, Button.CIRCLE_LIT, power.info
      );
      this.power = power;
      this.base  = base ;
    }
    
    
    protected void updateState() {
      this.enabled = base.funds() >= power.costCash;
      super.updateState();
    }
    
    
    protected void whenClicked() {
      final PlayTask task = new PlayTask() {
        public void doTask(PlayUI UI) {
          Target hovered = (Target) UI.selection.hovered();
          if (power.canUsePower(base, hovered)) {
            if (UI.mouseClicked()) {
              power.applyFromRuler(base, hovered);
            }
          }
          else {
            //  TODO:  Render a disabled icon!
          }
        }
        
        public Texture cursor() {
          return texture;
        }
      };
      BUI.assignTask(task);
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








