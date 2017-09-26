

package game;
import static game.BuildingSet.*;



public class TestWorld extends TestLoop {
  
  
  public static void main(String args[]) {
    
    World   world = new World();
    City    cityA = new City(world);
    City    cityB = new City(world);
    CityMap map   = new CityMap();
    
    cityA.name = "Xochimilco";
    cityB.name = "Tlacopan"  ;
    world.cities.add(cityA);
    world.cities.add(cityB);
    cityA.setWorldCoords(1, 1);
    cityB.setWorldCoords(3, 3);
    City.setupRoute(cityA, cityB, 2);

    cityA.map = map;
    map.performSetup(10);
    map.attachCity(cityA);
    
    cityB.tradeLevel.set(COTTON    ,  50);
    cityB.tradeLevel.set(POTTERY   ,  50);
    cityB.tradeLevel.set(RAW_COTTON, -50);
    cityB.tradeLevel.set(CLAY      , -50);
    cityB.inventory .set(RAW_COTTON,  50);
    cityB.inventory .set(CLAY      ,  50);
    
    TradeBuilding post1 = (TradeBuilding) PORTER_HOUSE.generate();
    post1.enterMap(map, 1, 6);
    post1.ID = "(Gets Cotton)";
    post1.tradeLevel.set(RAW_COTTON,  20);
    post1.tradeLevel.set(CLAY      ,  20);
    post1.tradeLevel.set(COTTON    , -20);
    post1.tradeLevel.set(POTTERY   , -20);
    post1.inventory .set(CLAY      ,  20);
    post1.inventory .set(RAW_COTTON,  20);
    post1.tradePartner = cityB;
    Tile.applyPaving(map, 1, 5, 8, 1, true);
    
    post1.inventory .set(COTTON ,  20);
    post1.inventory .set(POTTERY,  20);
    
    TradeBuilding post2 = (TradeBuilding) PORTER_HOUSE.generate();
    post2.enterMap(map, 5, 6);
    post2.ID = "(Gets Clay)";
    post2.tradeLevel.set(RAW_COTTON, -10);
    post2.tradeLevel.set(CLAY      ,  20);
    post2.inventory .set(RAW_COTTON,  10);
    
    Building kiln = (Building) KILN.generate();
    kiln.enterMap(map, 2, 3);
    
    Building weaver = (Building) WEAVER.generate();
    weaver.enterMap(map, 5, 3);
    
    runGameLoop(map);
  }
}




