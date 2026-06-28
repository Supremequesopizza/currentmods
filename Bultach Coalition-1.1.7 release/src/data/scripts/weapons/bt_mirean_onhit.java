package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.magiclib.util.MagicRender;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;
import static com.fs.starfarer.api.util.Misc.getAngleInDegrees;

public class bt_mirean_onhit implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(255, 133, 68, 150);
    private static final Color CORE_COLOR = new Color(255, 75, 34, 232);
    private static final Color AFTERMATH_COLOR = new Color(201, 123, 68, 178);
    private static final Color FLASH_COLOR = new Color(255, 209, 173, 203);
    private static final int NUM_PARTICLES = 20;

    private boolean light = false;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        float force = (projectile.getDamageAmount() * 0.05f);
        CombatUtils.applyForce(target, projectile.getVelocity(), force);

        engine.spawnExplosion(point, ZERO, PARTICLE_COLOR, 30f, 0.8f);
        engine.spawnExplosion(point, ZERO, CORE_COLOR, 10f, 0.6f);
        engine.spawnExplosion(point, ZERO, AFTERMATH_COLOR, 40f, 1.3f);
        engine.addSmoothParticle(point, ZERO, 100, 1f, 0.1f, FLASH_COLOR);
        engine.addSmoothParticle(point, ZERO, 150, 1f, 0.2f, FLASH_COLOR);

        engine.addSmoothParticle(point, ZERO, 50f, 0.5f, 0.1f, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, 25f, 0.5f, 0.25f, FLASH_COLOR);
        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 250f), (float) Math.random() * 360f),
                    5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
        }
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx","bultach_maul_shockwave_2"),
                point,
                ZERO,
                new Vector2f(50,50),
                new Vector2f(175,175),
                //angle,
                360*(float)Math.random(),
                0,
                new Color(255, 163, 48, 109),
                true,
                0,
                0.2f,
                .9f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx","bultach_maul_risidual"),
                point,
                ZERO,
                new Vector2f(125,125),
                new Vector2f(50,50),
                //angle,
                360*(float)Math.random(),
                0,
                new Color(255, 91, 36, 100),
                true,
                0.2f,
                0f,
                1.8f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx","bultach_maul_shockwave_2"),
                point,
                ZERO,
                new Vector2f(100,100),
                new Vector2f(75,75),
                //angle,
                360*(float)Math.random(),
                0,
                new Color(255, 100, 60, 73),
                true,
                0.15f,
                0f,
                1.4f
        );

        WaveDistortion wave = new WaveDistortion(point, ZERO);
        wave.setIntensity(1f);
        wave.setSize(100f);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(1f);
        wave.setLocation(projectile.getLocation());
        DistortionShader.addDistortion(wave);

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            light = true;
        }

        if (light) {
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    point,
                    ZERO,
                    70,
                    2,
                    false,
                    0,
                    360,
                    1f,
                    0.1f,
                    0.25f,
                    0.5f,
                    0.5f,
                    0f
            );
        }
    }
}