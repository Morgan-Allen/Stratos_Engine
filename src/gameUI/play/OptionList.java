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
import com.badlogic.gdx.math.Vector2;




public class OptionList extends UIGroup implements UIConstants {
  
  final PlayUI BUI;
  final Element subject;
  
  
  public OptionList(PlayUI UI, Element subject) {
    super(UI);
    this.BUI = UI;
    this.subject = subject;
    this.relAlpha = 0;
    setup();
  }
  
  
  protected String info() {
    return "Options for "+subject;
  }
  
  
  //  TODO:  Restore these!
  
  /*
  private class MissionButton extends Button {
    final City base;
    final Mission mission;
    
    MissionButton(PlayUI UI, String ID, String info, Mission m) {
      super(
        UI, ID, m.buttonImage(), Button.CIRCLE_LIT, info
      );
      this.base = UI.base;
      this.mission = m;
    }
    
    protected void whenClicked() {
      //
      //  If an existing mission matches this one, just select that instead.
      final Mission match = base.matchingMission(mission);
      if (match != null) {
        PlayUI.pushSelection(match);
        return;
      }
      //
      //  Otherwise, create a new mission for the target.
      base.toggleMission(mission, true);
      PlayUI.pushSelection(mission);
    }
  }
  
  
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
    
    final CityMap stage   = BUI.stage;
    final City    base    = BUI.base ;
    final List <UINode> options = new List();
    
    /*
    final Mission strike = MissionStrike.strikeFor(subject, base);
    final Mission recon  = MissionRecon .reconFor (subject, base);
    
    if (strike != null) options.add(new MissionButton(
      BUI, STRIKE_BUTTON_ID, "Destroy or raze subject", strike
    ));
    if (recon != null) options.add(new MissionButton(
      BUI, STRIKE_BUTTON_ID, "Explore an area or follow subject", recon
    ));
    
    for (Venue v : stage.allVenues()) if (v instanceof Technique.Source) {
      Technique.Source source = (Technique.Source) v;
      for (Technique t : source.techniquesAvailable()) {
        if (! t.canUsePower(base, subject)) continue;
        options.add(new PowerButton(BUI, t, base, subject));
      }
    }
    
    final int sizeB = OPT_BUTTON_SIZE, spaceB = sizeB + OPT_MARGIN;
    int sumWide = options.size() * spaceB, across = 0;
    for (UINode option : options) {
      option.alignToArea(across - (sumWide / 2), 0, sizeB, sizeB);
      option.attachTo(this);
      across += spaceB;
    }
    //*/
    
    /*
    final PlayUI BUI   = (PlayUI) UI;
    final Base   base  = BUI.played();
    final Actor  ruler = base.ruler();
    final List <UINode> options = new List();
    
    final Mission strike  = MissionStrike  .strikeFor  (subject, base);
    final Mission recover = MissionRecover .recoveryFor(subject, base);
    final Mission secure  = MissionSecurity.securityFor(subject, base);
    final Mission recon   = MissionRecon   .reconFor   (subject, base);
    final Mission contact = MissionContact .contactFor (subject, base);
    final Mission claim   = MissionClaim   .claimFor   (subject, base);
    
    if (strike != null) options.add(new OptionButton(
      BUI, STRIKE_BUTTON_ID, Mission.STRIKE_ICON,
      "Destroy or raze subject", strike
    ));
    if (recover != null) options.add(new OptionButton(
      BUI, CLAIMING_BUTTON_ID, Mission.CLAIM_ICON,
      "Recover or capture subject", recover
    ));
    if (secure != null) options.add(new OptionButton(
      BUI, SECURITY_BUTTON_ID, Mission.SECURITY_ICON,
      "Secure or protect subject", secure
    ));
    if (recon != null) options.add(new OptionButton(
      BUI, RECON_BUTTON_ID, Mission.RECON_ICON,
      "Explore an area or follow subject", recon
    ));
    if (contact != null) options.add(new OptionButton(
      BUI, CONTACT_BUTTON_ID, Mission.CONTACT_ICON,
      "Contact or negotiate with subject", contact
    ));
    if (claim != null) options.add(new OptionButton(
      BUI, CLAIMING_BUTTON_ID, Mission.CLAIM_ICON,
      "Claim this sector for your own", claim
    ));
    
    final boolean canCast = subject instanceof Target && ruler != null;
    if (canCast) for (Power power : ruler.skills.knownPowers()) {
      final Target subject = (Target) this.subject;
      if (power.icon == null || ! power.appliesTo(ruler, subject)) continue;
      UIGroup option = PowersPane.createButtonGroup(BUI, power, ruler, subject);
      options.add(option);
    }
    //*/
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








