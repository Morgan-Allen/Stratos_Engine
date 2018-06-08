

package game;
import graphics.common.*;



public class Faction extends Constant {
  
  
  String name;
  Colour colour;
  
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
  
  
  public String toString() {
    return name;
  }
}
