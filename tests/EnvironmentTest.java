import com.devagent.realworldsurvival3d.BuildingInterior;
import com.devagent.realworldsurvival3d.DayNightCycle;
import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Terrain;

import java.util.List;

/**
 * JVM test suite for Phase 7 Environment:
 *  - Day/Night 8-minute cycle & sun orientation
 *  - Diurnal lighting & atmospheric sky palettes
 *  - Weather-lite fog & atmospheric variation
 *  - Graphics presets (LOW, MEDIUM, HIGH)
 *  - Building interiors (walkable rooms, doorways, ceilings, props)
 */
public class EnvironmentTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String desc, boolean cond) {
        if (cond) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.out.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void checkNear(String desc, float actual, float expected, float eps) {
        if (Math.abs(actual - expected) <= eps) {
            System.out.println("  PASS  " + desc + " (" + actual + " ~ " + expected + ")");
            passed++;
        } else {
            System.out.println("  FAIL  " + desc + " (" + actual + " != " + expected + ", eps=" + eps + ")");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("DayNightCycle: 8-minute cycle progression & clock");
        testCycleAndClock();

        System.out.println("DayNightCycle: sun orientation & lighting palettes");
        testSunAndPalettes();

        System.out.println("DayNightCycle: weather-lite atmospheric modulation");
        testWeatherLite();

        System.out.println("DayNightCycle: graphics presets");
        testGraphicsPresets();

        System.out.println("BuildingInterior: walkable rooms & doorways");
        testBuildingInteriors();

        System.out.println("BuildingInterior: roof ceilings & collision containment");
        testInteriorContainment();

        System.out.println();
        System.out.println("EnvironmentTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testCycleAndClock() {
        DayNightCycle cycle = new DayNightCycle();
        checkNear("cycle duration is 480 seconds (8 min)", DayNightCycle.CYCLE_DURATION, 480f, 0.01f);

        // Initial time
        cycle.timeOfDay = 0.50f; // noon (12:00 PM)
        cycle.update(0f);
        check("noon is not night", !cycle.isNight);
        check("12:00 PM clock format", "12:00 PM".equals(cycle.getTimeString()));

        // Advance 120s (1/4 of cycle -> 0.75 -> 06:00 PM dusk)
        cycle.update(120f);
        checkNear("timeOfDay advanced by 0.25", cycle.timeOfDay, 0.75f, 0.01f);
        check("06:00 PM clock format", "06:00 PM".equals(cycle.getTimeString()));

        // Midnight (0.00 / 1.00)
        cycle.timeOfDay = 0.0f;
        cycle.update(0f);
        check("midnight is night", cycle.isNight);
        check("12:00 AM clock format", "12:00 AM".equals(cycle.getTimeString()));

        // 08:30 AM (8.5 / 24 = 0.35416)
        cycle.timeOfDay = 8.5f / 24.0f;
        cycle.update(0f);
        check("08:30 AM clock format", "08:30 AM".equals(cycle.getTimeString()));
    }

    private static void testSunAndPalettes() {
        DayNightCycle cycle = new DayNightCycle();

        // 1. Sun Direction Unit Vector
        for (float t = 0f; t < 1.0f; t += 0.1f) {
            cycle.timeOfDay = t;
            cycle.update(0f);
            float[] s = cycle.sunDirection;
            float len = (float) Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
            checkNear("sun direction normalized at time=" + t, len, 1.0f, 0.005f);
        }

        // 2. Noon vs Midnight comparison
        cycle.timeOfDay = 0.50f; // Noon
        cycle.update(0f);
        float noonSunY = cycle.sunDirection[1];
        float noonLightBrightness = (cycle.lightColor[0] + cycle.lightColor[1] + cycle.lightColor[2]) / 3f;
        float noonAmbSkyBrightness = (cycle.ambientSky[0] + cycle.ambientSky[1] + cycle.ambientSky[2]) / 3f;

        cycle.timeOfDay = 0.00f; // Midnight
        cycle.update(0f);
        float midnightSunY = cycle.sunDirection[1];
        float midnightLightBrightness = (cycle.lightColor[0] + cycle.lightColor[1] + cycle.lightColor[2]) / 3f;
        float midnightAmbSkyBrightness = (cycle.ambientSky[0] + cycle.ambientSky[1] + cycle.ambientSky[2]) / 3f;

        check("noon sun elevation is high in the sky (sunY > 0.8)", noonSunY > 0.8f);
        check("midnight sun elevation is below horizon (sunY < -0.8)", midnightSunY < -0.8f);
        check("noon sunlight is brighter than night moonlight", noonLightBrightness > midnightLightBrightness);
        check("noon sky ambient is brighter than midnight ambient", noonAmbSkyBrightness > midnightAmbSkyBrightness);

        // 3. Dusk warm color characteristics (R > B)
        cycle.timeOfDay = 0.74f; // Sunset
        cycle.update(0f);
        check("dusk horizon has warm reddish tone (R > B)", cycle.skyHorizon[0] > cycle.skyHorizon[2]);
        check("dusk sunlight is warm amber/orange (R > B)", cycle.lightColor[0] > cycle.lightColor[2]);
    }

    private static void testWeatherLite() {
        DayNightCycle cycle = new DayNightCycle();
        cycle.timeOfDay = 0.50f;

        // Clear weather
        cycle.setWeather(DayNightCycle.Weather.CLEAR);
        float clearFog = cycle.fogDensity;
        float clearLight = (cycle.lightColor[0] + cycle.lightColor[1] + cycle.lightColor[2]) / 3f;

        // Misty weather
        cycle.setWeather(DayNightCycle.Weather.MISTY);
        float mistyFog = cycle.fogDensity;
        float mistyLight = (cycle.lightColor[0] + cycle.lightColor[1] + cycle.lightColor[2]) / 3f;

        check("misty fog density is greater than clear fog", mistyFog > clearFog);
        check("misty light is attenuated compared to clear sky", mistyLight < clearLight);
        check("fog density within playable bounds [0.005, 0.035]", mistyFog >= 0.005f && mistyFog <= 0.035f);

        // Rain weather
        cycle.setWeather(DayNightCycle.Weather.RAIN_LITE);
        check("rain weather light multiplier applied", cycle.currentWeather.lightMultiplier < 1.0f);
    }

    private static void testGraphicsPresets() {
        DayNightCycle cycle = new DayNightCycle();

        cycle.setPreset(DayNightCycle.GraphicsPreset.LOW);
        checkNear("LOW preset draw distance is 120m", cycle.currentPreset.drawDistance, 120f, 0.1f);
        check("LOW preset disables shadows for performance", !cycle.currentPreset.shadowsEnabled);
        check("LOW preset targets 30 FPS", cycle.currentPreset.targetFps == 30);

        cycle.setPreset(DayNightCycle.GraphicsPreset.MEDIUM);
        checkNear("MEDIUM preset draw distance is 180m", cycle.currentPreset.drawDistance, 180f, 0.1f);
        check("MEDIUM preset enables shadows", cycle.currentPreset.shadowsEnabled);

        cycle.setPreset(DayNightCycle.GraphicsPreset.HIGH);
        checkNear("HIGH preset draw distance is 260m", cycle.currentPreset.drawDistance, 260f, 0.1f);
        check("HIGH preset uses 5x5 chunk radius (chunkRadius=2)", cycle.currentPreset.chunkRadius == 2);
    }

    private static void testBuildingInteriors() {
        BuildingInterior interior = new BuildingInterior();
        check("contains 4 key walkable interiors", interior.getInteriors().size() == 4);

        BuildingInterior.InteriorDefinition safehouse = interior.findInterior("Safehouse");
        check("Safehouse interior defined", safehouse != null);
        checkNear("Safehouse center X", safehouse.centerX, 210f, 0.1f);
        checkNear("Safehouse center Z", safehouse.centerZ, 60f, 0.1f);
        check("point (210, 60) is inside Safehouse", safehouse.containsPoint(210f, 60f));
        check("point (0, 0) is outside Safehouse", !safehouse.containsPoint(0f, 0f));

        // Check door opening on South wall: (210, 68)
        checkNear("doorway is on South wall", safehouse.doorZ, 68f, 0.1f);
        check("doorway width is >= 2.4m for comfortable entry", safehouse.doorWidth >= 2.4f);
        check("doorway height is >= 2.4m for standing entry", safehouse.doorHeight >= 2.4f);

        // Test other key interiors
        check("Workshop interior defined", interior.findInterior("Workshop") != null);
        check("Garage interior defined", interior.findInterior("Garage") != null);
        check("Ranger Hut interior defined", interior.findInterior("Ranger Hut") != null);
    }

    private static void testInteriorContainment() {
        BuildingInterior interior = new BuildingInterior();
        BuildingInterior.InteriorDefinition safehouse = interior.findInterior("Safehouse");

        // Verify interior room has floor clearance: no solid obstacle at interior center (210, 60)
        // at player standing height (y=ground + 1.0)
        float ground = Terrain.heightAt(safehouse.centerX, safehouse.centerZ);
        float playerY = ground + 0.9f;

        boolean centerBlocked = false;
        for (Obstacle o : safehouse.obstacles) {
            if (!o.solid) continue;
            if (o.contains(safehouse.centerX, playerY, safehouse.centerZ)) {
                centerBlocked = true;
                break;
            }
        }
        check("interior room center is clear and walkable (no wall blocks center)", !centerBlocked);

        // Verify door opening has no obstacle: player standing at doorway (210, ground + 0.9, 67.8)
        boolean doorBlocked = false;
        for (Obstacle o : safehouse.obstacles) {
            if (!o.solid) continue;
            if (o.contains(safehouse.doorX, playerY, safehouse.doorZ - 0.2f)) {
                doorBlocked = true;
                break;
            }
        }
        check("doorway opening is unobstructed (player enters freely)", !doorBlocked);

        // Verify perimeter walls exist: point on East wall (219.7, 60) is blocked
        boolean eastWallBlocked = false;
        for (Obstacle o : safehouse.obstacles) {
            if (o.solid && o.contains(safehouse.centerX + 9.7f, playerY, safehouse.centerZ)) {
                eastWallBlocked = true;
                break;
            }
        }
        check("east perimeter wall is solid", eastWallBlocked);

        // Verify roof slab covers overhead: point at (210, ground + 6.3, 60) has solid roof
        boolean roofPresent = false;
        for (Obstacle o : safehouse.obstacles) {
            if (o.solid && o.contains(safehouse.centerX, ground + safehouse.height + 0.2f, safehouse.centerZ)) {
                roofPresent = true;
                break;
            }
        }
        check("solid ceiling/roof slab covers interior overhead", roofPresent);
    }
}
