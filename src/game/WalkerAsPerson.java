

package game;
import util.*;
import static game.CityMap.*;
import static game.GameConstants.*;



public class WalkerAsPerson extends Walker {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public WalkerAsPerson(ObjectType type) {
    super(type);
  }
  
  
  public WalkerAsPerson(Session s) throws Exception {
    super(s);
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s);
  }
  

  
  /**  Aging, reproduction and life-cycle methods-
    */
  void updateAging() {
    super.updateAging();
    
    if (pregnancy > 0) {
      pregnancy += 1;
      if (pregnancy > PREGNANCY_LENGTH && home != null && inside == home) {
        
        float dieChance = AVG_CHILD_MORT / 100f;
        if (Rand.num() >= dieChance) {
          Tile at = home.at();
          Walker child = (Walker) CHILD.generate();
          child.enterMap(map, at.x, at.y, 1);
          child.inside = home;
          child.home   = home;
        }
        pregnancy = 0;
      }
      if (pregnancy > PREGNANCY_LENGTH + MONTH_LENGTH) {
        pregnancy = 0;
      }
    }
    
    if (ageSeconds % YEAR_LENGTH == 0) {
      if (senior() && Rand.index(100) < AVG_SENIOR_MORT) {
        setAsKilled("Old age");
      }
      
      if (woman() && fertile() && pregnancy == 0 && home != null) {
        float
          ageYears   = ageSeconds / (YEAR_LENGTH * 1f),
          fertSpan   = AVG_MENOPAUSE - AVG_MARRIED,
          fertility  = (AVG_MENOPAUSE - ageYears) / fertSpan,
          wealth     = BuildingForHome.wealthLevel(home),
          chanceRng  = MAX_PREG_CHANCE - MIN_PREG_CHANCE,
          chanceW    = MAX_PREG_CHANCE - (wealth * chanceRng),
          pregChance = fertility * chanceW / 100
        ;
        if (Rand.num() < pregChance) {
          pregnancy = 1;
        }
      }
    }
  }
  
  
  boolean child() {
    return ageYears() < AVG_PUBERTY;
  }
  
  
  boolean senior() {
    return ageYears() > AVG_RETIREMENT;
  }
  
  
  boolean fertile() {
    return ageYears() > AVG_MARRIED && ageYears() < AVG_MENOPAUSE;
  }
  
  
  boolean adult() {
    return ! (child() || senior());
  }
  
  
  boolean man() {
    return (sexData & SEX_MALE) != 0;
  }
  
  
  boolean woman() {
    return (sexData & SEX_FEMALE) != 0;
  }
  
}





