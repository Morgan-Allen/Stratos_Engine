


package test;
import game.*;
import static game.GameConstants.*;
import static content.GameContent.*;



public class TestBlankMap extends Test {
  
  
  final static BuildType BUILD_MENUS[][] = {
    MILITARY_BUILDINGS   ,
    SCIENCE_BUILDINGS    ,
    ECONOMIC_BUILDINGS   ,
    RESIDENTIAL_BUILDINGS,
    PSI_SCHOOL_BUILDINGS ,
    RESOURCE_BUILDINGS   ,
  };
  final static String BUILD_MENU_NAMES[] = {
    "Military"   , "Science"   , "Economic" ,
    "Residential", "Psi School", "Resource" 
  };
  
  
  public static void main(String args[]) {
    String filename = "saves/blank_map.tlt";
    
    Test test = new TestBlankMap();
    test.attachBuildMenu(BUILD_MENUS, BUILD_MENU_NAMES);
    
    Base base = loadCity(null, filename);
    if (base == null) base = setupTestCity(32, ALL_GOODS, true, MEADOW, JUNGLE);
    
    while (true) {
      test.runLoop(base, 10, true, filename);
    }
  }
  
}