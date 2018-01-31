

package gameUI.play;
import game.*;
import graphics.widgets.*;
import util.*;
import static game.GameConstants.*;



public class ActorPane extends DetailPane {
  
  
  final Actor actor;
  
  
  public ActorPane(HUD UI, Actor actor) {
    super(UI, actor);
    this.actor = actor;
  }
  
  
  protected void updateState() {
    
    this.text.setText("");
    final Description d = this.text;
    
    d.append(""+actor.toString());
    d.append("\n");
    
    int maxHP = actor.type().maxHealth;
    float hurt = actor.injury(), tire = actor.fatigue();
    int HP = (int) (maxHP - (hurt + tire));
    
    Type type = actor.type();
    d.append("\n  HP: "+HP+"/"+maxHP);
    d.append("\n  Melee/Range dmg:  "+type.meleeDamage+"/"+type.rangeDamage);
    d.append("\n  Armour class:     "+type.armourClass);
    d.append("\n  Sight/attack rng: "+type.sightRange+"/"+type.rangeDist);
    
    d.append("\n");
    d.appendAll("\n  Works at:  ", actor.work());
    d.appendAll("\n  Lives at:  ", actor.home());
    d.appendAll("\n  Currently: ", actor.task());
    d.appendAll("\n  Carrying:  ", actor.carryAmount(), " ", actor.carried());
    
    Series <Trait> traits = actor.allTraits();
    if (traits.size() > 0) {
      d.append("\n\nTraits:");
      for (Trait t : actor.allTraits()) {
        d.appendAll("\n  ", t, ":"+I.shorten(actor.levelOf(t), 1));
      }
    }
    
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








