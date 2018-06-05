

package gameUI.play;
import game.*;
import graphics.common.*;
import graphics.widgets.*;
import util.*;



public class DetailPane extends UIGroup {
  
  
  final public static ImageAsset
    BORDER_TEX = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_border",
      "media/GUI/Front/Panel.png"
    ),
    SCROLL_TEX = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_scroll_handle",
      "media/GUI/scroll_grab.gif"
    ),
    WIDGET_BACK = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_back_img",
      "media/GUI/Front/widget_back.png"
    ),
    WIDGET_BACK_LIT = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_back_lit",
      "media/GUI/Front/widget_back_lit.png"
    ),
    WIDGET_CLOSE = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_close_img",
      "media/GUI/Front/widget_close.png"
    ),
    WIDGET_CLOSE_LIT = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_close_lit",
      "media/GUI/Front/widget_close_lit.png"
    ),
    WIDGET_INFO = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_info_img",
      "media/GUI/Front/widget_info.png"
    ),
    WIDGET_INFO_LIT = ImageAsset.fromImage(
      DetailPane.class, "selection_pane_widget_info_lit",
      "media/GUI/Front/widget_info_lit.png"
    );
  
  final public static Alphabet DETAIL_FONT = Alphabet.loadAlphabet(
    DetailPane.class, "detail_font", "media/GUI/", "FontVerdana.xml"
  );
  
  
  Bordering border;
  Text header, text;
  Scrollbar scrollbar;
  
  final Selection.Focus subject;
  
  String categories[];
  float  catScrolls[];
  int    categoryID  ;
  private static Table <Class, String> defaults = new Table();
  
  
  
  public DetailPane(HUD UI, Selection.Focus subject, String... categories) {
    super(UI);
    this.subject = subject;
    
    this.categories = categories;
    this.catScrolls = new float[categories == null ? 0 :categories.length];
    this.categoryID = 0;
    
    this.border = new Bordering(UI, BORDER_TEX);
    border.left   = 20;
    border.right  = 20;
    border.bottom = 20;
    border.top    = 20;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.header = new Text(UI, DETAIL_FONT);
    header.scale = 1.00f;
    header.alignTop   (0, 35);
    header.alignAcross(0, 1 );
    header.attachTo(border.inside);
    
    this.text = new Text(UI, DETAIL_FONT);
    text.scale = 0.75f;
    text.alignVertical(0, 35);
    text.alignAcross  (0, 1 );
    text.attachTo(border.inside);
  }
  
  
  
  
  /**  Switching between categories-
    */
  private Class selectType() {
    if (subject == null) return null;
    Class TYPES[] = { Building.class, Actor.class, Mission.class };
    if (subject instanceof Building) return Building.class;
    for (Class c : TYPES) if (subject.getClass().isAssignableFrom(c)) return c;
    return null;
  }
  
  private void setCategory(int catID) {
    //catScrolls[categoryID] = 1 - scrollbar.scrollPos();
    this.categoryID = catID;
    //scrollbar.setScrollPos(1 - catScrolls[categoryID]);
    if (subject != null) defaults.put(selectType(), categories[catID]);
  }
  
  public boolean inCategory(String cat) {
    String myCat = categories[categoryID];
    return myCat.equals(cat);
  }
  
  
  
  /**  Performing regular state-updates...
    */
  protected void updateState() {
    
    if (subject != null) {
      header.setText(subject.fullName());
      header.append("\n");
    }
    else header.setText("");
    
    if (categories != null) {
      for (int i = 0; i < categories.length; i++) {
        final int index = i;
        final boolean CC = categoryID == i;
        header.append(new Text.Clickable() {
          public String fullName() { return ""+categories[index]+" "; }
          public void whenClicked(Object context) { setCategory(index); }
        }, CC ? Colour.GREEN : Text.LINK_COLOUR);
      }
    }
    
    updateText(text);
    
    super.updateState();
  }
  
  
  protected void updateText(Text text) {
    text.setText("");
  }
  
}







