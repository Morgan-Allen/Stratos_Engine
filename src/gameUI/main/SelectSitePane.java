/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.main;
import game.*;
import gameUI.misc.*;
import graphics.common.*;
import graphics.widgets.*;
import start.MainGame;
import util.*;



public class SelectSitePane extends MenuPane {
  
  
  Expedition expedition;
  World world;
  
  
  public SelectSitePane(HUD UI) {
    super(UI, MainScreen.MENU_NEW_GAME_SITE);
  }
  
  
  public void assignWorld(World w) {
    this.world = w;
    this.expedition = new Expedition();
  }
  
  
  protected void fillListing(List <UINode> listing) {
    //
    //  Pick a homeworld first.
    listing.add(createTextItem("Homeworld:", 1.2f, null, 1));
    
    for (final Area homeworld : world.areas()) {
      if (! homeworld.type.homeland()) continue;
      listing.add(new TextButton(UI, "  "+homeworld.type.name(), 1) {
        protected void whenClicked() { selectHomeworld(homeworld); }
        protected boolean toggled() { return hasHomeworld(homeworld); }
      });
    }
    listing.add(createTextItem(
      "Your homeworld will determine the initial colonists and finance "+
      "available to your settlement, along with technical expertise and "+
      "trade revenue.", 0.75f, Colour.LITE_GREY, 4
    ));
    //
    //  Then pick a sector.
    listing.add(createTextItem("Landing Site:", 1.2f, null, 1));
    
    for (final Area landing : world.areas()) {
      if (landing.type.homeland()) continue;
      listing.add(new TextButton(UI, "  "+landing.type.name(), 1) {
        public void whenClicked() { selectLanding(landing); }
        protected boolean toggled() { return hasLanding(landing); }
      });
    }
    listing.add(createTextItem(
      "Your landing site will determine the type of resources initially "+
      "available to your settlement, along with local species and other "+
      "threats.", 0.75f, Colour.LITE_GREY, 4
    ));
    
    //
    //  And include an option to proceed further...
    listing.add(new TextButton(UI, "  Continue", 1) {
      protected void whenClicked() { pushNextPane(); }
      protected boolean enabled() { return canProgress(); }
    });
  }
  
  
  protected void updateState() {
    super.updateState();
    
    final MainScreen screen = MainGame.mainScreen();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = false;
    screen.crewDisplay.hidden   = true ;
    if (expedition.landing() == null) screen.display.spinAtRate(9, 0);
  }
  
  
  

  /**  Handling homeworld selection-
    */
  private void selectHomeworld(Area homeworld) {
    final MainScreen screen = MainGame.mainScreen();
    screen.worldsDisplay.setSelection(homeworld);
    expedition.setHomeland(world.basesFor(homeworld).first());
    //homeworld.whenClicked(null);
  }
  
  
  private boolean hasHomeworld(Area world) {
    if (expedition.homeland() == null) return false;
    return expedition.homeland().area == world;
  }
  
  

  /**  Handling landing selection-
    */
  private void selectLanding(Area landing) {
    final MainScreen screen = MainGame.mainScreen();
    screen.display.setSelection(landing.type.name(), true);
    expedition.setLanding(landing);
    //landing.whenClicked(null);
  }
  
  
  private boolean hasLanding(Area landing) {
    return expedition.landing() == landing;
  }
  
  
  
  /**  Other navigation tasks.
    */
  private boolean canProgress() {
    if (expedition.homeland() == null) return false;
    if (expedition.landing () == null) return false;
    return true;
  }
  
  
  private void pushNextPane() {
    //expedition.destination().whenClicked(null);
    //expedition.backing().configStartingExpedition(expedition);
    navigateForward(new SelectCrewPane(UI, expedition), true);
  }
  
  
  protected void navigateBack() {
    final MainScreen screen = MainGame.mainScreen();
    screen.clearInfoPane();
    screen.worldsDisplay.clearEntries();
    screen.crewDisplay.clearDisplay();
    
    super.navigateBack();
  }
}







