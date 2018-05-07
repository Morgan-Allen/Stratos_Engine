

package game;
import static game.World.*;
import util.*;



//  TODO:  You should consider making this a Constant, just to be safe.

//  TODO:  Move the various Area-Config information from the .content package
//  in here...


public class WorldLocale {
  
  float mapX, mapY;
  Table <WorldLocale, Route> routes = new Table();
  
  String label;
  
  public float mapX() { return mapX; }
  public float mapY() { return mapY; }
  public String toString() { return label; }
}
