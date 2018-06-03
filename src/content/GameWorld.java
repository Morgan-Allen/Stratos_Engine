

package content;
import static game.GameConstants.*;
import static game.Type.*;
import game.*;
import static content.GameContent.*;
import util.*;




public class GameWorld {
  
  //
  //  TODO:  You need to assign default technologies here, and possibly other
  //  bonuses.
  final public static Faction
    FACTION_SETTLERS  = new Faction(
      GameWorld.class, "faction_settlers" , "Settlers",
      GameContent.RULER_BUILT
    ),
    FACTION_ANIMALS   = new Faction(
      GameWorld.class, "faction_animals"  , "Animals"
    ),
    FACTION_ARTILECTS = new Faction(
      GameWorld.class, "faction_artilects", "Artilects"
    )
  ;
  
  
  /**  Default geography:
    */
  public static World setupDefaultWorld() {
    World world = new World(ALL_GOODS);
    world.assignTypes(ALL_BUILDINGS, ALL_SHIPS(), ALL_CITIZENS(), ALL_SOLDIERS(), ALL_NOBLES());
    
    WorldLocale home = world.addLocale(1, 1, "Homeworld");
    Base homeBase = new Base(world, home);
    
    homeBase.setName("Homeworld Base");
    homeBase.setTradeLevel(PARTS   , 0, 5 );
    homeBase.setTradeLevel(MEDICINE, 0, 10);
    //cityA.initTradeLevels(
    //  PARTS   , 5f ,
    //  MEDICINE, 10f
    //);
    homeBase.initBuildLevels(
      TROOPER_LODGE, 2f ,
      HOLDING      , 10f
    );
    world.addBases(homeBase);
    
    
    WorldLocale sectorA = world.addLocale(5, 5, "Sector A");
    World.setupRoute(home, sectorA, AVG_CITY_DIST / 2, MOVE_LAND);
    
    
    world.addScenario(new ScenarioSectorA(world, sectorA));
    
    return world;
  }
  
}







