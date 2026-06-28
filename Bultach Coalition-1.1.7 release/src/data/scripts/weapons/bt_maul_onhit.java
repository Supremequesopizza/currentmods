package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.magiclib.util.MagicRender;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;


public class bt_maul_onhit implements OnHitEffectPlugin {

    static final Color CORE_EXPLOSION_COLOR = new Color(255, 163, 100, 255);
    static final Color CORE_GLOW_COLOR = new Color(255, 187, 117, 150);
    static final Color EXPLOSION_COLOR = new Color(255, 248, 176, 10);
    static final Color FLASH_GLOW_COLOR = new Color(255, 205, 128, 200);
    static final Color GLOW_COLOR = new Color(255, 236, 172, 50);
    static final Vector2f ZERO = new Vector2f();

    static final int NUM_PARTICLES = 50;

    static final DamagingExplosionSpec explosion = new DamagingExplosionSpec(0.05f,
            850,
            350f,
            0,
            0f,
            CollisionClass.PROJECTILE_FF,
            CollisionClass.PROJECTILE_FIGHTER,
            0,
            0,
            0,
            0,
            new Color(178, 122, 0, 255),
            new Color(255, 150, 35, 255)
    );

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof MissileAPI) return;

        explosion(point, engine, projectile);
    }

    static void explosion(Vector2f point, CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        // Blast visuals
        float CoreExplosionRadius = 500f;
        float CoreExplosionDuration = 4f;
        float ExplosionRadius = 1000f;
        float ExplosionDuration = 6.5f;
        float CoreGlowRadius = 750f;
        float CoreGlowDuration = 5f;
        float GlowRadius = 750f;
        float GlowDuration = 10f;
        float FlashGlowRadius = 1500f;
        float FlashGlowDuration = 0.15f;

        engine.spawnExplosion(point, ZERO, CORE_EXPLOSION_COLOR, CoreExplosionRadius, CoreExplosionDuration);
        engine.spawnExplosion(point, ZERO, EXPLOSION_COLOR, ExplosionRadius, ExplosionDuration);
        engine.addHitParticle(point, ZERO, CoreGlowRadius, 2f, CoreGlowDuration, CORE_GLOW_COLOR);
        engine.addSmoothParticle(point, ZERO, GlowRadius, 2f, GlowDuration, GLOW_COLOR);
        engine.addHitParticle(point, ZERO, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_GLOW_COLOR);

        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    MathUtils.getRandomNumberInRange(4, 8), 2f, MathUtils.getRandomNumberInRange(0.4f, 0.9f), CORE_EXPLOSION_COLOR);
        }

        explosion.setDamageType(DamageType.FRAGMENTATION);
        explosion.setShowGraphic(false);
        explosion.setMaxDamage(projectile.getDamageAmount());
        explosion.setMinDamage(explosion.getMaxDamage() * 0.5f);
        engine.spawnDamagingExplosion(explosion, projectile.getSource(), point);

        Global.getSoundPlayer().playSound("bt_maul_impact", 1f + MathUtils.getRandomNumberInRange(-0.1f, 0.1f), 1f, point, ZERO);

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_maul_shockwave"),
                point,
                new Vector2f(),
                new Vector2f(50, 50),
                new Vector2f(2500, 2500),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 228, 189, 255),
                true,
                0,
                0f,
                0.7f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_maul_risidual"),
                point,
                new Vector2f(),
                new Vector2f(96, 96),
                new Vector2f(880, 880),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 230, 230, 255),
                true,
                0,
                0.1f,
                0.3f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_explosion"),
                point,
                new Vector2f(),
                new Vector2f(128, 128),
                new Vector2f(1000, 1000),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 203, 135, 255),
                true,
                0.2f,
                0.0f,
                0.4f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_maul_risidual"),
                point,
                new Vector2f(),
                new Vector2f(250, 250),
                new Vector2f(350, 350),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 176, 127, 200),
                true,
                0.35f,
                1.6f,
                2.7f
        );

        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "bultach_siege_explosion"),
                point,
                new Vector2f(),
                new Vector2f(200, 200),
                new Vector2f(225, 225),
                //angle,
                360 * (float) Math.random(),
                0,
                new Color(255, 192, 153, 100),
                true,
                0.35f,
                0.0f,
                1.5f
        );

        WaveDistortion wave = new WaveDistortion(point, ZERO);
        wave.setIntensity(1.5f);
        wave.setSize(225f);
        wave.flip(true);
        wave.setLifetime(0f);
        wave.fadeOutIntensity(1f);
        wave.setLocation(projectile.getLocation());
        DistortionShader.addDistortion(wave);

        boolean light = false;
        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            light = true;
        }

        if (light) {
            bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    point,
                    ZERO,
                    225,
                    3,
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

            MagicRender.battlespace(
                    Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow"),
                    point,
                    new Vector2f(),
                    new Vector2f(100 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 2500 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)),
                    new Vector2f(),
                    360 * (float) Math.random(),
                    0,
                    new Color(255, 236, 177, 255),
                    true,
                    0,
                    0,
                    0.7f,
                    0.20f,
                    MathUtils.getRandomNumberInRange(0.05f, 0.2f),
                    0,
                    MathUtils.getRandomNumberInRange(0.4f, 0.6f),
                    MathUtils.getRandomNumberInRange(0.1f, 0.3f),
                    CombatEngineLayers.CONTRAILS_LAYER
            );
        }
    }
}



























