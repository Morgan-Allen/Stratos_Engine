


package test;
import content.Vassals;
import game.*;
import game.Task.JOB;



public class ViewDropshipFX extends ViewTest {
  
  
  public static void main(String args[]) {
    beginRenderTest(new ViewDropshipFX(), 16, "view_dropship_fx.rbl");
  }
  
  
  ActorAsVessel ship;
  
  
  protected void configScenario(World world, Area map, Base base) {
    
    world.settings.toggleFog = false;
    
    ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
    ship.enterMap(map, 4, 4, 1, base);
    ship.setLandPoint(map.tileAt(12, 12));
    
    Task landing = ship.targetTask(ship.landsAt(), 10, JOB.VISITING, ship);
    ship.assignTask(landing, ship);
    
    playUI().setLookPoint(map.tileAt(8, 8));
  }
  
  
  public void updateScenario() {
    super.updateScenario();
  }
}







