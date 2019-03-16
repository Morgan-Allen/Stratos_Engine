/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import game.*;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.widgets.*;
import gameUI.misc.*;
import util.*;

import com.badlogic.gdx.Input.Keys;
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
    ),
    ILLEGAL_ACTION_IMG = ImageAsset.fromImage(
      OptionList.class, "illegal_btn_img",
      "media/GUI/illegal_action.png"
    );
  
  
  final PlayUI BUI;
  List <UINode> options = new List();
  private float lastRefresh = -1;
  
  
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
  
  
  protected void setupFrom(AreaMap area, Base base) {
    
    for (UINode option : options) {
      option.detach();
    }
    options.clear();
    
    options.add(new MissionButton(
      BUI, STRIKE_BUTTON_ID, STRIKE_BUTTON_IMG,
      "Destroy or raze subject"
    ) {
      Mission initMission() {
        Mission m = new MissionForStrike(base);
        return m;
      }
    });
    
    options.add(new MissionButton(
      BUI, RECON_BUTTON_ID, RECON_BUTTON_IMG,
      "Explore area"
    ) {
      Mission initMission() {
        Mission m = new MissionForRecon(base);
        return m;
      }
    });
    
    for (ActorTechnique t : base.rulerPowers()) {
      final UIGroup option = new UIGroup(BUI);
      
      final Button button;
      button = new PowerButton(BUI, t, base);
      button.alignAcross(0, 1);
      button.alignDown  (0, 1);
      button.attachTo(option);
      
      final int cost = t.costCash;
      final BorderedLabel costLabel = new BorderedLabel(BUI);
      costLabel.attachTo(option);
      costLabel.alignHorizontal(0.5f, 0, 0);
      costLabel.alignBottom(0, 0);
      costLabel.text.scale = SMALL_FONT_SIZE;
      costLabel.setMessage("CR"+cost, false, 0.5f);
      
      options.add(option);
    }
    
    final int sizeB = OPT_BUTTON_SIZE, spaceB = sizeB + OPT_MARGIN;
    int sumWide = options.size() * spaceB, across = 0;
    for (UINode option : options) {
      option.alignToArea(across - (sumWide / 2), 10, sizeB, sizeB);
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
        boolean canUse = false;
        
        public void doTask(PlayUI UI) {
          if (mission == null) mission = initMission();
          
          Selection.Focus hovered = UI.selection.hovered();
          Mission match = base.matchingMission(mission.objective, hovered);
          canUse = false;
          
          if (match != null && UI.mouseClicked()) {
            canUse = true;
            PlayUI.pushSelection(match);
          }
          else if (canUse = mission.allowsFocus(hovered)) {
            if (UI.mouseClicked()) {
              mission.setLocalFocus((Target) hovered);
              mission.rewards.setAsBounty(0);
              mission.beginMission();
              PlayUI.pushSelection(mission);
              mission = null;
              UI.assignTask(null);
            }
          }
          
          if (KeyInput.wasTyped(Keys.ESCAPE)) {
            UI.assignTask(null);
          }
        }
        
        public ImageAsset cursor() {
          return canUse ? texture : ILLEGAL_ACTION_IMG;
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
        power.icon, Button.CIRCLE_LIT, power.name+": "+power.info
      );
      this.power = power;
      this.base  = base ;
    }
    
    protected void updateState() {
      this.enabled = true;
      enabled &= base.funds() >= power.costCash;
      //  TODO:  Require that the temple be staffed first...
      super.updateState();
    }
    
    protected void whenClicked() {
      final PlayTask task = new PlayTask() {
        boolean canUse = false;
        
        public void doTask(PlayUI UI) {
          Target hovered = (Target) UI.selection.hovered();
          canUse = power.canRulerUse(base, hovered);
          
          if (canUse && UI.mouseClicked()) {
            power.applyFromRuler(base, hovered);
          }
          
          if (KeyInput.wasTyped(Keys.ESCAPE)) {
            BUI.assignTask(null);
          }
        }
        
        public ImageAsset cursor() {
          return canUse ? texture : ILLEGAL_ACTION_IMG;
        }
      };
      BUI.assignTask(task);
    }
  }
  
  
  protected void updateState() {
    this.alignBottom(0, 0);
    this.alignHorizontal(0.5f, 0, 0);
    
    if (Rendering.activeTime() > lastRefresh + 1) {
      this.setupFrom(BUI.area, BUI.base);
      lastRefresh = Rendering.activeTime();
    }
    
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
  
  
  protected UINode selectionAt(Vec2D mousePos) {
    if (fadeout || relAlpha < 1) return null;
    return super.selectionAt(mousePos);
  }
}








