

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import gameUI.misc.*;
import start.*;
import util.*;



public class PlayUI extends HUD implements UIConstants {
  
  /**  Data fields, constructors, setup and save/load methods-
    */
  final static ImageAsset
    INSTALL_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_install_tab",
      "media/GUI/panels/installations_tab.png"
    );
  
  AreaMap stage;
  Base    base;
  Element home;
  
  Button     installTab ;
  DetailPane installPane;
  
  DetailPane detailPane;
  OptionList optionList;
  
  final Selection selection = new Selection(this);
  final Tracking  tracking  = new Tracking (this);
  
  Readout readout;
  ProgressOptions progressOptions;
  
  private PlayTask currentTask = null;
  
  
  
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
    
    readout = new Readout(this);
    readout.alignHorizontal(INFO_PANEL_WIDE, 0);
    readout.alignTop(0, READOUT_HIGH);
    readout.attachTo(this);
    
    progressOptions = new ProgressOptions(this);
    progressOptions.alignVertical  (0, 0);
    progressOptions.alignHorizontal(0, 0);
    progressOptions.attachTo(this);
    
    optionList = new OptionList(this);
    optionList.attachTo(this);
  }
  
  
  public void assignParameters(AreaMap stage, Base base) {
    if (stage == null || base == null) {
      I.complain("\nCANNOT ASSIGN NULL STAGE/BASE TO UI!");
      return;
    }
    this.stage = stage;
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
    stage = (AreaMap) s.loadObject();
    base  = (Base   ) s.loadObject();
    home  = (Element) s.loadObject();
    selection.loadState(s);
    tracking .loadState(s);
    optionList.setupFrom(stage, base);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(stage);
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
    /*
    if (UI.optionList != null && UI.optionList.subject != null) {
      return UI.optionList.subject;
    }
    //*/
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
      detailPane.alignLeft    (0, INFO_PANEL_WIDE);
      detailPane.alignVertical(BAR_BUTTON_SIZE, 0);
      detailPane.attachTo(this);
    }
  }
  
  
  /*
  public void setOptionList(OptionList list) {
    if (optionList != null) optionList.detach();
    this.optionList = list;
    if (optionList != null) {
      optionList.attachTo(this);
    }
  }
  //*/
  
  
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
    
    if (stage != null && base != null) {
      selection.updateSelection(stage, base);
      tracking.updateTracking(stage, selectionFocus());
    }
  }
  
  
  public void renderHUD(Rendering rendering) {
    if (currentTask != null) currentTask.doTask(this);
    super.renderHUD(rendering);
  }
  
}
















