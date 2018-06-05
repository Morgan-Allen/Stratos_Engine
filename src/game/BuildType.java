

package game;
import static game.GameConstants.*;
import graphics.common.*;
import util.*;



public class BuildType extends Type {
  
  
  public ModelAsset foundationModel = null;
  
  
  public boolean isUpgrade = false;
  public int entranceDir = Building.FACE_EAST;
  public Coord[] dockPoints = {};
  
  public ActorTechnique rulerPowers[] = {};
  public ActorTechnique actorPowers[] = {};
  
  public int featureAmount = AVG_SERVICE_GIVE;
  public int updateTime    = AVG_UPDATE_GAP;
  public int maxVisitors   = AVG_MAX_VISITORS;
  public int serviceCharge = 0;
  
  public int maxUpgrades = 0;
  public BuildType needsToBuild  [] = NO_PREREQS;
  public BuildType opposites     [] = NO_PREREQS;
  public BuildType upgradeTiers  [] = NO_TIERS;
  public BuildType allUpgrades   [] = NO_UPGRADES;
  public BuildType needsAsUpgrade[] = NO_UPGRADES;
  public ActorType vesselTemplate = null;
  
  public int residentClasses[] = {};
  public int maxResidents      = 0;
  public int homeComfortLevel  = 0;
  public int homeAmbienceNeed  = AMBIENCE_MIN;
  public int homeUseTime       = HOME_USE_TIME;
  public Tally <Good> homeUseGoods = new Tally();
  public Tally <Type> serviceNeeds = new Tally();
  
  public Good   buildsWith[] = NO_GOODS;
  public Recipe recipes   [] = {};
  public Good   shopItems [] = NO_GOODS;
  public Good   needed    [] = NO_GOODS;
  public Good   produced  [] = NO_GOODS;
  public int    maxStock     = AVG_MAX_STOCK;
  
  public Tally <ActorType> workerTypes = new Tally();
  int nestSpawnInterval = DAY_LENGTH;
  
  public Type gatherFlag = null;
  public int claimMargin = -1;
  public int gatherRange = AVG_GATHER_RANGE;
  public int maxDeliverRange = MAX_TRADER_RANGE;

  

  
  
  public BuildType(Class baseClass, String ID, int category) {
    super(baseClass, ID, category);
  }
  
  
  public void initFromTemplate(BuildType other) {
    
    this.model = other.model;
    this.modelVariants = other.modelVariants;
    this.name = other.name;
    this.tint = other.tint;
    
    this.setDimensions(other.wide, other.high, other.deep, other.clearMargin);
    this.claimMargin = other.claimMargin;
    this.builtFrom   = other.builtFrom;
    this.builtAmount = other.builtAmount;
    this.maxHealth   = other.maxHealth;
    this.armourClass = other.armourClass;
  }


  public void setUpgradeTiers(BuildType... tiers) {
    this.upgradeTiers = tiers;
  }
  
  
  public void setAllUpgrades(BuildType... upgrades) {
    this.allUpgrades = upgrades;
  }
  
  
  public void setFeatures(Good... features) {
    this.features = features;
  }
  
  
  public void setNeedsToBuild(BuildType... needs) {
    this.needsToBuild = needs;
  }
  
  

  public boolean rulerCanBuild(Base ruler, Area map) {
    if (! super.rulerCanBuild(ruler, map)) return false;
    
    //  TODO:  This will be too slow on large maps.  Use build-levels instead.
    
    boolean hasNeeds[] = new boolean[needsToBuild.length];
    boolean hasOpp = false;
    
    for (Building b : map.buildings()) {
      int i = 0;
      for (BuildType n : needsToBuild) {
        if (n == b.type()) hasNeeds[i] = true;
        i++;
      }
      if (Visit.arrayIncludes(opposites, b.type())) {
        hasOpp = true;
        break;
      }
    }
    
    for (boolean b : hasNeeds) if (! b) return false;
    if (hasOpp) return false;
    
    return true;
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




