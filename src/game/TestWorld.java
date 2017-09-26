

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
    
    map.performSetup(10);
    map.attachCity(cityA);
    
    cityB.demands  .set(CLAY      , 100);
    cityB.demands  .set(RAW_COTTON, -50);
    cityB.inventory.set(RAW_COTTON,  50);
    
    TradeBuilding post = new TradeBuilding(PORTER_HOUSE);
    post.enterMap(map, 2, 2);
    post.stockLevels.set(RAW_COTTON,  10);
    post.stockLevels.set(CLAY      , -20);
    post.inventory  .set(CLAY      ,  20);
    post.tradePartner = cityB;
    Tile.applyPaving(map, 2, 1, 8, 1, true);
    
    runGameLoop(map);
  }
}




