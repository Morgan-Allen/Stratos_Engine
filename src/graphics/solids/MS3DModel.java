/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package graphics.solids;
import graphics.common.*;
import graphics.solids.MS3DFile.*;
import util.*;

import java.io.*;
import java.util.Arrays;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;




public class MS3DModel extends SolidModel {
  
  
  final static boolean FORCE_DEFAULT_MATERIAL = false;
  private static boolean verbose = false, timeVerbose = false;
  
  private String basePath, filePath, xmlPath, xmlName;
  private XML config;
  
  private ModelData data;
  private ModelMesh mesh;
  private ModelNode root;
  private MS3DFile ms3d;
  

  private MS3DModel(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    super(path+fileName, sourceClass);
    basePath = path;
    filePath = path+fileName;
    xmlPath  = path+xmlFile;
    this.xmlName = xmlName;
    this.setKeyFile(filePath);
    this.setKeyFile(xmlPath );
  }
  
  
  public static MS3DModel loadFrom(
    String path, String fileName, Class sourceClass,
    String xmlFile, String xmlName
  ) {
    return new MS3DModel(path, fileName, sourceClass, xmlFile, xmlName);
  }
  
  
  
  protected State loadAsset() {
    
    long initTime = I.getTime();
    if (timeVerbose) I.say("Loading "+filePath);
    
    try {

      final FileInputStream FIS = new FileInputStream(new File(filePath));
      final BufferedInputStream BIS = new BufferedInputStream(FIS);
      final DataInput0 input = new DataInput0(BIS, true);
      ms3d = new MS3DFile(input);
      
      if (xmlName != null) {
        XML xml = XML.load(xmlPath);
        config = xml.matchChildValue("name", xmlName);
      }
      else config = null;
      input.close();
    }
    catch (Exception e) {
      I.report(e);
      return state = State.ERROR;
    }
    if (timeVerbose) I.say("  MS3D: "+(I.getTime() - initTime)+" MS");
    
    data = new ModelData();
    processMaterials();
    if (timeVerbose) I.say("  Materials: "+(I.getTime() - initTime)+" MS");
    
    processMesh();
    if (timeVerbose) I.say("  Mesh: "+(I.getTime() - initTime)+" MS");
    
    processJoints();
    if (timeVerbose) I.say("  Joints: "+(I.getTime() - initTime)+" MS");
    
    super.compileModel(new Model(data));
    if (config != null) loadAttachPoints(config.child("attachPoints"));
    
    if (timeVerbose) I.say("  Model: "+(I.getTime() - initTime)+" MS");
    return state = super.loadAsset();
  }
  
  
  protected State disposeAsset() {
    return super.disposeAsset();
  }
  


  private void processMaterials() {
    if (! FORCE_DEFAULT_MATERIAL) for (MS3DMaterial mat : ms3d.materials) {
      ModelMaterial m = new ModelMaterial();
      m.id        = mat.name;
      m.ambient   = color(mat.ambient );
      m.diffuse   = color(mat.diffuse );
      m.emissive  = color(mat.emissive);
      m.specular  = color(mat.specular);
      m.shininess = mat.shininess;
      m.opacity   = mat.transparency;
      m.type      = MaterialType.Phong;
      
      if (m.opacity == 0) {
        m.opacity = 1;
      }
      if (! mat.texture.isEmpty()) {
        ModelTexture tex = new ModelTexture();
        if (mat.texture.startsWith(".\\") || mat.texture.startsWith("//")) {
          mat.texture = mat.texture.substring(2);
        }
        if (verbose) I.say(""+mat.texture);
        tex.fileName = Assets.safePath(basePath+mat.texture);
        this.setKeyFile(tex.fileName);
        tex.id = mat.texture;
        tex.usage = ModelTexture.USAGE_DIFFUSE;
        m.textures = new Array <ModelTexture> ();
        m.textures.add(tex);
      }
      data.materials.add(m);
    }
    
    ModelMaterial mat = new ModelMaterial();
    mat.id        = "default";
    mat.ambient   = new Color(0.8f, 0.8f, 0.8f, 1f);
    mat.diffuse   = new Color(0.8f, 0.8f, 0.8f, 1f);
    mat.emissive  = new Color(0, 0, 0, 0);
    mat.specular  = new Color(0, 0, 0, 0);
    mat.shininess = 0;
    mat.opacity   = 1;
    mat.type      = MaterialType.Phong;
    data.materials.add(mat);
  }
  
  
  private static Color color(float[] col) {
    if (col[0] == 0 && col[1] == 0 && col[2] == 0) return null;
    return new Color(col[0], col[1], col[2], col[3]);
  }
  
  
  private void processMesh() {
    mesh = new ModelMesh();
    mesh.id = "mesh";

    data.meshes.add(mesh);

    Array<VertexAttribute> attrs = new Array<VertexAttribute>(
        VertexAttribute.class);
    attrs.add(VertexAttribute.Position());
    attrs.add(VertexAttribute.Normal());
    attrs.add(VertexAttribute.TexCoords(0));
    attrs.add(VertexAttribute.BoneWeight(0));
    attrs.add(VertexAttribute.BoneWeight(1));
    attrs.add(VertexAttribute.BoneWeight(2));

    mesh.attributes = attrs.toArray();

    final int n = 14;
    float[] verts = new float[ms3d.triangles.length * 3 * n];

    int p = 0;
    {
      for (MS3DTriangle tri : ms3d.triangles) {

        for (int j = 0; j < 3; j++) {
          MS3DVertex vert = ms3d.vertices[tri.indices[j]];
          
          verts[p * n + 0] = vert.vertex[0];
          verts[p * n + 1] = vert.vertex[1];
          verts[p * n + 2] = vert.vertex[2];
          
          verts[p * n + 3] = tri.normals[j][0];
          verts[p * n + 4] = tri.normals[j][1];
          verts[p * n + 5] = tri.normals[j][2];
          
          verts[p * n + 6] = tri.u[j];
          verts[p * n + 7] = tri.v[j];
          
          // there are actually 4 bone weights, but we use only 3

          verts[p * n + 8] = vert.boneid;
          if(vert.boneIds == null || vert.boneIds[0] == -1) {
            verts[p * n + 9] = 1f;
          }
          else {
            verts[p * n + 9] = vert.weights[0] / 100f;
            
            verts[p * n + 10] = vert.boneIds[0];
            verts[p * n + 11] = vert.weights[1] / 100f;
            
            verts[p * n + 12] = vert.boneIds[1];
            verts[p * n + 13] = vert.weights[2] / 100f;
          }            
          
          tri.indices[j] = (short) p;
          p++;
        }
      }
    }

    mesh.vertices = verts;

    root = new ModelNode();
    root.id = "node";
    root.meshId = "mesh";
    final float scale = config == null ? 1 : config.getFloat("scale");
    //I.say("Scale for "+this.filePath+" is "+scale);
    root.scale = new Vector3(scale, scale, scale);

    ModelMeshPart[] parts = new ModelMeshPart[ms3d.groups.length];
    ModelNodePart[] nparts = new ModelNodePart[ms3d.groups.length];
    
    int k = 0;
    for (MS3DGroup group : ms3d.groups) {
      final ModelMeshPart part = new ModelMeshPart();
      part.id = group.name;
      part.primitiveType = GL20.GL_TRIANGLES;
      part.indices = new short[group.trindices.length * 3];
      
      final short[] trindices = group.trindices;

      for (int i = 0; i < trindices.length; i++) {
        part.indices[i * 3 + 0] = ms3d.triangles[trindices[i]].indices[0];
        part.indices[i * 3 + 1] = ms3d.triangles[trindices[i]].indices[1];
        part.indices[i * 3 + 2] = ms3d.triangles[trindices[i]].indices[2];
      }

      final ModelNodePart npart = new ModelNodePart();
      int matID = group.materialIndex;
      if (FORCE_DEFAULT_MATERIAL) matID = -1;
      npart.meshPartId = group.name;
      npart.materialId = (matID == -1) ? "default" : ms3d.materials[matID].name;
      npart.bones = new ArrayMap();
      
      parts[k] = part;
      nparts[k] = npart;
      k++;
      // nparts[]
    }
    mesh.parts = parts;
    root.parts = nparts;

    data.nodes.add(root);
  }
  
  
  private void processJoints() {

    final ModelAnimation animation = new ModelAnimation();
    animation.id = AnimNames.FULL_RANGE;

    ArrayMap<String, ModelNode> lookup = new ArrayMap<String, ModelNode>(32);
    if (verbose) I.say("FPS: " + ms3d.fAnimationFPS); // whatever that is...
    
    for (int i = 0; i < ms3d.joints.length; i++) {
      MS3DJoint jo = ms3d.joints[i];
      for (ModelNodePart part : root.parts) {
        part.bones.put(jo.name, jo.cmatrix.cpy());
      }

      ModelNode mn = new ModelNode();
      
      mn.id = jo.name;
      mn.meshId = "mesh";
      mn.rotation = jo.lmatrix.getRotation(new Quaternion());
      mn.translation = jo.lmatrix.getTranslation(new Vector3());
      mn.scale = new Vector3(1, 1, 1);

      ModelNode parent = jo.parentName.isEmpty() ? root : lookup
          .get(jo.parentName);
      
      addChild(parent, mn);
      lookup.put(mn.id, mn);

      ModelNodeAnimation ani = new ModelNodeAnimation();
      ani.nodeId = mn.id;
      ani.translation = new Array();
      ani.rotation    = new Array();
      

      for (int j = 0; j < jo.positions.length; j++) {
        ModelNodeKeyframe pos = new ModelNodeKeyframe();
        ModelNodeKeyframe rot = new ModelNodeKeyframe();
        
        pos.keytime = rot.keytime = jo.rotations[j].time;
        
        final Vector3 posO = new Vector3(jo.positions[j].data);
        posO.mul(jo.lmatrix);
        pos.value = posO;
        ani.translation.add(pos);
        
        final Quaternion rotO = MS3DFile.fromEuler(jo.rotations[j].data);
        rotO.mulLeft(jo.lmatrix.getRotation(new Quaternion()));
        rot.value = rotO;
        ani.rotation.add(rot);
      }
      
      float lastTime = Float.NEGATIVE_INFINITY;
      for (ModelNodeKeyframe frame : ani.translation) {
        if (lastTime > frame.keytime) I.say("PROBLEM!");
        lastTime = frame.keytime;
      }
      
      lastTime = Float.NEGATIVE_INFINITY;
      for (ModelNodeKeyframe frame : ani.rotation) {
        if (lastTime > frame.keytime) I.say("PROBLEM!");
        lastTime = frame.keytime;
      }
      
      animation.nodeAnimations.add(ani);
    }
    
    data.animations.add(animation);
    loadKeyframes(animation);
    
    if (verbose) {
      I.say("MODEL IS: "+filePath);
      I.say("  TOTAL ANIMATIONS LOADED: "+data.animations.size);
    }
  }
  
  
  private void loadKeyframes(ModelAnimation animation) {
    if (animation == null || config == null) return;
    
    final XML animConfig = config.child("animations");
    float FPS = animConfig.getFloat("fps");
    if (FPS == 0 || FPS == 1) FPS = 1.0f;
    this.rotateOffset = animConfig.getFloat("rotate");
    
    /*
    if (verbose) for (ModelNodeAnimation node : animation.nodeAnimations) {
      I.add("\n  Total animations in "+node.nodeId+": "+node.keyframes.size);
      I.add(" (");
      for (ModelNodeKeyframe frame : node.keyframes) {
        I.add(" "+frame.keytime);
      }
      I.add(")");
    }
    //*/
    
    addLoop: for (XML animXML : animConfig.children()) {
      //
      // First, check to ensure that this animation has an approved name:
      final String name = animXML.value("name");
      if (! Sprite.isValidAnimName(name)) I.say(
        "WARNING: ANIMATION WITH IRREGULAR NAME: "+name+
        " IN MODEL: "+filePath
      );
      for (ModelAnimation oldAnim : data.animations) {
        if (oldAnim.id.equals(name)) continue addLoop;
      }
      
      // Either way, define the data-
      final float
        animStart  = Float.parseFloat(animXML.value("start"   )) / FPS,
        animEnd    = Float.parseFloat(animXML.value("end"     )) / FPS,
        animLength = Float.parseFloat(animXML.value("duration"));
      
      final ModelAnimation anim = new ModelAnimation();
      anim.id = name;

      // scaling for exact duration
      float scale = animLength / (animEnd - animStart);
      
      for (ModelNodeAnimation node : animation.nodeAnimations) {
        final ModelNodeAnimation nd = new ModelNodeAnimation();
        nd.nodeId = node.nodeId;
        
        if (verbose) I.say("  "+node.nodeId+" ("+name+")");
        nd.rotation    = new Array();
        nd.translation = new Array();
        nd.scaling     = new Array();
        putFrames(nd.rotation   , node.rotation   , animStart, animEnd, scale);
        putFrames(nd.translation, node.translation, animStart, animEnd, scale);
        putFrames(nd.scaling    , node.scaling    , animStart, animEnd, scale);
        anim.nodeAnimations.add(nd);
        
        /*
        if (nd.rotation.size == 0) continue;
        I.say("  Scaled frames are: ");
        for (ModelNodeKeyframe frame : nd.rotation) {
          I.say("    : "+frame.keytime);
        }
        I.say("  Done");
        //*/
      }
      
      if (verbose) {
        I.say("  Adding animation with name: "+name);
        I.say("  Start/end:                  "+animStart+"/"+animEnd);
      }
      data.animations.add(anim);
    }
  }
  
  
  private void putFrames(
    Array <?> newFrames, Array <?> oldFrames,
    float animStart, float animEnd, float scale
  ) {
    if (oldFrames == null) return;
    float lastTime = Float.NEGATIVE_INFINITY;
    
    for (ModelNodeKeyframe frame : (Array <ModelNodeKeyframe>) oldFrames) {
      
      if (frame.keytime >= animStart && frame.keytime <= animEnd) {
        // trimming the beginning and scaling
        ModelNodeKeyframe copy = new ModelNodeKeyframe();
        copy.keytime = frame.keytime - animStart;
        copy.keytime *= scale;
        copy.value   = frame.value;
        ((Array) newFrames).add(copy);
        
        if (frame.keytime < lastTime) {
          I.say("PROBLEM!");
        }
        lastTime = frame.keytime;
      }
    }
  }
  
  
  private static void addChild(ModelNode parent, ModelNode child) {
    if (parent.children == null) {
      parent.children = new ModelNode[] { child };
    } else {
      parent.children = Arrays.copyOf(parent.children,
          parent.children.length + 1);
      parent.children[parent.children.length - 1] = child;
    }
  }
}









