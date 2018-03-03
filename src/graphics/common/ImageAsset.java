/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;



public class ImageAsset extends Assets.Loadable {
  
  
  private static Object NO_FILE = new Object();
  
  private String  filePath;
  private Pixmap  pixels  ;
  private Texture texture ;
  private Colour  average ;
  
  
  private ImageAsset(Class sourceClass, String ID, String filePath) {
    super(ID, sourceClass, false);
    this.filePath = filePath;
  }
  
  
  public static ImageAsset fromImage(
    Class sourceClass, String ID, String filePath
  ) {
    final Object match = Assets.getResource(ID);
    if (match == NO_FILE) return null;
    if (match instanceof ImageAsset) return (ImageAsset) match;
    
    if (! Assets.exists(filePath)) {
      I.say("WARNING- NO SUCH IMAGE FILE: "+filePath);
      Assets.cacheResource(NO_FILE, ID);
      return null;
    }
    
    final ImageAsset asset = new ImageAsset(sourceClass, ID, filePath);
    asset.setKeyFile(filePath);
    Assets.cacheResource(asset, ID);
    return asset;
  }
  
  
  public static ImageAsset[] fromImages(
    Class sourceClass, String ID, String path, String... files
  ) {
    final ImageAsset assets[] = new ImageAsset[files.length];
    for (int i = 0; i < files.length; i++) {
      assets[i] = fromImage(sourceClass, ID+"_"+i, path+files[i]);
    }
    return assets;
  }
  
  
  public Pixmap asPixels() {
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return pixels;
  }
  
  
  public Texture asTexture() {
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return texture;
  }
  
  
  public Colour average() {
    if (! stateLoaded()) Assets.loadNow(this);
    if (! stateLoaded()) I.complain("IMAGE ASSET HAS NOT LOADED!- "+filePath);
    return average;
  }
  
  
  
  /**  Actual loading-
    */
  final static String
    TEX_PREFIX = "texture_for_",
    PIX_PREFIX = "pixels_for_" ,
    AVG_PREFIX = "average_for_";
  
  
  protected State loadAsset() {
    Texture texture = (Texture) Assets.getResource(TEX_PREFIX+filePath);
    Pixmap pixels   = (Pixmap ) Assets.getResource(PIX_PREFIX+filePath);
    Colour average  = (Colour ) Assets.getResource(AVG_PREFIX+filePath);
    return loadAsset(texture, pixels, average);
  }
  
  
  protected State loadAsset(
    Texture withTexture, Pixmap withPixels, Colour withAverage
  ) {
    
    if (withPixels != null) {
      this.pixels = withPixels;
    }
    else {
      pixels = new Pixmap(Gdx.files.internal(filePath));
      Assets.cacheResource(pixels, PIX_PREFIX+filePath);
    }
    
    if (withAverage != null) {
      this.average = withAverage;
    }
    else {
      average = new Colour();
      Colour sample = new Colour();
      
      float sumAlphas = 0;
      final int wide = pixels.getWidth(), high = pixels.getHeight();
      for (Coord c : Visit.grid(0, 0, wide, high, 10)) {
        sample.setFromRGBA(pixels.getPixel(c.x, c.y));
        sumAlphas += sample.a;
        average.r += sample.r * sample.a;
        average.g += sample.g * sample.a;
        average.b += sample.b * sample.a;
      }
      
      average.r /= sumAlphas;
      average.g /= sumAlphas;
      average.b /= sumAlphas;
      average.a = 1;
      average.set(average);
      Assets.cacheResource(average, AVG_PREFIX+filePath);
    }
    
    if (withTexture != null) {
      this.texture = withTexture;
    }
    else {
      texture = new Texture(pixels);
      texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
      Assets.cacheResource(texture, TEX_PREFIX+filePath);
    }
    
    return state = State.LOADED;
  }
  
  
  public static Texture getTexture(String fileName) {
    Object cached = Assets.getResource(TEX_PREFIX+fileName);
    
    if (cached == NO_FILE) return null;
    if (cached != null) return (Texture) cached;
    
    if (! Assets.exists(fileName)) {
      cached = NO_FILE;
    }
    else try {
      Texture loaded = new Texture(Gdx.files.internal(fileName));
      loaded.setFilter(TextureFilter.Linear, TextureFilter.Linear);
      cached = loaded;
    }
    catch (Exception e) {
      cached = NO_FILE;
    }
    
    if (cached == NO_FILE) {
      System.out.print("\nWARNING: NO TEXTURE: "+fileName);
      return null;
    }
    Assets.cacheResource(cached, TEX_PREFIX+fileName);
    return (Texture) cached;
  }
  
  
  protected State disposeAsset() {
    Texture texture = (Texture) Assets.getResource(TEX_PREFIX+filePath);
    Pixmap pixels   = (Pixmap ) Assets.getResource(PIX_PREFIX+filePath);
    Colour average  = (Colour ) Assets.getResource(AVG_PREFIX+filePath);
    
    if (pixels != null) {
      Assets.clearCachedResource(PIX_PREFIX+filePath);
      pixels.dispose();
    }
    if (average != null) {
      Assets.clearCachedResource(AVG_PREFIX+filePath);
      average = null;
    }
    if (texture != null) {
      Assets.clearCachedResource(TEX_PREFIX+filePath);
      texture.dispose();
    }
    return state = State.DISPOSED;
  }
  
  
  
  /**  Utility method for creating static constants-
    */
  public static ImageAsset withColor(final int size, Colour c, Class source) {
    final Color gdxColor = new Color(c.r, c.g, c.b, c.a);
    final ImageAsset asset = new ImageAsset(source, c+"_img", "IMAGE_ASSET_") {
      protected State loadAsset() {
        final Texture tex = new Texture(size, size, Pixmap.Format.RGBA8888);
        final Pixmap draw = new Pixmap (size, size, Pixmap.Format.RGBA8888);
        draw.setColor(gdxColor);
        draw.fillRectangle(0, 0, size, size);
        tex.draw(draw, 0, 0);
        return loadAsset(tex, draw, null);
      }
    };
    return asset;
  }
  
  
  public String toString() {
    return filePath;
  }
}




