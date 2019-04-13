

package game;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;



public class TaskDialog extends Task {
  
  
  /**  Data-fields, construction and save/load methods-
    */
  final static int
    MODE_INIT    = -1,
    MODE_CASUAL  =  0,
    MODE_CONTACT =  1,
    MODE_PLEAD   =  2
  ;
  
  final Actor with;
  boolean began;
  int mode = MODE_INIT;
  
  
  TaskDialog(Actor actor, Actor with, boolean began, int mode) {
    super(actor);
    this.with  = with;
    this.began = began;
    this.mode  = mode;
  }
  
  
  public TaskDialog(Session s) throws Exception {
    super(s);
    this.with  = (Actor) s.loadObject();
    this.began = s.loadBool();
    this.mode  = s.loadInt();
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    s.saveObject(with);
    s.saveBool(began);
    s.saveInt(mode);
  }
  
  
  
  /**  External factory methods-
    */
  static TaskDialog nextContactDialog(Actor actor) {
    Series <Active> others = (Series) actor.considered();
    return nextDialog(actor, others, MODE_CONTACT, null);
  }
  
  
  static TaskDialog nextCasualDialog(Actor actor) {
    Series <Active> others = (Series) actor.seen();
    return nextDialog(actor, others, MODE_CASUAL, null);
  }
  
  
  static TaskDialog nextDialog(
    Actor actor, Series <Active> assessed,
    int mode, Mission mission
  ) {
    if (! actor.map().world.settings.toggleDialog) return null;
    //
    //  Don't generate a new dialog if one is in progress...
    if (actor.jobType() == JOB.DIALOG) return null;
    //
    //  Find a promising new target-
    Pick <Active> pick = new Pick(0);
    for (Active a : assessed) {
      if (! a.mobile()) continue;
      pick.compare(a, dialogRating(actor, (Actor) a, mode, false));
    }
    if (pick.empty()) return null;
    Actor with = (Actor) pick.result();
    //
    //  Then configure and return...
    TaskDialog dialog = new TaskDialog(actor, with, true, MODE_CASUAL);
    dialog.configTask(mission, null, with, JOB.DIALOG, 1);
    if (! dialog.pathValid()) return null;
    return dialog;
  }
  
  
  static TaskDialog contactDialogFor(Actor actor, Actor with, Mission mission) {
    if (! actor.map().world.settings.toggleDialog) return null;
    
    TaskDialog dialog = new TaskDialog(actor, with, true, MODE_CONTACT);
    dialog.configTask(mission, null, with, JOB.DIALOG, 1);
    
    return dialog.pathValid() ? dialog : null;
  }
  
  
  
  /**  Priority-evaluation-
    */
  static float dialogRating(
    Actor actor, Actor with, int mode, boolean begun
  ) {
    //
    //  Basic sanity-checks first-
    if (with == actor || with.indoors()) return 0;
    if (! with.health.active()         ) return 0;
    if (! with.type().isPerson()       ) return 0;
    if (TaskCombat.hostile(actor, with)) return 0;
    float busyRating = 0;
    //
    //  We then assess the busyness of the other actor.  NOTE- To avoid an
    //  infinite regression, we never directly assess the priority of another
    //  dialog- it's either an automatic pass if they're already talking to
    //  us, or an automatic fail if they're talking to someone else.
    if (with.jobType() == JOB.DIALOG) {
      TaskDialog dialog = (TaskDialog) with.task();
      if (dialog.with != actor) return 0;
    }
    else if (with.inEmergency()) {
      return 0;
    }
    else {
      busyRating = with.jobPriority();
    }
    //
    //  If that check passes, assess the novelty of these actors relative to
    //  eachother, tweak for a few other factors, and return if that seems more
    //  important than what the other actor is currently doing.
    float noveltyFrom = actor.bonds.bondNovelty(with );
    float noveltyBack = with .bonds.bondNovelty(actor);
    float talkRating  = Nums.min(noveltyFrom, noveltyBack);
    
    if (! TaskCombat.allied(actor, with)) talkRating /= 2;
    talkRating *= 1 + (actor.bonds.bondLevel(with) / 2);
    talkRating *= ROUTINE;
    busyRating = Nums.max(busyRating, begun ? 0 : IDLE);
    return (busyRating < talkRating) ? talkRating : 0;
  }
  
  
  protected float successPriority() {
    Actor actor = (Actor) active;
    
    float rating = dialogRating(actor, with, mode, false) / ROUTINE;
    if (rating <= 0) return -1;
    
    //  TODO:  Include effects of curiosity if this is a brand new actor and/or
    //  belongs to an unfamiliar tribe...
    
    //  TODO:  And include effects of ambient danger...
    
    float empathy = actor.traits.levelOf(TRAIT_EMPATHY);
    float priority = 0;
    if (mode == MODE_CASUAL ) priority = CASUAL;
    if (mode == MODE_CONTACT) priority = ROUTINE;
    if (mode == MODE_PLEAD  ) priority = PARAMOUNT;
    
    if (actor.bonds.hasBond(with)) {
      priority *= 1 + (actor.bonds.bondLevel(with) / 2);
    }
    else if (mode == MODE_CASUAL) {
      priority *= actor.bonds.solitude();
    }
    
    return (1 + (empathy / 2)) * priority;
  }
  
  
  static Actor talksWith(Actor other) {
    if (other.jobType() != JOB.DIALOG) return null;
    return ((TaskDialog) other.task()).with;
  }
  


  /**  Behaviour-execution-
    */
  protected void onTarget(Target target) {
    Actor actor = (Actor) active;
    
    if (type == JOB.DIALOG) {
      Mission mission = mode == MODE_CONTACT ? (Mission) origin : null;
      float rating = dialogRating(actor, with, mode, true);
      
      if (began && rating > 0 && talksWith(with) != actor) {
        TaskDialog response = new TaskDialog(with, actor, false, mode);
        response.configTask(null, visits, actor, JOB.DIALOG, 1);
        if (response.pathValid()) with.assignTask(response, this);
      }
      
      float talkInc = 1f / DIALOG_LENGTH;
      float maxBond = MAX_CHAT_BOND / 100f;
      with.bonds.incBond(actor, talkInc * CHAT_BOND / 100f, maxBond);
      with.bonds.incNovelty(actor, 0 - talkInc);
      
      if (mission != null && mission.terms.hasTerms() && ! mission.terms.sent()) {
        mission.terms.sendTerms(with.base());
      }
      if (rating > 0 && talksWith(with) == actor) {
        configTask(origin, null, with, JOB.DIALOG, 1);
      }
      else {
        actor.bonds.incNovelty(with, -1);
        suggestJointActivity(actor, with);
      }
    }
  }


  float actionRange() {
    if (talksWith(with) != active) return AVG_SIGHT;
    return 2;
  }
  
  
  void suggestJointActivity(Actor suggests, Actor other) {
    
    boolean report = true;
    
    Task tasks[][] = new Task[2][];
    int aID = 0;
    Building home = suggests.home();
    Employer work = suggests.work();
    Series <Active> friendly = (Series) suggests.bonds.friendly();
    
    if (report) {
      I.say("\nAssessing joint activities for "+suggests+" and "+other);
      I.say("  Home: "+home);
      I.say("  Work: "+work);
    }
    
    for (Actor a : new Actor[] { suggests, other }) {
      Task explore = TaskExplore.configExploration(a);
      Task hunting = TaskHunting.nextHunting(a);
      Task dialog  = TaskDialog.nextDialog(a, friendly, MODE_CONTACT, null);
      Task resting = TaskResting.nextResting(a, home);
      Task keeping = home == null ? null : home.selectActorBehaviour(a);
      Task working = work == null ? null : work.selectActorBehaviour(a);
      tasks[aID] = new Task[] { explore, hunting, dialog, resting, keeping, working };
      aID++;
    }
    
    float baseRating = Rand.num() * ROUTINE;
    class InviteOption {
      Task forSuggests, forOther;
      float rating = 0;
    };
    Pick <InviteOption> pick = new Pick(baseRating);
    
    for (int i = 0; i < tasks[0].length; i++) {
      InviteOption o = new InviteOption();
      Task FS = o.forSuggests = tasks[0][i];
      Task FO = o.forOther    = tasks[1][i];
      
      if (report) {
        I.say("  ");
        if (FS == null) I.add("none");
        else I.add(FS.type()+" ("+FS.priority()+")");
        I.add(" | ");
        if (FO == null) I.add("none");
        else I.add(FO.type()+" ("+FO.priority()+")");
      }
      
      if (FS == null || FS.priority() <= 0) continue;
      if (FO == null || FO.priority() <= 0) continue;
      o.rating = (FS.priority() + FO.priority()) * Rand.num();
      
      if (report) {
        I.add(" -> "+o.rating);
      }
      
      pick.compare(o, o.rating);
    }
    
    InviteOption picked = pick.result();
    if (picked != null) {
      picked.forSuggests.assignCompany(other);
      picked.forOther.assignCompany(suggests);
      suggests.assignTask(picked.forSuggests, other);
      other   .assignTask(picked.forOther, suggests);
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  String animName() {
    return AnimNames.TALK;
  }
  
}


