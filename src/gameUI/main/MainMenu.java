/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.main;
import start.*;
import game.*;
import gameUI.misc.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;
import static util.Description.*;




public class MainMenu extends MenuPane implements UIConstants {
  
  final static ImageAsset
    START_BANNER = ImageAsset.fromImage(
      MainMenu.class, "start_banner", "media/Help/start_banner.png"
    );
  
  
  final MainScreen screen;
  
  
  public MainMenu(MainScreen UI) {
    super(UI, MainScreen.MENU_INIT);
    this.screen = UI;
  }
  
  
  protected void fillListing(List <UINode> listing) {
    
    final Image banner = new Image(UI, START_BANNER);
    banner.stretch = false;
    banner.expandToTexSize(1, false);
    listing.add(banner);
    
    listing.add(createTextButton("  New Game", 1, new Link() {
      public void whenClicked(Object context) { enterNewGameFlow(); }
    }));
    
    listing.add(createTextButton("  Tutorial", 1, new Link() {
      public void whenClicked(Object context) { enterTutorial(); }
    }));
    
    listing.add(createTextButton("  Continue Game", 1, new Link() {
      public void whenClicked(Object context) { enterSavesList(); }
    }));
    
    listing.add(createTextButton("  Info & Credits", 1, new Link() {
      public void whenClicked(Object context) { enterCredits(); }
    }));
    
    listing.add(createTextButton("  Quit", 1, new Link() {
      public void whenClicked(Object context) { enterQuitFlow(); }
    }));
  }
  
  
  public void enterNewGameFlow() {
    World world = MainGame.currentWorld();
    final SelectSitePane sitePane = new SelectSitePane(UI);
    sitePane.assignWorld(world);
    ChartUtils.updateWorldsCarousel(screen.worldsDisplay, screen, world);
    navigateForward(sitePane, true);
  }
  
  
  public void enterTutorial() {
    //  TODO:  Restore this...
    //final TutorialScenario tutorial = new TutorialScenario("tutorial_quick");
    //PlayLoop.setupAndLoop(tutorial);
  }
  
  
  public void enterSavesList() {
    final SavesListPane savesPane = new SavesListPane(UI);
    navigateForward(savesPane, true);
  }
  
  
  public void enterCredits() {
    final MenuPane creditsPane = new MenuPane(UI, MainScreen.MENU_CREDITS) {
      
      protected void fillListing(List <UINode> listing) {
        final String text = XML.load(
          "media/Help/GameCredits.xml"
        ).matchChildValue("name", "Credits").child("content").content();
        
        listing.add(createTextItem("Credits:", 1.2f, null, 1));
        listing.add(createTextItem(text, 0.75f, Colour.LITE_GREY, 0));
      }
    };
    navigateForward(creditsPane, true);
  }
  
  
  public void enterQuitFlow() {
    final MenuPane confirmPane = new MenuPane(UI, MainScreen.MENU_QUIT) {
      
      protected void fillListing(List <UINode> listing) {
        listing.add(createTextItem(
          "Are you sure you want to quit?", 1.0f, null, 1
        ));
        listing.add(createTextButton("  Just quit already", 1, new Link() {
          public void whenClicked(Object context) {
            PlayLoop.exitLoop();
          }
        }));
        listing.add(createTextButton("  Maybe not", 1, new Link() {
          public void whenClicked(Object context) {
            navigateBack();
          }
        }));
      }
    };
    navigateForward(confirmPane, true);
  }
}




