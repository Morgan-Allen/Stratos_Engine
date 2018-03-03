

package game;
import static game.GameConstants.*;
import util.*;



public class BuildType extends Type {
  
  
  public boolean isUpgrade = false;
  public boolean worksBeforeBuilt = false;
  
  public Technique rulerPowers[] = {};
  public Technique actorPowers[] = {};
  
  public Good features[] = NO_GOODS;
  public int featureAmount = AVG_SERVICE_GIVE;
  public int updateTime = AVG_UPDATE_GAP;
  
  public int maxUpgrades = 0;
  public BuildType upgradeTiers[] = NO_TIERS;
  public BuildType allUpgrades[] = NO_UPGRADES;
  public BuildType needsAsUpgrade[] = {};
  
  public Good homeFoods[] = {};
  public int homeSocialClass  = CLASS_COMMON;
  public int homeAmbienceNeed = AMBIENCE_MIN;
  public int homeUseTime      = HOME_USE_TIME;
  public Tally <Good> homeUseGoods = new Tally();
  public Tally <Type> serviceNeeds = new Tally();
  
  public Good   buildsWith[] = NO_GOODS;
  public Recipe recipes   [] = {};
  public Good   shopItems [] = NO_GOODS;
  public Good   needed    [] = NO_GOODS;
  public Good   produced  [] = NO_GOODS;
  public int    maxStock     = AVG_MAX_STOCK;
  
  public Type gatherFlag = null;
  public int claimMargin = -1;
  public int gatherRange     = AVG_GATHER_RANGE;
  public int maxDeliverRange = MAX_TRADER_RANGE;
  
  public Tally <ActorType> workerTypes = new Tally();
  public int maxResidents = 0;
  public int maxVisitors  = AVG_MAX_VISITORS;
  public int maxRecruits  = AVG_ARMY_SIZE;
  
  
  public void setUpgradeTiers(BuildType... tiers) {
    this.upgradeTiers = tiers;
  }
  
  
  public void setAllUpgrades(BuildType... upgrades) {
    this.allUpgrades = upgrades;
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
  //  Boost to skill use or manufacture-speed
  //  Techniques available to learn or as sovereign spells
  //  Max weapon/armour manufacturing level boost
  //  Other unique services (specific to project-custom venues.)
  
  //  I think most of that is already done or just requires some threading
  //  through upgrades to stack up FX or deepen requirements.
}




