import com.devagent.realworldsurvival3d.AudioMixer;
import com.devagent.realworldsurvival3d.DayNightCycle;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.SettingsConfig;
import com.devagent.realworldsurvival3d.Vehicle;
import java.util.Map;

/**
 * AudioSettingsTest - JVM unit test suite for Phase 11 (Audio & Settings).
 * Tests AudioMixer math, spatial 3D audio, vehicle engine pitch/volume,
 * cooldown gates, SettingsConfig presets, conversions, bounds, and serialization.
 */
public class AudioSettingsTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String desc) {
        if (condition) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.err.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void approx(float actual, float expected, float eps, String desc) {
        check(Math.abs(actual - expected) <= eps, desc + " (" + actual + " ~ " + expected + ")");
    }

    public static void main(String[] args) {
        System.out.println("=== AudioSettingsTest (Phase 11) ===");

        testAudioMixerVolumeMath();
        testAudioMixerSpatialDistance();
        testAudioMixerSpatialPanning();
        testAudioMixerEngineModulation();
        testAudioMixerCooldowns();
        testAudioMixerAssetRegistry();
        testSettingsConfigDefaultsAndClamping();
        testSettingsConfigConversions();
        testSettingsConfigSerialization();
        testVehicleAudioFlags();

        System.out.println("\nAudioSettingsTest: " + passed + " passed, " + failed + " failed\n");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    // Group 1: AudioMixer Volume Math & Decibel Conversions
    // -----------------------------------------------------------------
    private static void testAudioMixerVolumeMath() {
        System.out.println("AudioMixer: volume math & decibels");
        approx(AudioMixer.effectiveVolume(1.0f, 0.8f), 0.8f, 0.001f, "full master retains channel volume");
        approx(AudioMixer.effectiveVolume(0.5f, 0.8f), 0.4f, 0.001f, "half master scales channel volume");
        approx(AudioMixer.effectiveVolume(0.0f, 0.8f), 0.0f, 0.001f, "zero master silences channel");
        approx(AudioMixer.effectiveVolume(1.5f, 1.2f), 1.0f, 0.001f, "effective volume clamped at 1.0");
        approx(AudioMixer.effectiveVolume(-0.5f, 0.8f), 0.0f, 0.001f, "negative master clamped at 0.0");

        approx(AudioMixer.linearGainToDb(1.0f), 0.0f, 0.01f, "1.0 gain is 0 dB");
        approx(AudioMixer.linearGainToDb(0.5f), -6.02f, 0.05f, "0.5 gain is -6 dB");
        approx(AudioMixer.linearGainToDb(0.1f), -20.0f, 0.05f, "0.1 gain is -20 dB");
        approx(AudioMixer.linearGainToDb(0.0f), -60.0f, 0.01f, "0 gain hits -60 dB floor");

        approx(AudioMixer.dbToLinearGain(0.0f), 1.0f, 0.01f, "0 dB converts to 1.0 gain");
        approx(AudioMixer.dbToLinearGain(-6.02f), 0.5f, 0.02f, "-6 dB converts to ~0.5 gain");
        approx(AudioMixer.dbToLinearGain(-60.0f), 0.0f, 0.01f, "-60 dB converts to 0.0 gain");
    }

    // -----------------------------------------------------------------
    // Group 2: AudioMixer Spatial Distance Attenuation
    // -----------------------------------------------------------------
    private static void testAudioMixerSpatialDistance() {
        System.out.println("AudioMixer: spatial distance attenuation");
        float minD = 5.0f;
        float maxD = 45.0f;

        approx(AudioMixer.calculateDistanceGain(0f, 0f, 3f, 4f, minD, maxD), 1.0f, 0.001f,
                "distance at minAudible (5m) has full gain 1.0");
        approx(AudioMixer.calculateDistanceGain(0f, 0f, 0f, 2f, minD, maxD), 1.0f, 0.001f,
                "distance under minAudible (2m) has full gain 1.0");
        approx(AudioMixer.calculateDistanceGain(0f, 0f, 0f, 50f, minD, maxD), 0.0f, 0.001f,
                "distance beyond maxAudible (50m) has zero gain");
        approx(AudioMixer.calculateDistanceGain(0f, 0f, 0f, 25f, minD, maxD), 0.5f, 0.001f,
                "distance at midpoint (25m) has 0.5 gain");

        // Distance monotonicity check
        float g10 = AudioMixer.calculateDistanceGain(0f, 0f, 0f, 10f, minD, maxD);
        float g20 = AudioMixer.calculateDistanceGain(0f, 0f, 0f, 20f, minD, maxD);
        float g30 = AudioMixer.calculateDistanceGain(0f, 0f, 0f, 30f, minD, maxD);
        check(g10 > g20 && g20 > g30, "gain decreases monotonically with distance");
    }

    // -----------------------------------------------------------------
    // Group 3: AudioMixer Spatial Stereo Panning
    // -----------------------------------------------------------------
    private static void testAudioMixerSpatialPanning() {
        System.out.println("AudioMixer: spatial stereo panning");
        // Listener at origin facing North (yaw = 0 deg)
        // dx=0, dz=10 is directly ahead -> pan = 0
        approx(AudioMixer.calculateStereoPan(0f, 0f, 0f, 0f, 10f), 0.0f, 0.02f, "source straight ahead pans center");

        // dx=10, dz=0 is to the right (East) -> pan > 0.95
        float panRight = AudioMixer.calculateStereoPan(0f, 0f, 0f, 10f, 0f);
        check(panRight > 0.90f, "source to the East pans right (" + panRight + ")");

        // dx=-10, dz=0 is to the left (West) -> pan < -0.90
        float panLeft = AudioMixer.calculateStereoPan(0f, 0f, 0f, -10f, 0f);
        check(panLeft < -0.90f, "source to the West pans left (" + panLeft + ")");

        // Rotated listener: Facing East (yaw = 90 deg)
        // dx=10, dz=0 is now directly ahead in front of listener -> pan ~ 0
        approx(AudioMixer.calculateStereoPan(90f, 0f, 0f, 10f, 0f), 0.0f, 0.05f, "source matches rotated listener heading");

        // Channel volumes decomposition
        float[] centerCh = AudioMixer.calculateChannelVolumes(0.8f, 0.0f);
        approx(centerCh[0], 0.8f, 0.001f, "center pan left channel full");
        approx(centerCh[1], 0.8f, 0.001f, "center pan right channel full");

        float[] rightCh = AudioMixer.calculateChannelVolumes(0.8f, 1.0f);
        approx(rightCh[0], 0.0f, 0.001f, "full right pan mutes left channel");
        approx(rightCh[1], 0.8f, 0.001f, "full right pan retains right channel");

        float[] leftCh = AudioMixer.calculateChannelVolumes(0.8f, -1.0f);
        approx(leftCh[0], 0.8f, 0.001f, "full left pan retains left channel");
        approx(leftCh[1], 0.0f, 0.001f, "full left pan mutes right channel");
    }

    // -----------------------------------------------------------------
    // Group 4: AudioMixer Vehicle Engine Pitch & Volume
    // -----------------------------------------------------------------
    private static void testAudioMixerEngineModulation() {
        System.out.println("AudioMixer: vehicle engine pitch & volume modulation");
        float maxSpeed = 22.0f; // Scout SUV max speed

        // Stationary idle
        approx(AudioMixer.calculateEnginePitch(0f, maxSpeed), 0.70f, 0.001f, "stationary engine idle pitch is 0.70x");
        approx(AudioMixer.calculateEngineVolume(0f, maxSpeed, true), 0.35f, 0.001f, "stationary engine idle volume is 0.35");

        // Half speed
        approx(AudioMixer.calculateEnginePitch(11.0f, maxSpeed), 1.15f, 0.01f, "half-speed pitch is 1.15x");
        approx(AudioMixer.calculateEngineVolume(11.0f, maxSpeed, true), 0.675f, 0.01f, "half-speed volume is 0.675");

        // Max speed
        approx(AudioMixer.calculateEnginePitch(22.0f, maxSpeed), 1.60f, 0.001f, "top speed pitch revs to 1.60x");
        approx(AudioMixer.calculateEngineVolume(22.0f, maxSpeed, true), 1.00f, 0.001f, "top speed volume is 1.00");

        // Pitch clamp on overspeed
        approx(AudioMixer.calculateEnginePitch(35.0f, maxSpeed), 1.60f, 0.001f, "overspeed pitch clamped to 1.60x");
        approx(AudioMixer.calculateEngineVolume(35.0f, maxSpeed, true), 1.00f, 0.001f, "overspeed volume clamped to 1.00");

        // Unoccupied vehicle
        approx(AudioMixer.calculateEngineVolume(0f, maxSpeed, false), 0.0f, 0.001f, "unoccupied vehicle volume is 0.0");
        approx(AudioMixer.calculateEngineVolume(20f, maxSpeed, false), 0.0f, 0.001f, "unoccupied rolling vehicle volume is 0.0");
    }

    // -----------------------------------------------------------------
    // Group 5: AudioMixer Cooldown Rate Limiting
    // -----------------------------------------------------------------
    private static void testAudioMixerCooldowns() {
        System.out.println("AudioMixer: cooldown rate limiting");
        AudioMixer mixer = new AudioMixer();

        check(mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1000L), "footstep can play initially");
        mixer.recordPlay(AudioMixer.SFX_FOOTSTEP, 1000L);

        check(!mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1050L), "footstep blocked 50ms later (180ms cd)");
        check(!mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1150L), "footstep blocked 150ms later");
        check(mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1200L), "footstep allowed 200ms later");

        // Vehicle crash 300ms cooldown
        check(mixer.canPlay(AudioMixer.SFX_VEHICLE_CRASH, 2000L), "vehicle crash can play initially");
        mixer.recordPlay(AudioMixer.SFX_VEHICLE_CRASH, 2000L);
        check(!mixer.canPlay(AudioMixer.SFX_VEHICLE_CRASH, 2200L), "vehicle crash blocked 200ms later");
        check(mixer.canPlay(AudioMixer.SFX_VEHICLE_CRASH, 2350L), "vehicle crash allowed 350ms later");

        // Unthrottled SFX (e.g. SFX_LEVEL_WIN)
        check(mixer.canPlay(AudioMixer.SFX_LEVEL_WIN, 3000L), "level win can play");
        mixer.recordPlay(AudioMixer.SFX_LEVEL_WIN, 3000L);
        check(mixer.canPlay(AudioMixer.SFX_LEVEL_WIN, 3005L), "level win has no cooldown gate");

        mixer.resetCooldowns();
        check(mixer.canPlay(AudioMixer.SFX_VEHICLE_CRASH, 2000L), "resetCooldowns clears timestamps");
    }

    // -----------------------------------------------------------------
    // Group 6: AudioMixer Sound Registry & Asset Paths
    // -----------------------------------------------------------------
    private static void testAudioMixerAssetRegistry() {
        System.out.println("AudioMixer: sound registry & asset paths");
        for (int id = 1; id <= 26; id++) {
            String path = AudioMixer.getAssetPath(id);
            check(path != null && path.startsWith("audio/sfx/") && path.endsWith(".wav"),
                    "sfx " + id + " has valid asset path: " + path);
        }

        check(AudioMixer.isLooping(AudioMixer.SFX_VEHICLE_ENGINE), "vehicle engine marked looping");
        check(AudioMixer.isLooping(AudioMixer.SFX_HEAVY_BREATH), "heavy breath marked looping");
        check(AudioMixer.isLooping(AudioMixer.SFX_HEARTBEAT), "heartbeat marked looping");
        check(!AudioMixer.isLooping(AudioMixer.SFX_VEHICLE_CRASH), "vehicle crash marked one-shot");
        check(!AudioMixer.isLooping(AudioMixer.SFX_JUMP), "jump marked one-shot");
    }

    // -----------------------------------------------------------------
    // Group 7: SettingsConfig Defaults & Clamping
    // -----------------------------------------------------------------
    private static void testSettingsConfigDefaultsAndClamping() {
        System.out.println("SettingsConfig: defaults & boundary clamping");
        SettingsConfig cfg = new SettingsConfig();

        approx(cfg.masterVolume, 1.0f, 0.001f, "default masterVolume is 1.0");
        approx(cfg.musicVolume, 0.70f, 0.001f, "default musicVolume is 0.70");
        approx(cfg.sfxVolume, 0.85f, 0.001f, "default sfxVolume is 0.85");
        approx(cfg.lookSensitivity, 0.50f, 0.001f, "default lookSensitivity is 0.50");
        check(!cfg.invertLookY, "default invertLookY is false");
        check(cfg.graphicsQuality == SettingsConfig.QUALITY_HIGH, "default graphicsQuality is HIGH");
        check(cfg.fpsLimit == SettingsConfig.FPS_60, "default fpsLimit is 60");
        check(cfg.shadowsEnabled, "default shadowsEnabled is true");
        check(cfg.bloomEnabled, "default bloomEnabled is true");
        approx(cfg.buttonScale, 1.0f, 0.001f, "default buttonScale is 1.0");
        approx(cfg.buttonOpacity, 0.65f, 0.001f, "default buttonOpacity is 0.65");
        check(!cfg.leftHanded, "default leftHanded is false");
        approx(cfg.joystickSensitivity, 1.0f, 0.001f, "default joystickSensitivity is 1.0");

        // Clamping tests
        cfg.masterVolume = 1.8f;
        cfg.musicVolume = -0.4f;
        cfg.buttonScale = 2.5f;
        cfg.buttonOpacity = 0.1f;
        cfg.joystickSensitivity = 3.0f;
        cfg.graphicsQuality = 99;
        cfg.fpsLimit = 120;
        cfg.clampAll();

        approx(cfg.masterVolume, 1.0f, 0.001f, "masterVolume clamped to 1.0");
        approx(cfg.musicVolume, 0.0f, 0.001f, "musicVolume clamped to 0.0");
        approx(cfg.buttonScale, 1.3f, 0.001f, "buttonScale clamped to 1.3");
        approx(cfg.buttonOpacity, 0.4f, 0.001f, "buttonOpacity clamped to 0.4");
        approx(cfg.joystickSensitivity, 1.5f, 0.001f, "joystickSensitivity clamped to 1.5");
        check(cfg.graphicsQuality == SettingsConfig.QUALITY_HIGH, "invalid quality clamped to HIGH");
        check(cfg.fpsLimit == SettingsConfig.FPS_60, "invalid fps clamped to 60");
    }

    // -----------------------------------------------------------------
    // Group 8: SettingsConfig Mathematical Conversions
    // -----------------------------------------------------------------
    private static void testSettingsConfigConversions() {
        System.out.println("SettingsConfig: conversions & presets");
        SettingsConfig cfg = new SettingsConfig();

        // lookScale: 0.4 + slider * 1.6
        cfg.lookSensitivity = 0.0f;
        approx(cfg.lookScale(), 0.4f, 0.001f, "lookScale at 0.0 slider is 0.4");
        cfg.lookSensitivity = 0.5f;
        approx(cfg.lookScale(), 1.2f, 0.001f, "lookScale at 0.5 slider is 1.2");
        cfg.lookSensitivity = 1.0f;
        approx(cfg.lookScale(), 2.0f, 0.001f, "lookScale at 1.0 slider is 2.0");

        // Slider <-> Scale round-trip
        approx(SettingsConfig.scaleFromSlider(0.0f), 0.8f, 0.001f, "scale at 0 slider is 0.8");
        approx(SettingsConfig.scaleFromSlider(1.0f), 1.3f, 0.001f, "scale at 1 slider is 1.3");
        approx(SettingsConfig.sliderFromScale(1.05f), 0.5f, 0.001f, "slider from scale 1.05 is 0.5");

        // Slider <-> Opacity round-trip
        approx(SettingsConfig.opacityFromSlider(0.0f), 0.4f, 0.001f, "opacity at 0 slider is 0.4");
        approx(SettingsConfig.opacityFromSlider(1.0f), 1.0f, 0.001f, "opacity at 1 slider is 1.0");
        approx(SettingsConfig.sliderFromOpacity(0.7f), 0.5f, 0.001f, "slider from opacity 0.7 is 0.5");

        // Slider <-> JoySens round-trip
        approx(SettingsConfig.joySensFromSlider(0.0f), 0.5f, 0.001f, "joySens at 0 slider is 0.5");
        approx(SettingsConfig.joySensFromSlider(1.0f), 1.5f, 0.001f, "joySens at 1 slider is 1.5");
        approx(SettingsConfig.sliderFromJoySens(1.0f), 0.5f, 0.001f, "slider from joySens 1.0 is 0.5");

        // Quality cycling & names
        check(SettingsConfig.cycleQuality(SettingsConfig.QUALITY_LOW) == SettingsConfig.QUALITY_MEDIUM, "cycle LOW -> MEDIUM");
        check(SettingsConfig.cycleQuality(SettingsConfig.QUALITY_MEDIUM) == SettingsConfig.QUALITY_HIGH, "cycle MEDIUM -> HIGH");
        check(SettingsConfig.cycleQuality(SettingsConfig.QUALITY_HIGH) == SettingsConfig.QUALITY_LOW, "cycle HIGH -> LOW");
        check(SettingsConfig.qualityName(SettingsConfig.QUALITY_LOW).equals("Low"), "LOW name is Low");
        check(SettingsConfig.qualityName(SettingsConfig.QUALITY_MEDIUM).equals("Medium"), "MEDIUM name is Medium");
        check(SettingsConfig.qualityName(SettingsConfig.QUALITY_HIGH).equals("High"), "HIGH name is High");

        // FPS cycling & names
        check(SettingsConfig.cycleFps(SettingsConfig.FPS_60) == SettingsConfig.FPS_30, "cycle 60 -> 30");
        check(SettingsConfig.cycleFps(SettingsConfig.FPS_30) == SettingsConfig.FPS_60, "cycle 30 -> 60");
        check(SettingsConfig.fpsName(30).equals("30"), "FPS 30 string name");
        check(SettingsConfig.fpsName(60).equals("60"), "FPS 60 string name");

        // DayNight preset mapping
        cfg.graphicsQuality = SettingsConfig.QUALITY_LOW;
        check(cfg.getGraphicsPreset() == DayNightCycle.GraphicsPreset.LOW, "preset LOW maps to DayNight LOW");
        cfg.graphicsQuality = SettingsConfig.QUALITY_MEDIUM;
        check(cfg.getGraphicsPreset() == DayNightCycle.GraphicsPreset.MEDIUM, "preset MEDIUM maps to DayNight MEDIUM");
        cfg.graphicsQuality = SettingsConfig.QUALITY_HIGH;
        check(cfg.getGraphicsPreset() == DayNightCycle.GraphicsPreset.HIGH, "preset HIGH maps to DayNight HIGH");
    }

    // -----------------------------------------------------------------
    // Group 9: SettingsConfig Serialization
    // -----------------------------------------------------------------
    private static void testSettingsConfigSerialization() {
        System.out.println("SettingsConfig: string map export and import");
        SettingsConfig orig = new SettingsConfig();
        orig.masterVolume = 0.85f;
        orig.musicVolume = 0.40f;
        orig.sfxVolume = 0.60f;
        orig.lookSensitivity = 0.75f;
        orig.invertLookY = true;
        orig.graphicsQuality = SettingsConfig.QUALITY_MEDIUM;
        orig.fpsLimit = SettingsConfig.FPS_30;
        orig.shadowsEnabled = false;
        orig.bloomEnabled = false;
        orig.buttonScale = 1.15f;
        orig.buttonOpacity = 0.80f;
        orig.leftHanded = true;
        orig.joystickSensitivity = 1.25f;

        Map<String, String> map = orig.exportToStringMap();
        check(map.size() == 13, "export map contains all 13 keys (" + map.size() + ")");

        SettingsConfig loaded = new SettingsConfig();
        loaded.importFromStringMap(map);

        approx(loaded.masterVolume, 0.85f, 0.001f, "imported masterVolume preserved");
        approx(loaded.musicVolume, 0.40f, 0.001f, "imported musicVolume preserved");
        approx(loaded.sfxVolume, 0.60f, 0.001f, "imported sfxVolume preserved");
        approx(loaded.lookSensitivity, 0.75f, 0.001f, "imported lookSensitivity preserved");
        check(loaded.invertLookY, "imported invertLookY preserved");
        check(loaded.graphicsQuality == SettingsConfig.QUALITY_MEDIUM, "imported graphicsQuality preserved");
        check(loaded.fpsLimit == SettingsConfig.FPS_30, "imported fpsLimit preserved");
        check(!loaded.shadowsEnabled, "imported shadowsEnabled preserved");
        check(!loaded.bloomEnabled, "imported bloomEnabled preserved");
        approx(loaded.buttonScale, 1.15f, 0.001f, "imported buttonScale preserved");
        approx(loaded.buttonOpacity, 0.80f, 0.001f, "imported buttonOpacity preserved");
        check(loaded.leftHanded, "imported leftHanded preserved");
        approx(loaded.joystickSensitivity, 1.25f, 0.001f, "imported joystickSensitivity preserved");
    }

    // -----------------------------------------------------------------
    // Group 10: Vehicle Audio Flags
    // -----------------------------------------------------------------
    private static void testVehicleAudioFlags() {
        System.out.println("Vehicle: audio collision & handbrake flags");
        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout", 210f, 45f, 180f);
        Player player = new Player();
        suv.enter(player);

        OpenWorld world = new OpenWorld();
        world.recomputeActive(210f, 45f);

        check(!suv.justCollided, "vehicle justCollided is false initially");
        check(!suv.isHandbraking, "vehicle isHandbraking is false initially");

        // Update with handbrake true
        suv.update(0.1f, 1.0f, 0f, true, world, player);
        check(suv.isHandbraking, "isHandbraking flag set during handbrake input");

        // Set fast forward speed and move into safehouse wall (wall at z=52)
        suv.speed = 18.0f;
        suv.update(0.5f, 1.0f, 0f, false, world, player);
        check(suv.justCollided, "justCollided flag set upon solid obstacle collision");

        // Next frame without collision resets flag
        suv.update(0.05f, 0f, 0f, false, world, player);
        check(!suv.justCollided, "justCollided resets on subsequent clear update");
    }
}
