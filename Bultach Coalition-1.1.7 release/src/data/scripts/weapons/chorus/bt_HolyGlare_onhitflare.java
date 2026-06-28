package data.scripts.weapons.chorus;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;

import org.magiclib.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class bt_HolyGlare_onhitflare implements BeamEffectPlugin {
    private static final Color PARTICLE_COLOR = new Color(255, 233, 185, 150);
    private static final Color CORE_COLOR = new Color(255, 252, 243);
    private static final Color FLASH_COLOR = new Color(255, 249, 237);
    private static final int NUM_PARTICLES = 40;

    private final IntervalUtil flare = new IntervalUtil(0.15f, 0.15f);
    private boolean detonated = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        WeaponAPI weapon = beam.getWeapon();
        // Don't run if we are paused or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        Vector2f point = beam.getTo();

        // Update the interval and check if it's time to spawn a new flare
        flare.advance(amount);
        if (flare.intervalElapsed()) {
            spawnFlare(engine, beam, point);
        }

        if (!detonated) {
            // Spawn the initial explosion effects
            engine.spawnExplosion(point, ZERO, CORE_COLOR, 100f, 1f);
            engine.spawnExplosion(point, ZERO, FLASH_COLOR, 50f, 1f);

            // Create a damaging explosion effect
            DamagingExplosionSpec blast = new DamagingExplosionSpec(0.1f,
                    500f,
                    50f,
                    100f,
                    100f,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    10f,
                    10f,
                    0f,
                    0,
                    PARTICLE_COLOR,
                    null);
            blast.setDamageType(DamageType.KINETIC);
            blast.setShowGraphic(false);
            engine.spawnDamagingExplosion(blast, beam.getSource(), point, false);

            DamagingExplosionSpec blast2 = new DamagingExplosionSpec(0.1f,
                    500f,
                    50f,
                    100f,
                    100f,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    10f,
                    10f,
                    0f,
                    0,
                    PARTICLE_COLOR,
                    null);
            blast2.setDamageType(DamageType.HIGH_EXPLOSIVE);
            blast2.setShowGraphic(false);
            engine.spawnDamagingExplosion(blast2, beam.getSource(), point, false);

            // Add particles for visual effect
            engine.addSmoothParticle(point, ZERO, 400f, 0.5f, 0.1f, PARTICLE_COLOR);
            engine.addHitParticle(point, ZERO, 200f, 0.5f, 0.25f, FLASH_COLOR);
            for (int x = 0; x < NUM_PARTICLES; x++) {
                engine.addHitParticle(point,
                        MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 300f), (float) Math.random() * 360f),
                        6f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
            }

            detonated = true;
        }
    }

    private void spawnFlare(CombatEngineAPI engine, BeamAPI beam, Vector2f point) {
        MagicLensFlare.createSharpFlare(engine, beam.getSource(), point, 20, 1400, 0, new Color(255, 226, 183), new Color(255, 255, 255));
    }
}
