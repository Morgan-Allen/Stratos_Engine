
package game;



public class Faction extends Constant {
  
  
  String name;
  WorldLocale homeland;
  BuildType buildTypes[];
  
  
  public Faction(Class baseClass, String ID) {
    super(baseClass, ID, IS_TRAIT);
  }
  
  
  public WorldLocale homeland() {
    return homeland;
  }
  
  public BuildType[] buildTypes() {
    return buildTypes;
  }
}
