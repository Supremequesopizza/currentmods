package data.scripts.weapons;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud.DwellerShroudParams;
import com.fs.starfarer.api.util.Misc;

import org.magiclib.util.MagicRender;
import data.scripts.utilities.bt_yoinked_graphicLibEffects;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;


public class bt_vortex_onfire implements OnHitEffectPlugin, OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

    public static Color RIFT_LIGHTNING_COLOR = new Color(255, 241, 214,255);
    public static float RIFT_LIGHTNING_SPEED = 8000f;

    public static String RIFT_LIGHTNING_DAMAGE_REMOVER = "rift_lightning_damage_remover";
    public static String RIFT_LIGHTNING_FIRED_TAG = "rift_lightning_fired_tag";
    public static String RIFT_LIGHTNING_SOURCE_WEAPON = "rift_lightning_source_weapon";

    public static final String RIFT_DRONE_HULL_ID = "ork_vortex_drone";

    public static float EFFECT_BASE_DAMAGE = 250f;
    public static float EFFECT_EMP_DAMAGE = 350f;
    public static float FLUX_DAMAGE_ON_SHIELD_HIT_MULTIPLIER = 1.5f;
    public static DamageType DAMAGE_TYPE_ON_HULL = DamageType.HIGH_EXPLOSIVE;


    public static class FiredLightningProjectile {
        public DamagingProjectileAPI projectile;
    }

    protected List<FiredLightningProjectile> fired = new ArrayList<>();

    static final Color CORE_EXPLOSION_COLOR = new Color(255, 215, 156, 255);
    static final Color CORE_GLOW_COLOR = new Color(241, 232, 213, 150);
    static final Color EXPLOSION_COLOR = new Color(255, 233, 176, 10);
    static final Color FLASH_GLOW_COLOR = new Color(241, 226, 215, 200);
    static final Color GLOW_COLOR = new Color(255, 172, 241, 50);
    static final Vector2f ZERO_VECTOR = new Vector2f();
    static final int NUM_PARTICLES = 20;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null) return;

        ShipAPI ship = weapon.getShip();
        if (ship != null && ship.getHullSpec().getHullId().equals(RIFT_DRONE_HULL_ID)) {
            ship.getVelocity().set(0f, 0f);
            ship.setAngularVelocity(0f);

            if (ship.getShipTarget() == null || !ship.getShipTarget().isAlive() || ship.getShipTarget().isPhased() || ship.getOwner() == ship.getShipTarget().getOwner()) {
                ShipAPI newTarget = findClosestEnemyForDroneAI(ship, weapon, weapon.getRange());
                if (newTarget != null) {
                    ship.setShipTarget(newTarget);
                }
            }
        }

        if (engine.isInFastTimeAdvance()) {
            return;
        }

        List<FiredLightningProjectile> remove = new ArrayList<>();

        float maxRange = weapon.getRange();
        for (FiredLightningProjectile data : fired) {
            if (data.projectile == null) {
                remove.add(data);
                continue;
            }
            float dist = Misc.getDistance(data.projectile.getSpawnLocation(), data.projectile.getLocation());
            boolean firedAlready = data.projectile.getCustomData().containsKey(RIFT_LIGHTNING_FIRED_TAG);
            if (dist > maxRange || firedAlready || !engine.isEntityInPlay(data.projectile)) {
                remove.add(data);
                if (!firedAlready && engine.isEntityInPlay(data.projectile)) {
                    fireArc(data.projectile, weapon, null, null);
                }
            }
        }
        fired.removeAll(remove);
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        projectile.getDamage().getModifier().modifyMult(RIFT_LIGHTNING_DAMAGE_REMOVER, 0f);
        projectile.setCustomData(RIFT_LIGHTNING_SOURCE_WEAPON, weapon);

        FiredLightningProjectile data = new FiredLightningProjectile();
        data.projectile = projectile;
        fired.add(data);
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        WeaponAPI weapon = (WeaponAPI) projectile.getCustomData().get(RIFT_LIGHTNING_SOURCE_WEAPON);
        if (weapon == null) return;
        ShipAPI sourceShip = weapon.getShip();
        if (sourceShip == null) return;

        if (target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;
            if (shieldHit && targetShip.getShield() != null && targetShip.getShield().isWithinArc(point)) {
                float fluxDamage = EFFECT_BASE_DAMAGE * FLUX_DAMAGE_ON_SHIELD_HIT_MULTIPLIER;
                targetShip.getFluxTracker().increaseFlux(fluxDamage, true);
                engine.spawnEmpArc(sourceShip, point, targetShip, targetShip,
                        DamageType.ENERGY,
                        0f,
                        EFFECT_EMP_DAMAGE * 0.5f,
                        1000f,
                        "tachyon_lance_emp_impact",
                        10f,
                        RIFT_LIGHTNING_COLOR,
                        Color.WHITE);
            } else {
                engine.applyDamage(
                        targetShip,
                        point,
                        EFFECT_BASE_DAMAGE,
                        DAMAGE_TYPE_ON_HULL,
                        EFFECT_EMP_DAMAGE,
                        false,
                        false,
                        sourceShip);
            }
        } else if (target instanceof MissileAPI || target instanceof CombatAsteroidAPI) {
            engine.applyDamage(
                    target,
                    point,
                    EFFECT_BASE_DAMAGE,
                    DAMAGE_TYPE_ON_HULL,
                    EFFECT_EMP_DAMAGE,
                    false,
                    false,
                    sourceShip);
        }

        fireArc(projectile, weapon, point, target);
    }

    public static void fireArc(DamagingProjectileAPI projectile, WeaponAPI weapon, Vector2f point, CombatEntityAPI target) {
        boolean firedAlready = projectile.getCustomData().containsKey(RIFT_LIGHTNING_FIRED_TAG);
        if (firedAlready) return;

        projectile.setCustomData(RIFT_LIGHTNING_FIRED_TAG, true);

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        Vector2f from = projectile.getSpawnLocation();
        float dist = Float.MAX_VALUE;
        if (point != null) dist = Misc.getDistance(from, point);

        float maxRange = weapon.getRange();
        if (dist > maxRange || point == null) {
            dist = maxRange * (0.5f + 0.5f * (float) Math.random());
            if (projectile.didDamage()) {
                dist = maxRange;
            }
            point = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
            point.scale(dist);
            Vector2f.add(point, from, point);
        }

        float arcSpeed = RIFT_LIGHTNING_SPEED;
        DwellerShroud shroud = DwellerShroud.getShroudFor(ship);
        if (shroud != null) {
            float angle = Misc.getAngleInDegrees(ship.getLocation(), point);
            from = Misc.getUnitVectorAtDegreeAngle(angle + 90f - 180f * (float) Math.random());
            from.scale((0.5f + (float) Math.random() * 0.25f) * shroud.getShroudParams().maxOffset);
            Vector2f.add(ship.getLocation(), from, from);
        }

        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 8f;
        params.zigZagReductionFactor = 0.15f;
        params.fadeOutDist = 50f;
        params.minFadeOutMult = 10f;
        params.flickerRateMult = 0.3f;
        params.movementDurOverride = Math.max(0.05f, dist / arcSpeed);

        Color color = RIFT_LIGHTNING_COLOR;
        EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcVisual(from, ship, point, null,
                80f, color, new Color(255, 247, 243,255), params);
        arc.setCoreWidthOverride(40f);
        arc.setRenderGlowAtStart(false);
        arc.setFadedOutAtStart(true);
        arc.setSingleFlickerMode(true);

        spawnVisualExplosion(point, engine, projectile);

        if (shroud != null) {
            DwellerShroudParams shroudParams = shroud.getShroudParams();
            params = new EmpArcEntityAPI.EmpArcParams();
            params.segmentLengthMult = 4f;
            params.glowSizeMult = 4f;
            params.flickerRateMult = 0.5f + (float) Math.random() * 0.5f;
            params.flickerRateMult *= 1.5f;
            Color fringe = color;
            Color core = Color.white;
            float thickness = shroudParams.overloadArcThickness;
            float angle = Misc.getAngleInDegrees(from, ship.getLocation());
            angle = angle + 90f * ((float) Math.random() - 0.5f);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
            dist = shroudParams.maxOffset;
            dist = dist * 0.5f + dist * 0.5f * (float) Math.random();
            dist *= 0.5f;
            dir.scale(dist);
            Vector2f to = Vector2f.add(from, dir, new Vector2f());
            arc = (EmpArcEntityAPI)engine.spawnEmpArcVisual(
                    from, ship, to, ship, thickness, fringe, core, params);
            arc.setCoreWidthOverride(shroudParams.overloadArcCoreThickness);
            arc.setSingleFlickerMode(false);
        }
    }

    public static void spawnVisualExplosion(Vector2f point, CombatEngineAPI engine, DamagingProjectileAPI projectile) {
        float CoreExplosionRadius = 50f;
        float CoreExplosionDuration = 1f;
        float ExplosionRadius = 50f;
        float ExplosionDuration = 1f;
        float CoreGlowRadius = 50f;
        float CoreGlowDuration = 1f;
        float GlowRadius = 150f;
        float GlowDuration = 1f;
        float FlashGlowRadius = 200f;
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

        Global.getSoundPlayer().playSound("bt_tolladh_impact", 2f + MathUtils.getRandomNumberInRange(-0.1f, 0.1f), 0.5f, point, ZERO_VECTOR);

        if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"),
                    point, new Vector2f(), new Vector2f(20, 20), new Vector2f(250, 250),
                    360 * (float) Math.random(), 0, new Color(255, 246, 236, 255), true, 0, 0f, 0.5f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_holy_explosion"),
                    point, new Vector2f(), new Vector2f(46, 46), new Vector2f(110, 110),
                    360 * (float) Math.random(), 0, new Color(255, 240, 235, 255), true, 0, 0.1f, 0.3f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(78, 78), new Vector2f(125, 125),
                    360 * (float) Math.random(), 0, new Color(255, 247, 237, 255), true, 0.2f, 0.0f, 0.4f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(130, 130), new Vector2f(40, 40),
                    360 * (float) Math.random(), 0, new Color(255, 245, 233, 200), true, 0.35f, 0.0f, 1f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bt_cleave_aura"),
                    point, new Vector2f(), new Vector2f(100, 100), new Vector2f(20, 20),
                    360 * (float) Math.random(), 0, new Color(255, 203, 149, 100), true, 0.35f, 0.0f, 1.5f
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
                        new Vector2f(20 * MathUtils.getRandomNumberInRange(0.8f, 1.2f), 200 * MathUtils.getRandomNumberInRange(0.8f, 1.2f)),
                        new Vector2f(), 360 * (float) Math.random(), 0, new Color(252, 255, 226, 255),
                        true, 0, 0, 0.5f, 0.15f, MathUtils.getRandomNumberInRange(0.05f, 0.2f),
                        0, MathUtils.getRandomNumberInRange(0.4f, 0.6f), MathUtils.getRandomNumberInRange(0.1f, 0.3f),
                        CombatEngineLayers.CONTRAILS_LAYER
                );
            }
        }
    }

    private ShipAPI findClosestEnemyForDroneAI(ShipAPI droneSourceShip, WeaponAPI weaponForTargeting, float range) {
        ShipAPI closestEnemy = null;
        float minDistanceSq = range * range;
        List<ShipAPI> potentialTargets = AIUtils.getEnemiesOnMap(droneSourceShip);

        for (ShipAPI potentialTarget : potentialTargets) {
            if (potentialTarget == null || !potentialTarget.isAlive() || potentialTarget.isPhased()) {
                continue;
            }
            if (potentialTarget.isFighter() || potentialTarget.isDrone()) {
                continue;
            }
            if (potentialTarget.getOwner() == droneSourceShip.getOwner()) {
                continue;
            }

            float currentDistanceSq = MathUtils.getDistanceSquared(weaponForTargeting.getLocation(), potentialTarget.getLocation());
            if (currentDistanceSq < minDistanceSq) {
                minDistanceSq = currentDistanceSq;
                closestEnemy = potentialTarget;
            }
        }
        return closestEnemy;
    }
}