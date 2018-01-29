

package gameUI.play;
import graphics.common.*;
import graphics.widgets.*;



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
  Text text;
  
  final Selection.Focus subject;
  
  
  public DetailPane(HUD UI, Selection.Focus subject) {
    super(UI);
    this.subject = subject;
    
    this.border = new Bordering(UI, BORDER_TEX);
    border.left   = 20;
    border.right  = 20;
    border.bottom = 20;
    border.top    = 20;
    border.alignAcross(0, 1);
    border.alignDown  (0, 1);
    border.attachTo(this);
    
    this.text = new Text(UI, DETAIL_FONT);
    text.scale = 0.75f;
    text.alignToFill();
    text.attachTo(border.inside);
  }
  
  
}













