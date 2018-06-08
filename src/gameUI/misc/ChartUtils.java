/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package gameUI.misc;
import game.*;
import graphics.common.*;
import graphics.solids.*;
import graphics.widgets.*;
import graphics.charts.*;
import util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;



public class ChartUtils {
  
  
  final public static String
    LOAD_PATH        = "media/Charts/",
    PLANET_LOAD_FILE = "sectors.xml",
    STARS_LOAD_FILE  = "coordinates.xml";
  
  
  /**  Method for loading a carousel-display of homeworlds:
    */
  public static void updateWorldsCarousel(
    Carousel carousel, HUD UI, World world
  ) {
    carousel.clearEntries();
    
    for (final WorldLocale locale : world.locales()) {
      if (! locale.homeland()) continue;
      
      final UIGroup worldInfo = new UIGroup(UI);
      worldInfo.stretch = false;
      
      final Image b = new Image(UI, locale.planetImage());
      b.alignAcross(-0.5f, 1.5f);
      b.alignVertical(0, 0);
      b.attachTo(worldInfo);
      carousel.addEntryFor(locale, worldInfo);
    }
  }
  
  
  
  /**  Method for loading sector display information from external XML:
    */
  public static PlanetDisplay createPlanetDisplay(
    final String path, final String file
  ) {
    final PlanetDisplay display = new PlanetDisplay() {
      protected State loadAsset() {
        super.loadAsset();
        if (! stateLoaded()) return State.ERROR;
        loadPlanet(path, file, this);
        return State.LOADED;
      }
    };
    Assets.loadNow(display);
    return display;
  }
  
  
  public static void loadPlanet(
    String path, String file, PlanetDisplay display
  ) {
    final XML xml = XML.load(path+file);
    
    final XML
      modelNode   = xml.child("globeModel"),
      surfaceNode = xml.child("surfaceTex"),
      sectorsNode = xml.child("sectorsTex"),
      keysNode    = xml.child("sectorKeys");
    
    final Class baseClass = ChartUtils.class;
    final String baseName = modelNode.value("name");
    
    final MS3DModel globeModel = MS3DModel.loadFrom(
      path, baseName, baseClass, null, null
    );
    final ImageAsset
      sectorKeys = ImageAsset.fromImage(
        baseClass, baseName+"_sectors_key", path + keysNode.value("name")
      ),
      surfaceTex = ImageAsset.fromImage(
        baseClass, baseName+"_surface", path + surfaceNode.value("name")
      ),
      sectorsTex = ImageAsset.fromImage(
        baseClass, baseName+"_sectors_tex", path + sectorsNode.value("name")
      );
    Assets.loadNow(globeModel);
    Assets.loadNow(sectorKeys);
    Assets.loadNow(surfaceTex);
    Assets.loadNow(sectorsTex);
    display.attachModel(globeModel, surfaceTex, sectorsTex, sectorKeys);
    
    final XML sectors = xml.child("sectors");
    for (XML sector : sectors.children()) {
      final String name = sector.value("name");
      final Colour key = new Colour().set(
        sector.getFloat("R"),
        sector.getFloat("G"),
        sector.getFloat("B"),
        1
      );
      display.attachSector(name, key);
    }
  }
  
  
  
  /**  Method for loading object coordinates from an external XML file:
    */
  public static StarField createStarField(
    final String path, final String file
  ) {
    final StarField field = new StarField() {
      protected State loadAsset() {
        super.loadAsset();
        if (! stateLoaded()) return State.ERROR;
        loadStarfield(path, file, this);
        return State.LOADED;
      }
    };
    Assets.loadNow(field);
    return field;
  }
  
  
  public static void loadStarfield(
    String path, String file, StarField display
  ) {
    final XML xml = XML.load(path+file);
    
    //  First, get the texture atlas for field objects, along with textures for
    //  the upper axis and sectors chart-
    final XML
      imgNode   = xml.child("imageField" ),
      axisNode  = xml.child("axisImage"  ),
      chartNode = xml.child("sectorImage");
    final String
      imgFile   = path + imgNode  .value("name"),
      axisFile  = path + axisNode .value("name"),
      chartFile = path + chartNode.value("name");
    final Texture
      image    = ImageAsset.getTexture(imgFile),
      axisImg  = ImageAsset.getTexture(axisFile),
      chartImg = ImageAsset.getTexture(chartFile);
    
    final int
      gridW = imgNode.getInt("gridU"),
      gridH = imgNode.getInt("gridV");
    final float
      fieldSize = chartNode.getFloat("size");
    
    //  Then, load up the array of different star types and the specific
    //  systems associated-
    final Table <String, int[]> logoImages = new Table <String, int[]> ();
    for (XML type : xml.child("logoTypes").children()) {
      final String name = type.value("name");
      final int coords[] = new int[] {
        type.getInt("imgU"),
        type.getInt("imgV")
      };
      logoImages.put(name, coords);
    }
    
    display.setupWith(chartImg, axisImg, fieldSize);
    
    final XML systems = xml.child("systems");
    for (XML system : systems.children()) {
      final String
        name = system.value("name"),
        type = system.value("type");
      
      final Vec3D position = new Vec3D(
        system.getFloat("x"),
        system.getFloat("y"),
        system.getFloat("z")
      );
      display.addFieldObject(
        image, null,
        gridW, gridH, system.getInt("imgU"), system.getInt("imgV"),
        0.67f, 0, 100 * 0.67f, position
      );
      
      final int starImg[] = logoImages.get(type);
      display.addFieldObject(
        image, name,
        gridW, gridH, starImg[0], starImg[1],
        1, 0, 0, position
      );
    }
    
    final int selectCoords[] = logoImages.get("Selected");
    display.setSelectObject(
      image, gridW, gridH, selectCoords[0], selectCoords[1]
    );
    
    final int[][] starTypes = new int[1][];
    for (int[] type : logoImages.values()) { starTypes[0] = type; break; }
    display.addRandomScatter(image, gridW, gridH, starTypes, 10, 1);
  }
  
  
  
  /**  Methods for rendering planet and starfield displays-
    */
  public static void renderPlanet(
    PlanetDisplay display, UINode displayArea, WidgetsPass pass
  ) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    pass.end();
    
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glDepthMask(true);
    
    display.renderWith(
      pass.rendering, displayArea.trueBounds(), UIConstants.INFO_FONT
    );
    display.checkForAssetRefresh();
    
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glDepthMask(false);
    
    pass.begin();
  }
  
  
  public static void renderStars(
    StarField field, UINode displayArea, WidgetsPass pass
  ) {
    //  TODO:  Fiddling directly with OpenGL calls this way is messy and risky.
    //  See if you can centralise this somewhere (along with the Minimap
    //  rendering.)
    pass.end();
    
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glDepthMask(true);
    
    final Box2D fieldBounds = displayArea.trueBounds();
    field.renderWith(pass.rendering, fieldBounds, UIConstants.INFO_FONT);
    
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glDepthMask(false);
    
    pass.begin();
  }
}


















