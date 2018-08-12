

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
    FACTION_SETTLERS_A = new Faction(
      GameWorld.class, "faction_settlers_A" , "Settlers C",
      GameContent.RULER_BUILT
    ),
    FACTION_SETTLERS_B = new Faction(
      GameWorld.class, "faction_settlers_B" , "Settlers B",
      GameContent.RULER_BUILT
    ),
    FACTION_SETTLERS_C = new Faction(
      GameWorld.class, "faction_settlers_C" , "Settlers A",
      GameContent.RULER_BUILT
    ),
    FACTION_ANIMALS   = new Faction(
      GameWorld.class, "faction_animals"  , "Animals"
    ),
    FACTION_ARTILECTS = new Faction(
      GameWorld.class, "faction_artilects", "Artilects"
    )
  ;
  
  /*
    PLANET_ASRA_NOVI = new Sector(
      Verse.class, "Asra Novi", WORLDS_DIR+"asra_novi.png", FACTION_SUHAIL,
      "Asra Novi is a heavily-terraformed 'desert oasis' world noted for its "+
      "expertise in ecology and botanical science, together with polyamorous "+
      "traditions and luxury exports.",
  
    PLANET_PAREM_V = new Sector(
      Verse.class, "Parem V", WORLDS_DIR+"parem_v.png", FACTION_PROCYON,
      "Parem V was one of the first-settled systems in the known quadrant, "+
      "and though dour and repressive, remains host to numerous machine-"+
      "cults and revered arcane relics.",
    
    PLANET_HALIBAN = new Sector(
      Verse.class, "Haliban", WORLDS_DIR+"haliban.png", FACTION_ALTAIR,
      "Noted for it's spartan regimen and stern justice, Haliban's early "+
      "defection to the Calivor Republic have earned it several foes- and a "+
      "crucial role in quadrant defence strategy.",
    
    PLANET_AXIS_NOVENA = new Sector(
      Verse.class, "Axis Novena", WORLDS_DIR+"axis_novena.png", FACTION_TAYGETA,
      "Aided by it's low gravity and thin atmosphere, Axis Novena became the "+
      "centre of a large shipping industry and trade network- along with "+
      "rampant smuggling and black-market tech research.",
  //*/
  
  
  /**  Default geography:
    */
  public static World setupDefaultWorld() {
    
    World world = new World(ALL_GOODS);
    world.assignTypes(
      ALL_BUILDINGS,
      ALL_SHIPS(),
      ALL_CITIZENS(),
      ALL_SOLDIERS(),
      ALL_NOBLES()
    );
    world.assignMedia(
      World.KEY_ATTACK_FLAG , FLAG_STRIKE ,
      World.KEY_EXPLORE_FLAG, FLAG_RECON  ,
      World.KEY_DEFEND_FLAG , FLAG_SECURE ,
      World.KEY_CONTACT_FLAG, FLAG_CONTACT
    );
    
    world.setPlayerFaction(FACTION_SETTLERS_A);
    
    //  TODO:  Replace these with the named homeworlds!
    
    WorldLocale home = world.addLocale(1, 1, "Homeworld", true);
    Base homeBase = new Base(world, home, FACTION_SETTLERS_A);
    
    homeBase.setName("Homeworld Base");
    homeBase.trading.setTradeLevel(PARTS   , 0, 5 );
    homeBase.trading.setTradeLevel(MEDICINE, 0, 10);
    
    //cityA.initTradeLevels(
    //  PARTS   , 5f ,
    //  MEDICINE, 10f
    //);
    
    homeBase.initBuildLevels(
      TROOPER_LODGE, 2f ,
      HOLDING      , 10f
    );
    world.addBases(homeBase);
    
    
    WorldLocale sectorA = world.addLocale(5, 5, "Elysium Sector", false);
    World.setupRoute(home, sectorA, AVG_CITY_DIST / 2, MOVE_AIR);
    
    
    world.addScenario(new ScenarioSectorA(world, sectorA));
    
    return world;
  }
  
}






