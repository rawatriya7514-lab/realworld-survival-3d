package com.devagent.realworldsurvival3d;

/**
 * CharacterCustomization - Complete character creation and appearance data model (Phase 1).
 * Features:
 *   - Gender selection (MALE, FEMALE)
 *   - Face shape, skin tone, eyes, eyebrows, nose, mouth
 *   - Hairstyle & hair color, facial hair
 *   - Body proportions (height scale, shoulder width, muscularity)
 *   - Outfits, accessories, boots, armor
 *   - 5 Original Character Presets
 *   - Serialization to/from key-value strings for persistent saving
 *   - Pure Java (100% JVM unit testable)
 */
public class CharacterCustomization {

    public enum Gender { MALE, FEMALE }
    public enum FaceShape { OVAL, ROUND, SHARP, SQUARE }
    public enum EyeColor { DARK_BROWN, AMBER, JADE_GREEN, AZURE_BLUE }
    public enum EyebrowStyle { NATURAL, SLENDER, ARCHED, FIERCE, NOBLE }
    public enum Hairstyle { TOP_KNOT, LONG_FLOWING, BRAIDED, PONYTAIL, SCHOLAR_BUN, SHORT_SPIKE }
    public enum HairColor { JET_BLACK, SILVER_WHITE, CHESTNUT, RAVEN_BLUE, GOLDEN_BROWN }
    public enum FacialHair { CLEAN_SHAVEN, GOATEE, MUSTACHE, FULL_BEARD }
    public enum Outfit { WANDERING_ROBE, MARTIAL_TUNIC, SHADOW_SILK, DRAGON_PLATE, SCHOLAR_HANFU }
    public enum Accessory { NONE, BAMBOO_HAT, HEADBAND, JADE_PENDANT, FACE_MASK }
    public enum Boots { LEATHER_BOOTS, SILK_SHOES, TRAVELER_SANDALS, ARMORED_GREAVES }
    public enum ArmorStyle { NONE, SHOULDER_GUARDS, FOREARM_VAMBRACES, CHEST_LAMELLAR }

    // Core Properties
    public Gender gender = Gender.MALE;
    public FaceShape faceShape = FaceShape.OVAL;
    public EyeColor eyeColor = EyeColor.DARK_BROWN;
    public EyebrowStyle eyebrowStyle = EyebrowStyle.NATURAL;
    public Hairstyle hairstyle = Hairstyle.PONYTAIL;
    public HairColor hairColor = HairColor.JET_BLACK;
    public FacialHair facialHair = FacialHair.CLEAN_SHAVEN;
    public Outfit outfit = Outfit.WANDERING_ROBE;
    public Accessory accessory = Accessory.BAMBOO_HAT;
    public Boots boots = Boots.LEATHER_BOOTS;
    public ArmorStyle armor = ArmorStyle.FOREARM_VAMBRACES;

    // Skin Tone RGB (0.0 to 1.0)
    public float skinR = 0.92f, skinG = 0.78f, skinB = 0.68f;

    // Outfit Colors RGB
    public float outfitPrimaryR = 0.18f, outfitPrimaryG = 0.22f, outfitPrimaryB = 0.32f;
    public float outfitSecondaryR = 0.85f, outfitSecondaryG = 0.72f, outfitSecondaryB = 0.35f;

    // Body Proportions
    public float heightScale = 1.0f;       // 0.85 - 1.15
    public float shoulderWidth = 1.0f;     // 0.85 - 1.25
    public float buildMuscularity = 1.0f;   // 0.80 - 1.20

    public CharacterCustomization() {
        applyPreset(Preset.WANDERING_SWORDSMAN);
    }

    public enum Preset {
        WANDERING_SWORDSMAN,
        SHADOW_BLADE,
        MYSTIC_SCHOLAR,
        DRAGON_VANGUARD,
        JADE_LOTUS_PRODIGY
    }

    /** Applies an original pre-built character preset. */
    public void applyPreset(Preset preset) {
        if (preset == null) preset = Preset.WANDERING_SWORDSMAN;
        switch (preset) {
            case WANDERING_SWORDSMAN:
                gender = Gender.MALE;
                faceShape = FaceShape.SHARP;
                eyeColor = EyeColor.DARK_BROWN;
                eyebrowStyle = EyebrowStyle.FIERCE;
                hairstyle = Hairstyle.PONYTAIL;
                hairColor = HairColor.JET_BLACK;
                facialHair = FacialHair.GOATEE;
                outfit = Outfit.WANDERING_ROBE;
                accessory = Accessory.BAMBOO_HAT;
                boots = Boots.LEATHER_BOOTS;
                armor = ArmorStyle.FOREARM_VAMBRACES;
                skinR = 0.90f; skinG = 0.75f; skinB = 0.62f; // Sun-kissed bronze
                outfitPrimaryR = 0.22f; outfitPrimaryG = 0.24f; outfitPrimaryB = 0.28f; // Charcoal robe
                outfitSecondaryR = 0.82f; outfitSecondaryG = 0.65f; outfitSecondaryB = 0.30f; // Bamboo gold trim
                heightScale = 1.02f;
                shoulderWidth = 1.05f;
                buildMuscularity = 1.05f;
                break;

            case SHADOW_BLADE:
                gender = Gender.MALE;
                faceShape = FaceShape.SHARP;
                eyeColor = EyeColor.AMBER;
                eyebrowStyle = EyebrowStyle.SLENDER;
                hairstyle = Hairstyle.SHORT_SPIKE;
                hairColor = HairColor.RAVEN_BLUE;
                facialHair = FacialHair.CLEAN_SHAVEN;
                outfit = Outfit.SHADOW_SILK;
                accessory = Accessory.FACE_MASK;
                boots = Boots.SILK_SHOES;
                armor = ArmorStyle.FOREARM_VAMBRACES;
                skinR = 0.85f; skinG = 0.80f; skinB = 0.78f; // Moonlit pale
                outfitPrimaryR = 0.10f; outfitPrimaryG = 0.11f; outfitPrimaryB = 0.15f; // Midnight black
                outfitSecondaryR = 0.40f; outfitSecondaryG = 0.15f; outfitSecondaryB = 0.45f; // Shadow purple
                heightScale = 0.98f;
                shoulderWidth = 0.95f;
                buildMuscularity = 0.95f;
                break;

            case MYSTIC_SCHOLAR:
                gender = Gender.MALE;
                faceShape = FaceShape.OVAL;
                eyeColor = EyeColor.AZURE_BLUE;
                eyebrowStyle = EyebrowStyle.NOBLE;
                hairstyle = Hairstyle.SCHOLAR_BUN;
                hairColor = HairColor.SILVER_WHITE;
                facialHair = FacialHair.CLEAN_SHAVEN;
                outfit = Outfit.SCHOLAR_HANFU;
                accessory = Accessory.JADE_PENDANT;
                boots = Boots.SILK_SHOES;
                armor = ArmorStyle.NONE;
                skinR = 0.95f; skinG = 0.88f; skinB = 0.82f; // Porcelain
                outfitPrimaryR = 0.88f; outfitPrimaryG = 0.92f; outfitPrimaryB = 0.96f; // Cloud white
                outfitSecondaryR = 0.25f; outfitSecondaryG = 0.65f; outfitSecondaryB = 0.75f; // Jade azure trim
                heightScale = 1.0f;
                shoulderWidth = 0.98f;
                buildMuscularity = 0.92f;
                break;

            case DRAGON_VANGUARD:
                gender = Gender.MALE;
                faceShape = FaceShape.SQUARE;
                eyeColor = EyeColor.DARK_BROWN;
                eyebrowStyle = EyebrowStyle.FIERCE;
                hairstyle = Hairstyle.TOP_KNOT;
                hairColor = HairColor.JET_BLACK;
                facialHair = FacialHair.FULL_BEARD;
                outfit = Outfit.DRAGON_PLATE;
                accessory = Accessory.HEADBAND;
                boots = Boots.ARMORED_GREAVES;
                armor = ArmorStyle.CHEST_LAMELLAR;
                skinR = 0.88f; skinG = 0.72f; skinB = 0.58f; // Deep bronze
                outfitPrimaryR = 0.55f; outfitPrimaryG = 0.15f; outfitPrimaryB = 0.12f; // Crimson war coat
                outfitSecondaryR = 0.78f; outfitSecondaryG = 0.68f; outfitSecondaryB = 0.25f; // Gold leaf lamellar
                heightScale = 1.08f;
                shoulderWidth = 1.20f;
                buildMuscularity = 1.18f;
                break;

            case JADE_LOTUS_PRODIGY:
                gender = Gender.FEMALE;
                faceShape = FaceShape.OVAL;
                eyeColor = EyeColor.JADE_GREEN;
                eyebrowStyle = EyebrowStyle.ARCHED;
                hairstyle = Hairstyle.BRAIDED;
                hairColor = HairColor.JET_BLACK;
                facialHair = FacialHair.CLEAN_SHAVEN;
                outfit = Outfit.MARTIAL_TUNIC;
                accessory = Accessory.JADE_PENDANT;
                boots = Boots.TRAVELER_SANDALS;
                armor = ArmorStyle.SHOULDER_GUARDS;
                skinR = 0.96f; skinG = 0.85f; skinB = 0.78f; // Fair lotus
                outfitPrimaryR = 0.20f; outfitPrimaryG = 0.65f; outfitPrimaryB = 0.45f; // Jade emerald
                outfitSecondaryR = 0.95f; outfitSecondaryG = 0.92f; outfitSecondaryB = 0.85f; // Lotus silk white
                heightScale = 0.95f;
                shoulderWidth = 0.90f;
                buildMuscularity = 0.90f;
                break;
        }
    }

    /** Serializes customization attributes into a single delimited string. */
    public String serialize() {
        return gender.name() + "|"
                + faceShape.name() + "|"
                + eyeColor.name() + "|"
                + eyebrowStyle.name() + "|"
                + hairstyle.name() + "|"
                + hairColor.name() + "|"
                + facialHair.name() + "|"
                + outfit.name() + "|"
                + accessory.name() + "|"
                + boots.name() + "|"
                + armor.name() + "|"
                + skinR + "," + skinG + "," + skinB + "|"
                + outfitPrimaryR + "," + outfitPrimaryG + "," + outfitPrimaryB + "|"
                + outfitSecondaryR + "," + outfitSecondaryG + "," + outfitSecondaryB + "|"
                + heightScale + "," + shoulderWidth + "," + buildMuscularity;
    }

    /** Deserializes customization from string. */
    public void deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return;
        try {
            String[] tokens = str.split("\\|");
            if (tokens.length >= 1) gender = Gender.valueOf(tokens[0]);
            if (tokens.length >= 2) faceShape = FaceShape.valueOf(tokens[1]);
            if (tokens.length >= 3) eyeColor = EyeColor.valueOf(tokens[2]);
            if (tokens.length >= 4) eyebrowStyle = EyebrowStyle.valueOf(tokens[3]);
            if (tokens.length >= 5) hairstyle = Hairstyle.valueOf(tokens[4]);
            if (tokens.length >= 6) hairColor = HairColor.valueOf(tokens[5]);
            if (tokens.length >= 7) facialHair = FacialHair.valueOf(tokens[6]);
            if (tokens.length >= 8) outfit = Outfit.valueOf(tokens[7]);
            if (tokens.length >= 9) accessory = Accessory.valueOf(tokens[8]);
            if (tokens.length >= 10) boots = Boots.valueOf(tokens[9]);
            if (tokens.length >= 11) armor = ArmorStyle.valueOf(tokens[10]);

            if (tokens.length >= 12) {
                String[] rgb = tokens[11].split(",");
                if (rgb.length >= 3) {
                    skinR = Float.parseFloat(rgb[0]);
                    skinG = Float.parseFloat(rgb[1]);
                    skinB = Float.parseFloat(rgb[2]);
                }
            }
            if (tokens.length >= 13) {
                String[] rgb = tokens[12].split(",");
                if (rgb.length >= 3) {
                    outfitPrimaryR = Float.parseFloat(rgb[0]);
                    outfitPrimaryG = Float.parseFloat(rgb[1]);
                    outfitPrimaryB = Float.parseFloat(rgb[2]);
                }
            }
            if (tokens.length >= 14) {
                String[] rgb = tokens[13].split(",");
                if (rgb.length >= 3) {
                    outfitSecondaryR = Float.parseFloat(rgb[0]);
                    outfitSecondaryG = Float.parseFloat(rgb[1]);
                    outfitSecondaryB = Float.parseFloat(rgb[2]);
                }
            }
            if (tokens.length >= 15) {
                String[] prop = tokens[14].split(",");
                if (prop.length >= 3) {
                    heightScale = Float.parseFloat(prop[0]);
                    shoulderWidth = Float.parseFloat(prop[1]);
                    buildMuscularity = Float.parseFloat(prop[2]);
                }
            }
        } catch (Exception ignored) {}
    }

    /** Returns hair color RGB */
    public float[] getHairColorRgb() {
        switch (hairColor) {
            case SILVER_WHITE: return new float[]{0.90f, 0.90f, 0.95f};
            case CHESTNUT: return new float[]{0.35f, 0.20f, 0.12f};
            case RAVEN_BLUE: return new float[]{0.10f, 0.12f, 0.20f};
            case GOLDEN_BROWN: return new float[]{0.55f, 0.40f, 0.22f};
            case JET_BLACK:
            default: return new float[]{0.08f, 0.08f, 0.09f};
        }
    }
}
