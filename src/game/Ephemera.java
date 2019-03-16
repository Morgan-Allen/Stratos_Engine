/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package game;
import static game.GameConstants.*;
import graphics.common.*;
import graphics.sfx.*;
import util.*;




/**  This class is used to add transitory or conditional special effects to the
  *  world.
  */
public class Ephemera {
  
  
  /**  Fields, constants, and save/load methods.
    */
  static class Ghost {
    
    int size;
    Vec3D offset = new Vec3D();
    
    float inceptTime;
    float duration = 2.0f;
    
    Target tracked = null;
    Sprite sprite;
  }
  
  
  final AreaMap map;
  
  private boolean active = false;
  private Colour fadeColour = null;
  List <Ghost> ghosts = new List();
  //final Table <AreaMpPatch, List <Ghost>> ghosts = new Table(100);
  
  
  protected Ephemera(AreaMap world) {
    this.map = world;
  }
  
  
  protected void loadState(Session s) throws Exception {
  }
  
  
  protected void saveState(Session s) throws Exception {
  }
  
  
  
  /**  Allowing for screen-fade FX:
    */
  public void applyFadeColour(Colour fade) {
    this.fadeColour = fade;
  }
  
  
  protected void applyScreenFade(Rendering rendering) {
    rendering.foreColour = null;
    if (fadeColour != null) {
      final Colour c = new Colour().set(fadeColour);
      c.a -= 1f / rendering.frameRate();
      if (c.a <= 0) fadeColour = null;
      else rendering.foreColour = fadeColour = c;
    }
  }
  
  
  
  /**  Adding ghost FX to the register-
    */
  public Ghost addGhostFromModel(
    Target e, ModelAsset model, float size, float duration, float alpha
  ) {
    if (model == null || e == null || ! active) return null;
    Sprite s = model.makeSprite();
    e.renderedPosition(s.position);
    s.position.z += e.height() / 2;
    return addGhost(e, s, size, duration, alpha);
  }
  
  
  public Ghost addGhost(
    Sprite s, float size, float duration, float alpha
  ) {
    return addGhost(null, s, size, duration, alpha);
  }
  

  public Ghost addGhost(
    Target e, Sprite s, float size, float duration, float alpha
  ) {
    if (s == null || ! active) return null;
    final Ghost ghost = new Ghost();
    
    ghost.size       = (int) Nums.ceil(size);
    ghost.inceptTime = map.time() + map.timeInUpdate();
    ghost.sprite     = s;
    ghost.duration   = duration;
    ghost.tracked    = e;
    
    if (e != null && e.type().mobile) {
      final Element tracked = (Element) e;
      tracked.renderedPosition(ghost.offset);
      ghost.offset.sub(s.position).scale(-1);
    }
    
    if (alpha < 1) {
      ghost.inceptTime -= duration * (1 - alpha);
    }
    
    ///I.say("Adding ghost, duration: "+duration+", sprite: "+s.model());
    
    ghosts.add(ghost);
    return ghost;
  }
  
  
  public Sprite matchSprite(Target e, ModelAsset key) {
    final Ghost match = matchGhost(e, key);
    return match == null ? null : match.sprite;
  }
  
  
  public Ghost matchGhost(Target e, ModelAsset m) {
    Ghost match = null;
    for (Ghost g : ghosts) {
      if (g.tracked == e && g.sprite.model() == m) { match = g; break; }
    }
    return match;
  }
  
  
  public void updateGhost(Target e, float size, ModelAsset m, float duration) {
    if (e == null || m == null || ! active) return;
    //
    //  Search to see if a ghost exists in this area attached to the same
    //  element and using the same sprite-model.  If so, turn back the incept
    //  time.  Otherwise, initialise (and do the same.)
    Ghost match = matchGhost(e, m);
    if (match == null) match = addGhost(e, m.makeSprite(), size, duration, 1);
    match.inceptTime = map.time() + duration;
  }
  
  
  private boolean trackElement(Ghost ghost, Base base) {
    if (! (ghost.tracked instanceof Element)) return true;
    
    final Vec3D p = ghost.sprite.position;
    final Element m = (Element) ghost.tracked;
    //if (! m.visibleTo(base)) return false;
    
    m.renderedPosition(p);
    p.add(ghost.offset);
    
    return true;
  }
  
  
  
  /**  Actual rendering-
    */
  protected Batch <Ghost> visibleFor(Rendering rendering, Base base, float timeNow) {
    final Batch <Ghost> results = new Batch <Ghost> ();
    this.active = true;
    
    for (Ghost ghost : ghosts) {
      final float
        duration = ghost.duration,
        timeGone = timeNow - ghost.inceptTime;
      
      if (timeGone >= duration || ! trackElement(ghost, base)) {
        ghosts.remove(ghost);
        continue;
      }
      else {
        final Sprite s = ghost.sprite;
        if (! rendering.view.intersects(s.position, ghost.size)) continue;
        float alpha = (duration - timeGone) * 2 / duration;
        s.colour = Colour.transparency(alpha);
        results.add(ghost);
      }
    }
    
    return results;
  }
  
  
  public void renderGhost(Ghost g, Rendering r, Base b) {
    if (g.tracked != null && g.tracked.indoors()) return;
    final Vec3D p = g.sprite.position;
    //sprite.fog = b.intelMap.fogAt((int) p.x, (int) p.y);
    //if (sprite.fog == 0) return;
    g.sprite.readyFor(r);
  }
  
  
  public boolean active() {
    return active;
  }
  
  
  
  /**  Assorted extra utility methods-
    */
  static Vec3D hitPoint(Target applied, boolean hits) {
    final Vec3D HP = applied.renderedPosition(null);
    final float r = applied.radius() / 1, h = applied.height() / 2;
    HP.z += h;
    if (hits) return HP;
    HP.x += Rand.range(-r, r);
    HP.y += Rand.range(-r, r);
    HP.z += Rand.range(-h, h);
    return HP;
  }
  
  
  public static void applyCombatFX(
    Good type, Active uses, Target applied,
    boolean ranged, boolean hits, AreaMap map
  ) {
    if (type == null || uses == null || applied == null) return;
    if (! map.ephemera.active()) return;
    
    final float distance = AreaMap.distance(uses, applied);
    if (ranged) {
      applyShotFX(
        type.shotModel, type.burstModel,
        uses, applied, hits, 1 + (distance * 0.1f), map
      );
    }
    else {
      applyMeleeFX(type.slashModel, uses, applied, map);
    }
  }
  
  
  public static void applyMeleeFX(
    ModelAsset model, Active uses, Target applied, AreaMap map
  ) {
    //
    //  Put in a little 'splash' FX, in the direction of the arc.
    final float r = uses.radius();
    final Sprite slashFX = model.makeSprite();
    uses.renderedPosition(slashFX.position);
    slashFX.scale = r * 2;
    slashFX.position.z += uses.height() / 2;
    map.ephemera.addGhost(uses, slashFX, r, 0.33f, 1);
  }
  
  
  public static void applyShotFX(
    ShotFX.Model model, PlaneFX.Model burstModel,
    Active uses, Target applied,
    boolean hits, float duration, AreaMap map
  ) {
    final ShotFX shot = applyShotFX(
      model, uses, applied, hits, duration, map
    );
    applyBurstFX(burstModel, shot.origin, duration, map);
    applyBurstFX(burstModel, shot.target, duration, map);
  }
  
  
  public static ShotFX applyShotFX(
    ShotFX.Model model, Active uses, Target applied,
    boolean hits, float duration, AreaMap map
  ) {
    final ShotFX shot = (ShotFX) model.makeSprite();
    //  TODO:  Consider setting the fire point manually if the animation state
    //  hasn't matured yet?
    
    final Sprite sprite = ((Element) uses).sprite();
    uses.renderedPosition(sprite.position);
    sprite.attachPoint("fire", shot.origin);
    shot.target.setTo(hitPoint(applied, hits));
    
    shot.position.setTo(shot.origin).add(shot.target).scale(0.5f);
    final float size = shot.origin.sub(shot.target, null).length() / 2;
    
    map.ephemera.addGhost(null, shot, size + 1, duration, 1);
    return shot;
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Vec3D point,
    float duration, float scale, float alpha, AreaMap map
  ) {
    if (model == null) return;
    final Sprite s = model.makeSprite();
    s.position.setTo(point);
    s.scale = scale;
    map.ephemera.addGhost(null, s, 1, duration, alpha);
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Vec3D point, float duration, AreaMap map
  ) {
    applyBurstFX(model, point, duration, 1, 1, map);
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Target point, AreaMap map,
    float heightFraction, float duration
  ) {
    applyBurstFX(model, point, map, heightFraction, 1, 1, duration);
  }
  
  
  public static void applyBurstFX(
    PlaneFX.Model model, Target point, AreaMap map,
    float heightFraction, float scale, float alpha, float duration
  ) {
    final Vec3D pos = point.renderedPosition(null);
    pos.z += point.height() * heightFraction;
    
    final Sprite s = model.makeSprite();
    s.position.setTo(pos);
    s.scale = scale * 2 * point.radius();
    map.ephemera.addGhost(point, s, 1, duration, alpha);
  }
  
  
  public static void applyShieldFX(
    Good type, Active uses, Target attackedBy,
    boolean hits, boolean ranged, AreaMap map
  ) {
    if (uses == null || map == null) return;
    if (type == null || type.shieldModel == null) return;
    //  TODO:  Allow a custom texture and offset to be applied here!
    
    final Sprite visible = map.ephemera.matchSprite(
      uses, type.shieldModel
    );
    final ShieldFX shieldFX;
    if (visible != null) {
      shieldFX = (ShieldFX) visible;
      map.ephemera.updateGhost(uses, 1, type.shieldModel, 2);
    }
    else {
      shieldFX = new ShieldFX(type.shieldModel);
      shieldFX.scale = 0.5f * uses.height();
      uses.renderedPosition(shieldFX.position);
      shieldFX.position.z += uses.height() / 2;
      map.ephemera.addGhost(uses, shieldFX, 1, 2, 1);
    }
    if (attackedBy != null) {
      shieldFX.attachBurstFromPoint(attackedBy.renderedPosition(null), hits);
    }
    else shieldFX.resetGlow();
  }
}





