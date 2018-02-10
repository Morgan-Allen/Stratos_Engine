


package content;
import game.*;
import static game.GameConstants.*;
import static game.Technique.*;
import graphics.common.*;
import util.*;



public class StockExGoods {
  
  
  final public static int
    MEDIKIT_HEAL_AMOUNT = 10
  ;
  
  
  final public static Technique MEDIKIT_HEAL = new Technique(
    "tech_medikit_heal", "Medikit"
  ) {
    
    public boolean canUseActive(Actor using, Target subject) {
      if (! super.canUseActive(using, subject)) return false;
      return using == subject && using.injury() > (using.maxHealth() / 2);
    }
    
    public void applyCommonEffects(Target subject, City ruler, Actor actor) {
      final Actor healed = (Actor) subject;
      healed.liftDamage(MEDIKIT_HEAL_AMOUNT);
      healed.incCarried(MEDIKIT, -1);
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
    MEDIKIT.setUsable(MEDIKIT_HEAL);
  }
  
  final public static Good SHIELD_BAND = new Good("Shield Band", 125);
  
  final public static Good COMM_RELAY = new Good("Comm Relay", 350);
  
  
}










