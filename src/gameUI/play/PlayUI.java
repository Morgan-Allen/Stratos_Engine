

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import gameUI.misc.*;
import start.*;
import util.*;

import com.badlogic.gdx.Gdx;



public class PlayUI extends HUD implements UIConstants {
  
  
  /**  Data fields, constructors, setup and save/load methods-
    */
  final static ImageAsset
    BUILD_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_build_tab",
      "media/GUI/panels/build_tab.png"
    ),
    MARKETS_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_markets_tab",
      "media/GUI/panels/markets_tab.png"
    ),
    ROSTER_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_roster_tab",
      "media/GUI/panels/roster_tab.png"
    ),
    CHARTS_TAB_IMG = ImageAsset.fromImage(
      PlayUI.class, "img_charts_tab",
      "media/GUI/panels/charts_tab.png"
    ),
    DEFAULT_CURSOR = ImageAsset.fromImage(
      PlayUI.class, "img_default_cursor",
      "media/GUI/default_cursor.png"
    ),
    BLANK_CURSOR = ImageAsset.withColor(
      32, Colour.HIDE, PlayUI.class
    );
  
  AreaMap area;
  Base base;
  
  DetailPane buildOptions;
  DetailPane markets;
  DetailPane roster;
  DetailPane charts;
  DetailPane panes[];
  
  Button buildTab;
  Button marketsTab;
  Button rosterTab;
  Button chartsTab;
  Button paneTabs[];
  
  ReminderListing reminders;
  
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
  Tooltips tooltips;
  
  
  
  public PlayUI(Rendering rendering) {
    super(rendering);
    
    buildOptions = new PaneBuildOptions(this);
    markets      = new PaneMarkets(this);
    roster       = new PaneRoster(this);
    charts       = new PaneCharts(this);
    
    panes        = new DetailPane[] { buildOptions, markets, roster, charts };
    paneTabs     = new Button[4];
    
    ImageAsset TAB_IMAGES[] = {BUILD_TAB_IMG, MARKETS_TAB_IMG, ROSTER_TAB_IMG, CHARTS_TAB_IMG };
    String TAB_DESC[] = {"Build Options", "Markets", "Roster", "Charts"};
    int tabWide = INFO_PANEL_WIDE / 4, tabsHigh = (int) (PANEL_TABS_HIGH * 0.7f);
    
    for (final int i : new int[] { 0, 1, 2, 3 }) {
      Button button = new Button(
        this, "tab_button_"+i, TAB_IMAGES[i], TAB_DESC[i]
      ) {
        protected void whenClicked() {
          if (detailPane == panes[i]) setDetailPane(null);
          else setDetailPane(panes[i]);
        }
      };
      button.alignLeft(tabWide * i, tabWide);
      button.alignTop(0, tabsHigh);
      button.attachTo(this);
      paneTabs[i] = button;
    }
    buildTab   = paneTabs[0];
    marketsTab = paneTabs[1];
    rosterTab  = paneTabs[2];
    
    detailArea = new UIGroup(this);
    detailArea.alignLeft    (0, INFO_PANEL_WIDE);
    detailArea.alignVertical(0, tabsHigh);
    detailArea.attachTo(this);
    
    reminders = new ReminderListing(this);
    reminders.alignHorizontal(20, 0);
    reminders.alignVertical(0, 0);
    reminders.attachTo(detailArea);
    
    optionList = new OptionList(this);
    optionList.attachTo(this);
    
    readout = new Readout(this);
    readout.alignHorizontal(INFO_PANEL_WIDE, 0);
    readout.alignTop(0, READOUT_HIGH);
    readout.attachTo(this);
    
    progressOptions = new ProgressOptions(this);
    progressOptions.alignTop(0, 25);
    progressOptions.alignRight(0, 300);
    progressOptions.attachTo(this);
    
    cursor = new Cursor(this, DEFAULT_CURSOR);
    cursor.alignToArea(0, 0, CURSOR_SIZE, CURSOR_SIZE);
    cursor.attachTo(this);
    
    this.tooltips = new Tooltips(this, UIConstants.INFO_FONT);
    tooltips.attachTo(this);
  }
  
  
  public void assignParameters(AreaMap stage, Base base) {
    if (stage == null || base == null) {
      I.complain("\nCANNOT ASSIGN NULL STAGE/BASE TO UI!");
      return;
    }
    this.area = stage;
    this.base  = base ;
    optionList.setupFrom(stage, base);
  }
  
  
  public void setLookPoint(GameConstants.Target point) {
    if (point == null) return;
    rendering.view.lookedAt.setTo(point.exactPosition(null));
  }
  
  
  public void loadState(Session s) throws Exception {
    area = (AreaMap) s.loadObject();
    base = (Base   ) s.loadObject();
    selection.loadState(s);
    tracking .loadState(s);
    optionList.setupFrom(area, base);
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(area);
    s.saveObject(base );
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
    if (detailPane != null) {
      detailPane.detach();
    }
    this.detailPane = pane;
    if (detailPane != null) {
      detailPane.alignToFill();
      detailPane.attachTo(detailArea);
    }
    reminders.hidden = detailPane != null;
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
  
  
  public void renderWorldFX() {
    selection.renderWorldFX(rendering);
  }
  
  
  public void renderHUD(Rendering rendering) {
    if (cursorGDX == null) try {
      cursorGDX = Gdx.graphics.newCursor(BLANK_CURSOR.asPixels(), 0, 0);
      Gdx.graphics.setCursor(cursorGDX);
    }
    catch (Exception e) { I.report(e); }
    
    ImageAsset cursorTex = currentTask == null ? null : currentTask.cursor();
    if (cursorTex == null) cursorTex = DEFAULT_CURSOR;
    cursor.setBaseTexture(cursorTex);
    
    if (currentTask != null) {
      currentTask.doTask(this);
    }
    
    super.renderHUD(rendering);
  }
  
}





