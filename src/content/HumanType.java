


package content;
import game.*;
import graphics.common.*;
import graphics.solids.*;
import util.*;



public class HumanType extends ActorType {
  
  
  final static String
    HUMAN_FILE_DIR = "media/Actors/human/",
    HUMAN_XML_FILE = "HumanModels.xml"
  ;
  final static ModelAsset
    HUMAN_MODEL_MALE = MS3DModel.loadFrom(
      HUMAN_FILE_DIR, "male_final.ms3d",
      HumanType.class, HUMAN_XML_FILE, "MalePrime"
    ),
    HUMAN_MODEL_FEMALE = MS3DModel.loadFrom(
      HUMAN_FILE_DIR, "female_final.ms3d",
      HumanType.class, HUMAN_XML_FILE, "FemalePrime"
    ),
    ALL_HUMAN_MODELS[] = { HUMAN_MODEL_MALE, HUMAN_MODEL_FEMALE },
    HUMAN_MODEL_DEFAULT = (ModelAsset) Visit.last(ALL_HUMAN_MODELS)
  ;
  final static ImageAsset HUMAN_BLOOD_SKINS[] = ImageAsset.fromImages(
    HumanType.class, "human_blood_skins", HUMAN_FILE_DIR,
    "skin_blood_desert.gif",
    "skin_blood_wastes.gif",
    "skin_blood_tundra.gif",
    "skin_blood_forest.gif"
  );
  
  
  public ImageAsset costume;
  
  
  public HumanType(String ID, int socialClass) {
    super(ActorAsPerson.class, ID, IS_PERSON_ACT, socialClass);
  }
  
  
  public Type childType() {
    return GameContent.CHILD;
  }
  
  
  public Type nestType() {
    return GameContent.HOLDING;
  }
  
  
  void attachCostume(Class baseClass, String fileName) {
    costume = ImageAsset.fromImage(
      baseClass, "costume_"+entryKey(), HUMAN_FILE_DIR+fileName
    );
  }
  
  
  public Sprite makeSpriteFor(Element e) {
    final ActorAsPerson a = (ActorAsPerson) e;
    ModelAsset model = a.man() ? HUMAN_MODEL_MALE : HUMAN_MODEL_FEMALE;
    ImageAsset skin = HUMAN_BLOOD_SKINS[a.varID() % 4];
    
    SolidSprite s = (SolidSprite) model.makeSprite();
    s.setOverlaySkins(
      AnimNames.MAIN_BODY,
      skin   .asTexture(),
      costume.asTexture()
    );
    String partsAllowed[] = {
      AnimNames.MAIN_BODY,
      //a.gear.device().modelPartID,
      //a.gear.outfit().modelPartID
    };
    for (String groupName : ((SolidModel) model).partNames()) {
      boolean valid = false;
      for (String p : partsAllowed) if (groupName.equals(p)) valid = true;
      if (! valid) s.togglePart(groupName, false);
    }
    return s;
  }
  
  
  public void prepareMedia(Sprite s, Element e) {
    super.prepareMedia(s, e);
  }
  
  
  protected static String generateName(
    String forenames[], String surnames[], String nicknames[]
  ) {
    final StringBuffer s = new StringBuffer();
    
    s.append(Rand.pickFrom(forenames));
    if (nicknames != null) {
      s.append(" '");
      s.append(Rand.pickFrom(nicknames));
      s.append("'");
    }
    if (surnames != null) {
      s.append(" ");
      s.append(Rand.pickFrom(surnames));
    }
    return s.toString();
  }
}


