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
    
    for (final WorldLocale homeworld : world.locales()) {
      if (! homeworld.homeland()) continue;
      listing.add(new TextButton(UI, "  "+homeworld.name(), 1) {
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
    
    for (final WorldLocale landing : world.locales()) {
      if (landing.homeland()) continue;
      listing.add(new TextButton(UI, "  "+landing.name(), 1) {
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
    
    final MainScreen screen = MainScreen.current();
    screen.display.showLabels   = true ;
    screen.display.showWeather  = false;
    screen.worldsDisplay.hidden = false;
    screen.crewDisplay.hidden   = true ;
    if (expedition.landing() == null) screen.display.spinAtRate(9, 0);
  }
  
  
  

  /**  Handling homeworld selection-
    */
  private void selectHomeworld(WorldLocale homeworld) {
    final MainScreen screen = MainScreen.current();
    screen.worldsDisplay.setSelection(homeworld);
    expedition.setHomeland(world.baseAt(homeworld));
    //homeworld.whenClicked(null);
  }
  
  
  private boolean hasHomeworld(WorldLocale world) {
    if (expedition.homeland() == null) return false;
    return expedition.homeland().locale == world;
  }
  
  

  /**  Handling landing selection-
    */
  private void selectLanding(WorldLocale landing) {
    final MainScreen screen = MainScreen.current();
    screen.display.setSelection(landing.name(), true);
    expedition.setLanding(landing);
    //landing.whenClicked(null);
  }
  
  
  private boolean hasLanding(WorldLocale landing) {
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
    MainScreen.current().clearInfoPane();
    super.navigateBack();
  }
}







