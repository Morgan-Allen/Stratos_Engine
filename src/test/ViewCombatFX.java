

package test;
import content.*;
import static content.GameContent.*;
import game.*;
import gameUI.play.*;
import start.PlayLoop;



//  TODO:  This may be a little too labour-intensive for rapid prototyping
//         during the asset-loading phase.  See if that can be slimmed down?



public class ViewCombatFX extends ViewTest {
  
  
  public static void main(String args[]) {
    beginRenderTest(new ViewCombatFX(), 32, "view_combat_fx.rbl");
  }
  
  
  
  Actor fights, enemy;
  
  
  
  protected void configScenario(World world, Area map, Base base) {
    
    world.settings.toggleInjury  = false;
    world.settings.toggleFatigue = false;
    world.settings.toggleFog     = false;
    
    
    fights = (Actor) Trooper.TROOPER.generate();
    enemy  = (Actor) TRIPOD.generate();
    
    fights.type().initAsMigrant((ActorAsPerson) fights);
    enemy .type().initAsAnimal ((ActorAsAnimal) enemy );
    
    fights.enterMap(map, 5, 5, 1, base);
    enemy.enterMap(map, 9, 9, 1, map.locals);
    
    TaskCombat combatF = TaskCombat.configCombat(fights, enemy);
    fights.assignTask(combatF, fights);
    
    TaskCombat combatE = TaskCombat.configCombat(enemy, fights);
    enemy.assignTask(combatE, fights);
    
    
    PlayUI.pushSelection(fights);
    playUI().tracking.zoomNow(fights.trackPosition());
  }
  
  
  public void updateScenario() {
    super.updateScenario();
  }
  
  
}





