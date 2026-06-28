package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class bt_reconstructed_PhaseStats extends BaseShipSystemScript {

    public static final float SHIP_ALPHA_MULT = 0.25f;
    public static final float MAX_TIME_MULT = 3f;

    public static final boolean FLUX_LEVEL_AFFECTS_SPEED = true;
    public static final float MIN_SPEED_MULT = 0.33f;
    public static final float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.5f;

    private static final Color NEBULA_TRAIL_COLOR = new Color(255, 0, 0, 30);
    private static final float NEBULA_TRAIL_INTERVAL = 0.1f;
    private static final Color EXPLOSION_BASE_COLOR = new Color(255, 0, 0, 125);
    private static final Color SHOCKWAVE_COLOR = new Color(255, 0, 0, 50);
    private static final Color SHOCKWAVE_FX_COLOR = new Color(255, 0, 68, 100);
    private static final float BOOST_FADEOUT_DURATION = 10f;

    private float activeTimeSoFar = 0f;
    private final IntervalUtil nebulaTrailTimer = new IntervalUtil(NEBULA_TRAIL_INTERVAL, NEBULA_TRAIL_INTERVAL);

    protected final Object STATUSKEY2 = new Object();
    protected final Object STATUSKEY3 = new Object();

    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }

    private float getBlastRadius(ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case FRIGATE: return 125f;
            case DESTROYER: return 175f;
            case CRUISER: default: return 250f;
        }
    }

    private float getSpeedSurge(ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case FRIGATE: return 200f;
            case DESTROYER: return 150f;
            case CRUISER: default: return 125f;
        }
    }

    protected float getDisruptionLevel(ShipAPI ship) {
        if (FLUX_LEVEL_AFFECTS_SPEED) {
            float threshold = ship.getMutableStats().getDynamic().getMod(
                    Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
            if (threshold <= 0) return 1f;
            float level = ship.getHardFluxLevel() / threshold;
            if (level > 1f) level = 1f;
            return level;
        }
        return 0f;
    }

    public float getSpeedMult(ShipAPI ship, float effectLevel) {
        if (getDisruptionLevel(ship) <= 0f) return 1f;
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
    }

    protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
        ShipSystemAPI cloak = playerShip.getSystem();
        if (cloak == null) return;

        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (getDisruptionLevel(playerShip) <= 0f) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                        cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
            } else {
                String speedPercentStr = (int) Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                        cloak.getSpecAPI().getIconSpriteName(),
                        "phase coil stress",
                        "top speed at " + speedPercentStr, true);
            }
        }
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        boolean player = ship == Global.getCombatEngine().getPlayerShip();


        id = id + "_" + ship.getId();


        String surgeKey = id + "_surge";
        String fluxKey  = id + "_flux";
        String timeKey  = id + "_time";
        String firedKey = id + "_fired";
        String enteredKey = id + "_entered";

        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        if (Global.getCombatEngine().isPaused()) return;

        ShipSystemAPI cloak = ship.getSystem();
        if (cloak == null) return;


        if (state == State.COOLDOWN || state == State.IDLE) {
            cleanup(stats, ship, surgeKey, fluxKey, timeKey, firedKey, enteredKey);
            return;
        }


        if (FLUX_LEVEL_AFFECTS_SPEED && (state == State.ACTIVE || state == State.OUT || state == State.IN)) {
            float mult = getSpeedMult(ship, effectLevel);
            if (mult < 1f) {
                stats.getMaxSpeed().modifyMult(fluxKey, mult);
            } else {
                stats.getMaxSpeed().unmodify(fluxKey);
            }
            if (cloak instanceof PhaseCloakSystemAPI) {
                ((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
            }
        }


        if (state == State.IN && ship.getCustomData().get(enteredKey) == null) {
            activeTimeSoFar = 0f;
            ship.setCustomData(enteredKey, Boolean.TRUE);


            WaveDistortion wave = new WaveDistortion(ship.getLocation(), ship.getVelocity());
            wave.setIntensity(25f);
            wave.setSize(ship.getCollisionRadius() * 2.5f);
            wave.fadeOutIntensity(0.75f);
            wave.setLifetime(0.75f);
            DistortionShader.addDistortion(wave);
        }


        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);

            activeTimeSoFar += Global.getCombatEngine().getElapsedInLastFrame();
            float decayMult = Math.max(0f, 1f - (activeTimeSoFar / BOOST_FADEOUT_DURATION));
            float surgeBonus = getSpeedSurge(ship.getHullSize()) * decayMult * getSpeedMult(ship, effectLevel);


            stats.getMaxSpeed().modifyFlat(surgeKey, surgeBonus);
        } else if (state == State.OUT) {
            ship.setPhased(effectLevel > 0.5f);
        }


        if (state == State.ACTIVE) {
            nebulaTrailTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (nebulaTrailTimer.intervalElapsed()) {
                Vector2f particleLoc = MathUtils.getRandomPointOnCircumference(
                        ship.getLocation(), ship.getCollisionRadius() * 0.4f);
                Vector2f particleVel = Vector2f.sub(particleLoc, ship.getLocation(), new Vector2f());
                particleVel.scale(0.1f);
                Vector2f.add(particleVel, ship.getVelocity(), particleVel);
                Global.getCombatEngine().addNebulaParticle(
                        particleLoc, particleVel, ship.getCollisionRadius() * 0.4f, 1.5f,
                        0.1f, 0.2f, 1.5f, NEBULA_TRAIL_COLOR, false);
            }
        }


        if (state == State.OUT && effectLevel < 0.2f && ship.getCustomData().get(firedKey) == null) {
            DamagingExplosionSpec spec = new DamagingExplosionSpec(
                    0.1f,
                    getBlastRadius(ship.getHullSize()),
                    getBlastRadius(ship.getHullSize()) * 0.5f,
                    0, 0,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f, 0f, 0f, 0,
                    new Color(0, 0, 0, 0),
                    null
            );
            spec.setDamageType(DamageType.ENERGY);
            spec.setUseDetailedExplosion(false);
            spec.setMaxEMPDamage(250f);
            spec.setMinEMPDamage(0);
            Global.getCombatEngine().spawnDamagingExplosion(spec, ship, ship.getLocation(), false);
            Global.getCombatEngine().spawnExplosion(
                    ship.getLocation(), ship.getVelocity(),
                    EXPLOSION_BASE_COLOR,
                    getBlastRadius(ship.getHullSize()),
                    0.4f);


            StandardLight light = new StandardLight();
            light.setLocation(ship.getLocation());
            light.setVelocity(ship.getVelocity());
            light.setColor(EXPLOSION_BASE_COLOR);
            light.setIntensity(1.0f);
            light.setSize(getBlastRadius(ship.getHullSize()) * 2f);
            light.fadeOut(0.6f);
            LightShader.addLight(light);

            WaveDistortion wave = new WaveDistortion(ship.getLocation(), ship.getVelocity());
            wave.setSize(getBlastRadius(ship.getHullSize()));
            wave.setIntensity(getBlastRadius(ship.getHullSize()) * 0.2f);
            wave.fadeInSize(0.8f);
            wave.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(wave);

            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"),
                    ship.getLocation(), ship.getVelocity(),
                    new Vector2f(getBlastRadius(ship.getHullSize()) * 0.2f, getBlastRadius(ship.getHullSize()) * 0.2f),
                    new Vector2f(getBlastRadius(ship.getHullSize()), getBlastRadius(ship.getHullSize())),
                    MathUtils.getRandomNumberInRange(0, 360), 10f,
                    SHOCKWAVE_FX_COLOR,
                    true, 0.1f, 0.6f, 0.6f
            );


            for (int i = 0; i < 6; i++) {
                Vector2f from = MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius());
                CombatEntityAPI target = findNearbyTarget(ship, from, 500f);
                Global.getCombatEngine().spawnEmpArc(
                        ship, from, ship, target,
                        DamageType.ENERGY, 200f, 300f, 500f,
                        null, 10f, SHOCKWAVE_COLOR, Color.WHITE
                );
            }


            ship.setJitterUnder(this, SHOCKWAVE_COLOR, 1f, 10, 5f, 10f);
            ship.setJitter(this, SHOCKWAVE_COLOR, 0.5f, 5, 0f, 5f);

            ship.setCustomData(firedKey, Boolean.TRUE);
        }



        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
        ship.setApplyExtraAlphaToEngines(true);

        float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(timeKey, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(timeKey, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(timeKey);
        }
    }

    private void cleanup(MutableShipStatsAPI stats, ShipAPI ship,
                         String surgeKey, String fluxKey, String timeKey,
                         String firedKey, String enteredKey) {

        stats.getMaxSpeed().unmodify(surgeKey);
        stats.getMaxSpeed().unmodify(fluxKey);
        stats.getTimeMult().unmodify(timeKey);
        Global.getCombatEngine().getTimeMult().unmodify(timeKey);


        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);
        ship.setApplyExtraAlphaToEngines(false);

        ShipSystemAPI cloak = ship.getSystem();
        if (cloak instanceof PhaseCloakSystemAPI) {
            ((PhaseCloakSystemAPI) cloak).setMinCoilJitterLevel(0f);
        }

        ship.getCustomData().remove(firedKey);
        ship.getCustomData().remove(enteredKey);
        activeTimeSoFar = 0f;
    }

    private CombatEntityAPI findNearbyTarget(ShipAPI ship, Vector2f from, float range) {
        CombatEngineAPI engine = Global.getCombatEngine();


        for (MissileAPI m : engine.getMissiles()) {
            if (m.getOwner() != ship.getOwner() && !m.isFading() &&
                    MathUtils.getDistance(from, m.getLocation()) <= range) {
                return m;
            }
        }


        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() != ship.getOwner() && other.isAlive() && !other.isHulk() &&
                    MathUtils.getDistance(from, other.getLocation()) <= range) {
                return other;
            }
        }


        SimpleEntity dummy = new SimpleEntity(
                MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius()));
        engine.addEntity(dummy);
        return dummy;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        id = id + "_" + ship.getId();

        String surgeKey = id + "_surge";
        String fluxKey  = id + "_flux";
        String timeKey  = id + "_time";
        String firedKey = id + "_fired";
        String enteredKey = id + "_entered";

        cleanup(stats, ship, surgeKey, fluxKey, timeKey, firedKey, enteredKey);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) { return null; }
}
