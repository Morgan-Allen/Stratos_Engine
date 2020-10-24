

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class TestBuilding extends Test {
  
  
  public static void main(String args[]) {
    testBuilding(true);
  }
  
  
  static boolean testBuilding(boolean graphics) {
    Test test = new TestBuilding();
    
    CityMap map = setupTestCity(16);
    World world = map.city.world;
    world.settings.toggleFog         = false;
    world.settings.toggleFatigue     = false;
    world.settings.toggleHunger      = false;
    world.settings.toggleMigrate     = false;
    world.settings.toggleBuildEvolve = false;
    
    
    Building farm = (Building) FARM_PLOT.generate();
    farm.enterMap(map, 1, 1, 1);
    Element tree = (Element) JUNGLE_TREE1.generate();
    tree.enterMap(map, 6, 2, 1);
    Element toRaze[] = { farm, tree };
    
    BuildingForHome home = (BuildingForHome) HOUSE.generate();
    map.planning.placeObject(home, 6, 3);
    BuildingForHome palace = (BuildingForHome) PALACE.generate();
    map.planning.placeObject(palace, 1, 3);
    
    BuildingForTrade post = (BuildingForTrade) PORTER_POST.generate();
    post.enterMap(map, 2, 10, 0);
    post.ID = "(Stock of Goods)";
    post.setTradeLevels(true,
      CLAY   , 40,
      STONE  , 80,
      WOOD   , 60,
      COTTON , 20,
      POTTERY, 20
    );
    fillWorkVacancies(post);
    map.planning.placeObject(post);
    
    Building mason = (Building) MASON.generate();
    mason.enterMap(map, 9, 6, 0);
    fillWorkVacancies(mason);
    map.planning.placeObject(mason);
    
    Building toBuild[] = { post, home, palace, mason };
    Series <Element> road = CityMapPlanning.placeStructure(
      ROAD, map, false, 2, 2, 10, 1
    );
    
    Tally <Good> startingMaterials = totalMaterials(map);
    Tally <Good> endMaterials = null;
    
    int     numPaved     = 0;
    boolean razingOkay   = false;
    boolean buildingOkay = false;
    boolean pavingOkay   = false;
    boolean setupOkay    = false;
    boolean testMaterial = false;
    boolean materialOkay = false;
    boolean didDamage    = false;
    boolean repairsOkay  = false;
    boolean testOkay     = false;
    
    final int RUN_TIME = YEAR_LENGTH;
    
    while (map.time < RUN_TIME || graphics) {
      map = test.runLoop(map, 10, graphics, "saves/test_building.tlt");
      
      if (! razingOkay) {
        boolean allRazed = true;
        for (Element e : toRaze) {
          if (e.onMap()) allRazed = false;
        }
        razingOkay = allRazed;
      }
      
      if (! buildingOkay) {
        boolean allBuilt = true;
        for (Building b : toBuild) {
          if (b.buildLevel() < 1) allBuilt = false;
        }
        buildingOkay = allBuilt;
      }
      
      if (! pavingOkay) {
        boolean allPaved = true;
        numPaved = 0;
        for (Element e : road) {
          if (e.complete()) numPaved += 1;
          else allPaved = false;
        }
        pavingOkay = allPaved;
      }
      
      setupOkay = razingOkay && buildingOkay && pavingOkay;
      
      if (setupOkay && ! testMaterial) {
        endMaterials = totalMaterials(map);
        float diff = getDiffs(
          startingMaterials, endMaterials,
          false, BUILD_GOODS
        );
        materialOkay = diff < 1.0f;
        testMaterial = true;
      }
      
      if (setupOkay && ! didDamage) {
        
        for (Building b : toBuild) {
          if (b == home) {
            home.setCurrentTier(HOUSE_T1);
          }
          else for (Good m : b.type.builtFrom) {
            float level = b.materialLevel(m);
            b.setMaterialLevel(m, level * 0.75f);
          }
        }
        didDamage = true;
      }
      
      if (didDamage && ! repairsOkay) {
        boolean allFixed = true;
        for (Building b : toBuild) {
          if (b.buildLevel() < 1) allFixed = false;
        }
        repairsOkay = allFixed;
      }
      
      if ((repairsOkay && materialOkay) && ! testOkay) {
        I.say("\nBUILDING TEST CONCLUDED SUCCESSFULLY!");
        testOkay = true;
        if (! graphics) return true;
      }
    }
    
    I.say("\nBUILDING TEST FAILED!");
    I.say("  Current time:  "+map.time+"/"+RUN_TIME);
    I.say("  Razing okay:   "+razingOkay  );
    I.say("  Building okay: "+buildingOkay);
    I.say("  Paving okay:   "+pavingOkay  );
    I.say("  Number paved:  "+numPaved    );
    I.say("  Material okay: "+materialOkay);
    I.say("  Did damage:    "+didDamage   );
    I.say("  Repairs okay:  "+repairsOkay );
    
    I.say("\nStructure conditions:");
    for (Building b : toBuild) {
      I.say("  "+b);
      I.say("    Build level: "+b.buildLevel());
      for (Good g : b.materials()) {
        float level = b.materialLevel(g), need = b.materialNeed(g);
        I.say("    "+g+": "+level+"/"+need);
      }
    }
    I.say("\nStructure inventories:");
    for (Building b : toBuild) {
      I.say("  "+b);
      for (Good g : b.inventory.keys()) {
        float level = b.inventory.valueFor(g);
        float need  = b.demandFor(g);
        I.say("    "+g+": "+level+"/"+need);
      }
    }
    
    return false;
  }
  
  
  static Tally <Good> totalMaterials(CityMap map) {
    Tally <Good> total = new Tally();
    for (Coord c : Visit.grid(0, 0, map.size, map.size, 1)) {
      Element b = map.above(c);
      if (b == null || b.at() != map.tileAt(c)) continue;
      
      for (Good m : b.materials()) {
        total.add(b.materialLevel(m), m);
      }
      if (b.type.isBuilding()) {
        total.add(((Building) b).inventory);
      }
    }
    for (Actor a : map.actors) {
      if (a.carried != null && a.carried instanceof Good) {
        total.add(a.carryAmount, (Good) a.carried);
      }
    }
    
    return total;
  }
  
  
  static float getDiffs(
    Tally <Good> before, Tally <Good> after,
    boolean report, Good... compared
  ) {
    if (report) I.say("\nReporting differences in goods:");
    float diffs = 0;
    for (Good g : compared) {
      float bef = before.valueFor(g), aft = after.valueFor(g);
      if (bef == 0 && aft == 0) continue;
      if (report) I.say("  "+g+": "+bef+" -> "+aft+": "+(aft - bef));
      diffs += Nums.abs(aft - bef);
    }
    return diffs;
  }
  
}














