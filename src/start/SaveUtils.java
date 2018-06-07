/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package start;
import game.*;
import util.*;
import graphics.common.*;
import graphics.widgets.*;
import java.io.File;



public class SaveUtils {

  final static String
    SAVES_DIR    = "saves/",
    EXT          = ".str"  ,
    PAD_NAME     = "I"     ,
    DIVIDER      = "- "    ,
    DAYS_SEP     = ", "    ,
    DAY_LABEL    = "Day"   ,
    HOURS_LABEL  = "Hours"
  ;
  

  public static String fullSavePath(String prefix, String suffix) {
    if (suffix == null || prefix == null) {
      I.complain("MUST HAVE BOTH SUFFIX AND PREFIX");
    }
    return SAVES_DIR+prefix+suffix+EXT;
  }
  
  
  public static String inSavesFolder(String fullPath) {
    if (! fullPath.startsWith(SAVES_DIR)) return fullPath;
    return fullPath.substring(SAVES_DIR.length(), fullPath.length());
  }
  
  
  public static boolean saveExists(String saveFile) {
    if (saveFile == null) return false;
    saveFile = inSavesFolder(saveFile);
    final File file = new File(SAVES_DIR+saveFile);
    if (! file.exists()) return false;
    else return true;
  }
  
  
  public static String[] latestSaves() {
    final File savesDir = new File(SAVES_DIR);
    if (! savesDir.exists()) savesDir.mkdir();
    
    final List <String> latest = new List <String> ();
    for (File saved : savesDir.listFiles()) {
      final String name = saved.getName();
      if (! name.endsWith(EXT)) continue;
      
      String prefix = name.substring(0, name.length() - EXT.length());
      latest.add(prefix);
    }
    return latest.toArray(String.class);
  }
  
}



