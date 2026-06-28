package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class bt_hellburn_dotOnHit implements OnHitEffectPlugin {

    private static final float DURATION = 3f;
    private static final float TOTAL_DAMAGE = 200f;
    private static final float SPREAD_RADIUS = 80f;
    private static final int MIN_INSTANCES = 2;
    private static final int MAX_INSTANCES = 4;

    private static final Color CORE_EXPLOSION_COLOR = new Color(255, 163, 100, 255);
    private static final Color EXPLOSION_COLOR = new Color(255, 248, 176, 10);
    private static final Color FLASH_GLOW_COLOR = new Color(255, 205, 128, 200);
    private static final int NUM_PARTICLES = 15;
    private static final Vector2f ZERO = new Vector2f();
    private static final String SOUND_ID = "bt_maul_impact";

    @Override
    public void onHit(final DamagingProjectileAPI projectile, final CombatEntityAPI target, final Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {

        spawnSmallExplosion(point, engine);

        if (!shieldHit && target instanceof ShipAPI) {
            int numInstances = MathUtils.getRandomNumberInRange(MIN_INSTANCES, MAX_INSTANCES);

            for (int i = 0; i < numInstances; i++) {
                final Vector2f randomPoint = MathUtils.getRandomPointInCircle(point, SPREAD_RADIUS);

                engine.addPlugin(new EveryFrameCombatPlugin() {
                    private final float initialFacing = target.getFacing();
                    private final Vector2f shipRefHitLoc = VectorUtils.rotateAroundPivot(randomPoint, target.getLocation(), -initialFacing, new Vector2f());
                    private final IntervalUtil damageTimer = new IntervalUtil(0.25f, 0.25f);
                    private final IntervalUtil FXTimer = new IntervalUtil(0.1f, 0.1f);
                    private float totalDuration = 0;

                    @Override
                    public void advance(float amount, List<InputEventAPI> events) {
                        if (engine.isPaused()) return;

                        totalDuration += amount;
                        if (totalDuration >= DURATION) {
                            engine.removePlugin(this);
                            return;
                        }

                        damageTimer.advance(amount);
                        FXTimer.advance(amount);

                        Vector2f hitLoc = VectorUtils.rotateAroundPivot(shipRefHitLoc, target.getLocation(), target.getFacing(), new Vector2f());

                        if (damageTimer.intervalElapsed()) {
                            engine.applyDamage(target, hitLoc, TOTAL_DAMAGE / DURATION * damageTimer.getIntervalDuration(), DamageType.ENERGY, 0, false, true, projectile.getSource());
                        }

                        if (FXTimer.intervalElapsed()) {
                            engine.addSwirlyNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(10, 30), MathUtils.getRandomNumberInRange(1, 2), 0.8f, 0.3f, MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(200, 255), MathUtils.getRandomNumberInRange(100, 160), MathUtils.getRandomNumberInRange(0, 60), 150), true);
                            engine.addNebulaParticle(hitLoc, target.getVelocity(), MathUtils.getRandomNumberInRange(15, 45), MathUtils.getRandomNumberInRange(3, 4), 0.8f, 0.3f, MathUtils.getRandomNumberInRange(0.4f, 0.6f), new Color(MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), MathUtils.getRandomNumberInRange(20, 40), 100), true);
                        }
                    }

                    @Override
                    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {}
                    @Override
                    public void renderInWorldCoords(ViewportAPI viewport) {}
                    @Override
                    public void renderInUICoords(ViewportAPI viewport) {}
                    @Override
                    public void init(CombatEngineAPI engine) {}
                });
            }
        }

        if (shieldHit) {
            target.getShield().setActiveArc(target.getShield().getActiveArc() - 4f);
        }
    }

    private void spawnSmallExplosion(Vector2f point, CombatEngineAPI engine) {
        float coreExplosionRadius = 85f;
        float explosionRadius = 170f;
        float flashGlowRadius = 250f;
        float flashGlowDuration = 0.15f;
        float distortionSize = 40f;
        float distortionIntensity = 1f;

        engine.spawnExplosion(point, ZERO, CORE_EXPLOSION_COLOR, coreExplosionRadius, 1.5f);
        engine.spawnExplosion(point, ZERO, EXPLOSION_COLOR, explosionRadius, 2.5f);
        engine.addHitParticle(point, ZERO, flashGlowRadius, 1f, flashGlowDuration, FLASH_GLOW_COLOR);

        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(
                    point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(10f, 40f), (float) Math.random() * 360f),
                    MathUtils.getRandomNumberInRange(4, 8),
                    1f,
                    MathUtils.getRandomNumberInRange(0.4f, 0.9f),
                    CORE_EXPLOSION_COLOR
            );
        }

        WaveDistortion wave = new WaveDistortion(point, ZERO);
        wave.setIntensity(distortionIntensity);
        wave.setSize(distortionSize);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(1f);
        wave.setLocation(point);
        DistortionShader.addDistortion(wave);

        Global.getSoundPlayer().playSound(SOUND_ID, 2.2f, 0.2f, point, ZERO);
    }
}