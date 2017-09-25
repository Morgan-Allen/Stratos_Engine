

package game;
import static game.BuildingSet.*;



public class TestWorld extends TestLoop {
  
  
  public static void main(String args[]) {
    
    World world = new World();
    City cityA = new City(world);
    City cityB = new City(world);
    CityMap map = new CityMap();
    
    cityA.map = map;
    world.cities.add(cityA);
    world.cities.add(cityB);
    map.performSetup(10);
    map.attachCity(cityA);
    
    CraftBuilding post = new CraftBuilding(PORTER_HOUSE);
    post.enterMap(map, 2, 2);
    Tile.applyPaving(map, 2, 1, 8, 1, true);
    
    
    runGameLoop(map);
  }
}
