

package game;
import static game.GameConstants.*;
import graphics.common.*;



public class Faction extends Constant implements BaseRelations.Postured {
  
  
  String name;
  Colour colour;
  int tint = CITY_COLOR;
  
  WorldLocale homeland = null;
  BuildType buildTypes[] = {};
  
  
  public Faction(
    Class baseClass, String ID,
    String name, BuildType... buildTypes
  ) {
    super(baseClass, ID, IS_TRAIT);
    this.name = name;
    this.buildTypes = buildTypes;
  }
  
  
  public WorldLocale homeland() {
    return homeland;
  }
  
  public BuildType[] buildTypes() {
    return buildTypes;
  }
  
  public Faction faction() {
    return this;
  }
  
  public BaseRelations relations(World world) {
    return world.factionCouncil(this).relations;
  }
  
  
  
  
  public String toString() {
    return name;
  }
  
  public int tint() {
    return tint;
  }
}





