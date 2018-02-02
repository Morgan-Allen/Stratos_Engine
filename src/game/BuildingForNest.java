

package game;


//  TODO:  This needs to be handled differently from other buildings on the
//  map, insofar as it will generally belong to an entirely different and
//  possibly hostile faction.
//
//  It should not be repaired, or lift fog of war, or provide a suitable place
//  of refuge for anything other than resident creatures.



public class BuildingForNest extends Building {
  
  
  public BuildingForNest(Type type) {
    super(type);
  }
  
  
  public BuildingForNest(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  
  
  
  public Task selectActorBehaviour(Actor actor) {
    return null;
  }
  
}