/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package util;
import java.lang.reflect.Array;
import java.util.Map;



public class Tally <K> {
  
  
  final Table <K, Float> store = new Table <K, Float> ();
  
  
  public static Tally with(Object... args) {
    Tally t = new Tally();
    t.setWith(args);
    return t;
  }
  
  
  public Tally setWith(Object... args) {
    Object split[][] = Visit.splitByModulus(args, 2);
    for (int i = split[0].length; i-- > 0;) {
      float value = -1;
      Object num = split[1][i];
      if (num instanceof Float  ) value = (Float  ) num;
      if (num instanceof Integer) value = (Integer) num;
      set((K) split[0][i], value);
    }
    return this;
  }
  
  
  public float valueFor(K key) {
    final Float val = store.get(key);
    return val == null ? 0 : val;
  }
  
  
  public boolean hasEntry(K key) {
    return store.get(key) != null;
  }
  
  
  public float set(K key, float value) {
    if (value == 0) store.remove(key);
    else store.put(key, value);
    return value;
  }
  
  
  public float add(float value, K key) {
    final float oldVal = valueFor(key), newVal = oldVal + value;
    return set(key, newVal);
  }
  
  
  public void add(Tally <K> other) {
    for (K k : other.keys()) add(other.valueFor(k), k);
  }
  
  
  public void clear() {
    store.clear();
  }
  
  
  public boolean empty() {
    return store.isEmpty();
  }
  
  
  public Iterable <K> keys() {
    return store.keySet();
  }
  
  
  public K[] keysToArray(Class keyClass) {
    final K array[] = (K[]) Array.newInstance(keyClass, store.size());
    return store.keySet().toArray(array);
  }
  
  
  public K[] positiveKeys(Class keyClass) {
    Batch <K> pos = new Batch();
    for (K k : keys()) {
      float v = valueFor(k);
      if (v > 0) pos.add(k);
    }
    return pos.toArray(keyClass);
  }
  
  
  public K[] negativeKeys(Class keyClass) {
    Batch <K> neg = new Batch();
    for (K k : keys()) {
      float v = valueFor(k);
      if (v < 0) neg.add(k);
    }
    return neg.toArray(keyClass);
  }
  
  
  public K highestValued() {
    K highest = null;
    float bestVal = Float.NEGATIVE_INFINITY;
    
    for (Map.Entry <K, Float> e : store.entrySet()) {
      final float val = e.getValue();
      if (val > bestVal) { bestVal = val; highest = e.getKey(); }
    }
    return highest;
  }
  
  
  public float total() {
    float total = 0;
    for (Map.Entry <K, Float> e : store.entrySet()) {
      total += e.getValue();
    }
    return total;
  }
  
  
  public int size() {
    return store.size();
  }
  
  
  
  public String toString() {
    return store.toString();
  }
}

