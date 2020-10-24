/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.common;
import util.*;
import java.io.*;



public class GroupSprite extends Sprite {
  
  
  public static class GroupModel extends ModelAsset {
    
    static class Entry extends Vec3D {
      ModelAsset model;
      boolean turns;
    }
    
    Batch <Entry> entryList = new Batch();
    Entry entryArray[] = null;
    
    
    
    public GroupModel(Class sourceClass, String modelName) {
      super(sourceClass, modelName);
    }
    
    public Object sortingKey() {
      return this;
    }
    
    public Sprite makeSprite() {
      return new GroupSprite(this);
    }
    
    public boolean hasAnimation(String name) {
      return false;
    }
    
    protected State loadAsset() {
      return state = State.LOADED;
    }
    
    protected State disposeAsset() {
      return state = State.DISPOSED;
    }
    
    public void attach(ModelAsset kid, float xoff, float yoff, float zoff, boolean turns) {
      Entry e = new Entry();
      e.model = kid;
      e.turns = turns;
      e.set(xoff, yoff, zoff);
      entryList.add(e);
      entryArray = null;
    }
    
    public void attach(ModelAsset kid, float xoff, float yoff, float zoff) {
      attach(kid, xoff, yoff, zoff, false);
    }
    
    public Entry[] entries() {
      if (entryArray != null) return entryArray;
      return entryArray = entryList.toArray(Entry.class);
    }
  }
  
  
  
  final public static int
    NO_SORTING       = -1,
    SORT_BY_Z_ORDER  =  0,
    SORT_BY_ADDITION =  1;
  
  final GroupModel model;
  Sprite modules[];
  private Mat3D rotMatrix = new Mat3D();
  
  
  public GroupSprite(GroupModel model) {
    this.model = model;
    initSprites();
  }
  
  
  public void loadFrom(DataInputStream in) throws Exception {
    super.loadFrom(in);
    final int numMods = in.readInt();
    modules = new Sprite[numMods];
    for (int i = numMods; i-- > 0;) {
      modules[i] = ModelAsset.loadSprite(in);
    }
  }
  
  
  public void saveTo(DataOutputStream out) throws Exception {
    super.saveTo(out);
    out.writeInt(modules.length);
    for (Sprite s : modules) {
      ModelAsset.saveSprite(s, out);
    }
  }
  
  
  public ModelAsset model() {
    return model;
  }
  
  
  
  /**  Actual content modification-
    */
  public int indexOf(Sprite sprite) {
    if (sprite == null) return -1;
    int i = 0;
    for (Sprite h : modules) {
      if (h == sprite) return i; else i++;
    }
    return -1;
  }
  
  
  public Sprite atIndex(int n) {
    if (n < 0 || n >= modules.length) return null;
    return modules[n];
  }
  
  
  public Sprite[] attached() {
    return modules;
  }
  
  
  
  /**  Rendering and updates-
    */
  void initSprites() {
    modules = new Sprite[model.entries().length];
    int index = 0;
    for (GroupModel.Entry e : model.entries()) {
      modules[index++] = e.model.makeSprite();
    }
  }
  
  
  public void setAnimation(String animName, float progress, boolean loop) {
    for (Sprite module : modules) module.setAnimation(animName, progress, true);
  }
  
  
  public void setFacing(int index) {
    return;
  }
  
  
  public void readyFor(Rendering rendering) {
    rendering.recordAsRendered(this);
    
    boolean report = I.used60Frames && false;
    
    float groupRot = Nums.round(rotation + 360 + 22, 45, false) % 360;
    rotMatrix.setIdentity();
    rotMatrix.rotateZ(Nums.toRadians(0 - groupRot));
    
    if (report) {
      I.say("\nRendering "+model);
      I.say("  Rotation: "+rotation+", rounded: "+groupRot);
    }
    
    for (int i = 0; i < modules.length; i++) {
      final Sprite module = modules[i];
      final GroupModel.Entry off = model.entries()[i];
      
      module.rotation = groupRot;
      module.position.setTo(off);
      module.position.scale(scale);
      if (off.turns) rotMatrix.trans(module.position);
      module.position.add(this.position);
      
      if (report) {
        I.say("  Offset for "+module.model()+": "+off);
      }
      
      module.colour   = colour;
      module.fog      = fog;
      module.passType = passType;
      module.scale    = scale;
      module.readyFor(rendering);
    }
  }
  
}











