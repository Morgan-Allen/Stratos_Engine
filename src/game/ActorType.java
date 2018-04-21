

package game;
import static game.GameConstants.*;
import util.*;



public class ActorType extends Type {
  
  
  String nameValues[][] = new String[0][0];
  
  public int  socialClass  = CLASS_COMMON;
  public int  genderRole   = SEX_EITHER;
  public int  hireCost     = AVG_HIRE_COST;
  public Type patronGods[] = null;
  
  public boolean isPorter = false;
  public Tally <ActorType> crewTypes = new Tally();
  
  public int moveSpeed = AVG_MOVE_SPEED;
  public int moveMode  = MOVE_LAND;
  
  public Tally <Trait> initTraits = new Tally();
  
  public Terrain habitats[] = NO_HABITAT;
  public boolean predator   = false;
  public boolean organic    = true;
  public int     lifespan   = LIFESPAN_LENGTH;
  public Good    meatType   = null;
  public Type    nestType   = null;
  
  
  public ActorTechnique[] classTechniques = {};
  
  public ActorType(Class baseClass, String ID, int category, int socialClass) {
    super(baseClass, ID, category);
    this.socialClass = socialClass;
    this.mobile      = true;
  }
  
  public ActorType(Class baseClass, String ID, int category) {
    this(baseClass, ID, category, CLASS_COMMON);
  }
  
  
  public void initAsMigrant(ActorAsPerson a) {
    
    float age = Rand.range(AVG_MARRIED, AVG_MENOPAUSE);
    age += Rand.num() - 0.5f;
    
    int sex = genderRole;
    if (sex == SEX_EITHER) {
      sex = Rand.yes() ? SEX_FEMALE : SEX_MALE;
    }
    
    a.health.setAgeYears(age);
    a.health.setSexData(sex);
    a.health.setHunger(Rand.num() - 0.5f);
    
    for (Trait t : initTraits.keys()) {
      a.traits.setLevel(t, initTraits.valueFor(t));
    }
    
    for (Trait t : ALL_PERSONALITY) {
      a.traits.setLevel(t, Rand.range(-1, 1));
    }
    
    ///I.say("INITIAL LEVELS: "+a.levels);
  }
  
  
  public void initAsAnimal(ActorAsAnimal a) {
    float age = Rand.num() * a.type().lifespan;
    a.health.setAgeYears(age / YEAR_LENGTH);
    a.health.setHunger(Rand.num() - 0.5f);
    return;
  }
  
  
  public Type childType() {
    return this;
  }
  
  
  public Type nestType() {
    return nestType;
  }
  
  
  public boolean isCommoner() { return socialClass == CLASS_COMMON ; }
  public boolean isSoldier () { return socialClass == CLASS_SOLDIER; }
  public boolean isNoble   () { return socialClass == CLASS_NOBLE  ; }
}


