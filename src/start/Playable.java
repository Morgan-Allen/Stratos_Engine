/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package start;
import graphics.common.*;
import graphics.widgets.*;



public interface Playable {
  
  void beginGameSetup();
  void updateGameState();
  void renderVisuals(Rendering rendering);
  HUD UI(boolean loading);
  
  boolean isLoading();
  float loadProgress();
  boolean shouldExitLoop();
  boolean wipeAssetsOnExit();
}
