package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class bt_holydem_aura extends BaseEveryFrameCombatPlugin {

    private DamagingProjectileAPI proj;
    private SpriteAPI flareSpriteMain;
    private SpriteAPI flareSpriteBeam;

    private float nebulaTimer = 0f;
    private static final float NEBULA_INTERVAL = 0.05f;
    private static final Random random = new Random();

    private static final Color AURA_NEBULA_BASE_COLOR = new Color(80, 20, 100, 40);
    private static final Color LENS_FLARE_MAIN_COLOR = new Color(120, 30, 80, 60);
    private static final Color LENS_FLARE_BEAM_COLOR = new Color(150, 40, 60, 70);
    private static final Color GLINT_COLOR = new Color(180,50,80,150);

    private float flareRotation = 0f;
    private static final float FLARE_ROTATION_SPEED = 45f;

    public bt_holydem_aura(DamagingProjectileAPI proj) {
        this.proj = proj;
        this.flareSpriteMain = Global.getSettings().getSprite("fx", "bt_holy_explosion");
        this.flareSpriteBeam = Global.getSettings().getSprite("fx", "bt_flare1");
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

        Vector2f projLoc = proj.getLocation();

        flareRotation += FLARE_ROTATION_SPEED * amount;

        nebulaTimer += amount;
        if (nebulaTimer >= NEBULA_INTERVAL) {
            nebulaTimer -= NEBULA_INTERVAL;

            int particlesThisInterval = MathUtils.getRandomNumberInRange(1, 2);

            for (int i = 0; i < particlesThisInterval; i++) {
                float collisionRad = proj.getCollisionRadius();
                float spawnOffsetRadius = collisionRad > 0 ? collisionRad * 0.5f + 3f : 5f;
                Vector2f particleLoc = MathUtils.getRandomPointInCircle(projLoc, spawnOffsetRadius);

                Vector2f particleVel = new Vector2f();
                if (proj.getVelocity().lengthSquared() > 0) {
                    particleVel.x += proj.getVelocity().x * MathUtils.getRandomNumberInRange(0.01f, 0.03f);
                    particleVel.y += proj.getVelocity().y * MathUtils.getRandomNumberInRange(0.01f, 0.03f);
                }
                float outwardSpeed = MathUtils.getRandomNumberInRange(10f, 20f);
                Vector2f expansionDir = MathUtils.getPointOnCircumference(null, outwardSpeed, (float) Math.random() * 360f);
                Vector2f.add(particleVel, expansionDir, particleVel);

                float initialRadius = MathUtils.getRandomNumberInRange(15f, 25f);
                float endRadiusMultiplier = MathUtils.getRandomNumberInRange(2.0f, 3.0f);

                float totalDuration = MathUtils.getRandomNumberInRange(1.0f, 1.8f);
                float rampUpFraction = 0.1f;
                float fullFraction = MathUtils.getRandomNumberInRange(0.2f, 0.3f);

                engine.addNebulaParticle(
                        particleLoc, particleVel, initialRadius, endRadiusMultiplier,
                        rampUpFraction, fullFraction, totalDuration, AURA_NEBULA_BASE_COLOR, false);
            }
        }

        if (flareSpriteMain != null) {
            MagicRender.singleframe(
                    flareSpriteMain,
                    projLoc,
                    new Vector2f(120f, 15f),
                    flareRotation,
                    LENS_FLARE_MAIN_COLOR,
                    true);
        }

        if (flareSpriteBeam != null) {
            MagicRender.singleframe(
                    flareSpriteBeam,
                    projLoc,
                    new Vector2f(100f, 12f),
                    flareRotation + 45f,
                    LENS_FLARE_BEAM_COLOR,
                    true);

            MagicRender.singleframe(
                    flareSpriteBeam,
                    projLoc,
                    new Vector2f(80f, 10f),
                    flareRotation - 45f,
                    LENS_FLARE_BEAM_COLOR,
                    true);
        }

        if (random.nextFloat() < 0.3f) {
            if (flareSpriteMain != null) {
                float glintSize = MathUtils.getRandomNumberInRange(8f, 15f);
                float collisionR = proj.getCollisionRadius();
                Vector2f glintLoc = MathUtils.getRandomPointInCircle(projLoc, collisionR > 0 ? collisionR * 0.2f : 3f);

                MagicRender.singleframe(
                        flareSpriteMain,
                        glintLoc,
                        new Vector2f(glintSize, glintSize),
                        MathUtils.getRandomNumberInRange(0, 360),
                        GLINT_COLOR,
                        true);
            }
        }
    }
}