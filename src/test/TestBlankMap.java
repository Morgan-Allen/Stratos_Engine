


package test;
import game.*;
import static game.GameConstants.*;
import static content.GameWorld.*;
import static content.GameContent.*;



public class TestBlankMap extends LogicTest {
  
  
  final static BuildType BUILD_MENUS[][] = {
    MILITARY_BUILDINGS  ,
    GUILD_BUILDINGS     ,
    COMMERCE_BUILDINGS  ,
    PSI_SCHOOL_BUILDINGS,
  };
  final static String BUILD_MENU_NAMES[] = {
    "Military", "Guilds", "Commerce", "Psi School"
  };
  
  
  public static void main(String args[]) {
    String filename = "saves/blank_map.tlt";
    
    LogicTest test = new TestBlankMap();
    test.attachBuildMenu(BUILD_MENUS, BUILD_MENU_NAMES);
    
    Base base = loadCity(null, filename);
    if (base == null) base = setupTestBase(
      FACTION_SETTLERS, ALL_GOODS, 32, true, MEADOW, JUNGLE
    );
    
    while (true) {
      test.runLoop(base, 10, true, filename);
    }
  }
  
}