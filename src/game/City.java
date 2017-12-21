

package game;
import util.*;
import static game.GameConstants.*;



public class City implements Session.Saveable, Trader {
  
  
  /**  Data fields, construction and save/load methods-
    */
  public static enum GOVERNMENT {
    IMPERIAL, FEUDAL, BARBARIAN
  }
  public static enum POSTURE {
    ENEMY  ,
    ALLY   ,
    VASSAL ,
    LORD   ,
    NEUTRAL,
    TRADING,
  };
  final static float
    LOY_DEVOTED  =  1.0F,
    LOY_FRIENDLY =  0.5F,
    LOY_CIVIL    =  0.0F,
    LOY_STRAINED = -0.5F,
    LOY_NEMESIS  = -1.0F,
    LOYALTIES[]  = { 1f, 0.5f, 0, -0.5f, -1 }
  ;
  final static String LOYALTY_DESC[] = {
    "Devoted", "Friendly", "Civil", "Strained", "Nemesis"
  };
  final static float
    LOY_ATTACK_PENALTY  = -0.25f,
    LOY_CONQUER_PENALTY = -0.50f,
    LOY_REBEL_PENALTY   = -0.25f,
    LOY_TRIBUTE_BONUS   =  0.05f,
    LOY_FADEOUT_TIME    =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2,
    
    PRESTIGE_MAX        =  100,
    PRESTIGE_AVG        =  50,
    PRESTIGE_MIN        =  0,
    
    PRES_VICTORY_GAIN   =  25,
    PRES_DEFEAT_LOSS    = -15,
    PRES_REBEL_LOSS     = -10,
    PRES_FADEOUT_TIME   =  AVG_TRIBUTE_YEARS * YEAR_LENGTH * 2
  ;
  
  static class Relation {
    City    with;
    POSTURE posture;
    float   loyalty;
    
    int madeVassalDate = -1;
    int lastRebelDate  = -1;
    
    Tally <Good> suppliesDue  = new Tally();
    Tally <Good> suppliesSent = new Tally();
  }
  
  String name = "City";
  int tint = CITY_COLOR;
  
  World world;
  float mapX, mapY;
  
  GOVERNMENT government = GOVERNMENT.FEUDAL;
  float prestige = PRESTIGE_AVG;
  CityCouncil council = new CityCouncil(this);
  Table <City, Integer > distances = new Table();
  Table <City, Relation> relations = new Table();
  
  int   currentFunds = 0;
  float population   = 0;
  float armyPower    = 0;
  Tally <Good> tradeLevel = new Tally();
  Tally <Good> inventory  = new Tally();
  Tally <Type> buildLevel = new Tally();
  
  List <Formation> formations = new List();
  
  boolean active;
  CityMap map;
  
  Tally <Type> makeTotals = new Tally();
  Tally <Type> usedTotals = new Tally();
  
  
  
  City(World world) {
    this.world = world;
  }
  
  
  public City(Session s) throws Exception {
    s.cacheInstance(this);
    
    name = s.loadString();
    
    world = (World) s.loadObject();
    mapX  = s.loadFloat();
    mapY  = s.loadFloat();
    
    government = GOVERNMENT.values()[s.loadInt()];
    prestige = s.loadFloat();
    council.loadState(s);
    for (int n = s.loadInt(); n-- > 0;) {
      distances.put((City) s.loadObject(), s.loadInt());
    }
    for (int n = s.loadInt(); n-- > 0;) {
      Relation r = new Relation();
      r.with    = (City) s.loadObject();
      r.posture = POSTURE.values()[s.loadInt()];
      r.loyalty = s.loadFloat();
      r.madeVassalDate = s.loadInt();
      r.lastRebelDate  = s.loadInt();
      s.loadTally(r.suppliesDue );
      s.loadTally(r.suppliesSent);
      relations.put(r.with, r);
    }
    
    currentFunds = s.loadInt();
    population   = s.loadFloat();
    armyPower    = s.loadFloat();
    s.loadTally(tradeLevel);
    s.loadTally(inventory );
    s.loadTally(buildLevel);
    
    s.loadObjects(formations);
    
    active = s.loadBool();
    map    = (CityMap) s.loadObject();
    
    s.loadTally(makeTotals);
    s.loadTally(usedTotals);
  }
  
  
  public void saveState(Session s) throws Exception {
    
    s.saveString(name);
    
    s.saveObject(world);
    s.saveFloat(mapX);
    s.saveFloat(mapY);
    
    s.saveInt(government.ordinal());
    s.saveFloat(prestige);
    council.saveState(s);
    s.saveInt(distances.size());
    for (City c : distances.keySet()) {
      s.saveObject(c);
      s.saveInt(distances.get(c));
    }
    s.saveInt(relations.size());
    for (Relation r : relations.values()) {
      s.saveObject(r.with);
      s.saveInt(r.posture.ordinal());
      s.saveFloat(r.loyalty);
      s.saveInt(r.madeVassalDate);
      s.saveInt(r.lastRebelDate );
      s.saveTally(r.suppliesDue );
      s.saveTally(r.suppliesSent);
    }
    
    s.saveInt(currentFunds);
    s.saveFloat(population);
    s.saveFloat(armyPower );
    s.saveTally(tradeLevel);
    s.saveTally(inventory );
    s.saveTally(buildLevel);
    
    s.saveObjects(formations);
    
    s.saveBool(active);
    s.saveObject(map);
    
    s.saveTally(makeTotals);
    s.saveTally(usedTotals);
  }
  
  
  
  /**  Supplemental setup/query methods for economy, trade and geography-
    */
  static void setupRoute(City a, City b, int distance) {
    a.distances.put(b, distance);
    b.distances.put(a, distance);
  }
  
  
  void setWorldCoords(float mapX, float mapY) {
    this.mapX = mapX;
    this.mapY = mapY;
  }
  
  
  void attachMap(CityMap map) {
    this.map    = map ;
    this.active = true;
  }
  
  
  void initBuildLevels(Object... buildLevelArgs) {
    this.buildLevel.setWith(buildLevelArgs);
    this.population = buildLevel.valueFor(HOUSE   ) * AVG_HOUSE_POP;
    this.armyPower  = buildLevel.valueFor(GARRISON) * AVG_ARMY_POWER;
  }
  
  
  float distance(City other) {
    Integer dist = distances.get(other);
    return dist == null ? -1 : (float) dist;
  }
  
  
  
  /**  Setting up basic relations-
    */
  Relation relationWith(City other) {
    if (other == null) return null;
    Relation r = relations.get(other);
    if (r == null) {
      relations.put(other, r = new Relation());
      r.with    = other;
      r.posture = POSTURE.NEUTRAL;
      r.loyalty = LOY_CIVIL;
    }
    return r;
  }
  
  
  POSTURE posture(City other) {
    if (other == null) return null;
    return relationWith(other).posture;
  }
  
  
  float loyalty(City other) {
    if (other == null) return -100;
    return relationWith(other).loyalty;
  }
  
  
  static void setPosture(City a, City b, POSTURE p, boolean symmetric) {
    if (p == null) p = POSTURE.NEUTRAL;
    //
    //  You cannot have more than one Lord at a time, so break relations with
    //  any former master-
    if (p == POSTURE.LORD) {
      City formerLord = a.currentLord();
      if (formerLord == b) return;
      if (formerLord != null) setPosture(a, formerLord, POSTURE.NEUTRAL, true);
      a.relationWith(b).madeVassalDate = a.world.time;
    }
    
    a.relationWith(b).posture = p;
    
    //
    //  If you're enforcing symmetry, make sure the appropriate posture is
    //  reflected in the other city-
    if (symmetric) {
      POSTURE reverse = POSTURE.NEUTRAL;
      if (p == POSTURE.VASSAL) reverse = POSTURE.LORD  ;
      if (p == POSTURE.LORD  ) reverse = POSTURE.VASSAL;
      if (p == POSTURE.ALLY  ) reverse = POSTURE.ALLY  ;
      if (p == POSTURE.ENEMY ) reverse = POSTURE.ENEMY ;
      setPosture(b, a, reverse, false);
    }
  }
  
  
  void toggleRebellion(City lord, boolean is) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD) return;
    
    if (is) {
      r.lastRebelDate = world.time;
      r.suppliesSent.clear();
      incLoyalty(lord, this, LOY_REBEL_PENALTY / 2);
    }
    else {
      r.lastRebelDate = -1;
    }
  }
  
  
  boolean isVassalOf(City o) { return posture(o) == POSTURE.LORD  ; }
  boolean isLordOf  (City o) { return posture(o) == POSTURE.VASSAL; }
  boolean isEnemyOf (City o) { return posture(o) == POSTURE.ENEMY ; }
  boolean isAllyOf  (City o) { return posture(o) == POSTURE.ALLY  ; }
  
  
  //  TODO:  Reconsider the 'static' tags here?
  
  
  static void incLoyalty(City a, City b, float inc) {
    Relation r = a.relationWith(b);
    float loyalty = Nums.clamp(r.loyalty + inc, -1, 1);
    r.loyalty = loyalty;
  }
  
  
  static void incPrestige(City c, float inc) {
    c.prestige = Nums.clamp(c.prestige + inc, PRESTIGE_MIN, PRESTIGE_MAX);
  }
  
  
  City currentLord() {
    for (Relation r : relations.values()) {
      if (r.posture == POSTURE.LORD) return r.with;
    }
    return null;
  }
  
  
  City capitalLord() {
    City c = this;
    while (true) {
      City l = c.currentLord();
      if (l == null) return c;
      else c = l;
    }
  }
  
  
  boolean isVassalOfSameLord(City o) {
    City lord = capitalLord();
    return lord != null && o.capitalLord() == lord;
  }
  
  
  boolean isLoyalVassalOf(City o) {
    Relation r = relationWith(o);
    if (r == null || r.posture != POSTURE.LORD) return false;
    return r.lastRebelDate == -1;
  }
  
  
  Batch <City> vassalsInRevolt() {
    Batch <City> all = new Batch();
    for (Relation r : relations.values()) {
      if (r.with.yearsSinceRevolt(this) > 0) all.add(r.with);
    }
    return all;
  }
  
  
  float yearsSinceRevolt(City lord) {
    Relation r = relationWith(lord);
    if (r.posture != POSTURE.LORD         ) return -1;
    if (r == null || r.lastRebelDate == -1) return -1;
    return (world.time - r.lastRebelDate) * 1f / YEAR_LENGTH;
  }
  
  
  
  /**  Setting and accessing tribute and trade levels-
    */
  static void setSuppliesDue(City a, City b, Tally <Good> suppliesDue) {
    if (suppliesDue == null) suppliesDue = new Tally();
    Relation r = a.relationWith(b);
    r.suppliesDue = suppliesDue;
  }
  
  
  static float goodsSent(City a, City b, Good g) {
    Relation r = a.relationWith(b);
    return r == null ? 0 : r.suppliesSent.valueFor(g);
  }
  
  
  static float suppliesDue(City a, City b, Good g) {
    Relation r = a.relationWith(b);
    return r == null ? 0 : r.suppliesDue.valueFor(g);
  }
  
  
  public Tally <Good> tradeLevel() { return tradeLevel; }
  public Tally <Good> inventory () { return inventory ; }
  public City homeCity() { return this; }
  
  
  
  /**  Regular updates-
    */
  void updateCity() {
    boolean  updateStats = world.time % MONTH_LENGTH == 0;
    boolean  activeMap   = map != null;
    City     lord        = currentLord();
    //
    //  Local player-owned cities (i.e, with their own map), must derive their
    //  vitual statistics from that small-scale city map:
    if (updateStats && activeMap) {
      council.updateCouncil(true);
      
      int citizens = 0;
      for (Actor a : map.actors) if (a.homeCity == this) {
        citizens += 1;
      }
      population = citizens * POP_PER_CITIZEN;
      
      float armyPower = 0;
      for (Building b : map.buildings) {
        if (b.type.category == Type.IS_ARMY_BLD) {
          armyPower += Formation.powerSum(b.recruits(), map);
        }
      }
      this.armyPower = armyPower;
      
      //I.say("POWER OF "+this+" IS "+armyPower);
      
      inventory.clear();
      buildLevel.clear();
      
      for (Building b : map.buildings) {
        if (b.type.category == Type.IS_TRADE_BLD) {
          BuildingForTrade post = (BuildingForTrade) b;
          for (Good g : post.inventory.keys()) {
            inventory.add(post.inventory.valueFor(g), g);
          }
        }
        buildLevel.add(1, b.type);
      }
    }
    //
    //  Foreign off-map cities must update their internal ratings somewhat
    //  differently-
    if (updateStats && ! activeMap) {
      council.updateCouncil(false);
      
      float popRegen  = MONTH_LENGTH * 1f / LIFESPAN_LENGTH;
      float usageInc  = MONTH_LENGTH * 1f / YEAR_LENGTH;
      float idealPop  = buildLevel.valueFor(HOUSE   ) * AVG_HOUSE_POP ;
      float idealArmy = buildLevel.valueFor(GARRISON) * AVG_ARMY_POWER;
      
      if (population < idealPop) {
        population = Nums.min(idealPop , population + popRegen);
      }
      for (Formation f : formations) {
        idealArmy -= f.powerSum();
      }
      if (idealArmy < 0) idealArmy = 0;
      
      if (armyPower < idealArmy) {
        armyPower = Nums.min(idealArmy, armyPower + popRegen);
      }
      
      //I.say("POWER OF "+this+" IS "+armyPower);
      
      for (Good g : tradeLevel.keys()) {
        float demand = tradeLevel.valueFor(g);
        float supply = 0 - demand;
        float amount = inventory .valueFor(g);
        if (demand > 0) amount -= usageInc * demand;
        if (supply > 0) amount += usageInc * supply;
        inventory.set(g, Nums.clamp(amount, 0, Nums.abs(demand)));
      }
      
      if (isLoyalVassalOf(lord)) {
        if (council.considerRevolt(lord, MONTH_LENGTH)) {
          toggleRebellion(lord, true);
        }
        //  TODO:  You may have to generate caravans to visit player cities...
        else if (lord.map == null) {
          Relation r = relationWith(lord);
          for (Good g : r.suppliesDue.keys()) {
            float sent = r.suppliesDue.valueFor(g) * usageInc * 1.1f;
            r.suppliesSent.add(sent, g);
          }
        }
      }
    }
    //
    //  Either way, we allow prestige and loyalty to return gradually to
    //  defaults over time:
    if (updateStats) {
      float presDrift = MONTH_LENGTH * PRESTIGE_AVG * 1f / PRES_FADEOUT_TIME;
      float presDiff = PRESTIGE_AVG - prestige;
      if (Nums.abs(presDiff) > presDrift) {
        presDiff = presDrift * (presDiff > 0 ? 1 : -1);
      }
      prestige += presDiff;
      
      for (Relation r : relations.values()) {
        float diff = LOY_CIVIL - r.loyalty;
        diff *= MONTH_LENGTH * 1f / LOY_FADEOUT_TIME;
        r.loyalty += diff;
      }
    }
    //
    //  And, once per year, tally up any supply obligations to your current
    //  lord (with the possibility of entering a state of revolt if those are
    //  failed.)
    if (world.time % YEAR_LENGTH == 0) {
      if (isLoyalVassalOf(lord)) {
        Relation r = relationWith(lord);
        int timeAsVassal = world.time - r.madeVassalDate;
        boolean failedSupply = false, doCheck = timeAsVassal >= YEAR_LENGTH;
        
        if (doCheck) for (Good g : r.suppliesDue.keys()) {
          float sent = r.suppliesSent.valueFor(g);
          float due  = r.suppliesDue .valueFor(g);
          if (sent < due) failedSupply = true;
        }
        r.suppliesSent.clear();
        
        if (failedSupply) {
          toggleRebellion(lord, true);
        }
        else {
          incLoyalty(lord, this, LOY_TRIBUTE_BONUS);
        }
      }
      //
      //  Wipe the slate clean for all other relations:
      for (Relation r : relations.values()) if (r.with != lord) {
        r.suppliesSent.clear();
      }
      //
      //  And, finally, lose prestige based on any vassals in recent revolt-
      //  and if the time gone exceeds a certain threshold, end vassal status.
      for (City revolts : vassalsInRevolt()) {
        float years = revolts.yearsSinceRevolt(this);
        float maxT = AVG_TRIBUTE_YEARS, timeMult = (maxT - years) / maxT;
        
        if (years < AVG_TRIBUTE_YEARS) {
          incPrestige(this, PRES_REBEL_LOSS * timeMult);
          incLoyalty(this, revolts, LOY_REBEL_PENALTY * 0.5f * timeMult);
        }
        else {
          setPosture(this, revolts, POSTURE.ENEMY, true);
        }
      }
    }
    //
    //  And update any formations active-
    for (Formation f : formations) {
      f.update();
    }
  }
  
  
  
  /**  Graphical, debug and interface methods-
    */
  static String descLoyalty(float l) {
    Pick <String> pick = new Pick();
    for (int i = LOYALTIES.length; i-- > 0;) {
      float dist = Nums.abs(l - LOYALTIES[i]);
      pick.compare(LOYALTY_DESC[i], 0 - dist);
    }
    return pick.result();
  }
  
  
  public String toString() {
    return name;
  }
}









