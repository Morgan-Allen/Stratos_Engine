


package test;
import content.Vassals;
import game.*;
import game.Task.JOB;
import util.I;
import util.Rand;



public class ViewDropshipFX extends ViewTest {
  
  
  public static void main(String args[]) {
    beginRenderTest(new ViewDropshipFX(), 16, "view_dropship_fx.rbl");
  }
  
  
  ActorAsVessel ship;
  AreaTile visitPoints[];
  int index = 1;
  
  
  protected void configScenario(World world, Area map, Base base) {
    
    world.settings.toggleFog = false;
    
    visitPoints = new AreaTile[] {
      map.tileAt(0 , 0 ), map.tileAt(12, 0 ),
      map.tileAt(12, 12), map.tileAt(0 , 12)
    };
    
    ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
    ship.enterMap(map, 0, 0, 1, base);
    ship.setLandPoint(visitPoints[index++]);
    
    ///I.say("Assigning initial land point: "+ship.landsAt());
    
    Task landing = ship.targetTask(ship.landsAt(), 2, JOB.VISITING, ship);
    ship.assignTask(landing, ship);
    
    //playUI().setLookPoint(map.tileAt(8, 8));
  }
  
  
  public void updateScenario() {
    super.updateScenario();
    
    if (ship.task() == null && ! ship.landed()) {
      AreaTile goes = visitPoints[index++];
      index = index % visitPoints.length;
      ship.setLandPoint(goes);
      ///I.say("Assigning new land point: "+goes);
      
      Task landing = ship.targetTask(ship.landsAt(), 2, JOB.VISITING, ship);
      ship.assignTask(landing, ship);
    }
  }
}










