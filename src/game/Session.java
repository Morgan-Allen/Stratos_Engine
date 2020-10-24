/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import util.*;
import java.io.*;
import java.lang.reflect.*;




/**  NOTE:  Saveable objects ALSO need to implement a public constructor that
  *  takes a Session as it's sole argument, or an exception will occur, AND
  *  the object must call cacheInstance() as soon as possible once initialised,
  *  or an exception will occur.  Alternatively, they may implement a static
  *  public loadConstant method taking the Session as it's argument.
  *  
  *  The Saveable interface is accessible from within the Session class.
  */
public final class Session {
  
  public static boolean
    verbose     = false,
    checkType   = true ,
    verboseType = false;
  
  final static int
    CLASS_CAPACITY  = 200,
    OBJECT_CAPACITY = 50000,
    
    OP_NONE      = -1,
    OP_SAVE      =  0,
    OP_SAVE_DONE =  1,
    OP_LOAD      =  2,
    OP_LOAD_DONE =  3;
  
  private static class Classing {
    Class basis;
    boolean constant;
    Object loadMethod;
  }
  
  final Table <Class <?>, Integer > classIDs    = new Table(CLASS_CAPACITY );
  final Table <Integer  , Classing> loadClasses = new Table(CLASS_CAPACITY );
  final Table <Saveable , Integer > saveIDs     = new Table(OBJECT_CAPACITY);
  final Table <Integer  , Saveable> loadIDs     = new Table(OBJECT_CAPACITY);
  private int nextObjectID = 0, nextClassID = 0;
  
  public static interface Saveable {
    void loadState(Session s) throws Exception;
    void saveState(Session s) throws Exception;
  }
  
  private int operation = OP_NONE;
  private DataOutputStream out   ;
  private DataInputStream  in    ;
  
  private Saveable rootItems[] = new Saveable[0];
  private Stack <Saveable> allItems = new Stack();
  
  
  private Session() {
    return;
  }
  
  
  public void saveClass(Class c) throws Exception {
    if (c == null) { out.writeInt(-1); return; }
    final Integer classID = classIDs.get(c);
    if (classID == null) {
      //
      //  Then we need to save the full binary name of this class and cache
      //  it's ID-
      if (verbose) I.say("  Saving new class- "+c.getName()+" ID: "+nextClassID);
      out.writeInt(nextClassID);
      Assets.writeString(out, c.getName());
      classIDs.put(c, nextClassID);
      nextClassID += 1;
    }
    else {
      out.writeInt(classID);
    }
  }
  
  
  public Class loadClass() throws Exception {
    Classing c = loadClassing();
    return c == null ? null : c.basis;
  }
  
  
  private Classing loadClassing() throws Exception {
    final int classID = in.readInt();
    if (classID == -1) return null;
    
    Classing c = loadClasses.get(classID);
    if (c != null) return c;
    
    final String className = Assets.readString(in);
    if (verbose) I.say("  Loading new class: "+className);
    
    final Class <?> loadClass = Class.forName(className);
    c = new Classing();
    c.basis = loadClass;
    c.constant = Constant.class.isAssignableFrom(loadClass);
    
    if (c.constant) try {
      c.loadMethod = loadClass.getMethod("loadConstant", Session.class);
      if (! Modifier.isStatic(((Method) c.loadMethod).getModifiers())) {
        I.say("WARNING: loadConstant method is not static! "+loadClass);
      }
    }
    catch (NoSuchMethodException e) {}
    else try {
      c.loadMethod = loadClass.getConstructor(Session.class);
    }
    catch (NoSuchMethodException e) {}
    
    if (c.loadMethod == null) {
      I.say("\n");
      I.complain("NO SUITABLE LOAD-METHOD FOUND FOR "+loadClass+"!\n");
      return null;
    }
    
    loadClasses.put(classID, c);
    return c;
  }
  

  public static Session saveSession(
    String saveFile, Saveable... items
  ) throws Exception {
    final Session s = new Session();

    s.out = new DataOutputStream(new BufferedOutputStream(
      new FileOutputStream(saveFile))
    );
    
    Assets.clearReferenceIDs();
    s.operation = OP_SAVE;
    
    if (verbose) I.say("\nSAVING SESSION, ROOT ITEMS: "+items.length);
    
    s.rootItems = items;
    s.saveInt(items.length);
    for (Saveable item : items) {
      s.saveObject(item);
    }
    
    while (s.allItems.size() > 0) {
      Saveable item = s.allItems.removeFirst();
      if (verbose) I.say("  Saving state for: "+item);
      item.saveState(s);
    }
    
    s.finish();
    return s;
  }
  
  
  public static Session loadSession(String saveFile, boolean loadNow) {
    final Session s = new Session();
    
    final File asFile = new File(saveFile);
    try {
      s.in = new DataInputStream(new BufferedInputStream(
        new FileInputStream(asFile))
      );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    s.operation = OP_LOAD;
    
    final Thread loadThread = new Thread() {
      public void run() {
        try {
          Assets.clearReferenceIDs();
          
          final int numItems = s.loadInt();
          if (verbose) I.say("\nLOADING SESSION, ROOT ITEMS: "+numItems);
          
          s.rootItems = new Saveable[numItems];
          for (int n = 0; n < numItems; n++) {
            s.rootItems[n] = s.loadObject();
          }
          
          while (s.allItems.size() > 0) {
            Saveable item = s.allItems.removeFirst();
            if (verbose) I.say("  Will load state: "+item.getClass());
            item.loadState(s);
          }
          
          s.finish();
          Thread.sleep(250);
        }
        catch (Exception e) { e.printStackTrace(); }
      }
    };
    if (loadNow) loadThread.run  ();
    else         loadThread.start();
    return s;
  }
  
  
  public void saveObject(Object o) throws Exception {
    writeType(TYPE_OBJECT);
    
    if (! (o instanceof Saveable)) {
      if (o != null) {
        I.say("\nWARNING: OBJECT NOT SAVEABLE: "+o);
      }
      out.writeInt(-1);
      return;
    }
    final Saveable s = (Saveable) o;
    
    Integer e = saveIDs.get(s);
    if (e == null) {
      final int nextID = nextObjectID++;
      saveIDs.put(s, nextID);
      
      if (verbose) I.say("  Saving new reference: "+s+", ID: "+nextID);
      out.writeInt(nextID);
      saveClass(s.getClass());
      
      if (s instanceof Constant) {
        s.saveState(this);
      }
      else {
        allItems.addLast(s);
      }
    }
    else {
      out.writeInt(e);
    }
  }
  
  
  public Saveable loadObject() throws Exception {
    checkType(TYPE_OBJECT);
    
    final int loadID = in.readInt();
    if (loadID == -1) return null;
    
    Saveable loaded = loadIDs.get(loadID);
    if (loaded != null) return loaded;
    
    if (verbose) I.say("  Loading reference, ID: "+loadID);
    
    Classing c = loadClassing();
    if (c == null) return null;
    
    Class loadClass = c.basis;
    Object loadMethod = c.loadMethod;
    try {
      if (c.constant) {
        final Method loadConstant = (Method) loadMethod;
        loadClass = loadConstant.getDeclaringClass();
        loaded = (Saveable) loadConstant.invoke(null, this);
      }
      else {
        final Constructor loadObject = (Constructor) loadMethod;
        loadClass = loadObject.getDeclaringClass();
        loaded = (Saveable) loadObject.newInstance(this);
      }
    }
    catch (InstantiationException e) { I.complain(
      "PROBLEM WITH "+loadClass.getName()+"\n"+
      "ALL CLASSES IMPLEMENTING SAVEABLE MUST IMPLEMENT A PUBLIC CONSTRUCTOR "+
      "TAKING THE SESSION AS IT'S SOLE ARGUMENT, OR A STATIC loadConstant("+
      "Session s) METHOD THAT RETURNS A SAVEABLE OBJECT. THANK YOU."
    ); }
    
    loadIDs.put(loadID, loaded);
    
    if (c.constant) {
      loaded.loadState(this);
    }
    else {
      allItems.addLast(loaded);
    }
    
    if (verbose) I.say("  Loaded new reference: "+loadClass);
    return loaded;
  }
  
  
  public Saveable[] loaded() {
    return rootItems;
  }
  
  
  public void finish() throws Exception {
    saveIDs .clear();
    classIDs.clear();
    loadIDs .clear();
    loadClasses.clear();
    
    if (out != null) {
      out.flush();
      out.close();
      operation = OP_SAVE_DONE;
    }
    if (in != null) {
      in.close();
      operation = OP_LOAD_DONE;
    }
  }
  
  
  
  /**  These methods allow Saveable objects to import/export their internal
    *  data, and permit direct access to the data input/output streams if
    *  required.
    */
  final static int
    //
    //  Primitive types first-
    TYPE_BYTEA1 = 0,
    TYPE_BYTEA2 = 1,
    TYPE_FLOATA = 2,
    TYPE_FLOAT  = 3,
    TYPE_INT    = 4,
    TYPE_BOOL   = 5,
    TYPE_STRING = 6,
    //
    //  Then saveable objects-
    TYPE_OBJECT  = 7,
    TYPE_OBJECTS = 8,
    TYPE_OBJECTA = 9,
    TYPE_TALLY   = 10,
    TYPE_TABLE   = 11,
    TYPE_ENUM    = 12,
    TYPE_ENUMS   = 13,
    TYPE_KEY     = 14
  ;
  final static String TYPE_DESC[] = {
    "1D Byte Array", "2D Byte Array", "Float Array",
    "Float", "Int", "Bool", "String",
    "Object", "Object Series", "Object Array",
    "Tally", "Table", "Enum", "Enum Series", "Key"
  };
  
  private void checkType(int type) throws Exception {
    if (! checkType) return;
    
    int read = in.readInt();
    if (read != type) {
      String readDesc = ""+read;
      if (read == Nums.clamp(read, TYPE_DESC.length)) {
        readDesc = TYPE_DESC[read];
      }
      I.complain("EXPECTED TO READ: "+TYPE_DESC[type]+", FOUND: "+readDesc);
    }
    else {
      if (verbose && verboseType) I.say("    READING "+TYPE_DESC[type]);
    }
  }
  
  private void writeType(int type) throws Exception {
    if (! checkType) return;
    
    out.writeInt(type);
    if (verbose && verboseType) I.say("    WRITING "+TYPE_DESC[type]);
  }
  
  
  public DataOutputStream output() { return out; }
  public DataInputStream  input () { return in ; }
  
  
  public void loadByteArray(byte array[]) throws Exception {
    checkType(TYPE_BYTEA1);
    in.read(array);
  }
  
  
  public void saveByteArray(byte array[]) throws Exception {
    writeType(TYPE_BYTEA1);
    out.write(array);
  }
  
  
  public void loadByteArray(byte array[][]) throws Exception {
    checkType(TYPE_BYTEA2);
    for (byte a[] : array) loadByteArray(a);
  }
  
  
  public void saveByteArray(byte array[][]) throws Exception {
    writeType(TYPE_BYTEA2);
    for (byte a[] : array) saveByteArray(a);
  }
  
  
  public float[] loadFloatArray(float array[]) throws Exception {
    checkType(TYPE_FLOATA);
    final int s = loadInt();
    if (s == -1) return null;
    if (array == null || array.length != s) array = new float[s];
    for (int n = 0; n < s; n++) array[n] = loadFloat();
    return array;
  }
  
  
  public void saveFloatArray(float array[]) throws Exception {
    writeType(TYPE_FLOATA);
    if (array == null) { saveInt(-1); return; }
    saveInt(array.length);
    for (float f : array) saveFloat(f);
  }
  
  
  public float loadFloat() throws Exception {
    checkType(TYPE_FLOAT);
    return in.readFloat();
  }
  
  
  public void saveFloat(float f) throws Exception {
    writeType(TYPE_FLOAT);
    out.writeFloat(f);
  }
  
  
  public int loadInt() throws Exception {
    checkType(TYPE_INT);
    return in.readInt();
  }
  

  public void saveInt(int i) throws Exception {
    writeType(TYPE_INT);
    out.writeInt(i);
  }
  
  
  public boolean loadBool() throws Exception {
    checkType(TYPE_BOOL);
    return in.readBoolean();
  }
  
  
  public void saveBool(boolean b) throws Exception {
    writeType(TYPE_BOOL);
    out.writeBoolean(b);
  }
  
  
  public String loadString() throws Exception {
    checkType(TYPE_STRING);
    final int len = in.readInt();
    if (len == -1) return null;
    final byte chars[] = new byte[len];
    in.read(chars);
    return new String(chars);
  }
  
  
  public void saveString(String s) throws Exception {
    writeType(TYPE_STRING);
    if (s == null) { out.writeInt(-1); return; }
    final byte chars[] = s.getBytes();
    out.writeInt(chars.length);
    out.write(chars);
  }
  
  
  
  /**  Saving and Loading series of objects-
    */
  public void saveObjects(Series objects) throws Exception {
    writeType(TYPE_OBJECTS);
    if (objects == null) { saveInt(-1); return; }
    saveInt(objects.size());
    for (Object o : objects) saveObject((Saveable) o);
  }
  
  
  public Series loadObjects(Series objects) throws Exception {
    checkType(TYPE_OBJECTS);
    final int count = loadInt();
    if (count == -1) return null;
    for (int n = count; n-- > 0;) objects.add(loadObject());
    return objects;
  }
  
  
  public void saveObjectArray(Object objects[]) throws Exception {
    writeType(TYPE_OBJECTA);
    if (objects == null) { saveInt(-1); return; }
    saveInt(objects.length);
    for (Object o : objects) saveObject((Saveable) o);
  }
  
  
  public Object[] loadObjectArray(Class typeClass) throws Exception {
    checkType(TYPE_OBJECTA);
    final int count = loadInt();
    if (count == -1) return null;
    final Object objects[] = (Object[]) Array.newInstance(typeClass, count);
    for (int n = 0; n < count; n++) objects[n] = loadObject();
    return objects;
  }
  
  
  public void saveTally(Tally t) throws Exception {
    writeType(TYPE_TALLY);
    saveInt(t.size());
    for (Object o : t.keys()) {
      saveObject((Saveable) o);
      saveFloat(t.valueFor(o));
    }
  }
  
  
  public Tally loadTally(Tally t) throws Exception {
    checkType(TYPE_TALLY);
    for (int n = loadInt(); n-- > 0;) {
      final Object o = loadObject();
      final float val = loadFloat();
      t.set(o, val);
    }
    return t;
  }
  
  
  public void saveTable(Table t) throws Exception {
    writeType(TYPE_TABLE);
    saveInt(t.size());
    for (Object o : t.entrySet()) {
      java.util.Map.Entry entry = (java.util.Map.Entry) o;
      saveObject(entry.getKey  ());
      saveObject(entry.getValue());
    }
  }
  
  
  public Table loadTable(Table t) throws Exception {
    checkType(TYPE_TABLE);
    for (int n = loadInt(); n-- > 0;) {
      final Object key = loadObject();
      final Object val = loadObject();
      t.put(key, val);
    }
    return t;
  }
  
  
  
  /**  Utility methods for handling enums and common table keys-
    */
  public void saveEnum(Enum e) throws Exception {
    writeType(TYPE_ENUM);
    if (e == null) saveInt(-1);
    else saveInt(e.ordinal());
  }
  
  
  public Enum loadEnum(Enum from[]) throws Exception {
    checkType(TYPE_ENUM);
    final int ID = loadInt();
    return (ID == -1) ? null : from[ID];
  }
  
  
  public void saveEnums(Series enums) throws Exception {
    writeType(TYPE_ENUMS);
    if (enums == null) {saveInt(-1); return; }
    saveInt(enums.size());
    for (Object o : enums) saveEnum((Enum) o);
  }
  
  
  public Series loadEnums(Series enums, Enum from[]) throws Exception {
    checkType(TYPE_ENUMS);
    final int numE = loadInt();
    if (numE == -1) return null;
    for (int i = numE; i-- > 0;) enums.add(loadEnum(from));
    return enums;
  }
  
  
  public void saveKey(Object key) throws Exception {
    writeType(TYPE_KEY);
    if (key instanceof Class) {
      saveInt(0);
      saveClass((Class) key);
    }
    else if (key instanceof String) {
      saveInt(1);
      saveString((String) key);
    }
    else if (key instanceof Saveable) {
      saveInt(2);
      saveObject((Session.Saveable) key);
    }
    else I.complain("KEYS MUST BE CLASSES, STRINGS, OR SAVEABLE!");
  }
  
  
  public Object loadkey() throws Exception {
    checkType(TYPE_KEY);
    final Object key;
    final int keyType = loadInt();
    
    if (keyType == 0) {
      key = loadClass();
    }
    else if (keyType == 1) {
      key = loadString();
    }
    else {
      key = loadObject();
    }
    return key;
  }
  
  
  public static boolean isValidKey(Object o) {
    if (o instanceof Class) return true;
    if (o instanceof String) return true;
    if (o instanceof Saveable) return true;
    return false;
  }
  
}


