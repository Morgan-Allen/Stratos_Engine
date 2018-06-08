/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.main;
import start.*;
import content.*;
import static content.GameContent.*;
import game.*;
import static game.GameConstants.*;
import gameUI.misc.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class SelectCrewPane extends MenuPane {
  
  final Expedition expedition;
  
  private UINode
    advisHeader,
    colonHeader,
    colonFooter;
  private Text
    colonLabels[];
  
  
  public SelectCrewPane(HUD UI, Expedition expedition) {
    super(UI, MainScreen.MENU_NEW_GAME_CREW);
    this.expedition = expedition;
  }
  
  
  ActorType[] COLONIST_BACKGROUNDS() {
    return new ActorType[] {
      Trooper.TROOPER,
      GameContent.ECOLOGIST,
      GameContent.ENGINEER,
      GameContent.PHYSICIAN
    };
  }
  
  Good[] COLONIST_GOODS() {
    return new Good[] {
      GameContent.CARBS,
      GameContent.PROTEIN,
      GameContent.PARTS,
      GameContent.PLASTICS
    };
  }
  
  int maxFunds() {
    return 5000;
  }
  
  int maxColonists() {
    return 4;
  }
  
  
  protected void fillListing(List< UINode> listing) {

    listing.add(colonHeader = createTextItem("Colonists", 1.2f, null, 1));

    ActorType backgrounds[] = COLONIST_BACKGROUNDS();
    colonLabels = new Text[backgrounds.length];
    int labelIndex = 0;
    
    for (final ActorType b : backgrounds) {
      
      final UIGroup counter = new UIGroup(UI);
      final Text label = new Text(UI, UIConstants.INFO_FONT);
      colonLabels[labelIndex++] = label;
      label.scale = 0.75f;
      label.setText(b.name);
      label.setToLineSize();
      label.alignAcross(0, 0.5f);
      label.attachTo(counter);
      
      TextButton plus = new TextButton(UI, " + ", 1) {
        protected void whenClicked() { incColonists(b, 1); }
        protected boolean enabled() { return canIncColonists(b, 1); }
      };
      plus.alignAcross(0.5f, 0.65f);
      plus.attachTo(counter);
      
      TextButton minus = new TextButton(UI, " - ", 1) {
        protected void whenClicked() { incColonists(b, -1); }
        protected boolean enabled() { return canIncColonists(b, -1); }
      };
      minus.alignAcross(0.65f, 0.8f);
      minus.attachTo(counter);
      
      counter.alignTop(0, 15);
      listing.add(counter);
    }
    listing.add(colonFooter = createTextItem(
      "Colonists provide the backbone of your workforce, giving you a "+
      "headstart in establishing defences or trade.",
      0.75f, Colour.LITE_GREY, 3
    ));
    
    /*
    Good goods[] = COLONIST_GOODS();
    for (final Good g : goods) {
      
      final UIGroup counter = new UIGroup(UI);
      final Text label = new Text(UI, UIConstants.INFO_FONT);
      colonLabels[labelIndex++] = label;
      label.scale = 0.75f;
      label.setText(g.name);
      label.setToLineSize();
      label.alignAcross(0, 0.5f);
      label.attachTo(counter);
      
      TextButton plus = new TextButton(UI, " + ", 1) {
        protected void whenClicked() { incGoods(g, 1); }
        protected boolean enabled() { return canIncGoods(g, 1); }
      };
      plus.alignAcross(0.5f, 0.65f);
      plus.attachTo(counter);
      
      TextButton minus = new TextButton(UI, " - ", 1) {
        protected void whenClicked() { incGoods(g, -1); }
        protected boolean enabled() { return canIncGoods(g, -1); }
      };
      minus.alignAcross(0.65f, 0.8f);
      minus.attachTo(counter);
      
      counter.alignTop(0, 15);
      listing.add(counter);
    }
    //*/
    
    listing.add(new TextButton(UI, "  Begin Game", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
  }
  
  
  protected void updateState() {
    final MainScreen screen = MainGame.mainScreen();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = true ;
    screen.crewDisplay.hidden   = false;
    
    int numC = numColonists(), maxC = maxColonists();
    updateTextItem(colonHeader, "Colonists ("+numC+"/"+maxC+")", null);
    
    Type backgrounds[] = COLONIST_BACKGROUNDS();
    if (colonLabels != null) for (int i = colonLabels.length; i-- > 0;) {
      final Type b = backgrounds[i];
      final Text t = colonLabels[i];
      final int numM = numMigrants(b);
      final Colour tint = numM > 0 ? Text.LINK_COLOUR : Colour.LITE_GREY;
      t.setText("");
      t.append("  "+b.name+" ("+numM+")", tint);
    }
    
    screen.crewDisplay.setupFrom(expedition, true);
    
    super.updateState();
  }
  
  
  private void updateHelpFor(UINode footer, String helpText) {
    updateTextItem(footer, helpText, Colour.LITE_GREY);
  }
  
  
  
  /**  Handling colonist selection-
    */
  private void incColonists(ActorType b, int inc) {
    if (inc > 0) {
      Actor a = (Actor) b.generate();
      b.initAsMigrant(a);
      expedition.toggleStaff(a, true);
    }
    else {
      for (Actor a : expedition.staff()) if (a.type() == b) {
        expedition.toggleStaff(a, false);
        break;
      }
    }
    //updateHelpFor(colonFooter, b.info);
  }
  
  
  private boolean canIncColonists(Type b, int inc) {
    if (inc > 0) return numColonists() < maxColonists();
    else return numMigrants(b) > 0;
  }
  
  
  private int numMigrants(Type b) {
    int c = 0;
    for (Actor a : expedition.staff()) if (a.type() == b) c++;
    return c;
  }
  
  
  private int numColonists() {
    return expedition.staff().size();
  }
  
  
  /*
  private void incGoods(Good g, int inc) {
    expedition.goods().add(inc, g);
    expedition.incFunds(g.price * inc * -1);
  }
  
  
  private boolean canIncGoods(Good g, int inc) {
    int amount = (int) expedition.goods().valueFor(g);
    if (inc > 0 && expedition.funds() < g.price) return false;
    if (inc > 0 && amount > 20) return false;
    if (inc < 0 && amount <= 0) return false;
    return true;
  }
  //*/
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (numColonists() != maxColonists()) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    navigateToRoot();
    
    String savePath = LaunchWithGlobe.SAVE_PATH;
    final World world = MainGame.currentWorld();
    world.assignSavePath(savePath);
    
    WorldScenario hook = world.scenarioFor(expedition.landing());
    if (hook == null) return;
    
    //  TODO:  You need a separate pane to specify these items!
    if (true) {
      Tally <Good> goods = Tally.with(PARTS, 20, PLASTICS, 20, CARBS, 20);
      expedition.configAssets(
        GameWorld.FACTION_SETTLERS,
        5000, goods, GameContent.BASTION
      );
    }
    
    hook.assignExpedition(expedition);
    hook.initScenario(MainGame.mainGame());
    
    MainGame.playScenario(hook, world);
  }
  
  
  protected void navigateBack() {
    super.navigateBack();
  }
  
}





