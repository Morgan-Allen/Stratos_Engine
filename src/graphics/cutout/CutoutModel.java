/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.cutout;
import graphics.common.*;
import util.*;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;



public class CutoutModel extends ModelAsset {
  
  public static final int
    VERTEX_SIZE = 3 + 1 + 2,  //  (position, colour and texture coords.)
    SIZE = 4 * VERTEX_SIZE,   //  (4 vertices, 1 per corner.)
    X0 = 0, Y0 = 1, Z0 = 2,
    C0 = 3, U0 = 4, V0 = 5
  ;
  final public static float VERT_PATTERN[] = {
    0, 1, 0,
    1, 1, 0,
    0, 0, 0,
    1, 0, 0
  };
  final public static short VERT_INDICES[] = {
    0, 2, 1, 1, 2, 3
  };
  
  
  String fileName;
  int numFrames = -1, framesWide = -1;
  
  static class AnimRange { String name; int start; int end; boolean turns; }
  Table <String, AnimRange> animations = new Table();
  
  static class AnimOverlay { String name; float x, y; CutoutModel media; }
  List <AnimOverlay> overlays = new List();
  
  final Box2D window;
  final float size, high;
  final boolean splat;
  
  protected Texture texture;
  protected Texture lightSkin;
  protected float screenWide, screenHigh, maxScreenHigh, minScreenHigh;
  
  //  TODO:  This could probably be unified with the work for GroupSprites
  //  within a common subclass.
  protected float overX, overY;
  
  protected float allFaces[][];
  final Table <String, Integer> faceLookup = new Table();
  
  
  
  private CutoutModel(
    Class modelClass, String ID, String fileName,
    Box2D window, float size, float high, boolean splat
  ) {
    super(modelClass, ID);
    this.fileName = fileName;
    this.window   = window  ;
    this.size     = size    ;
    this.high     = high    ;
    this.splat    = splat   ;
  }
  
  
  public static CutoutModel fromSplatImage(
    Class sourceClass, String ID, String fileName, float size
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    return new CutoutModel(
      sourceClass, ID, fileName,
      window, size, 0, true
    );
  }
  
  
  public static CutoutModel fromImage(
    Class sourceClass, String ID, String fileName, float size, float height
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    return new CutoutModel(
      sourceClass, ID, fileName,
      window, size, height, false
    );
  }
  
  
  public static CutoutModel fromImageFrames(
    Class sourceClass, String ID,
    String fileName, int numFrames, int framesWide,
    float size, float height, boolean splat
  ) {
    final Box2D window = new Box2D().set(0, 0, 1, 1);
    CutoutModel model = new CutoutModel(
      sourceClass, ID, fileName,
      window, size, height, height <= 0
    );
    model.numFrames  = numFrames ;
    model.framesWide = framesWide;
    return model;
  }
  
  
  public static CutoutModel[] fromImages(
    Class sourceClass, String ID,
    String path, float size, float height, boolean splat,
    String... files
  ) {
    final CutoutModel models[] = new CutoutModel[files.length];
    for (int i = 0; i < files.length; i++) {
      final String fileName = path+files[i];
      final Box2D window = new Box2D().set(0, 0, 1, 1);
      models[i] = new CutoutModel(
        sourceClass, ID+"_"+i, fileName,
        window, size, height, splat
      );
    }
    return models;
  }
  
  
  public static CutoutModel[][] fromImageGrid(
    Class sourceClass, String ID, String fileName,
    int gridX, int gridY, float size, float height, boolean splat
  ) {
    final CutoutModel grid[][] = new CutoutModel[gridX][gridY];
    final float stepX = 1f / gridX, stepY = 1f / gridY;
    for (Coord c : Visit.grid(0, 0, gridX, gridY, 1)) {
      final float gx = c.x * stepX, gy = c.y * stepY;
      final Box2D window = new Box2D().set(gx, gy, stepX, stepY);
      grid[c.x][gridY - (c.y + 1)] = new CutoutModel(
        sourceClass, ID+"_"+c.x+"_"+c.y, fileName,
        window, size, height, splat
      );
    }
    return grid;
  }
  
  
  public void attachAnimRange(String name, int start, int end, boolean turns) {
    AnimRange range = new AnimRange();
    animations.put(name, range);
    range.name  = name;
    range.start = start;
    range.end   = end;
    range.turns = turns;
  }
  
  
  public void attachAnimRange(String name, boolean turns) {
    int start = 0, end = 1;
    if (numFrames != -1 ) end = numFrames;
    attachAnimRange(name, start, end, turns);
  }
  
  
  public void attachOverlay(CutoutModel media, float x, float y, String... names) {
    AnimOverlay over = new AnimOverlay();
    over.name = names[0];
    over.x = x;
    over.y = y;
    over.media = media;
    for (String name : names) over.media.attachAnimRange(name, false);
    media.overX = x;
    media.overY = y;
    overlays.add(over);
  }
  
  
  boolean isOverlay() {
    return overX != 0 || overY != 0;
  }
  
  
  public boolean hasAnimation(String name) {
    return animations.containsKey(name);
  }
  
  
  public CutoutSprite makeSprite() {
    if (! stateLoaded()) {
      I.complain("CANNOT CREATE SPRITE UNTIL LOADED: "+this);
    }
    
    CutoutSprite s = new CutoutSprite(this, 0);
    
    if (! overlays.empty()) {
      CutoutSprite overs[] = new CutoutSprite[overlays.size()];
      int i = 0;
      for (AnimOverlay over : overlays) {
        CutoutSprite o = new CutoutSprite(over.media, 0);
        overs[i++] = o;
      }
      s.overlays = overs;
    }
    return s;
  }
  
  
  public Object sortingKey() {
    return texture;
  }
  
  
  
  /**  Asset-loading and disposal:
    */
  protected State loadAsset() {
    if (fileName != null) {
      texture = ImageAsset.getTexture(fileName);
    }
    if (texture == null) {
      return state = State.ERROR;
    }
    
    setupVertices();
    
    if (fileName != null) {
      String litName = fileName.substring(0, fileName.length() - 4);
      litName+="_lights.png";
      if (assetExists(litName)) lightSkin = ImageAsset.getTexture(litName);
    }
    
    return state = State.LOADED;
  }
  
  
  protected State disposeAsset() {
    if (texture   != null) texture  .dispose();
    if (lightSkin != null) lightSkin.dispose();
    return state = State.DISPOSED;
  }
  
  
  /**  Vertex-manufacture methods during initial setup-
    */
  //      A
  //     /-\---/-\
  //   C/ D \ /   \E
  //    \ | / \   /
  //     \-/---\-/
  //      B
  //  This might or might not be hugely helpful, but if you tilt your head to
  //  the right so that X/Y axes are flipped and imagine this as an isometric
  //  box framing the image contents, then:
  //
  //    screenWide    = A to B
  //    screenHigh    = actual height of image (C to E)
  //    maxScreenHigh = D to E
  //    minScreenHigh = D to C, and
  //  -all measured in world-units, *but* relative to screen coordinates.  See
  //   below.
  
  private void setupVertices() {
    //
    //  For the default-sprite geometry, we do a naive translation of some
    //  rectangular vertex-points, keeping the straightforward UV but
    //  translating the screen-coordinates into world-space.  The effect is
    //  something like a 2D cardboard-cutout, propped up at an angle on-stage.
    //
    final Vector3 temp = new Vector3();
    final Batch <float[]> faces = new Batch();
    int animW = 1, animH = 1, numFrames = 1;
    float size = this.size;
    boolean turns = false;
    
    if (this.numFrames != -1 && (numFrames = this.numFrames) > 1) {
      animW = framesWide;
      animH = Nums.ceil(numFrames / 8f);
    }
    if (size == -1) {
      float windowWide = texture.getWidth() / animW;
      size = windowWide / (Nums.ROOT2 * Viewport.DEFAULT_SCALE);
    }
    
    final Texture t = texture;
    final float relHeight =
      (t.getHeight() * window.ydim() / animH) /
      (t.getWidth () * window.xdim() / animW)
    ;
    final float viewAngle = Nums.toRadians(Viewport.DEFAULT_ELEVATE);
    
    screenWide    =  size * Nums.ROOT2;
    screenHigh    =  screenWide * relHeight;
    maxScreenHigh = (screenWide * Nums.sin(viewAngle)) / 2;
    minScreenHigh =  0 - maxScreenHigh;
    maxScreenHigh += this.high * Nums.cos(viewAngle);
    
    for (Coord anim : Visit.grid(0, 0, animH, animW, 1)) {
      
      final float vertices[] = new float[SIZE];
      faces.add(vertices);
      
      for (int i = 0, p = 0; i < vertices.length; i += VERTEX_SIZE) {
        float
          x = VERT_PATTERN[p++],
          y = VERT_PATTERN[p++],
          z = VERT_PATTERN[p++],
          u = 0, v = 0
        ;
        
        temp.set(screenWide * (x - 0.5f), (screenHigh * y) + minScreenHigh, z);
        Viewport.isometricInverted(temp, temp);
        vertices[X0 + i] = temp.x;
        vertices[Y0 + i] = temp.y;
        vertices[Z0 + i] = temp.z;
        vertices[C0 + i] = Sprite.WHITE_BITS;
        
        u += anim.y * 1f / animW;
        u += x / animW;
        u = (window.xpos() * (1 - u)) + (window.xmax() * u);
        vertices[U0 + i] = u;
        
        v += anim.x * 1f / animH;
        v += (1 - y) / animH;
        v = (window.ypos() * (1 - v)) + (window.ymax() * v);
        vertices[V0 + i] = v;
      }
    }
    
    this.allFaces = faces.toArray(float[].class);
    
    if (! hasAnimation(AnimNames.STILL)) {
      attachAnimRange(AnimNames.STILL, 0, animW - 1, turns);
    }
    if (! hasAnimation(AnimNames.STAND)) {
      attachAnimRange(AnimNames.STAND, 0, numFrames, turns);
    }
  }
  
  
  public String fileName() {
    if (fileName != null) return fileName;
    return null;
  }
  
  
  
  /**  Icon extraction-
    */
  public static ImageAsset extractIcon(
    final Assets.Loadable model, final String ID,
    final int x, final int y, final int w, final int h
  ) {
    if (model == null) return null;
    
    return new ImageAsset(model.sourceClass, ID, "") {
      protected State loadAsset() {
        if (! model.stateLoaded()) {
          Assets.loadNow(model);
        }
        if (! model.stateLoaded()) {
          return state = State.ERROR;
        }
        
        Pixmap original = null;
        if (model instanceof CutoutModel) {
          Texture extracts = ((CutoutModel) model).texture;
          TextureData texData = extracts.getTextureData();
          if (! texData.isPrepared()) texData.prepare();
          original = texData.consumePixmap();
        }
        if (model instanceof ImageAsset) {
          original = ((ImageAsset) model).asPixels();
        }
        if (original == null) {
          return state = State.ERROR;
        }
        
        try {
          Pixmap icon = new Pixmap(w, h, Pixmap.Format.RGBA8888);
          icon.drawPixmap(original, x, y, w, h, 0, 0, w, h);
          
          this.pixels = icon;
          this.texture = new Texture(icon);
          return state = State.LOADED;
        }
        catch (Exception e) {
          I.report(e);
          return state = State.ERROR;
        }
      }
    };
  }
  
}





