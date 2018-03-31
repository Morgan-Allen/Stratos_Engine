


package content;
import game.*;
import static game.GameConstants.*;
import static game.ActorTechnique.*;
import graphics.common.*;
import util.*;



public class StockExGoods {
  
  
  final public static int
    MEDIKIT_HEAL_AMOUNT = 10
  ;
  
  
  final public static ActorTechnique MEDIKIT_HEAL = new ActorTechnique(
    "tech_medikit_heal", "Medikit"
  ) {
    
    public boolean canUseActive(Actor using, Target subject) {
      if (! super.canUseActive(using, subject)) return false;
      return using.health.injury() > (using.health.maxHealth() / 2);
    }
    
    public void applyCommonEffects(Target subject, Base ruler, Actor actor) {
      final Actor healed = (Actor) subject;
      healed.health.liftDamage(MEDIKIT_HEAL_AMOUNT);
      healed.outfit.incCarried(MEDIKIT, -1);
    }
  };
  static {
    MEDIKIT_HEAL.attachMedia(
      null, null,
      "Heals the subject for up to "+MEDIKIT_HEAL_AMOUNT+" damage.",
      AnimNames.LOOK
    );
    MEDIKIT_HEAL.setProperties(TARGET_SELF, Task.FULL_HELP, MEDIUM_POWER);
    MEDIKIT_HEAL.setCosting(0, MINOR_AP_COST, NO_TIRING, NO_RANGE);
    MEDIKIT_HEAL.setMinLevel(1);
  }
  
  
  final public static Good MEDIKIT = new Good("Medikit", 25);
  static {
    MEDIKIT.setUsable(2, MEDIKIT_HEAL);
    //  TODO:  Assign default recipes for each Good!  Buildings can have
    //  variants as and when required...
  }
  
  final public static Good SHIELD_BAND = new Good("Shield Band", 125);
  
  final public static Good COMM_RELAY = new Good("Comm Relay", 350);
  
  
  final static Good ALL_SOLD[] = { MEDIKIT };
  
  
}










