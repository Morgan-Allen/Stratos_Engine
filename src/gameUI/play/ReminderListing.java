/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.play;
import gameUI.misc.*;
import graphics.widgets.*;
import start.*;
import game.*;
import util.*;
import static game.WorldScenario.*;



public class ReminderListing extends UIGroup {
  
  
  final PlayUI UI;
  final List <Entry> entries = new List <Entry> ();
  
  
  public ReminderListing(PlayUI UI) {
    super(UI);
    this.UI = UI;
  }
  
  
  
  /**  Maintaining the list of reminders-
    */
  protected static class Entry extends UIGroup implements UIConstants {
    
    final Object refers;
    boolean active;
    
    final int high, wide;
    float fadeVal, down;
    
    
    protected Entry(PlayUI UI, Object refers, int wide, int high) {
      super(UI);
      this.refers = refers;
      this.high   = high  ;
      this.wide   = wide  ;
      active  = true ;
      fadeVal =  0   ;
      down    = -1   ;
    }
  }
  
  
  private Entry entryThatRefers(Object refers) {
    for (Entry e : entries) {
      if (e.refers == refers) return e;
    }
    return null;
  }
  
  
  private boolean hasEntryRefers(Object refers) {
    return entryThatRefers(refers) != null;
  }
  
  
  private Entry addEntry(Object refers, int atIndex) {
    //
    //  We first determine the kind of reminder-entry appropriate for the
    //  object being referred to-
    //final Base played = UI.base;
    Entry entry = null;
    if (refers instanceof Mission) {
      entry = new MissionReminder(UI, (Mission) refers);
    }
    if (refers instanceof Objective) {
      entry = new ObjectiveReminder(UI, (Objective) refers);
    }
    /*
    if (refers instanceof MessagePane) {
      entry = new MessageReminder(UI, refers, (MessagePane) refers);
    }
    if (refers == oldMessages) {
      entry = new CommsPane.Reminder(UI, oldMessages, this);
    }
    if (refers instanceof BaseAdvice.Topic) {
      final MessagePane advicePane = played.advice.configAdvicePanel(
        null, refers, UI
      );
      entry = new MessageReminder(UI, refers, advicePane);
    }
    //*/
    if (entry == null) {
      I.complain("\nNO SUPPORTED ENTRY FOR "+refers);
      return null;
    }
    //
    //  Then we must insert the new entry at the right position in the list
    //  (skipping over anything inactive.)
    Entry before = null;
    int index = 0;
    for (Entry e : entries) if (e.active && (index++ == atIndex - 1)) {
      before = e; break;
    }
    if      (atIndex == 0  ) entries.addFirst(entry);
    else if (before == null) entries.addLast (entry);
    else entries.addAfter(entries.match(before), entry);
    entry.attachTo(this);
    return entry;
  }
  
  
  protected void updateState() {
    //
    //  Include all currently ongoing missions and any special messages:
    List <Object> needShow = new List <Object> ();
    final Base played = UI.base;
    Scenario scenario = MainGame.currentScenario();
    //final float currentTime = played.world.time();
    
    for (Objective objective : scenario.objectives()) {
      needShow.add(objective);
    }
    for (final Mission mission : played.missions()) {
      needShow.add(mission);
    }
    //
    //  Now, in essence, insert entries for anything not currently listed, and
    //  delete entries for anything listed that shouldn't be.
    
    boolean report = I.used60Frames && false;
    if (report) I.say("\nTotal entries: "+entries.size());
    
    int index = 0; for (Object s : needShow) {
      if (! hasEntryRefers(s)) addEntry(s, index);
      index++;
    }
    for (Entry e : entries) {
      if (e.active && ! needShow.includes(e.refers)) {
        e.active = false;
        e.fadeVal = 1;
      }
      if (e.fadeVal <= 0 && ! e.active) {
        e.detach();
        entries.remove(e);
      }
      if (! UI.trueBounds().intersects(e.trueBounds())) {
        e.hidden = true;
      }
      else {
        e.hidden = false;
      }
    }
    //
    //  Then iterate across all current entries and make sure their appearance
    //  is in order-
    final int padding = 20;
    int down = 0;
    
    float slowFadeInc = 1f / (1 * UI.rendering.frameRate());
    float defDriftRate = 60 * 1f / UI.rendering.frameRate();
    
    for (Entry e : entries) {
      //
      //  Adjust the entry's transparency-
      if (e.active) {
        e.fadeVal = Nums.clamp(e.fadeVal + slowFadeInc, 0, 1);
      }
      else {
        e.fadeVal = Nums.clamp(e.fadeVal - slowFadeInc, 0, 1);
      }
      e.relAlpha = e.fadeVal;
      //
      //  Have it drift into the correct position-
      final float gap = down - e.down;
      float drift = Nums.min(defDriftRate, Nums.abs(gap));
      if (gap == 0 || e.down == -1) e.down = down;
      else e.down += (gap > 0 ? 1 : -1) * drift;
      e.alignLeft(0           , e.wide);
      e.alignTop ((int) e.down, e.high);
      //
      //  Increment for the next entry, and proceed.
      down += e.high + padding;
    }
    
    super.updateState();
  }
  
}


