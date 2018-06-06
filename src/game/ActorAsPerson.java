

package game;
import util.*;
import static game.Task.*;
import static game.ActorTraits.*;
import static game.GameConstants.*;



public class ActorAsPerson extends Actor {
  
  
  /**  Data fields, construction and save/load methods-
    */
  private List <Task> todo = new List();
  
  
  public ActorAsPerson(ActorType type) {
    super(type);
  }
  
  
  public ActorAsPerson(Session s) throws Exception {
    super(s);
    
    s.loadObjects(todo);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
    
    s.saveObjects(todo);
  }
  
  
  
  
  /**  Spawning new behaviours:
    */
  void beginNextBehaviour() {
    //
    //  TODO:  You will need to ensure that work/home/formation venues are
    //  present on the same map to derive related behaviours!
    //
    //  Establish some facts about the citizen first:
    boolean adult = health.adult();
    boolean agent = type().socialClass == CLASS_SOLDIER;
    assignTask(null, this);
    //
    //  Adults will search for work and a place to live:
    //  Children and retirees don't work:
    if (adult && work() == null) ActorUtils.findWork(map, this);
    if (adult && home() == null) ActorUtils.findHome(map, this);
    if (work() != null && ! adult) work().setWorker(this, false);
    
    //
    //  If you're seriously hungry/beat/tired, try going home:
    //  TODO:  Work this in as an emergency reaction?  It's a bit of a hack
    //  here.
    
    Building rests = TaskResting.findRestVenue(this, map);
    float needRest = TaskResting.restUrgency(this, rests);
    if (idle() && needRest > Rand.num() + 0.5f) {
      assignTask(TaskResting.nextResting(this, rests), this);
    }
    
    //
    //  See if there's a missions worth joining.  Or, if you have an active
    //  mission, undertake the next associated task-
    if (idle() && mission() == null && agent) {
      Pick <Mission> pick = new Pick(Task.ROUTINE * Rand.num());
      
      //  TODO:  Use a Choice for this?
      for (Mission f : base().missions) {
        if (! f.rewards.isBounty()) continue;
        Task t = f.selectActorBehaviour(this);
        float priority = t == null ? 0 : t.priority();
        pick.compare(f, priority * (0.5f + Rand.num()));
      }
      
      Mission joins = pick.result();
      if (joins != null) joins.toggleRecruit(this, true);
    }
    if (idle() && mission() != null && mission().active()) {
      assignTask(mission().selectActorBehaviour(this), this);
    }
    //
    //  Then check to see if your todo items include something pertinent-
    if (idle()) {
      for (Task task : todo) {
        int check = task.checkResume();
        if (check == RESUME_NO) {
          todo.remove(task);
        }
        if (check == RESUME_YES) {
          todo.remove(task);
          assignTask(task, this);
          break;
        }
      }
    }
    //
    //  Failing that, see if your home, place of work, purchases or other idle
    //  impulses have anything to say.
    if (idle()) {
      ActorChoice choice = new ActorChoice(this);
      
      if (agent) {
        choice.add(TaskHunting.nextHunting      (this));
        choice.add(TaskCombat .nextDefending    (this));
        choice.add(TaskExplore.configExploration(this));
        choice.add(TaskDialog .nextContactDialog(this));
      }
      
      choice.add(TaskWander  .nextWandering(this));
      choice.add(TaskFirstAid.nextFirstAid (this));
      choice.add(TaskResting .nextRelaxing (this));
      choice.add(TaskResting .nextResting  (this, rests));
      choice.add(TaskPurchase.nextPurchase (this));
      choice.add(selectTechniqueUse(false, (Series) considered()));
      
      if (work() != null && ((Element) work()).complete()) {
        choice.add(work().selectActorBehaviour(this));
      }
      if (home() != null && home().complete()) {
        choice.add(home().selectActorBehaviour(this));
      }
      
      assignTask(choice.weightedPick(), this);
    }
  }
  
  
  void updateReactions() {
    if (! map.world.settings.toggleReacts) return;
    
    //  TODO:  Missions should be given a chance to provide reactions as well.
    
    Task reaction = task() == null ? null : task().reaction();
    if (ActorChoice.wouldSwitch(this, task(), reaction, false, false)) {
      assignTask(reaction, this);
    }
    
    TaskCombat combat = TaskCombat.nextReaction(this, seen());
    if (ActorChoice.wouldSwitch(this, task(), combat, false, false)) {
      assignTask(combat, this);
    }
    
    TaskRetreat retreat = TaskRetreat.configRetreat(this);
    if (ActorChoice.wouldSwitch(this, task(), retreat, false, false)) {
      assignTask(retreat, this);
    }
    
    TaskDialog dialog = TaskDialog.nextCasualDialog(this);
    if (ActorChoice.wouldSwitch(this, task(), dialog, false, false)) {
      assignTask(dialog, this);
    }
    
    if (health.cooldown() == 0) {
      Task use = selectTechniqueUse(true, seen());
      if (use != null) assignReaction(use);
    }
  }
  
  
  Task selectTechniqueUse(boolean reaction, Series <Active> assessed) {
    
    class Reaction { ActorTechnique used; Target subject; float rating; }
    Pick <Reaction> pick = new Pick(0);
    
    boolean talk = assessed.size() > 1 && false;
    if (talk) {
      I.say("\n"+this+" Assessing techniques to use.");
    }
    
    for (Active other : assessed) {
      for (ActorTechnique used : traits.known()) {
        if (used.canActorUse(this, other)) {
          Reaction r = new Reaction();
          r.rating  = used.rateUse(this, other);
          r.subject = other;
          r.used    = used;
          pick.compare(r, r.rating);
          if (talk) I.say("  "+r.used+" -> "+r.subject+": "+r.rating);
        }
      }
      for (Good g : outfit.carried.keys()) for (ActorTechnique used : g.allows) {
        if (used.canActorUse(this, other)) {
          Reaction r = new Reaction();
          r.rating  = used.rateUse(this, other);
          r.subject = other;
          r.used    = used;
          pick.compare(r, r.rating);
          if (talk) I.say("  "+r.used+" -> "+r.subject+": "+r.rating);
        }
      }
    }
    if (pick.empty()) return null;
    
    Reaction r = pick.result();
    if (talk) I.say("  PICKED: "+r.used+" -> "+r.subject+": "+r.rating);
    return r.used.useFor(this, r.subject);
  }
  
  
  float updateFearLevel() {
    backup().clear();
    return TaskRetreat.fearLevel(this, backup());
  }
  
  
  public void addTodo(Task task) {
    todo.addFirst(task);
  }
  
  
  public void assignTask(Task task, Object source) {
    
    //  If the new task is an emergency, don't keep the old task.
    //  If the new task is assigned by the old task, don't keep the old task.
    //  If the old task is complete, or the new task is null, don't keep the old task.
    
    final Task old = task();
    boolean keepOld = true;
    if (task == null || old == null || old.complete()) keepOld = false;
    else if (task.emergency() || source == old) keepOld = false;
    if (keepOld) addTodo(old);
    
    super.assignTask(task, source);
  }
  
  
  public Series <Task> todo() {
    return todo;
  }
  
  
  
  /**  Aging, reproduction and life-cycle methods-
    */
  ActorHealth initHealth() {
    return new ActorHealth(this) {
      void updateLifeCycle(Base city, boolean onMap) {
        super.updateLifeCycle(city, onMap);
        
        WorldSettings settings = city.world.settings;
        
        if (pregnancy > 0) {
          boolean canBirth = (home() != null && inside() == home()) || ! onMap;
          pregnancy += 1;
          if (pregnancy > PREGNANCY_LENGTH && canBirth) {
            float dieChance = AVG_CHILD_MORT / 100f;
            if (! settings.toggleChildMort) dieChance = 0;
            
            //I.say(this+" FINISHED TERM...");
            
            if (Rand.num() >= dieChance) {
              completePregnancy(home(), onMap);
            }
            else {
              pregnancy = 0;
              //I.say(this+" LOST THEIR CHILD.");
            }
          }
          if (pregnancy > PREGNANCY_LENGTH + DAY_LENGTH) {
            pregnancy = 0;
            //I.say(this+" CANCELLED PREGNANCY!");
          }
        }
        
        if (ageSeconds % YEAR_LENGTH == 0) {
          
          boolean canDie = settings.toggleAging;
          if (senior() && canDie && Rand.index(100) < AVG_SENIOR_MORT) {
            setAsKilled("Old age");
          }
          
          boolean canConceive = home() != null || ! onMap;
          if (woman() && fertile() && pregnancy == 0 && canConceive) {
            float
              ageYears   = ageSeconds / (YEAR_LENGTH * 1f),
              fertSpan   = AVG_MENOPAUSE - AVG_MARRIED,
              fertility  = (AVG_MENOPAUSE - ageYears) / fertSpan,
              wealth     = BuildingForHome.wealthLevel(actor),
              chanceRng  = MAX_PREG_CHANCE - MIN_PREG_CHANCE,
              chanceW    = MAX_PREG_CHANCE - (wealth * chanceRng),
              pregChance = fertility * chanceW / 100
            ;
            if (Rand.num() < pregChance) {
              beginPregnancy();
              //I.say(this+" BECAME PREGNANT!  TIME TO TERM: "+(PREGNANCY_LENGTH+map.time()));
            }
          }
        }
      }
      
      
      public void completePregnancy(Building venue, boolean onMap) {
        
        ActorAsPerson child  = (ActorAsPerson) type().childType().generate();
        ActorAsPerson father = (ActorAsPerson) traits.bondedWith(BOND_MARRIED);
        setBond(actor , child, BOND_PARENT, BOND_CHILD, 0.5f);
        setBond(father, child, BOND_PARENT, BOND_CHILD, 0.5f);
        pregnancy = 0;
        
        if (onMap) {
          AreaTile at = venue.at();
          child.enterMap(map, at.x, at.y, 1, base());
          child.setInside(venue, true);
          venue.setResident(child, true);
        }
        
        //I.say(this+" GAVE BIRTH TO "+child);
      }
      
      
      public float growLevel() {
        return Nums.min(1, ageYears() / AVG_MARRIED);
      }
      
      
      public boolean child() {
        return ageYears() < AVG_PUBERTY;
      }
      
      
      public boolean senior() {
        return ageYears() > AVG_RETIREMENT;
      }
      
      
      public boolean fertile() {
        return ageYears() > AVG_MARRIED && ageYears() < AVG_MENOPAUSE;
      }
      
      
      public boolean adult() {
        return ! (child() || senior());
      }
      
      
      public boolean man() {
        return (sexData & SEX_MALE) != 0;
      }
      
      
      public boolean woman() {
        return (sexData & SEX_FEMALE) != 0;
      }
    };
  }
  
  
  
  /**  Graphical methods...
    */
  //  TODO:  Restore this!
  /*
  private static Composite faceComposite(Human c) {
    
    final String key = ""+c.hashCode();
    final Composite cached = Composite.fromCache(key);
    if (cached != null) return cached;
    
    final boolean report = mediaVerbose && I.talkAbout == c;
    if (report) {
      I.say("\nGetting new face composite for "+c+" (key "+key+")");
    }
    
    final int PS = SelectionPane.PORTRAIT_SIZE;
    final Composite composite = Composite.withSize(PS, PS, key);
    composite.layer(PORTRAIT_BASE);
    
    final int bloodID = raceID(c);
    final boolean male = c.traits.male();
    final int ageStage = c.health.agingStage();
    
    if (report) {
      I.say("  Blood/male/age-stage: "+bloodID+" "+male+" "+ageStage);
    }
    
    int faceOff[], bloodOff[] = RACE_FACE_OFFSETS[bloodID];
    if (ageStage == 0) {
      faceOff = CHILD_FACE_OFF;
    }
    else {
      int looks = (int) c.traits.traitLevel(HANDSOME) + 2 - ageStage;
      if (looks > 0) faceOff = male ? M_HOT_FACE_OFF : F_HOT_FACE_OFF;
      else if (looks == 0) faceOff = male ? M_AVG_FACE_OFF : F_AVG_FACE_OFF;
      else faceOff = ELDER_FACE_OFF;
    }
    
    final int UV[] = new int[] {
      0 + (faceOff[0] + bloodOff[0]),
      5 - (faceOff[1] + bloodOff[1])
    };
    composite.layerFromGrid(BASE_FACES, UV[0], UV[1], 6, 6);
    
    if (ageStage > ActorHealth.AGE_JUVENILE) {
      final int hairGene = c.traits.geneValue("hair", 4);
      
      int hairID = BLACK_HAIR_INDEX;
      if (hairGene > RACE_TONE_SHADES[bloodID]) {
        hairID = BLACK_HAIR_INDEX - hairGene;
      }
      if (ageStage >= ActorHealth.AGE_SENIOR && hairGene > 1) {
        hairID = WHITE_HAIR_INDEX;
      }
      int fringeOff[] = (male ? M_HAIR_OFF : F_HAIR_OFF)[hairID];
      composite.layerFromGrid(BASE_FACES, fringeOff[0], fringeOff[1], 6, 6);
      
      ImageAsset portrait = c.career.vocation().portraitFor(c);
      if (portrait == null) portrait = c.career.birth().portraitFor(c);
      composite.layerFromGrid(portrait, 0, 0, 1, 1);
    }
    
    return composite;
  }
  //*/
}












