

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import gameUI.misc.*;
import start.*;
import util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;



public class PlayUI extends HUD implements UIConstants {
  
  /**  Data fields, constructors, setup and save/load methods-
    */
  final static ImageAsset
    INSTALL_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_install_tab",
      "media/GUI/panels/installations_tab.png"
    ),
    DEFAULT_CURSOR = ImageAsset.fromImage(
      PlayUI.class, "img_default_cursor",
      "media/GUI/default_cursor.png"
    ),
    BLANK_CURSOR = ImageAsset.withColor(
      32, Colour.HIDE, PlayUI.class
    );
  
  Area    area;
  Base    base;
  Element home;
  
  Button     installTab ;
  DetailPane installPane;
  
  UIGroup    detailArea;
  DetailPane detailPane;
  OptionList optionList;
  Readout readout;
  ProgressOptions progressOptions;
  
  final public Selection selection = new Selection(this);
  final public Tracking  tracking  = new Tracking (this);
  
  private PlayTask currentTask = null;
  
  Cursor cursor;
  com.badlogic.gdx.graphics.Cursor cursorGDX = null;
  
  
  
  public PlayUI(Rendering rendering) {
    super(rendering);
    
    if (INSTALL_TAB_IMG != null) {
      installTab = new Button(
        this, "install_button", INSTALL_TAB_IMG, "Install Facilities"
      ) {
        protected void whenClicked() {
          if (installPane == detailPane) setDetailPane(null);
          else setDetailPane(installPane);
        }
      };
      installTab.alignLeft  (0, PANEL_TAB_SIZE );
      installTab.alignBottom(0, BAR_BUTTON_SIZE);
      installTab.attachTo(this);
    }
    
    installPane = new InstallPane(this);
    
    detailArea = new UIGroup(this);
    detailArea.alignLeft    (0, INFO_PANEL_WIDE);
    detailArea.alignVertical(BAR_BUTTON_SIZE, 0);
    detailArea.attachTo(this);
    
    optionList = new OptionList(this);
    optionList.attachTo(this);
    
    readout = new Readout(this);
    readout.alignHorizontal(INFO_PANEL_WIDE, 0);
    readout.alignTop(0, READOUT_HIGH);
    readout.attachTo(this);
    
    progressOptions = new ProgressOptions(this);
    progressOptions.alignVertical  (0, 0);
    progressOptions.alignHorizontal(0, 0);
    progressOptions.attachTo(this);
    
    cursor = new Cursor(this, DEFAULT_CURSOR);
    cursor.alignToArea(0, 0, CURSOR_SIZE, CURSOR_SIZE);
    cursor.attachTo(this);
  }
  
  
  public void assignParameters(Area stage, Base base) {
    if (stage == null || base == null) {
      I.complain("\nCANNOT ASSIGN NULL STAGE/BASE TO UI!");
      return;
    }
    this.area = stage;
    this.base  = base ;
    optionList.setupFrom(stage, base);
  }
  
  
  public void assignHomePoint(Element home) {
    if (home == null) {
      I.complain("\nCANNOT ASSIGN NULL HOME TO UI!");
      return;
    }
    this.home = home;
    rendering.view.lookedAt.setTo(home.trackPosition());
  }
  
  
  public void loadState(Session s) throws Exception {
    area = (Area) s.loadObject();
    base  = (Base   ) s.loadObject();
    home  = (Element) s.loadObject();
    selection.loadState(s);
    tracking .loadState(s);
    optionList.setupFrom(area, base);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(area);
    s.saveObject(base );
    s.saveObject(home );
    selection.saveState(s);
    tracking .saveState(s);
  }
  
  
  
  /**  Static methods for convenience-
    */
  public static void pushSelection(Selection.Focus clicked) {
    final PlayUI UI = MainGame.playUI();
    if (UI != null) UI.selection.presentSelectionPane(clicked);
  }
  
  
  public static Selection.Focus selectionFocus() {
    final PlayUI UI = MainGame.playUI();
    if (UI == null) return null;
    if (UI.detailPane != null && UI.detailPane.subject != null) {
      return UI.detailPane.subject;
    }
    return null;
  }
  
  
  public static Base playerBase() {
    final PlayUI UI = MainGame.playUI();
    if (UI == null) return null;
    return UI.base;
  }
  
  
  
  /**  Custom methods-
    */
  public void setDetailPane(DetailPane pane) {
    if (detailPane != null) detailPane.detach();
    this.detailPane = pane;
    if (detailPane != null) {
      detailPane.alignToFill();
      detailPane.attachTo(detailArea);
    }
  }
  
  
  public void assignTask(PlayTask task) {
    this.currentTask = task;
  }
  
  
  public PlayTask currentTask() {
    return currentTask;
  }
  
  
  
  
  /**  General method overrides-
    */
  public void updateInput() {
    super.updateInput();
    
    if (area != null && base != null) {
      selection.updateSelection(area, base);
      tracking.updateTracking(area, selectionFocus());
    }
  }
  
  
  
  public void renderHUD(Rendering rendering) {
    if (cursorGDX == null) try {
      cursorGDX = Gdx.graphics.newCursor(BLANK_CURSOR.asPixels(), 0, 0);
      Gdx.graphics.setCursor(cursorGDX);
    }
    catch (Exception e) { I.report(e); }
    
    Texture cursorTex = currentTask == null ? null : currentTask.cursor();
    if (cursorTex == null) cursorTex = DEFAULT_CURSOR.asTexture();
    cursor.setBaseTexture(cursorTex);
    
    if (currentTask != null) {
      currentTask.doTask(this);
    }
    
    super.renderHUD(rendering);
  }
  
}





