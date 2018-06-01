

package game;
import static game.GameConstants.*;



public class WorldCalendar {
  
  
  final static int
    STATE_LIGHT     = 0,
    STATE_DARKNESS  = 1,
    STATE_GREY_DAYS = 2
  ;
  
  final World world;
  
  
  WorldCalendar(World world) {
    this.world = world;
  }
  
  
  void loadState(Session s) throws Exception {
    return;
  }
  
  
  void saveState(Session s) throws Exception {
    return;
  }
  
  
  
  public int currentDay() {
    return world.time % DAY_LENGTH;
  }
  
  
  public float dayProgress() {
    return (world.time % DAY_LENGTH) * 1f / DAY_LENGTH;
  }
  
  
  public boolean isDay() {
    float prog = dayProgress();
    return prog >= 0.25f && prog < 0.75f;
  }
  
  
  public boolean isNight() {
    return ! isDay();
  }
}