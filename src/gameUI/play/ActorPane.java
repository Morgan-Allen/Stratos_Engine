

package gameUI.play;
import game.*;
//import game.actor.*;
//import game.venue.*;
import graphics.widgets.*;
import util.*;



public class ActorPane extends DetailPane {
  
  
  final Actor actor;
  
  
  public ActorPane(HUD UI, Actor actor) {
    super(UI, actor);
    this.actor = actor;
  }
  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    /*
    final int
      level  = actor.traits.characterLevel(),
      remXP  = actor.traits.remainderXP(),
      needXP = actor.traits.requiredXP(),
      maxHP  = actor.traits.maxHP(),
      HP     = (int) (maxHP - actor.traits.injury()),
      tired  = (int) actor.traits.fatigue()
    ;

    d.append(""+actor.toString());
    if (! actor.species().creature) {
      d.append("\n"+actor.species());
      d.append(" Level: "+level+" (XP "+remXP+"/"+needXP+")");
    }
    d.append("\n");
    
    d.append("\nHP: ");
    d.append(HP+"/"+maxHP);
    if (tired > 0) d.append(" (Fatigue "+tired+")");
    d.append("\nDamage: "+actor.traits.damage());
    d.append("\nArmour: "+actor.traits.armour());
    d.append("\nCredits: "+actor.gear.credits());
    
    final Series <Item> allItems = actor.gear.allItems();
    if (! allItems.empty()) {
      d.append("\nCarried:");
      for (Item i : allItems) d.appendAll("\n  ", i);
    }
    
    d.append("\n\nHome at: ");
    if (actor.home() == null) d.append("None");
    else d.append(actor.home());
    
    d.append("\nCurrently: ");
    final Plan doing = actor.plan();
    if (doing == null) {
      d.append("Thinking");
    }
    else if (doing.mission() != null) {
      d.append("On ");
      doing.mission().describeMission(d);
      d.append(" ("+I.shorten(doing.priority(), 1)+")");
    }
    else {
      doing.describePlan(d);
      d.append(" ("+I.shorten(doing.priority(), 1)+")");
    }
    
    d.append("\n\nSkills:");
    for (Trait t : actor.traits.allTraits(Trait.TYPE_SKILL)) {
      int skillL = (int) actor.traits.levelOf(t);
      d.append("\n  "+t+": "+skillL);
    }
    //*/
    
    super.updateState();
  }
  
}









