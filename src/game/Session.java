/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import util.*;
import java.io.*;
import java.lang.reflect.*;



//  TODO:  See if it's possible to check for correct use of the cacheInstance
//  method?  Technically there's already a check for that below, but it's
//  better to catch early if possible.


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
    verbose    = false,
    saveCounts = false,
    checkType  = false;
  
  final static int
    CLASS_CAPACITY  = 200,
    OBJECT_CAPACITY = 50000,
    
    OP_NONE      = -1,
    OP_SAVE      =  0,
    OP_SAVE_DONE =  1,
    OP_LOAD      =  2,
    OP_LOAD_DONE =  3;
  
  private Table             <Class <?> , Vars.Int>
    classCounts = new Table <Class <?> , Vars.Int> (CLASS_CAPACITY );
  final Table               < Saveable , Integer >
    saveIDs     = new Table < Saveable , Integer > (OBJECT_CAPACITY);
  final Table               < Class <?>, Integer >
    classIDs    = new Table < Class <?>, Integer > (CLASS_CAPACITY );
  final Table               < Integer  , Saveable>
    loadIDs     = new Table < Integer  , Saveable> (OBJECT_CAPACITY);
  final Table               < Integer  , Object  >
    loadMethods = new Table < Integer  , Object  > (CLASS_CAPACITY );
  private int
    nextObjectID =  0,
    nextClassID  =  0,
    lastObjectID = -1;
  
  private Saveable items[] = new Saveable[0];
  
  private int operation = OP_NONE;
  private DataOutputStream out   ;
  private DataInputStream  in    ;
  private DataOutputStream counts;
  private int bytesIn = 0, bytesOut = 0, fileSize = 0;
  
  
  
  /**  Methods for saving and loading session data:
    */
  public static Session saveSession(
    String saveFile, Saveable... items
  ) throws Exception {
    final Session s = new Session();
    s.out = new DataOutputStream(new BufferedOutputStream(
      new FileOutputStream(saveFile))
    );
    if (saveCounts) s.counts = new DataOutputStream(new BufferedOutputStream(
      new FileOutputStream(saveFile+".counts_out")
    ));
    
    Assets.clearReferenceIDs();
    s.operation = OP_SAVE;
    
    s.items = items;
    s.saveInt(items.length);
    for (Saveable item : items) s.saveObject(item);
    s.finish();
    
    if (saveCounts) {
      I.say("\nDISPLAYING TOTAL SAVE COUNTS:");
      for (Class CC : s.classCounts.keySet()) {
        final Vars.Int count = s.classCounts.get(CC);
        I.say("  Saved "+count.val+" of "+CC.getName());
      }
    }
    return s;
  }
  
  
  public static Session loadSession(String saveFile, boolean loadNow) {
    final Session s = new Session();
    
    final File asFile = new File(saveFile);
    try {
      s.in = new DataInputStream(new BufferedInputStream(
        new FileInputStream(asFile))
      );
      if (saveCounts) s.counts = new DataOutputStream(new BufferedOutputStream(
        new FileOutputStream(saveFile+".counts_in")
      ));
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    
    s.fileSize = (int) asFile.length();
    s.operation = OP_LOAD;
    
    final Thread loadThread = new Thread() {
      public void run() {
        try {
          Assets.clearReferenceIDs();
          final int numItems = s.loadInt();
          s.items = new Saveable[numItems];
          for (int n = 0; n < numItems; n++) s.items[n] = s.loadObject();
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
  
  
  public static boolean fileExists(String filename) {
    final File asFile = new File(filename);
    return asFile.exists();
  }
  
  
  public float loadProgress() {
    return ((float) bytesIn) / fileSize;
  }
  
  
  public void finish() throws Exception {
    saveIDs .clear();
    classIDs.clear();
    loadIDs .clear();
    loadMethods.clear();
    
    if (counts != null) {
      counts.flush();
      counts.close();
    }
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
  
  
  public boolean savingDone () { return operation == OP_SAVE_DONE; }
  public boolean loadingDone() { return operation == OP_LOAD_DONE; }
  
  private Session() {}
  public Saveable[] loaded() { return items; }
  
  
  
  /**  NOTE:  Classes that implement this interface must ALSO implement a
    *  public constructor taking a Session as it's sole argument, with a
    *  cacheInstance call made right away, OR, a static loadConstant method
    *  taking the Session as it's argument.  In the former case, they must also
    *  call cacheInstance() as soon as possible in the root constructor.
    */
  public static interface Saveable {
    void saveState(Session s) throws Exception;
  }
  
  
  private static Object loadMethodFor(Class loadClass) {
    Object loadMethod = null;
    try {
      loadMethod = loadClass.getConstructor(Session.class);
      //final Constructor c = (Constructor) loadMethod;
    }
    catch (NoSuchMethodException e) {}
    if (loadMethod == null) try {
      loadMethod = loadClass.getMethod("loadConstant", Session.class);
      if (! Modifier.isStatic(((Method) loadMethod).getModifiers())) {
        I.say("WARNING: loadConstant method is not static! "+loadClass);
        loadMethod = null;
      }
    }
    catch (NoSuchMethodException e) {}
    return loadMethod;
  }
  
  
  public static void checkSaveable(String className) {
    try {
      final Class <?> loadClass = Class.forName(className);
      final int mods = loadClass.getModifiers();
      if (! Saveable.class.isAssignableFrom(loadClass)) return;
      if (loadClass.isInterface() || Modifier.isAbstract(mods)) return;
      final Object loadMethod = loadMethodFor(loadClass);
      
      if (loadMethod == null) I.complain(
        "WARNING:  Class "+className+" implements Saveable interface but does "+
        "not implement a public Constructor, or static loadConstant method, "+
        "with a Session argument."
      );
      if (verbose) I.say("Saveable class okay: "+loadClass.getSimpleName());
    }
    catch (ClassNotFoundException e) {}
  }
  
  
  
  /**  Saving and Loading of classes themselves-
    */
  public void saveClass(Class c) throws Exception {
    if (c == null) { out.writeInt(-1); return; }
    final Integer classID = classIDs.get(c);
    if (classID == null) {
      //
      //  Then we need to save the full binary name of this class and cache
      //  it's ID-
      //I.say("Saving new class- "+c.getName()+" ID: "+nextClassID);
      out.writeInt(nextClassID);
      Assets.writeString(out, c.getName());
      classIDs.put(c, nextClassID++);
    }
    else out.writeInt(classID);
  }
  
  
  public Class loadClass() throws Exception {
    final int classID = in.readInt();
    if (classID == -1) return null;
    final Object loadMethod = loadMethod(classID);
    if (loadMethod instanceof Constructor)
      return ((Constructor) loadMethod).getDeclaringClass();
    else if (loadMethod instanceof Method)
      return ((Method) loadMethod).getDeclaringClass();
    else return (Class) loadMethod;
  }
  
  
  private Object loadMethod(final int classID) throws Exception {
    Object loadMethod = loadMethods.get(classID);
    if (loadMethod == null) {
      final String    className = Assets.readString(in);
      final Class <?> loadClass = Class.forName(className);
      loadMethod = loadMethodFor(loadClass);
      if (loadMethod == null) loadMethod = loadClass;
      loadMethods.put(classID, loadMethod);
    }
    return loadMethod;
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
    
    final Integer saveID = saveIDs.get(s);
    if (saveID == null) {
      
      //I.say("Saving new object, class- "+s.getClass().getName());
      final int nextID = nextObjectID++;
      saveIDs.put(s, nextID);
      out.writeInt(nextID);
      saveClass(s.getClass());
      
      final int initBytes = bytesOut;
      s.saveState(this);
      
      if (verbose) I.say(
        "Saved new object: "+s.getClass().getName()+
        " total bytes saved: "+(bytesOut - initBytes)
      );
      if (counts != null) {
        counts.writeChars("\n  "+nextID+" ("+s.getClass());
        counts.writeChars("): "+(bytesOut - initBytes));
        
        Vars.Int count = classCounts.get(s.getClass());
        if (count == null) {
          classCounts.put(s.getClass(), count = new Vars.Int());
        }
        count.val++;
      }
    }
    else {
      out.writeInt(saveID);
    }
  }
  
  
  
  /**  This method is intended to help avoid self-referential loop conditions by
    *  being called by Saveable objects IMMEDIATELY after being initialised and
    *  BEFORE any member fields or variables have been loaded.  (NOTE:  This
    *  does not apply to objects created by a static loadConstant() method,
    *  or to any subclasses invoking the constructor with super calls.)
    */
  public void cacheInstance(Saveable s) {
    if (s == null) I.complain("CANNOT CACHE NULL INSTANCE!");
    loadIDs.put(lastObjectID, s);
  }
  
  //  This object exists for a similar reason (see below.)
  final static Saveable MARK_LOCK = new Saveable() {
    public void saveState(Session s) throws Exception {}
  };
  
  
  public Saveable loadObject() throws Exception {
    checkType(TYPE_OBJECT);
    
    //I.say("Loading object...");
    final int loadID = in.readInt();
    if (loadID == -1) return null;
    //I.say("Loading object of ID: "+loadID);
    
    Saveable loaded = loadIDs.get(loadID);
    if (loaded != null) {
      //I.say("Loading existing object: "+loaded);
      if (loaded == MARK_LOCK) {
        //  Hopefully this can't happen now...
        I.complain(
          "LOADING HAS HIT A SELF-REFERENTIAL LOOP CONDITION..."
        );
      }
      else {
        return loaded;
      }
    }
    //
    //  We use the MARK_LOCK Object as a placeholder to check if a given
    //  Saveable is being referred to before it can be cached, indicating a
    //  self-referential loop condition.
    loadIDs.put(lastObjectID = loadID, MARK_LOCK);
    final Object loadMethod = loadMethod(in.readInt());
    Class loadClass = null;
    final int initBytes = bytesIn;
    try {
      if (loadMethod instanceof Constructor) {
        final Constructor loadObject = (Constructor) loadMethod;
        loadClass = loadObject.getDeclaringClass();
        if (verbose) I.say("Loading new object of type "+loadClass.getName());
        loaded = (Saveable) loadObject.newInstance(this);
        if (verbose) I.say("Finished loading "+loadClass.getName());
      }
      else if (loadMethod instanceof Method) {
        final Method loadConstant = (Method) loadMethod;
        loadClass = loadConstant.getDeclaringClass();
        if (verbose) I.say("Loading new constant, type "+loadClass.getName());
        loaded = (Saveable) loadConstant.invoke(null, this);
        cacheInstance(loaded);
        if (verbose) I.say("Finished loading "+loadClass.getName());
      }
      else I.complain("NOT A SUITABLE LOADING METHOD: "+loadMethod);
    }
    catch (InstantiationException e) { I.complain(
      "PROBLEM WITH "+loadClass.getName()+"\n"+
      "ALL CLASSES IMPLEMENTING SAVEABLE MUST IMPLEMENT A PUBLIC CONSTRUCTOR "+
      "TAKING THE SESSION AS IT'S SOLE ARGUMENT, OR A STATIC loadConstant("+
      "Session s) METHOD THAT RETURNS A SAVEABLE OBJECT. THANK YOU."
    ); }
    final Saveable cached = loadIDs.get(loadID);
    if (cached != loaded) I.complain(
      "PROBLEM WITH "+loadClass.getName()+"\n"+
      "ALL OBJECTS IMPLEMENTING SAVEABLE SHOULD CACHE THEMSELVES USING THE "+
      "Session.cacheInstance(Saveable s) METHOD *IMMEDIATELY* AFTER BEING "+
      "INSTANCED DURING LOADING- I.E, FIRST THING IN THE ROOT CONSTRUCTOR, "+
      "BEFORE ANY MEMBER FIELDS OR VARIABLES HAVE BEEN LOADED.  (THIS DOES "+
      "NOT APPLY TO THOSE THAT IMPLEMENT loadConstant(Session s).)  THANK YOU."
    );
    
    if (counts != null) {
      final Saveable s = (Saveable) loaded;
      counts.writeChars("\n  "+loadID+" ("+s.getClass());
      counts.writeChars("): "+(bytesIn - initBytes));
      counts.flush();
    }
    return loaded;
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
      //I.say("READING "+TYPE_DESC[type]);
    }
  }
  
  private void writeType(int type) throws Exception {
    if (! checkType) return;
    
    out.writeInt(type);
    //I.say("WRITING "+TYPE_DESC[type]);
  }
  
  
  public DataOutputStream output() { return out; }
  public DataInputStream  input () { return in ; }
  
  
  public int bytesIn () { return bytesIn ; }
  public int bytesOut() { return bytesOut; }
  
  
  public void loadByteArray(byte array[]) throws Exception {
    checkType(TYPE_BYTEA1);
    bytesIn += array.length;
    in.read(array);
  }
  
  
  public void saveByteArray(byte array[]) throws Exception {
    writeType(TYPE_BYTEA1);
    out.write(array);
    bytesOut += array.length;
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
    bytesIn += 4;
    return in.readFloat();
  }
  
  
  public void saveFloat(float f) throws Exception {
    writeType(TYPE_FLOAT);
    out.writeFloat(f);
    bytesOut += 4;
  }
  
  
  public int loadInt() throws Exception {
    checkType(TYPE_INT);
    bytesIn += 4;
    return in.readInt();
  }
  

  public void saveInt(int i) throws Exception {
    writeType(TYPE_INT);
    out.writeInt(i);
    bytesOut += 4;
  }
  
  
  public boolean loadBool() throws Exception {
    checkType(TYPE_BOOL);
    bytesIn += 1;
    return in.readBoolean();
  }
  
  
  public void saveBool(boolean b) throws Exception {
    writeType(TYPE_BOOL);
    out.writeBoolean(b);
    bytesOut += 1;
  }
  
  
  public String loadString() throws Exception {
    checkType(TYPE_STRING);
    final int len = in.readInt();
    if (len == -1) return null;
    final byte chars[] = new byte[len];
    in.read(chars);
    bytesIn += len + 4;
    return new String(chars);
  }
  
  
  public void saveString(String s) throws Exception {
    writeType(TYPE_STRING);
    if (s == null) { out.writeInt(-1); return; }
    final byte chars[] = s.getBytes();
    out.writeInt(chars.length);
    out.write(chars);
    bytesOut += chars.length + 4;
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


