package com.devagent.realworldsurvival3d;

/**
 * DayNightCycle - Pure Java diurnal simulation and atmospheric lighting controller.
 *
 * Implements Phase 7 Environment:
 *  - 8-minute full day/night cycle (480 seconds)
 *  - Sun & moon orbiting direction vectors
 *  - Diurnal color palettes (Dawn, Day, Dusk, Night) with smooth cosine blending
 *  - Weather-lite atmospheric modulation (Clear, Overcast, Misty, Rain)
 *  - Dynamic fog density & color
 *  - Graphics presets (LOW, MEDIUM, HIGH)
 */
public class DayNightCycle {

    public static final float CYCLE_DURATION = 480.0f; // 8 minutes in seconds
    public static final float DAWN_START = 0.20f;      // 04:48 AM
    public static final float DAY_START = 0.28f;       // 06:43 AM
    public static final float DUSK_START = 0.70f;      // 16:48 PM
    public static final float NIGHT_START = 0.78f;     // 18:43 PM

    public enum Weather {
        CLEAR(0.008f, 1.0f),
        OVERCAST(0.013f, 0.85f),
        MISTY(0.024f, 0.65f),
        RAIN_LITE(0.018f, 0.75f);

        public final float baseFogDensity;
        public final float lightMultiplier;

        Weather(float baseFogDensity, float lightMultiplier) {
            this.baseFogDensity = baseFogDensity;
            this.lightMultiplier = lightMultiplier;
        }
    }

    public enum GraphicsPreset {
        LOW(120.0f, 1, false, 1.4f, 30),
        MEDIUM(180.0f, 1, true, 1.0f, 60),
        HIGH(260.0f, 2, true, 0.7f, 60);

        public final float drawDistance;
        public final int chunkRadius;
        public final boolean shadowsEnabled;
        public final float fogMultiplier;
        public final int targetFps;

        GraphicsPreset(float drawDistance, int chunkRadius, boolean shadowsEnabled,
                       float fogMultiplier, int targetFps) {
            this.drawDistance = drawDistance;
            this.chunkRadius = chunkRadius;
            this.shadowsEnabled = shadowsEnabled;
            this.fogMultiplier = fogMultiplier;
            this.targetFps = targetFps;
        }
    }

    // Time of day in [0.0, 1.0)
    public float timeOfDay = 0.26f; // starts at early dawn / sunrise
    public boolean isPaused = false;
    public float timeScale = 1.0f;

    // Active Weather & Graphics Preset
    public Weather currentWeather = Weather.CLEAR;
    public GraphicsPreset currentPreset = GraphicsPreset.MEDIUM;

    // Calculated Lighting Uniforms
    public final float[] sunDirection = new float[3];
    public final float[] lightColor = new float[3];
    public final float[] ambientSky = new float[3];
    public final float[] ambientGround = new float[3];
    public final float[] skyZenith = new float[3];
    public final float[] skyHorizon = new float[3];
    public final float[] fogColor = new float[3];
    public float fogDensity = 0.008f;
    public boolean isNight = false;

    // Fixed palette keyframes: [R, G, B]
    // 0: DAWN (warm golden sunrise)
    private static final float[] DAWN_LIGHT   = {1.00f, 0.72f, 0.45f};
    private static final float[] DAWN_AMB_SKY = {0.38f, 0.36f, 0.48f};
    private static final float[] DAWN_AMB_GND = {0.18f, 0.16f, 0.14f};
    private static final float[] DAWN_ZENITH  = {0.15f, 0.16f, 0.35f};
    private static final float[] DAWN_HORIZON = {0.92f, 0.48f, 0.30f};
    private static final float[] DAWN_FOG     = {0.75f, 0.42f, 0.32f};

    // 1: DAY (bright azure sky, clear sunlight)
    private static final float[] DAY_LIGHT   = {1.00f, 0.96f, 0.88f};
    private static final float[] DAY_AMB_SKY = {0.42f, 0.48f, 0.62f};
    private static final float[] DAY_AMB_GND = {0.20f, 0.20f, 0.18f};
    private static final float[] DAY_ZENITH  = {0.15f, 0.35f, 0.72f};
    private static final float[] DAY_HORIZON = {0.70f, 0.82f, 0.94f};
    private static final float[] DAY_FOG     = {0.68f, 0.78f, 0.88f};

    // 2: DUSK (rich orange/crimson twilight)
    private static final float[] DUSK_LIGHT   = {1.00f, 0.55f, 0.22f};
    private static final float[] DUSK_AMB_SKY = {0.32f, 0.34f, 0.50f};
    private static final float[] DUSK_AMB_GND = {0.16f, 0.14f, 0.12f};
    private static final float[] DUSK_ZENITH  = {0.08f, 0.11f, 0.24f};
    private static final float[] DUSK_HORIZON = {0.86f, 0.44f, 0.24f};
    private static final float[] DUSK_FOG     = {0.62f, 0.36f, 0.27f};

    // 3: NIGHT (midnight deep blue, cool silver moonlight)
    private static final float[] NIGHT_LIGHT   = {0.30f, 0.42f, 0.65f};
    private static final float[] NIGHT_AMB_SKY = {0.10f, 0.14f, 0.22f};
    private static final float[] NIGHT_AMB_GND = {0.05f, 0.06f, 0.08f};
    private static final float[] NIGHT_ZENITH  = {0.02f, 0.03f, 0.07f};
    private static final float[] NIGHT_HORIZON = {0.07f, 0.11f, 0.20f};
    private static final float[] NIGHT_FOG     = {0.06f, 0.08f, 0.14f};

    public DayNightCycle() {
        update(0f);
    }

    /**
     * Advances the simulation by deltaTime seconds and recalculates lighting uniforms.
     */
    public void update(float deltaTime) {
        if (!isPaused && deltaTime > 0f) {
            timeOfDay += (deltaTime * timeScale) / CYCLE_DURATION;
            while (timeOfDay >= 1.0f) timeOfDay -= 1.0f;
            while (timeOfDay < 0.0f) timeOfDay += 1.0f;
        }

        // Calculate Sun & Moon Direction Unit Vector
        // Angle theta: 0 = dawn (east horizon), 0.25 = noon (zenith), 0.5 = dusk (west), 0.75 = midnight
        // Offset so time 0.25 is sunrise, 0.5 is noon, 0.75 is sunset, 0.0/1.0 is midnight
        double angle = (timeOfDay - 0.25) * 2.0 * Math.PI;
        float sunY = (float) Math.sin(angle);
        float sunX = (float) Math.cos(angle);
        float sunZ = 0.35f * (float) Math.cos(angle * 0.5);

        // Normalize
        float len = (float) Math.sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ);
        if (len > 0.0001f) {
            sunDirection[0] = sunX / len;
            sunDirection[1] = sunY / len;
            sunDirection[2] = sunZ / len;
        } else {
            sunDirection[0] = 0f;
            sunDirection[1] = 1f;
            sunDirection[2] = 0f;
        }

        isNight = (timeOfDay < DAWN_START || timeOfDay >= NIGHT_START);

        // Interpolate Diurnal Lighting Palettes
        interpolatePalette();

        // Apply Weather Modulation
        float weatherLight = currentWeather.lightMultiplier;
        lightColor[0] *= weatherLight;
        lightColor[1] *= weatherLight;
        lightColor[2] *= weatherLight;

        fogDensity = currentWeather.baseFogDensity * currentPreset.fogMultiplier;
    }

    private void interpolatePalette() {
        float[] cLightA, cLightB;
        float[] cAmbSkyA, cAmbSkyB;
        float[] cAmbGndA, cAmbGndB;
        float[] cZenithA, cZenithB;
        float[] cHorizonA, cHorizonB;
        float[] cFogA, cFogB;
        float t;

        if (timeOfDay < DAWN_START) {
            // Night -> Dawn transition
            float span = DAWN_START;
            t = smoothStep((timeOfDay) / span);
            cLightA = NIGHT_LIGHT; cLightB = DAWN_LIGHT;
            cAmbSkyA = NIGHT_AMB_SKY; cAmbSkyB = DAWN_AMB_SKY;
            cAmbGndA = NIGHT_AMB_GND; cAmbGndB = DAWN_AMB_GND;
            cZenithA = NIGHT_ZENITH; cZenithB = DAWN_ZENITH;
            cHorizonA = NIGHT_HORIZON; cHorizonB = DAWN_HORIZON;
            cFogA = NIGHT_FOG; cFogB = DAWN_FOG;
        } else if (timeOfDay < DAY_START) {
            // Dawn -> Day
            float span = DAY_START - DAWN_START;
            t = smoothStep((timeOfDay - DAWN_START) / span);
            cLightA = DAWN_LIGHT; cLightB = DAY_LIGHT;
            cAmbSkyA = DAWN_AMB_SKY; cAmbSkyB = DAY_AMB_SKY;
            cAmbGndA = DAWN_AMB_GND; cAmbGndB = DAY_AMB_GND;
            cZenithA = DAWN_ZENITH; cZenithB = DAY_ZENITH;
            cHorizonA = DAWN_HORIZON; cHorizonB = DAY_HORIZON;
            cFogA = DAWN_FOG; cFogB = DAY_FOG;
        } else if (timeOfDay < DUSK_START) {
            // Full Day
            cLightA = DAY_LIGHT; cLightB = DAY_LIGHT;
            cAmbSkyA = DAY_AMB_SKY; cAmbSkyB = DAY_AMB_SKY;
            cAmbGndA = DAY_AMB_GND; cAmbGndB = DAY_AMB_GND;
            cZenithA = DAY_ZENITH; cZenithB = DAY_ZENITH;
            cHorizonA = DAY_HORIZON; cHorizonB = DAY_HORIZON;
            cFogA = DAY_FOG; cFogB = DAY_FOG;
            t = 0f;
        } else if (timeOfDay < NIGHT_START) {
            // Day -> Dusk -> Night
            float span = NIGHT_START - DUSK_START;
            t = smoothStep((timeOfDay - DUSK_START) / span);
            cLightA = DUSK_LIGHT; cLightB = NIGHT_LIGHT;
            cAmbSkyA = DUSK_AMB_SKY; cAmbSkyB = NIGHT_AMB_SKY;
            cAmbGndA = DUSK_AMB_GND; cAmbGndB = NIGHT_AMB_GND;
            cZenithA = DUSK_ZENITH; cZenithB = NIGHT_ZENITH;
            cHorizonA = DUSK_HORIZON; cHorizonB = NIGHT_HORIZON;
            cFogA = DUSK_FOG; cFogB = NIGHT_FOG;
        } else {
            // Deep Night
            cLightA = NIGHT_LIGHT; cLightB = NIGHT_LIGHT;
            cAmbSkyA = NIGHT_AMB_SKY; cAmbSkyB = NIGHT_AMB_SKY;
            cAmbGndA = NIGHT_AMB_GND; cAmbGndB = NIGHT_AMB_GND;
            cZenithA = NIGHT_ZENITH; cZenithB = NIGHT_ZENITH;
            cHorizonA = NIGHT_HORIZON; cHorizonB = NIGHT_HORIZON;
            cFogA = NIGHT_FOG; cFogB = NIGHT_FOG;
            t = 0f;
        }

        lerp3(lightColor, cLightA, cLightB, t);
        lerp3(ambientSky, cAmbSkyA, cAmbSkyB, t);
        lerp3(ambientGround, cAmbGndA, cAmbGndB, t);
        lerp3(skyZenith, cZenithA, cZenithB, t);
        lerp3(skyHorizon, cHorizonA, cHorizonB, t);
        lerp3(fogColor, cFogA, cFogB, t);
    }

    private static float smoothStep(float x) {
        float cx = Math.max(0f, Math.min(1f, x));
        return cx * cx * (3f - 2f * cx);
    }

    private static void lerp3(float[] out, float[] a, float[] b, float t) {
        out[0] = a[0] + (b[0] - a[0]) * t;
        out[1] = a[1] + (b[1] - a[1]) * t;
        out[2] = a[2] + (b[2] - a[2]) * t;
    }

    /**
     * Formatted digital game clock representation (e.g. "06:30 AM", "12:15 PM").
     */
    public String getTimeString() {
        float totalHours = timeOfDay * 24.0f;
        int hours = (int) totalHours;
        int minutes = (int) ((totalHours - hours) * 60.0f);
        boolean pm = hours >= 12;
        int displayHours = hours % 12;
        if (displayHours == 0) displayHours = 12;

        return String.format("%02d:%02d %s", displayHours, minutes, pm ? "PM" : "AM");
    }

    public void setPreset(GraphicsPreset preset) {
        if (preset != null) {
            this.currentPreset = preset;
            update(0f);
        }
    }

    public void setWeather(Weather weather) {
        if (weather != null) {
            this.currentWeather = weather;
            update(0f);
        }
    }
}
