package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.magiclib.util.MagicRender;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;


public class bt_clangtorp_onhit implements OnHitEffectPlugin {

    // Visual constants
    static final Color CORE_EXPLOSION_COLOR = new Color(255, 81, 255, 197);
    static final Color CORE_GLOW_COLOR = new Color(245, 102, 255, 150);
    static final Color EXPLOSION_COLOR = new Color(217, 158, 255, 39);
    static final Color FLASH_GLOW_COLOR = new Color(243, 187, 238, 200);
    static final Color GLOW_COLOR = new Color(252, 172, 255, 50);
    static final Vector2f ZERO_VECTOR = new Vector2f();
    static final int NUM_PARTICLES = 50;

    // Scaling constants
    static final float FLUX_BASELINE = 16000f;
    static final float ARMOR_BASELINE = 1400f;
    static final float MIN_SCALING_MULTIPLIER = 0.10f;
    static final float MAX_SCALING_MULTIPLIER = 0.80f;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI)) {
             spawnVisualExplosion(point, engine, projectile);
            return;
        }

        ShipAPI ship = (ShipAPI) target;
        float baseDamage = projectile.getDamageAmount();
        ShipAPI source = projectile.getSource();

        if (shieldHit) {

            float targetFluxCapacity = ship.getMutableStats().getFluxCapacity().getModifiedValue();
            float fluxMultiplier = targetFluxCapacity / FLUX_BASELINE;
            fluxMultiplier = Math.max(MIN_SCALING_MULTIPLIER, Math.min(MAX_SCALING_MULTIPLIER, fluxMultiplier));

            float fluxDamageToDeal = baseDamage * fluxMultiplier;
            ship.getFluxTracker().increaseFlux(fluxDamageToDeal, true);

            engine.spawnEmpArc(
                    source,
                    point,
                    ship,
                    ship,
                    DamageType.ENERGY,
                    0f,
                    0f,
                    1000f,
                    "tachyon_lance_emp_impact",
                    10f + 5f * fluxMultiplier,
                    CORE_EXPLOSION_COLOR,
                    FLASH_GLOW_COLOR
            );

        } else {

            float targetArmorRating = ship.getArmorGrid().getArmorRating();
            float armorMultiplier = targetArmorRating / ARMOR_BASELINE;
            armorMultiplier = Math.max(MIN_SCALING_MULTIPLIER, Math.min(MAX_SCALING_MULTIPLIER, armorMultiplier));

            float heDamageToDeal = baseDamage * armorMultiplier;

             engine.applyDamage(
                    ship,
                    point,
                    heDamageToDeal,
                    DamageType.HIGH_EXPLOSIVE,
                    0f,
                    false,
                    false,
                    source,
                    true
            );
        }

        spawnVisualExplosion(point, engine, projectile);
    }

    static void spawnVisualExplosion(Vector2f point, CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        float CoreExplosionRadius = 50f;
        float CoreExplosionDuration = 1f;
        float ExplosionRadius = 50f;
        float ExplosionDuration = 1f;
        float CoreGlowRadius = 100f;
        float CoreGlowDuration = 1f;
        float GlowRadius = 300f;
        float GlowDuration = 1f;
        float FlashGlowRadius = 400f;
        float FlashGlowDuration = 0.05f;

        engine.spawnExplosion(point, ZERO_VECTOR, CORE_EXPLOSION_COLOR, CoreExplosionRadius, CoreExplosionDuration);
        engine.spawnExplosion(point, ZERO_VECTOR, EXPLOSION_COLOR, ExplosionRadius, ExplosionDuration);
        engine.addHitParticle(point, ZERO_VECTOR, CoreGlowRadius, 1f, CoreGlowDuration, CORE_GLOW_COLOR);
        engine.addSmoothParticle(point, ZERO_VECTOR, GlowRadius, 1f, GlowDuration, GLOW_COLOR);
        engine.addHitParticle(point, ZERO_VECTOR, FlashGlowRadius, 1f, FlashGlowDuration, FLASH_GLOW_COLOR);

        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    MathUtils.getRandomNumberInRange(4, 8), 1f, MathUtils.getRandomNumberInRange(0.4f, 0.9f), CORE_EXPLOSION_COLOR);
        }

        Global.getSoundPlayer().playSound("bt_divine_CLANG_explosion", 1f + MathUtils.getRandomNumberInRange(-0.1f, 0.1f), 1.3f, point, ZERO_VECTOR);

        if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_holy_explosion"),
                    point, new Vector2f(), new Vector2f(50, 50), new Vector2f(700, 700),
                    360 * (float) Math.random(), 0, new Color(255, 206, 241, 231), true, 0, 0f, 0.5f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_holy_explosion"),
                    point, new Vector2f(), new Vector2f(96, 96), new Vector2f(220, 220),
                    360 * (float) Math.random(), 0, new Color(243, 180, 255, 131), true, 0, 0.1f, 0.3f
            );
             MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(128, 128), new Vector2f(250, 250),
                    360 * (float) Math.random(), 0, new Color(239, 178, 255, 168), true, 0.2f, 0.0f, 0.4f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(250, 250), new Vector2f(70, 70),
                    360 * (float) Math.random(), 0, new Color(234, 192, 255, 158), true, 0.35f, 0.0f, 1f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(200, 200), new Vector2f(50, 50),
                    360 * (float) Math.random(), 0, new Color(255, 149, 253, 45), true, 0.35f, 0.0f, 1.5f
            );
        }

        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
             WaveDistortion wave = new WaveDistortion(point, ZERO_VECTOR);
             wave.setIntensity(1.5f);
             wave.setSize(225f);
             wave.flip(true);
             wave.setLifetime(0f);
             wave.fadeOutIntensity(1f);
             wave.setLocation(point);
             DistortionShader.addDistortion(wave);

             bt_yoinked_graphicLibEffects.CustomRippleDistortion(
                    point, ZERO_VECTOR, 90, 3, false, 0, 360, 1f,
                    0.1f, 0.25f, 0.5f, 0.5f, 0f
             );
            if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
                 MagicRender.battlespace(
                         Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow"),
                         point, new Vector2f(),
                         new Vector2f(50 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 900 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)),
                         new Vector2f(), 360 * (float) Math.random(), 0, new Color(255, 190, 250, 255),
                         true, 0, 0, 0.5f, 0.15f, MathUtils.getRandomNumberInRange(0.05f, 0.2f),
                         0, MathUtils.getRandomNumberInRange(0.4f, 0.6f), MathUtils.getRandomNumberInRange(0.1f, 0.3f),
                         CombatEngineLayers.CONTRAILS_LAYER
                 );
            }
        }
    }
}