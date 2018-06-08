


package test;
import content.Vassals;
import game.*;
import game.Task.JOB;
import graphics.common.Rendering;
import util.*;



public class ViewDropshipFX extends ViewTest {
  
  
  public static void main(String args[]) {
    beginRenderTest(new ViewDropshipFX(), 16, "view_dropship_fx.rbl");
  }
  
  
  ActorAsVessel ship;
  int counter = 0;
  Vec3D oldPos = new Vec3D(), newPos = new Vec3D();
  
  
  protected void configScenario(World world, Area map, Base base) {
    
    world.settings.toggleFog = false;
    
    ship = (ActorAsVessel) Vassals.DROPSHIP.generate();
    ship.enterMap(map, 4, 4, 1, base);
    ship.setLandPoint(map.tileAt(12, 12));
    
    Task landing = ship.targetTask(ship.landsAt(), 10, JOB.VISITING, ship);
    ship.assignTask(landing, ship);
    
    oldPos = ship.exactPosition(oldPos);
    
    ///playUI().setLookPoint(map.tileAt(8, 8));
  }
  
  
  public void updateScenario() {
    super.updateScenario();
  }
  
  
  public void renderVisuals(Rendering rendering) {
    //ship.renderElement(rendering, base());
    super.renderVisuals(rendering);
  }
  
  
  
}















