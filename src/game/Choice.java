/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import util.*;



public class Choice {
  
  
  /**  Data fields, constructors and setup-
    */
  public static boolean
    verbose       = false,
    verboseReject = false,
    verboseSwitch = false;
  
  final public Actor actor;
  final Batch <Task> plans = new Batch <Task> ();
  public boolean isVerbose = false;
  
  
  public Choice(Actor actor) {
    this.actor = actor;
  }
  
  
  public Choice(Actor actor, Series <Task> plans) {
    this(actor);
    for (Task p : plans) add(p);
  }
  
  
  public boolean add(Task plan) {
    if (plan == null) return false;
    plans.add(plan);
    return true;
  }
  
  
  public int size() {
    return plans.size();
  }
  
  
  public boolean empty() {
    return plans.size() == 0;
  }
  
  
  
  /**  Picks a plan from those assigned earlier using priorities to weight the
    *  likelihood of their selection.
    */
  public Task pickMostUrgent(float minPriority) {
    final Task b = weightedPick(false);
    if (b == null || b.priority() < minPriority) return null;
    else return b;
  }
  
  
  public Task pickMostUrgent() {
    return weightedPick(false);
  }
  
  
  public Task weightedPick() {
    return weightedPick(true);
  }
  
  
  private static float competeThreshold(
    Actor actor, float topPriority, boolean fromCurrent
  ) {
    float thresh = Task.SWITCH_DIFF;
    
    if (topPriority > Task.PARAMOUNT) {
      final float extra = (topPriority - Task.PARAMOUNT) / Task.PARAMOUNT;
      thresh *= 1 + extra;
    }
    
    final float stubborn = actor.levelOf(TRAIT_DILIGENCE) / 2f;
    thresh *= 1 + stubborn;
    if (fromCurrent) thresh += 1;
    
    return Nums.clamp(topPriority - thresh, 0, 100);
  }
  
  
  private Task weightedPick(boolean free) {
    final boolean report = (verbose && I.talkAbout == actor) || isVerbose;
    
    if (plans.size() == 0) {
      if (report) I.say("  ...Empty choice!");
      return null;
    }
    if (report) {
      String label = actor.type().name;
      I.say("\n"+actor+" ("+label+") is making a choice.");
      I.say("  Range of choice is "+plans.size()+", free? "+free);
    }
    //
    //  Firstly, acquire the priorities for each plan.  If the permitted range
    //  of priorities is zero, simply return the most promising.
    
    float bestPriority = 0;
    Task picked = null;
    final float weights[] = new float[plans.size()];
    int i = 0;
    
    for (Task plan : plans) {
      
      final float priority = plan.priority();
      if (priority > bestPriority) { bestPriority = priority; picked = plan; }
      weights[i++] = priority;
      if (report) {
        I.say("  Considering- "+plan);
        I.say("    Priority "+priority);
      }
    }
    if (! free) {
      if (report) {
        I.say("  Top Pick:  "+picked);
      }
      return picked;
    }
    //
    //  Eliminate all weights outside the permitted range, so that only plans
    //  of comparable attractiveness to the most important are considered-
    final float minPriority = competeThreshold(actor, bestPriority, false);
    if (report) {
      I.say("  Best priority: "+bestPriority);
      I.say("  Min. priority: "+minPriority );
    }
    float sumWeights = i = 0;
    for (; i < plans.size(); i++) {
      weights[i] = Nums.max(0, weights[i] - minPriority);
      sumWeights += weights[i];
    }
    if (sumWeights == 0) {
      if (report) I.say("Picked: "+picked);
      return picked;
    }
    //
    //  Finally, select a candidate at random using weights based on priority-
    float randPick = Rand.num() * sumWeights;
    i = 0;
    for (Task plan : plans) {
      final float chance = weights[i++];
      if (randPick < chance) { picked = plan; break; }
      else randPick -= chance;
    }
    if (report) I.say("Picked: "+picked);
    return picked;
  }
  
  
  
  public static Task switchFor(
    Actor actor, Task last, Task next, boolean stubborn,
    boolean report
  ) {
    if (wouldSwitch(actor, last, next, stubborn, report)) return next;
    else return last;
  }
  
  
  public static boolean wouldSwitch(
    Actor actor, Task last, Task next, boolean stubborn,
    boolean report
  ) {
    report &= verboseSwitch;
    if (report) I.say("\nConsidering switch from "+last+" to "+next);
    
    final float
      lastPriority = last.priority(),
      nextPriority = next.priority();
    if (report) {
      I.say("  Last priority: "+lastPriority);
      I.say("  Next priority: "+nextPriority);
    }
    if (nextPriority <= 0) return false;
    if (lastPriority <= 0) return true ;
    
    final float minPriority = stubborn ?
      competeThreshold(actor, nextPriority, true) :
      nextPriority;
    
    if (report) {
      I.say("  Min. priority for last is: "+minPriority);
      I.say("  Threshold: "+(nextPriority - minPriority));
      I.say("  Stubbornness: "+actor.levelOf(TRAIT_DILIGENCE));
      I.say("  Would switch from last to next? "+(lastPriority < minPriority));
    }
    return lastPriority < minPriority;
  }
  
}




