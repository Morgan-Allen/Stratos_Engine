

package game;
import static game.GameConstants.*;



public class WorldCalendar {
  
  
  final static int
    STATE_LIGHT      = 0,
    STATE_DARKNESS   = 1,
    STATE_GREY_DAYS  = 2
  ;
  
  
  public static int dayState(int time) {
    int calDay = (time / DAY_LENGTH) % DAYS_PER_YEAR;
    int maxDay = DAYS_PER_MONTH * MONTHS_PER_YEAR;
    
    if (calDay > maxDay) return STATE_GREY_DAYS;
    
    int month = calDay / DAYS_PER_MONTH;
    return (month % 2) == 1 ? STATE_DARKNESS : STATE_LIGHT;
  }
  
  
}




