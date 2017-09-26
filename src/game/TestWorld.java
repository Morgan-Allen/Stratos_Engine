

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
    map.performSetup(10);
    map.attachCity(cityA);
    
    City.setupRoute(cityA, cityB, 2);
    cityB.stockLevels.set(CLAY  , 100);
    cityB.stockLevels.set(COTTON, -50);
    cityB.inventory  .set(COTTON,  50);
    
    TradeBuilding post = new TradeBuilding(PORTER_HOUSE);
    post.enterMap(map, 2, 2);
    post.stockLevels.set(COTTON,   10);
    post.stockLevels.set(CLAY  ,  -20);
    post.inventory  .set(CLAY  ,   20);
    Tile.applyPaving(map, 2, 1, 8, 1, true);
    
    runGameLoop(map);
  }
}








