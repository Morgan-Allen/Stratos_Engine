

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;
import static game.GameConstants.*;



public class PaneActor extends DetailPane {
  
  
  final Actor actor;
  
  
  public PaneActor(HUD UI, Actor actor) {
    super(UI, actor, "(STATUS)", "(SKILLS)", "(PSYCH)");
    this.actor = actor;
  }
  
  protected void updateText(Text text) {
    text.setText("");
    final Description d = text;
    
    int classLevel = actor.traits.classLevel();
    int fullXP = actor.traits.classLevelFullXP();
    int XP = (int) (actor.traits.classLevelProgress() * fullXP);
    
    d.append("  Level "+classLevel+" "+actor.type());
    d.append("\n  XP: "+XP+"/"+fullXP);
    d.append("\n");
    
    if (inCategory("(STATUS)")) describeStatus(d);
    if (inCategory("(SKILLS)")) describeSkills(d);
    if (inCategory("(PSYCH)" )) describePsych (d);
  }
  
  protected void describeStatus(Description d) {
    
    int maxHP = actor.type().maxHealth;
    float hurt = actor.health.injury();
    float tire = actor.health.fatigue();
    float hung = actor.health.hunger();
    int HP = (int) (maxHP - (hurt + tire + hung));
    
    Type type = actor.type();
    d.append("\n  HP:               "+HP+"/"+maxHP);
    d.append("\n  Hurt/Tire/Hung:   "+(int) hurt+"/"+(int) tire+"/"+(int) hung);
    d.append("\n  Melee/Range dmg:  "+type.meleeDamage+"/"+type.rangeDamage);
    d.append("\n  Armour class:     "+type.armourClass);
    d.append("\n  Sight/attack rng: "+type.sightRange+"/"+type.rangeDist);
    
    Task task = actor.task();
    float priority = task == null ? 0 : task.priority() / Task.PARAMOUNT;
    
    d.append("\n");
    d.appendAll("\n  Works at:  ", actor.work());
    d.appendAll("\n  Lives at:  ", actor.home());
    d.appendAll("\n  Mission:   ", actor.mission());
    d.appendAll("\n  Currently: ", actor.task());
    d.appendAll("\n  Urgency:   ", I.percent(priority));
    
    Tally <Good> carried = actor.outfit.carried();
    d.append("\n  Carrying: ");
    if (carried.empty()) d.append("None");
    else for (Good g : carried.keys()) {
      d.appendAll("\n    ", I.shorten(carried.valueFor(g), 1), " ", g);
    }
  }
  
  protected void describeSkills(Description d) {
    if (actor.type().isPerson()) {
      d.append("\n  Skills:");
      
      int skillI = 0;
      for (Trait t : ALL_SKILLS) {
        int level = (int) actor.traits.levelOf(t);
        
        if (skillI++ % 2 == 0) d.append("\n    ");
        else d.append(" ");
        
        Colour c = Colour.GREY;
        if (level > 3) c = Colour.LITE_GREY;
        if (level > 6) c = Colour.WHITE;
        
        Text.appendColour(I.padToLength(t.name, 6)+": ", c, d);
        Text.appendColour(I.padToLength(""+level, 2)   , c, d);
      }
    }
  }
  
  protected void describePsych(Description d) {
    if (actor.type().isPerson()) {
      d.append("\n  Traits:");
      for (Trait t : ALL_PERSONALITY) {
        int level = (int) (actor.traits.levelOf(t) * 100);
        
        String name = level > 0 ? t.name : (String) Visit.last(t.traitRangeNames);
        level = Nums.abs(level);
        
        Colour c = Colour.GREY;
        if (level > 25) c = Colour.LITE_GREY;
        if (level > 75) c = Colour.WHITE;
        
        d.append("\n    ");
        Text.appendColour(I.padToLength(name, 9)+": ", c, d);
        Text.appendColour(I.padToLength(level+"%", 5)  , c, d);
      }
    }
  }
  
}








