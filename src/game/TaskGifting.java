

package game;
import static game.GameConstants.*;
import util.*;



public class TaskGifting extends TaskDialog {
  
  
  /**  Data fields, construction and save/load methods-
    */
  Good gifted;
  Carrier store;
  

  TaskGifting(Actor actor, Actor with, boolean began) {
    super(actor, with, began);
  }
  
  
  public TaskGifting(Session s) throws Exception {
    super(s);
    this.store  = (Carrier) s.loadObject();
    this.gifted = (Good   ) s.loadObject();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(store);
    s.saveObject(gifted);
  }
  

  
  /**  Initial config and factory methods, plus mission-support-
    */
  static TaskGifting nextGiftingFor(Actor actor, TaskDialog dialog) {
    Carrier store = null;
    
    if (dialog.origin instanceof Mission) {
      store = actor.base().headquarters();
    }
    if (store == null) {
      store = actor.home();
    }
    if (store == null || store.base().activeMap() != actor.map()) {
      store = actor;
    }
    
    Good gifted = nextGiftPickup(actor, dialog.with, store);
    if (gifted == null) return null;
    
    TaskGifting task = new TaskGifting(actor, dialog.with, true);
    task.gifted = gifted;
    task.store  = store;
    
    if (store != actor) {
      task.configTask(dialog.origin, (Pathing) store, null, JOB.COLLECTING, 1);
    }
    else {
      task.configTask(dialog.origin, null, dialog.with, JOB.DIALOG, 1);
    }
    return task;
  }
  
  
  static Good nextGiftPickup(Actor gives, Actor gets, Carrier store) {
    if (store == null || gives == null || gets == null) return null;
    
    Pick <Good> pickGift = new Pick();
    Building home = gets.home();
    Base away = gets.offmapBase();
    
    if (home != null) for (Good g : home.needed()) {
      if (store.inventory().valueFor(g) < 2) continue;
      float rating = home.demandFor(g) * g.price;
      pickGift.compare(g, rating);
    }
    
    if (away != null) for (Good g : away.needLevels().keys()) {
      if (store.inventory().valueFor(g) < 2) continue;
      float rating = away.needLevel(g) * g.price;
      pickGift.compare(g, rating);
    }
    
    return pickGift.result();
  }
  
  
  static void performMissionPickup(Mission mission) {
    Base home = mission.homeBase();
    
    for (Actor a : mission.recruits) {
      Actor receives = MissionForContact.findTalkSubject(mission, a, true);
      Good gift = nextGiftPickup(a, receives, home);
      home.inventory().add(-1, gift);
      a.outfit.incCarried(gift, 1);
    }
  }
  
  
  static void performMisionDelivery(Mission mission) {
    for (Actor a : mission.recruits()) {
      TaskGifting g = (TaskGifting) a.todo(TaskGifting.class);
      if (g != null) g.transferGift(a, g.with);
    }
  }
  
  
  
  /**  Behaviour-execution methods...
    */
  protected void onVisit(Pathing visits) {
    ActorAsPerson actor = (ActorAsPerson) active;
    
    if (type == JOB.COLLECTING) {
      if (store.inventory().valueFor(gifted) < 1) return;
      
      actor.outfit.incCarried(gifted, 1);
      store.inventory().add(-1, gifted);
      
      if (with.map() != actor.map()) {
        actor.addTodo(this);
      }
      else {
        configTask(origin, null, with, JOB.DIALOG, 1);
      }
    }
  }
  
  
  protected int checkResume() {
    if (origin instanceof Mission && ((Mission) origin).complete()) {
      return RESUME_NO;
    }
    if (((Actor) active).map() == with.map()) {
      return RESUME_YES;
    }
    return RESUME_WAIT;
  }
  
  
  protected void onTarget(Target target) {
    super.onTarget(target);
    
    Actor gives = (Actor) active;
    Actor other = (Actor) target;
    
    if (type == JOB.DIALOG && talksWith(other) == gives && gifted != null) {
      transferGift(gives, other);
    }
  }
  
  
  protected void transferGift(Actor gives, Actor other) {
    gives.outfit.incCarried(gifted, -1);
    other.outfit.incCarried(gifted,  1);
    
    float boost = GIFT_BOND  / 100f;
    boost *= 0.5f + (gifted.price / 50f);
    other.bonds.incBond(gives, boost, MAX_GIFT_BOND / 100f);
    
    gifted = null;
  }
  
}





