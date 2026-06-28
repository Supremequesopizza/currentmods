package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.hullmods.bt_MurtachTempBuffControllerHullmod;
import data.scripts.utils.bultach_utils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BrutePhaseStatsAlt extends BaseShipSystemScript {

    private static final Color AFTERIMAGE_COLOR = new Color(179, 59, 59, 60);
    private static final float AFTERIMAGE_EFFECT_DURATION = 0.4f;
    private static final Color EXPLOSION_BASE_COLOR = new Color(160, 55, 99, 197);
    private static final Color EMP_COLOR = new Color(198, 31, 204);
    private static final DamageType EXPLOSION_DAMAGE_TYPE = DamageType.ENERGY;

    private static final Color NEBULA_ENTRY_COLOR = new Color(178, 68, 143, 155);
    private static final Color NEBULA_TRAIL_COLOR = new Color(179, 59, 59, 40);
    private static final float NEBULA_TRAIL_SIZE_MIN = 45f;
    private static final float NEBULA_TRAIL_SIZE_MAX = 95f;
    private static final float NEBULA_TRAIL_DURATION = 2f;
    private static final float NEBULA_TRAIL_RAMP_UP_FRACTION = 0.1f;
    private static final float NEBULA_TRAIL_FULL_FRACTION = 0.2f;

    public static final float MAX_ACTIVE_TIME_FOR_SCALING = 5.5f;
    public static final float EXPLOSION_RADIUS_BASE_MULT = 0.8f;
    public static final float EXPLOSION_RADIUS_MAX_BONUS_MULT = 3.0f;
    public static final float EXPLOSION_DAMAGE_BASE_MULT = 0.5f;
    public static final float EXPLOSION_DAMAGE_MAX_BONUS_MULT = 3.0f;

    public static final float AFTERIMAGE_INTERVAL = 0.15f;
    public static final float NEBULA_TRAIL_INTERVAL = 0.05f;

    public static final float BACKWARD_MOVEMENT_THRESHOLD_DEGREES = 135f;
    public static final float DIRECTIONAL_SPEED_PENALTY_FLAT = 70f;

    private static final float INITIAL_SURGE_DURATION_SECONDS = 2.25f;
    private static final float INITIAL_SURGE_SPEED_BONUS_FLAT = 275f;
    private static final float ENTRY_PARTICLE_TRAIL_DURATION_SECONDS = 2.35f;
    private static final float CUPPING_TRAIL_START_DELAY = 0.45f;
    private static final float CUPPING_TRAIL_SPAWN_MIN_INTERVAL = 0.08f;
    private static final float CUPPING_TRAIL_SPAWN_MAX_INTERVAL = 0.12f;

    private static final float ENTRY_ARC_EFFECT_DURATION_SECONDS = 1.45f;
    private static final float ENTRY_ARC_SPAWN_INTERVAL_SECONDS = 0.2f;
    private static final float FORCED_OVERLOAD_DURATION_SECONDS = 4.5f;
    private static final float APPROACHING_CRITICAL_THRESHOLD_SECONDS = 1.5f;

    private static final Color EXIT_ARC_CORE_COLOR = new Color(225, 50, 123, 200);
    private static final Color EXIT_ARC_FRINGE_COLOR = new Color(180, 0, 81, 150);
    private static final float EXIT_ARC_THICKNESS = 12f;

    private static final float MAX_TIME_PENALTY_DAMAGE_MULT_ON_OVERLOAD = 0.3f;
    private static final float MAX_TIME_VISUAL_INTENSITY_PENALTY_MULT = 0.5f;

    private static final float CUTTING_IT_CLOSE_ROF_THRESHOLD = 0.65f;
    private static final float CUTTING_IT_CLOSE_FLUX_THRESHOLD = 0.85f;

    private static final String OVERLOAD_EXTRA_SOUND_ID = "bt_murtach_dive_fail";

    private IntervalUtil afterImageTimer;
    private IntervalUtil nebulaTrailTimer;
    private IntervalUtil entryArcSpawnTimer;
    private IntervalUtil cuppingTrailSpawnTimer;
    private float activeTimeSoFar = 0f;
    private boolean entryEffectsDone = false;
    private Vector2f systemActivationLocation = null;
    private boolean isMaxTimePenaltyApplicable = false;

    private boolean rofThresholdCuePlayed = false;
    private boolean fluxThresholdCuePlayed = false;

    private static final float REACTION_LEAD_TIME = 0.3f;

    private String ID_TIME_MULT = "_time_mult";
    private String ID_MAX_SPEED = "_max_speed";
    private String ID_ACCEL = "_accel";
    private String ID_DECEL = "_decel";
    private String ID_MAX_TURN_RATE = "_max_turn_rate";
    private String ID_BACKWARD_PENALTY = "_backward_penalty";
    private String ID_INITIAL_SURGE_SPEED = "_initial_surge_speed";
    private String ID_CLOSE_CALL_ROF_BUFF = "_close_call_rof_buff";
    private String ID_CLOSE_CALL_FLUX_BUFF = "_close_call_flux_buff";

    public BrutePhaseStatsAlt() {
        afterImageTimer = new IntervalUtil(AFTERIMAGE_INTERVAL, AFTERIMAGE_INTERVAL);
        nebulaTrailTimer = new IntervalUtil(NEBULA_TRAIL_INTERVAL, NEBULA_TRAIL_INTERVAL);
        entryArcSpawnTimer = new IntervalUtil(ENTRY_ARC_SPAWN_INTERVAL_SECONDS, ENTRY_ARC_SPAWN_INTERVAL_SECONDS);
        cuppingTrailSpawnTimer = new IntervalUtil(CUPPING_TRAIL_SPAWN_MIN_INTERVAL, CUPPING_TRAIL_SPAWN_MAX_INTERVAL);
    }

    public static float getMaxTimeMult(final MutableShipStatsAPI stats) {
        return 1.0f + 4.0f * stats.getDynamic().getValue("phase_time_mult", 0.5f);
    }

    @Override
    public void apply(final MutableShipStatsAPI stats, String id, final State state, final float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) {
            return;
        }

        final String systemIdBase = id + "_" + ship.getId();

        if (state == State.IN) {
            activeTimeSoFar = 0f;
            entryEffectsDone = false;
            isMaxTimePenaltyApplicable = false;
            systemActivationLocation = new Vector2f(ship.getLocation());

            rofThresholdCuePlayed = false;
            fluxThresholdCuePlayed = false;

            ship.setPhased(true);
            ship.setExtraAlphaMult(1.0f - 0.25f * effectLevel);
            ship.setApplyExtraAlphaToEngines(true);
        } else if (state == State.ACTIVE) {
            activeTimeSoFar += engine.getElapsedInLastFrame();
            ship.setPhased(true);
            ship.setExtraAlphaMult(1.0f - 0.25f * effectLevel);

            float rofTriggerTime = CUTTING_IT_CLOSE_ROF_THRESHOLD * MAX_ACTIVE_TIME_FOR_SCALING - REACTION_LEAD_TIME;
            float fluxTriggerTime = CUTTING_IT_CLOSE_FLUX_THRESHOLD * MAX_ACTIVE_TIME_FOR_SCALING - REACTION_LEAD_TIME;

            if (!rofThresholdCuePlayed && activeTimeSoFar >= rofTriggerTime && activeTimeSoFar < MAX_ACTIVE_TIME_FOR_SCALING) {
                Global.getSoundPlayer().playSound("bt_murtach_threshold_cue", 1.0f, 1.5f, ship.getLocation(), ship.getVelocity());
                rofThresholdCuePlayed = true;
            }

            if (!fluxThresholdCuePlayed && activeTimeSoFar >= fluxTriggerTime && activeTimeSoFar < MAX_ACTIVE_TIME_FOR_SCALING) {
                Global.getSoundPlayer().playSound("bt_murtach_threshold_cue", 1.4f, 1.8f, ship.getLocation(), ship.getVelocity());
                fluxThresholdCuePlayed = true;
            }

            if (activeTimeSoFar < INITIAL_SURGE_DURATION_SECONDS) {
                float surgeProgress = activeTimeSoFar / INITIAL_SURGE_DURATION_SECONDS;
                float currentBonus = INITIAL_SURGE_SPEED_BONUS_FLAT * (1f - surgeProgress);
                stats.getMaxSpeed().modifyFlat(systemIdBase + ID_INITIAL_SURGE_SPEED, currentBonus);
            } else {
                stats.getMaxSpeed().unmodify(systemIdBase + ID_INITIAL_SURGE_SPEED);
            }
            if (!entryEffectsDone) {
                Vector2f shipLoc = ship.getLocation();
                float cloudRadius = ship.getCollisionRadius();
                int numParticles = 125;
                float initialCloudParticleDuration = MathUtils.getRandomNumberInRange(2.5f, 3.5f);
                float initialCloudFullFraction = MathUtils.getRandomNumberInRange(0.4f, 0.6f);
                for (int i = 0; i < numParticles; i++) {
                    float particleSpeed = MathUtils.getRandomNumberInRange(65f, 145f);
                    float angle = (float) Math.random() * 360f;
                    Vector2f particleVel = MathUtils.getPointOnCircumference(null, particleSpeed, angle);
                    float initialSize = MathUtils.getRandomNumberInRange(50f, 110f);
                    float endSizeMult = MathUtils.getRandomNumberInRange(2.0f, 4.0f);
                    engine.addNebulaParticle(new Vector2f(shipLoc), particleVel, initialSize, endSizeMult, 0.1f, initialCloudFullFraction, initialCloudParticleDuration, NEBULA_ENTRY_COLOR, false);
                }
                for (int i = 0; i < 10; i++) {
                    Vector2f pointInCloud = MathUtils.getRandomPointInCircle(shipLoc, cloudRadius * 0.4f);
                    Vector2f pointInCloud2 = MathUtils.getRandomPointInCircle(shipLoc, cloudRadius * 0.6f);
                    engine.spawnEmpArcVisual(pointInCloud, null, pointInCloud2, null, MathUtils.getRandomNumberInRange(5f, 10f), EMP_COLOR, Color.WHITE);
                }
                entryEffectsDone = true;
            }
            if (activeTimeSoFar > CUPPING_TRAIL_START_DELAY && activeTimeSoFar < ENTRY_PARTICLE_TRAIL_DURATION_SECONDS) {
                cuppingTrailSpawnTimer.advance(engine.getElapsedInLastFrame());
                if (cuppingTrailSpawnTimer.intervalElapsed()) {
                    ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
                    List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();
                    if (!segments.isEmpty() && systemActivationLocation != null) {
                        List<Vector2f> allBoundPoints = new ArrayList<>();
                        for (BoundsAPI.SegmentAPI seg : segments) {
                            allBoundPoints.add(new Vector2f(seg.getP1()));
                        }
                        final Vector2f finalSystemActivationLocation = systemActivationLocation;
                        Collections.sort(allBoundPoints, new Comparator<Vector2f>() {
                            @Override
                            public int compare(Vector2f p1, Vector2f p2) {
                                return Float.compare(MathUtils.getDistanceSquared(p1, finalSystemActivationLocation), MathUtils.getDistanceSquared(p2, finalSystemActivationLocation));
                            }
                        });
                        float actualSpawningDuration = ENTRY_PARTICLE_TRAIL_DURATION_SECONDS - CUPPING_TRAIL_START_DELAY;
                        float timeSinceTrailStart = activeTimeSoFar - CUPPING_TRAIL_START_DELAY;
                        float trailProgress = Math.min(1f, Math.max(0f, timeSinceTrailStart / actualSpawningDuration));
                        float opacityMultiplier = Math.max(0.05f, (1f - trailProgress) * (1f - trailProgress));
                        Color cuppingParticleColor = new Color(NEBULA_ENTRY_COLOR.getRed(), NEBULA_ENTRY_COLOR.getGreen(), NEBULA_ENTRY_COLOR.getBlue(), (int) (NEBULA_ENTRY_COLOR.getAlpha() * opacityMultiplier));
                        int pointsToSpawnFrom = Math.min(allBoundPoints.size(), 9);
                        for (int k = 0; k < pointsToSpawnFrom; k++) {
                            Vector2f spawnPoint = allBoundPoints.get(k);
                            Vector2f particleVel = MathUtils.getRandomPointInCircle(null, MathUtils.getRandomNumberInRange(10f, 25f));
                            Vector2f.add(particleVel, (Vector2f) new Vector2f(ship.getVelocity()).scale(0.05f), particleVel);
                            engine.addNebulaParticle(new Vector2f(spawnPoint), particleVel, MathUtils.getRandomNumberInRange(NEBULA_TRAIL_SIZE_MIN * 0.6f, NEBULA_TRAIL_SIZE_MAX * 0.85f), 1.7f, 0.05f, 0.3f, MathUtils.getRandomNumberInRange(1.9f, 2.75f), cuppingParticleColor, false);
                        }
                    }
                }
            }
            entryArcSpawnTimer.advance(engine.getElapsedInLastFrame());
            if (activeTimeSoFar < ENTRY_ARC_EFFECT_DURATION_SECONDS && entryArcSpawnTimer.intervalElapsed() && systemActivationLocation != null) {
                ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
                List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();
                if (!segments.isEmpty()) {
                    for (int i = 0; i < 2; i++) {
                        Vector2f arcSource = segments.get(MathUtils.getRandomNumberInRange(0, segments.size() - 1)).getP1();
                        Vector2f entryAreaArcPoint = MathUtils.getRandomPointInCircle(systemActivationLocation, ship.getCollisionRadius() * 0.75f);
                        engine.spawnEmpArcVisual(arcSource, ship, entryAreaArcPoint, null, MathUtils.getRandomNumberInRange(6f, 10f), EMP_COLOR, Color.LIGHT_GRAY);
                    }
                }

            }
            if (activeTimeSoFar >= MAX_ACTIVE_TIME_FOR_SCALING) {
                isMaxTimePenaltyApplicable = true;
            }
            afterImageTimer.advance(engine.getElapsedInLastFrame());
            if (afterImageTimer.intervalElapsed()) {
                bultach_utils.afterimage(ship, AFTERIMAGE_COLOR, AFTERIMAGE_EFFECT_DURATION);
            }
            nebulaTrailTimer.advance(engine.getElapsedInLastFrame());
            if (nebulaTrailTimer.intervalElapsed()) {
                Vector2f particleLoc = MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.5f);
                Vector2f particleVel = Vector2f.sub(particleLoc, ship.getLocation(), new Vector2f());
                particleVel.scale(0.1f);
                Vector2f.add(particleVel, ship.getVelocity(), particleVel);
                engine.addNebulaParticle(particleLoc, particleVel, MathUtils.getRandomNumberInRange(NEBULA_TRAIL_SIZE_MIN, NEBULA_TRAIL_SIZE_MAX), 1.5f, NEBULA_TRAIL_RAMP_UP_FRACTION, NEBULA_TRAIL_FULL_FRACTION, NEBULA_TRAIL_DURATION, NEBULA_TRAIL_COLOR, false);
            }
        } else if (state == State.OUT) {
            ship.setPhased(false);
            ship.setExtraAlphaMult(1.0f);

            float scaleFactor = Math.min(1f, activeTimeSoFar / MAX_ACTIVE_TIME_FOR_SCALING);
            float radiusMult = EXPLOSION_RADIUS_BASE_MULT + (EXPLOSION_RADIUS_MAX_BONUS_MULT - EXPLOSION_RADIUS_BASE_MULT) * scaleFactor;
            float damageMult = EXPLOSION_DAMAGE_BASE_MULT + (EXPLOSION_DAMAGE_MAX_BONUS_MULT - EXPLOSION_DAMAGE_BASE_MULT) * scaleFactor;
            float visualIntensityFactor = 1f;

            if (isMaxTimePenaltyApplicable) {
                Global.getSoundPlayer().playSound("bt_exit_phase", 0.7f, 0.7f, ship.getLocation(), ship.getVelocity());
                Global.getSoundPlayer().playSound(OVERLOAD_EXTRA_SOUND_ID, 1.0f, 0.9f, ship.getLocation(), ship.getVelocity());
                ship.getFluxTracker().beginOverloadWithTotalBaseDuration(FORCED_OVERLOAD_DURATION_SECONDS);
                damageMult *= MAX_TIME_PENALTY_DAMAGE_MULT_ON_OVERLOAD;
                visualIntensityFactor = MAX_TIME_VISUAL_INTENSITY_PENALTY_MULT;
            } else {
                Global.getSoundPlayer().playSound("bt_exit_phase", 1.0f, 1.0f, ship.getLocation(), ship.getVelocity());
                float chargeRatio = activeTimeSoFar / MAX_ACTIVE_TIME_FOR_SCALING;
                boolean rofBuffTriggered = chargeRatio >= CUTTING_IT_CLOSE_ROF_THRESHOLD && chargeRatio < 1.0f;
                boolean fluxBuffTriggered = chargeRatio >= CUTTING_IT_CLOSE_FLUX_THRESHOLD && chargeRatio < 1.0f;

                if (rofBuffTriggered || fluxBuffTriggered) {
                    Map<String, Object> customData = ship.getCustomData();
                    customData.put(bt_MurtachTempBuffControllerHullmod.HULLMOD_ACTIVATION_SIGNAL, true);
                    customData.put("SYSTEM_ID", systemIdBase);

                    float visualScaleForHullmod = 1f;
                    if (rofBuffTriggered) {
                        customData.put("ROF_ID", systemIdBase + ID_CLOSE_CALL_ROF_BUFF);
                    }
                    if (fluxBuffTriggered) {
                        customData.put("FLUX_ID", systemIdBase + ID_CLOSE_CALL_FLUX_BUFF);
                    }
                    if (rofBuffTriggered && fluxBuffTriggered) {
                        visualScaleForHullmod = 1.5f;
                    }
                    customData.put("VISUAL_SCALE", visualScaleForHullmod);

                    if (!ship.getVariant().hasHullMod(bt_MurtachTempBuffControllerHullmod.HULLMOD_ID)) {
                        Global.getLogger(BrutePhaseStatsAlt.class).error("Ship " + ship.getHullSpec().getHullName() + " is missing required hullmod: " + bt_MurtachTempBuffControllerHullmod.HULLMOD_ID);
                    }
                }
            }

            float currentExplosionRadius = ship.getCollisionRadius() * 2f * radiusMult;
            float maxDamage = 1250f * damageMult;

            DamagingExplosionSpec spec = new DamagingExplosionSpec(
                    0.1f,
                    currentExplosionRadius,
                    currentExplosionRadius * 0.6f,
                    maxDamage,
                    maxDamage * 0.35f,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    0f,
                    0f,
                    0,
                    new Color(0,0,0,0),
                    null
            );
            spec.setDamageType(EXPLOSION_DAMAGE_TYPE);
            spec.setUseDetailedExplosion(false);
            spec.setSoundSetId(null);
            engine.spawnDamagingExplosion(spec, ship, ship.getLocation(), false);

            Color currentExplosionColor = new Color(EXPLOSION_BASE_COLOR.getRed(), EXPLOSION_BASE_COLOR.getGreen(), EXPLOSION_BASE_COLOR.getBlue(), (int) (EXPLOSION_BASE_COLOR.getAlpha() * visualIntensityFactor));
            engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), currentExplosionColor, currentExplosionRadius, (0.3f * radiusMult) * visualIntensityFactor);
            Vector2f loc = ship.getLocation();

            StandardLight light = new StandardLight();
            light.setLocation(loc);
            light.setVelocity(ship.getVelocity());
            Color lightColor = new Color(EXPLOSION_BASE_COLOR.getRed(), EXPLOSION_BASE_COLOR.getGreen(), EXPLOSION_BASE_COLOR.getBlue(), (int)(EXPLOSION_BASE_COLOR.getAlpha() * visualIntensityFactor * 0.8f));
            light.setColor(lightColor);
            light.setIntensity(0.35f * damageMult * visualIntensityFactor);
            light.setSize(950.0f * radiusMult);
            light.fadeOut(1.0f);
            LightShader.addLight(light);

            WaveDistortion wave = new WaveDistortion(loc, ship.getVelocity());
            wave.setSize(262.5f * radiusMult);
            wave.setIntensity(85.0f * radiusMult * visualIntensityFactor);
            wave.fadeInSize(1.2f);
            wave.fadeOutIntensity(0.9f);
            DistortionShader.addDistortion(wave);
            Color shockwaveColor = new Color(EXPLOSION_BASE_COLOR.getRed(), EXPLOSION_BASE_COLOR.getGreen(), EXPLOSION_BASE_COLOR.getBlue(), (int) (EXPLOSION_BASE_COLOR.getAlpha() * visualIntensityFactor));
            MagicRender.battlespace(Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"), loc, ship.getVelocity(), new Vector2f(20 * radiusMult, 20 * radiusMult), new Vector2f(925 * radiusMult, 925 * radiusMult), MathUtils.getRandomNumberInRange(0, 360), 100f * radiusMult, shockwaveColor, true, 0.0f, 0.15f, 0.75f);

            List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(loc, currentExplosionRadius);
            Color currentExitArcCoreColor = new Color(EXIT_ARC_CORE_COLOR.getRed(), EXIT_ARC_CORE_COLOR.getGreen(), EXIT_ARC_CORE_COLOR.getBlue(), (int) (EXIT_ARC_CORE_COLOR.getAlpha() * visualIntensityFactor));
            Color currentExitArcFringeColor = new Color(EXIT_ARC_FRINGE_COLOR.getRed(), EXIT_ARC_FRINGE_COLOR.getGreen(), EXIT_ARC_FRINGE_COLOR.getBlue(), (int) (EXIT_ARC_FRINGE_COLOR.getAlpha() * visualIntensityFactor));
            ship.getExactBounds().update(loc, ship.getFacing());
            List<BoundsAPI.SegmentAPI> shipSegments = ship.getExactBounds().getSegments();
            if (!shipSegments.isEmpty()) {
                int hostileTargetCount = 0;
                for (CombatEntityAPI tmp : entities) {
                    if (tmp == ship || tmp.getOwner() == ship.getOwner() || MathUtils.getDistance(ship, tmp) > currentExplosionRadius) continue;
                    Vector2f shipBoundPoint = shipSegments.get(MathUtils.getRandomNumberInRange(0, shipSegments.size() - 1)).getP1();
                    engine.spawnEmpArcVisual(shipBoundPoint, ship, tmp.getLocation(), tmp, EXIT_ARC_THICKNESS, currentExitArcCoreColor, currentExitArcFringeColor);
                    if (tmp.getOwner() != ship.getOwner()) hostileTargetCount++;
                }
                int decorativeArcCount = Math.max(0, 3 - hostileTargetCount / 2);
                for (int k = 0; k < decorativeArcCount; k++) {
                    Vector2f shipBoundPoint = shipSegments.get(MathUtils.getRandomNumberInRange(0, shipSegments.size() - 1)).getP1();
                    Vector2f randomBlastEdgePoint = MathUtils.getRandomPointOnCircumference(loc, currentExplosionRadius * MathUtils.getRandomNumberInRange(0.7f, 1f));
                    engine.spawnEmpArcVisual(shipBoundPoint, ship, randomBlastEdgePoint, null, EXIT_ARC_THICKNESS * 0.7f, currentExitArcCoreColor, currentExitArcFringeColor);
                }
            }
            for (CombatEntityAPI tmp : entities) {
                if (tmp.getOwner() == ship.getOwner() || MathUtils.getDistance(ship, tmp) > currentExplosionRadius) continue;
                if (tmp instanceof ShipAPI) {
                    ShipAPI victim = (ShipAPI) tmp;
                    if (victim == ship) continue;
                    if (victim.getShield() != null && victim.getShield().isOn() && victim.getShield().isWithinArc(loc)) {
                    } else {
                        for (int x = 0; x < 5; ++x) {
                            engine.spawnEmpArc(ship, MathUtils.getRandomPointInCircle(victim.getLocation(), victim.getCollisionRadius()), victim, victim, EXPLOSION_DAMAGE_TYPE, maxDamage / 30f, maxDamage / 15f, 1000f, null, 3.0f, EMP_COLOR, EMP_COLOR);
                        }
                    }
                }
            }
            activeTimeSoFar = 0f;
            entryEffectsDone = false;
        }

        if (state == State.IN || state == State.ACTIVE) {
            final float shipTimeMult = 1.0f + (getMaxTimeMult(stats) - 1.0f) * effectLevel;
            stats.getTimeMult().modifyMult(systemIdBase + ID_TIME_MULT, shipTimeMult);
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().getTimeMult().modifyMult(systemIdBase + ID_TIME_MULT, 1.0f / shipTimeMult);
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(systemIdBase + ID_TIME_MULT);
            }
            stats.getMaxSpeed().modifyPercent(systemIdBase + ID_MAX_SPEED, 200.0f * effectLevel);
            stats.getAcceleration().modifyFlat(systemIdBase + ID_ACCEL, 1500.0f * effectLevel);
            stats.getDeceleration().modifyPercent(systemIdBase + ID_DECEL, 1000.0f * effectLevel);
            stats.getMaxTurnRate().modifyPercent(systemIdBase + ID_MAX_TURN_RATE, 265f * effectLevel);

            stats.getMaxSpeed().unmodify(systemIdBase + ID_BACKWARD_PENALTY);
            Vector2f shipVelocity = ship.getVelocity();
            if (shipVelocity.lengthSquared() > 0.001f) {
                float angleDiff = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(new Vector2f(0, 0), shipVelocity));
                if (Math.abs(angleDiff) > BACKWARD_MOVEMENT_THRESHOLD_DEGREES) {
                    stats.getMaxSpeed().modifyFlat(systemIdBase + ID_BACKWARD_PENALTY, -DIRECTIONAL_SPEED_PENALTY_FLAT * effectLevel);
                }
            }
        } else if (state == State.OUT) {
            stats.getTimeMult().unmodify(systemIdBase + ID_TIME_MULT);
            Global.getCombatEngine().getTimeMult().unmodify(systemIdBase + ID_TIME_MULT);
            stats.getMaxSpeed().unmodify(systemIdBase + ID_MAX_SPEED);
            stats.getAcceleration().unmodify(systemIdBase + ID_ACCEL);
            stats.getDeceleration().unmodify(systemIdBase + ID_DECEL);
            stats.getMaxTurnRate().unmodify(systemIdBase + ID_MAX_TURN_RATE);
            stats.getMaxSpeed().unmodify(systemIdBase + ID_BACKWARD_PENALTY);
            stats.getMaxSpeed().unmodify(systemIdBase + ID_INITIAL_SURGE_SPEED);
        }


        if (state == State.ACTIVE) {
            ship.getCustomData().put(data.scripts.shipsystems.ai.bt_murtach_bioAI.ACTIVE_TIME_KEY, activeTimeSoFar);
        } else {
            ship.getCustomData().remove(data.scripts.shipsystems.ai.bt_murtach_bioAI.ACTIVE_TIME_KEY);
        }

        ship.getCustomData().put(data.scripts.shipsystems.ai.bt_murtach_bioAI.SHIP_TARGET_KEY, ship.getShipTarget());

    }

    @Override
    public void unapply(final MutableShipStatsAPI stats, final String id) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        final String systemIdBase = id + "_" + ship.getId();

        stats.getTimeMult().unmodify(systemIdBase + ID_TIME_MULT);
        Global.getCombatEngine().getTimeMult().unmodify(systemIdBase + ID_TIME_MULT);
        stats.getMaxSpeed().unmodify(systemIdBase + ID_MAX_SPEED);
        stats.getAcceleration().unmodify(systemIdBase + ID_ACCEL);
        stats.getDeceleration().unmodify(systemIdBase + ID_DECEL);
        stats.getMaxTurnRate().unmodify(systemIdBase + ID_MAX_TURN_RATE);
        stats.getMaxSpeed().unmodify(systemIdBase + ID_BACKWARD_PENALTY);
        stats.getMaxSpeed().unmodify(systemIdBase + ID_INITIAL_SURGE_SPEED);

        activeTimeSoFar = 0f;
        entryEffectsDone = false;
        isMaxTimePenaltyApplicable = false;
        systemActivationLocation = null;

        ship.setPhased(false);
        ship.setExtraAlphaMult(1.0f);
    }

    @Override
    public StatusData getStatusData(final int index, final State state, final float effectLevel) {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        if (playerShip == null) return null;

        Map<String, Object> customData = playerShip.getCustomData();
        int currentIndex = index;

        if (currentIndex == 0) {
            if (isMaxTimePenaltyApplicable && (state == State.ACTIVE || state == State.OUT || state == State.COOLDOWN)) {
                return new StatusData("SYSTEM OVERLOAD IMMINENT", true);
            }
            float scaleFactor = Math.min(1f, activeTimeSoFar / MAX_ACTIVE_TIME_FOR_SCALING);
            float currentChargeDisplayVal = EXPLOSION_DAMAGE_BASE_MULT + (EXPLOSION_DAMAGE_MAX_BONUS_MULT - EXPLOSION_DAMAGE_BASE_MULT) * scaleFactor;
            return new StatusData("Aggregated Entropy: " + Math.round(currentChargeDisplayVal * 100f) + "%", false);
        }
        currentIndex--;

        if (currentIndex == 0) {
            if (!isMaxTimePenaltyApplicable && (state == State.ACTIVE) && (activeTimeSoFar >= (MAX_ACTIVE_TIME_FOR_SCALING - APPROACHING_CRITICAL_THRESHOLD_SECONDS)) && activeTimeSoFar < MAX_ACTIVE_TIME_FOR_SCALING) {
                return new StatusData("APPROACHING CRITICAL CHARGE", true);
            }
            return null;
        }
        currentIndex--;

        if (customData.containsKey(bt_MurtachTempBuffControllerHullmod.UI_SIGNAL_ROF_ACTIVE)) {
            if (currentIndex == 0) {
                float rofMult = (Float) customData.get(bt_MurtachTempBuffControllerHullmod.UI_SIGNAL_ROF_ACTIVE);
                return new StatusData(String.format("Entropy Skimmed: +%d%% ROF", (int) ((rofMult - 1f) * 100f)), false);
            }
            currentIndex--;
        }

        if (customData.containsKey(bt_MurtachTempBuffControllerHullmod.UI_SIGNAL_ARMOR_CAP_REDUCED)) {
            if (currentIndex == 0) {
                float reduction = (Float) customData.get(bt_MurtachTempBuffControllerHullmod.UI_SIGNAL_ARMOR_CAP_REDUCED);
                return new StatusData(String.format("Temporal Deception, Synth-armor capacity reclaimed: +%d%%", (int) (reduction * 100f)), false);
            }
            currentIndex--;
        }

        return null;
    }
}
