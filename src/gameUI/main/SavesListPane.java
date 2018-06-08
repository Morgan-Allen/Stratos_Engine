/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.main;
import start.*;
import graphics.widgets.*;
import util.*;



public class SavesListPane extends MenuPane {
  

  
  public SavesListPane(HUD UI) {
    super(UI, MainScreen.MENU_SAVES_LIST);
  }
  
  
  protected void fillListing(List <UINode> listing) {
    final String saves[] = SaveUtils.latestSaves();
    
    listing.add(createTextItem("Saved Games:", 1.2f, null, 1));
    
    for (final String path : saves) {
      
      listing.add(createTextButton("  "+path, 1, new Description.Link() {
        public void whenClicked(Object context) {
          String fullPath = SaveUtils.fullSavePath(path, "");
          MainGame.loadGameState(fullPath);
        }
      }));
    }
  }
  
}







