/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.misc;
import graphics.common.*;
import graphics.widgets.*;



public interface UIConstants {
  
  
  final public static String
    BUTTONS_PATH = "media/GUI/Buttons/",
    TABS_PATH    = "media/GUI/Tabs/",
    
    SECTORS_BUTTON_ID  = "sectors_button",
    SECTORS_PANE_ID    = "sectors_pane"  ,
    BUDGETS_BUTTON_ID  = "budgets_button",
    BUDGETS_PANE_ID    = "budgets_pane"  ,
    ROSTER_BUTTON_ID   = "roster_button" ,
    ROSTER_PANE_ID     = "roster_pane"   ,
    INSTALL_BUTTON_ID  = "install_button",
    INSTALL_PANE_ID    = "install_pane"  ,
    
    OPTIONS_BUTTON_ID  = "game_options_button",
    OPTIONS_PANE_ID    = "game_options_pane"  ,
    
    STRIKE_BUTTON_ID   = "strike_mission_button"  ,
    RECON_BUTTON_ID    = "recon_mission_button"   ,
    SECURITY_BUTTON_ID = "security_mission_button",
    CONTACT_BUTTON_ID  = "contact_mission_button" ,
    CLAIMING_BUTTON_ID = "claiming_mission_button";
  
  final public static Alphabet INFO_FONT = Alphabet.loadAlphabet(
    UIConstants.class, "info_font", "media/GUI/", "FontVerdana.xml"
  );
  
  
  final public static int
    BAR_BUTTON_SIZE = 40,
    BAR_SPACING     = 2 ,
    BAR_MAX_SLOTS   = 9 ;
  
  final public static float
    BIG_FONT_SIZE   = 1.0f ,
    SMALL_FONT_SIZE = 0.75f;
  final public static int
    MARGIN_SIZE    = 10 ,
    HEADER_HIGH    = 35 ;
  
  final public static int
    
    MINIMAP_WIDE      = 200,
    MINIMAP_HIGH      = 200,
    CHARTS_WIDE       = 500,
    CHART_INFO_WIDE   = 240,
    
    READOUT_HIGH      =  20,
    QUICKBAR_HIGH     =  65,
    PANEL_TAB_SIZE    =  65,
    PANEL_TABS_HIGH   =  65,
    
    MESSAGE_PANE_WIDE = 500,
    MESSAGE_PANE_HIGH = 200,
    OPT_BUTTON_SIZE   = 40 ,
    OPT_MARGIN        = 2  ,
    
    INFO_PANEL_WIDE   = 300,
    SCROLLBAR_WIDE    =  15,
    DEFAULT_MARGIN    =  10,
    MIN_WIDGET_SIZE   =  20;
}









