package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class bt_holytorp_aura extends BaseEveryFrameCombatPlugin {

    private DamagingProjectileAPI proj;
    private SpriteAPI flareSpriteMain;
    private SpriteAPI flareSpriteBeam;

    private float nebulaTimer = 0f;
    private static final float NEBULA_INTERVAL = 0.05f;
    private static final Random random = new Random();


    private static final Color AURA_NEBULA_BASE_COLOR_UNARMED = new Color(196, 175, 154, 35);
    private static final Color AURA_NEBULA_BASE_COLOR_ARMED = new Color(220, 200, 180, 70);

    private static final Color LENS_FLARE_MAIN_COLOR_UNARMED = new Color(255, 229, 200, 75);
    private static final Color LENS_FLARE_BEAM_COLOR_UNARMED = new Color(255, 223, 175, 85);
    private static final Color LENS_FLARE_MAIN_COLOR_ARMED = new Color(255, 239, 210, 175);
    private static final Color LENS_FLARE_BEAM_COLOR_ARMED = new Color(255, 233, 195, 185);
    private static final Color GLINT_COLOR = new Color(255,255,255,200);

    private boolean isArmedState = false;

    // Jitter parameters
    private static final float FLARE_JITTER_MAX_OFFSET = 1f;
    private static final float FLARE_JITTER_MAX_SIZE_MULT = 0.1f;

    public bt_holytorp_aura(DamagingProjectileAPI proj) {
        this.proj = proj;
        this.flareSpriteMain = Global.getSettings().getSprite("fx", "bt_holy_explosion");
        this.flareSpriteBeam = Global.getSettings().getSprite("fx", "bt_flare1");

        Global.getLogger(bt_holytorp_aura.class).info("Aura plugin constructor called for projectile: " + (this.proj != null ? this.proj.hashCode() : "null_projectile_passed_to_constructor"));
        if (this.flareSpriteMain == null) {
            Global.getLogger(bt_holytorp_aura.class).error("Failed to load flareSpriteMain! Sprite ID: ('fx', 'bt_holy_explosion')");
        }
        if (this.flareSpriteBeam == null) {
            Global.getLogger(bt_holytorp_aura.class).error("Failed to load flareSpriteBeam! Sprite ID: ('fx', 'bt_flare1')");
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (proj == null) {
            if (Global.getCombatEngine() != null) Global.getCombatEngine().removePlugin(this);
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        if (!engine.isEntityInPlay(proj)) {
            engine.removePlugin(this);
            return;
        }

        if (proj instanceof MissileAPI) {
            isArmedState = ((MissileAPI) proj).isMinePrimed();
        }

        if (proj.isFading()) {
            isArmedState = true;
        }

        Vector2f projLoc = proj.getLocation();
        float projFacing = proj.getFacing();

        nebulaTimer += amount;
        if (nebulaTimer >= NEBULA_INTERVAL) {
            nebulaTimer -= NEBULA_INTERVAL;

            int particlesThisInterval = isArmedState ? MathUtils.getRandomNumberInRange(4, 6) : MathUtils.getRandomNumberInRange(2, 3);

            Color currentNebulaBaseColor = isArmedState ? AURA_NEBULA_BASE_COLOR_ARMED : AURA_NEBULA_BASE_COLOR_UNARMED;

            for (int i = 0; i < particlesThisInterval; i++) {
                float collisionRad = proj.getCollisionRadius();
                float spawnOffsetRadius = collisionRad > 0 ? collisionRad * 0.7f + 5f : 10f;
                spawnOffsetRadius *= isArmedState ? 1.3f : 1f;
                Vector2f particleLoc = MathUtils.getRandomPointInCircle(projLoc, spawnOffsetRadius);

                Vector2f particleVel = new Vector2f();
                if (proj.getVelocity().lengthSquared() > 0) {
                    particleVel.x += proj.getVelocity().x * MathUtils.getRandomNumberInRange(0.01f, 0.05f);
                    particleVel.y += proj.getVelocity().y * MathUtils.getRandomNumberInRange(0.01f, 0.05f);
                }
                float outwardSpeed = isArmedState ? MathUtils.getRandomNumberInRange(25f, 45f) : MathUtils.getRandomNumberInRange(15f, 35f);
                Vector2f expansionDir = MathUtils.getPointOnCircumference(null, outwardSpeed, (float) Math.random() * 360f);
                Vector2f.add(particleVel, expansionDir, particleVel);

                float initialRadius = isArmedState ? MathUtils.getRandomNumberInRange(30f, 50f) : MathUtils.getRandomNumberInRange(25f, 40f);
                float endRadiusMultiplier = isArmedState ? MathUtils.getRandomNumberInRange(4.0f, 6.0f) : MathUtils.getRandomNumberInRange(3.5f, 5.5f);

                float totalDuration = isArmedState ? MathUtils.getRandomNumberInRange(2.0f, 3.2f) : MathUtils.getRandomNumberInRange(1.5f, 2.8f);
                float rampUpFraction = 0.08f;
                float fullFraction = MathUtils.getRandomNumberInRange(0.2f, 0.4f);

                int baseAlpha = currentNebulaBaseColor.getAlpha();
                int particleAlpha = MathUtils.getRandomNumberInRange(baseAlpha - 10, baseAlpha + 20);
                particleAlpha = Math.max(20, Math.min(120, particleAlpha));

                Color finalParticleColor = new Color(currentNebulaBaseColor.getRed(),
                        currentNebulaBaseColor.getGreen(),
                        currentNebulaBaseColor.getBlue(),
                        particleAlpha);

                engine.addNebulaParticle(
                        particleLoc, particleVel, initialRadius, endRadiusMultiplier,
                        rampUpFraction, fullFraction, totalDuration, finalParticleColor, false);
            }
        }


        Color currentMainFlareColor = isArmedState ? LENS_FLARE_MAIN_COLOR_ARMED : LENS_FLARE_MAIN_COLOR_UNARMED;
        Color currentBeamFlareColor = isArmedState ? LENS_FLARE_BEAM_COLOR_ARMED : LENS_FLARE_BEAM_COLOR_UNARMED;


        float mainFlareFixedSizeX = isArmedState ? 300f : 250f;
        float mainFlareFixedSizeY = isArmedState ? 40f : 30f;


        float beamFlareWidth = isArmedState ? 200f : 170f;
        float beamFlareHeight = isArmedState ? 25f : 20f;



        float jitterOffsetX = MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_OFFSET, FLARE_JITTER_MAX_OFFSET);
        float jitterOffsetY = MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_OFFSET, FLARE_JITTER_MAX_OFFSET);
        Vector2f jitteredFlareRenderLoc = new Vector2f(projLoc.x + jitterOffsetX, projLoc.y + jitterOffsetY);

        if (flareSpriteMain != null) {
            float sizeMultX = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            float sizeMultY = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            MagicRender.singleframe(
                    flareSpriteMain,
                    jitteredFlareRenderLoc,
                    new Vector2f(mainFlareFixedSizeX * sizeMultX, mainFlareFixedSizeY * sizeMultY),
                    0f,
                    currentMainFlareColor,
                    true);
        }

        if (flareSpriteBeam != null) {
            float sizeMultX_beam1 = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            float sizeMultY_beam1 = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            MagicRender.singleframe(
                    flareSpriteBeam,
                    jitteredFlareRenderLoc,
                    new Vector2f(mainFlareFixedSizeX * sizeMultX_beam1, mainFlareFixedSizeY * sizeMultY_beam1),
                    0f,
                    currentBeamFlareColor,
                    true);

            float sizeMultX_beam2 = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            float sizeMultY_beam2 = 1f + MathUtils.getRandomNumberInRange(-FLARE_JITTER_MAX_SIZE_MULT, FLARE_JITTER_MAX_SIZE_MULT);
            MagicRender.singleframe(
                    flareSpriteBeam,
                    jitteredFlareRenderLoc,
                    new Vector2f(beamFlareWidth * sizeMultX_beam2, beamFlareHeight * sizeMultY_beam2),
                    90f + MathUtils.getRandomNumberInRange(-5f, 5f),
                    currentBeamFlareColor,
                    true);
        }

        if (isArmedState && random.nextFloat() < (isArmedState ? 0.5f : 0.1f) ) {
            if (flareSpriteMain != null) {
                float glintSize = isArmedState ? MathUtils.getRandomNumberInRange(20f, 40f) : MathUtils.getRandomNumberInRange(15f, 30f);
                int glintAlpha = (int)(GLINT_COLOR.getAlpha() * (0.6f + random.nextFloat() * 0.4f));
                Color timedGlintColor = new Color(GLINT_COLOR.getRed(), GLINT_COLOR.getGreen(), GLINT_COLOR.getBlue(), glintAlpha);

                float collisionR = proj.getCollisionRadius();
                Vector2f glintLoc = MathUtils.getRandomPointInCircle(projLoc, collisionR > 0 ? collisionR * 0.3f : 6f);

                MagicRender.singleframe(
                        flareSpriteMain,
                        glintLoc,
                        new Vector2f(glintSize, glintSize),
                        MathUtils.getRandomNumberInRange(0, 360),
                        timedGlintColor,
                        true);
            }
        }
    }
}