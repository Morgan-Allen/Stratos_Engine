

package game;
import static game.BuildingSet.*;



public class TestWorld extends TestLoop {
  
  
  public static void main(String args[]) {
    
    World   world = new World();
    City    cityA = new City(world);
    City    cityB = new City(world);
    CityMap map   = new CityMap();
    
    cityA.map = map;
    world.cities.add(cityA);
    world.cities.add(cityB);
    City.setupRoute(cityA, cityB, 2);
    cityA.setWorldCoords(1, 1);
    cityB.setWorldCoords(3, 3);
    
    map.performSetup(10);
    map.attachCity(cityA);
    
    /*
    cityB.stockLevel.set(CLAY      , 100);
    cityB.stockLevel.set(RAW_COTTON, -50);
    cityB.inventory .set(RAW_COTTON,  50);
    //*/
    
    TradeBuilding post1 = (TradeBuilding) PORTER_HOUSE.generate();
    post1.enterMap(map, 2, 2);
    post1.stockLevel.set(RAW_COTTON,  10);
    post1.stockLevel.set(CLAY      , -20);
    post1.inventory .set(CLAY      ,  20);
    post1.tradePartner = cityB;
    
    TradeBuilding post2 = (TradeBuilding) PORTER_HOUSE.generate();
    post2.enterMap(map, 2, 2);
    post2.stockLevel.set(RAW_COTTON, -10);
    post2.stockLevel.set(CLAY      ,  20);
    post2.inventory .set(RAW_COTTON,  10);
    Tile.applyPaving(map, 2, 1, 8, 1, true);
    
    runGameLoop(map);
  }
}










