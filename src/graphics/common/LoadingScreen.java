


package graphics.common;
import graphics.widgets.*;
import util.*;



public class LoadingScreen extends HUD {
  
  
  final ImageAsset
    titleImage,
    backImage,
    progFillImg,
    progBackImg;
  
  private ProgressBar progBar;
  private boolean initDone = false;
  
  
  public LoadingScreen(
    Rendering rendering,
    ImageAsset titleImage  ,
    ImageAsset backImage   ,
    ImageAsset progFillImg ,
    ImageAsset progBackImg
  ) {
    super(rendering);
    this.titleImage = titleImage;
    this.backImage  = backImage ;
    this.progFillImg = progFillImg;
    this.progBackImg = progBackImg;
  }
  
  
  public void renderHUD(Rendering rendering) {
    if (! initDone) {
      
      Assets.loadNow(titleImage );
      Assets.loadNow(backImage  );
      Assets.loadNow(progFillImg);
      Assets.loadNow(progBackImg);
      
      final Image backImage = new Image(this, this.backImage);
      backImage.alignToFill();
      backImage.stretch = true;
      backImage.attachTo(this);
      
      final Image titleImage = new Image(this, this.titleImage);
      titleImage.alignToCentre();
      titleImage.expandToTexSize(1, true);
      titleImage.lockToPixels = true;
      titleImage.attachTo(this);
      
      progBar = new ProgressBar(
        this, progFillImg, progBackImg
      );
      progBar.alignHorizontal(0.5f, titleImage.texWide(), 0);
      progBar.alignVertical(0.5f, 25, 0 - (25 + (titleImage.texHigh() / 2)));
      progBar.attachTo(this);
      
      initDone = true;
    }
    super.renderHUD(rendering);
  }
  
  
  public void update(String label, float progress) {
    if (! initDone) return;
    progBar.progress = progress;
  }
}



