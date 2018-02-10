

package game;
import static game.GameConstants.*;
import util.*;



public class BuildType extends Type {
  
  public Good homeFoods [] = {};
  public Good buildsWith[] = NO_GOODS;
  public boolean worksBeforeBuilt = false;
  
  public int homeSocialClass  = CLASS_COMMON;
  public int homeAmbienceNeed = AMBIENCE_MIN;
  
  public Good needed  [] = NO_GOODS;
  public Good produced[] = NO_GOODS;
  public Good canOrder[] = NO_GOODS;
  public Good features[] = NO_GOODS;
  public Type gatherFlag = null;
  public Trait craftSkill = null;
  
  public int updateTime      = AVG_UPDATE_GAP  ;
  public int craftTime       = AVG_CRAFT_TIME  ;
  public int gatherRange     = AVG_GATHER_RANGE;
  public int maxDeliverRange = MAX_TRADER_RANGE;
  public int maxStock        = AVG_MAX_STOCK   ;
  public int homeUseTime     = HOME_USE_TIME   ;
  public int featureAmount   = AVG_SERVICE_GIVE;
  
  public Tally <ActorType> workerTypes = new Tally();
  public int maxResidents = 0;
  public int maxVisitors  = AVG_MAX_VISITORS;
  public int maxRecruits  = AVG_ARMY_SIZE;
  
  
  
  //  TODO:  MERGE THESE IF YOU CAN
  public BuildType    upgradeTiers[] = NO_TIERS;
  public Tally <Type> upgradeNeeds = new Tally();
  public Tally <Good> homeUseGoods = new Tally();
  
  public boolean isUpgrade = false;
  public BuildType needsAsUpgrade[] = {};
  
  public Technique rulerPowers[] = {};
  public Technique actorPowers[] = {};
  
  
  public void setUpgradeTiers(BuildType... tiers) {
    this.upgradeTiers = tiers;
  }
  
  
  public void setFeatures(Good... features) {
    this.features = features;
  }
  
  
  public boolean hasFeature(Good feature) {
    return Visit.arrayIncludes(features, feature);
  }
  
  public BuildType(Class baseClass, String ID, int category) {
    super(baseClass, ID, category);
  }
  
  
  //  TODO:
  
  //  Bonuses to venue HP, upgrade-slots, recruit-slots, armour, etc.
  //  Items to make or sell
  //  Boost to skill use or manufacture-speed
  //  Techniques available to learn or as sovereign spells
  //  Max weapon/armour manufacturing level boost
  //  Other unique services (specific to project-custom venues.)
  
  //  I think most of that is already done or just requires some threading
  //  through upgrades to stack up FX or deepen requirements.
}



