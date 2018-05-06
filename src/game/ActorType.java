

package game;
import static game.GameConstants.*;
import util.*;




public class ActorType extends Type {
  
  
  String nameValues[][] = new String[0][0];
  
  
  public int moveSpeed  = AVG_MOVE_SPEED;
  public int moveMode   = MOVE_LAND;
  public int carryLimit = AVG_CARRY_LIMIT;
  
  public Terrain habitats[] = NO_HABITAT;
  public boolean predator   = false;
  public boolean organic    = true;
  public int     lifespan   = LIFESPAN_LENGTH;
  public Good    meatType   = null;
  public Type    nestType   = null;
  
  public int socialClass = CLASS_COMMON;
  public int genderRole  = SEX_EITHER;
  public int hireCost    = AVG_HIRE_COST;
  public Tally <Trait> initTraits = new Tally();
  
  public int classLevelXP = BASE_CLASS_XP;
  public Tally <Trait> coreSkills = new Tally();
  public ActorTechnique[] classTechniques = {};
  
  public boolean isPorter = false;
  public Tally <ActorType> crewTypes = new Tally();
  
  
  
  
  public ActorType(Class baseClass, String ID, int category, int socialClass) {
    super(baseClass, ID, category);
    this.socialClass = socialClass;
    this.mobile      = true;
  }
  
  
  public ActorType(Class baseClass, String ID, int category) {
    this(baseClass, ID, category, CLASS_COMMON);
  }
  
  
  public void initAsMigrant(Actor a) {
    
    float age = Rand.range(AVG_MARRIED, AVG_MENOPAUSE);
    age += Rand.num() - 0.5f;
    
    int sex = genderRole;
    if (sex == SEX_EITHER) {
      sex = Rand.yes() ? SEX_FEMALE : SEX_MALE;
    }
    
    a.health.setAgeYears(age);
    a.health.setSexData(sex);
    a.health.setHunger(Rand.num() - 0.5f);
    
    
    //  TODO:  Vary this more, especially per class...
    for (Trait t : ALL_ATTRIBUTES) {
      float value = MAX_SKILL_LEVEL / 2f;
      a.traits.setLevel(t, value);
    }
    
    for (Trait t : ALL_SKILLS) {
      float value = initTraits.valueFor(t) * 1f;
      if (value > 0) value += Rand.index(INIT_SKILL_RANGE + 1);
      a.traits.setLevel(t, value);
    }
    
    for (Trait t : ALL_PERSONALITY) {
      float value = (initTraits.valueFor(t) - 50) / 50f;
      value += Rand.range(-1, 1) * INIT_PERS_RANGE / 100f;
      value = Nums.clamp(value, -1, 1);
      a.traits.setLevel(t, value);
    }
    
    a.traits.setClassLevel(1);
  }
  
  
  public void initAsAnimal(Actor a) {
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


