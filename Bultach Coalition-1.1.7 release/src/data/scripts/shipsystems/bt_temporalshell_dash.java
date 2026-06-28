package data.scripts.shipsystems;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.utils.bultach_utils;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class bt_temporalshell_dash extends BaseShipSystemScript {

    public static final Color JITTER_COLOR = new Color(255, 236, 171, 106);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 247, 196, 100);
    private static final Color AFTERIMAGE_COLOR = new Color(255, 249, 204, 52);
    public static final float AFTERIMAGE_DURATION = 1.22f;

    private final IntervalUtil arcTimer = new IntervalUtil(0.55f, 0.55f);
    private final IntervalUtil afterImageTimer = new IntervalUtil(0.45f, 0.45f);
    private final Color arcFringe = new Color(255, 228, 147, 100);
    private final Color arcCore = new Color(255, 234, 213, 160);

    private boolean HAS_FIRED_LIGHTNING = false;
    private boolean runOnce = false;
    private WaveDistortion wave;

    private static final Map<HullSize, Float> DISTORTION_RADIUS_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> DISTORTION_INTENSITY_MULT = new HashMap<>();
    private static final Map<HullSize, Float> LIGHTNING_COUNT_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> LIGHTNING_SIZE_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> LIGHTNING_THICKNESS_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> JITTER_INTENSITY_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> SOUND_PITCH_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> SOUND_VOLUME_SCALE = new HashMap<>();
    private static final Map<HullSize, Float> PARTICLE_SPRITE_SCALE = new HashMap<>();

    public static final float SHIP_TIME_MULT_FLAT = 30f;
    private static final Map<HullSize, Float> MAX_SPEED_BONUS_FLAT = new HashMap<>();
    private static final Map<HullSize, Float> DIRECTIONAL_SPEED_PENALTY_MAP = new HashMap<>();

    public static final float BACKWARD_MOVEMENT_THRESHOLD_DEGREES = 135f;


    static {
        DISTORTION_RADIUS_SCALE.put(HullSize.DEFAULT, 400f);
        DISTORTION_INTENSITY_MULT.put(HullSize.DEFAULT, 0.075f);
        LIGHTNING_COUNT_SCALE.put(HullSize.DEFAULT, 6.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.DEFAULT, 80.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.DEFAULT, 8f);
        JITTER_INTENSITY_SCALE.put(HullSize.DEFAULT, 1.0f);
        SOUND_PITCH_SCALE.put(HullSize.DEFAULT, 1.0f);
        SOUND_VOLUME_SCALE.put(HullSize.DEFAULT, 1.0f);
        PARTICLE_SPRITE_SCALE.put(HullSize.DEFAULT, 1.0f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.DEFAULT, 20f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.DEFAULT, 20f);

        DISTORTION_RADIUS_SCALE.put(HullSize.FIGHTER, 100f);
        DISTORTION_INTENSITY_MULT.put(HullSize.FIGHTER, 0.03f);
        LIGHTNING_COUNT_SCALE.put(HullSize.FIGHTER, 1.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.FIGHTER, 25.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.FIGHTER, 3f);
        JITTER_INTENSITY_SCALE.put(HullSize.FIGHTER, 0.3f);
        SOUND_PITCH_SCALE.put(HullSize.FIGHTER, 1.3f);
        SOUND_VOLUME_SCALE.put(HullSize.FIGHTER, 0.5f);
        PARTICLE_SPRITE_SCALE.put(HullSize.FIGHTER, 0.4f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.FIGHTER, -20f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.FIGHTER, 10f);

        DISTORTION_RADIUS_SCALE.put(HullSize.FRIGATE, 200f);
        DISTORTION_INTENSITY_MULT.put(HullSize.FRIGATE, 0.05f);
        LIGHTNING_COUNT_SCALE.put(HullSize.FRIGATE, 9.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.FRIGATE, 90.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.FRIGATE, 9f);
        JITTER_INTENSITY_SCALE.put(HullSize.FRIGATE, 0.5f);
        SOUND_PITCH_SCALE.put(HullSize.FRIGATE, 1.15f);
        SOUND_VOLUME_SCALE.put(HullSize.FRIGATE, 0.7f);
        PARTICLE_SPRITE_SCALE.put(HullSize.FRIGATE, 0.6f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.FRIGATE, -30f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.FRIGATE, 80f);

        DISTORTION_RADIUS_SCALE.put(HullSize.DESTROYER, 300f);
        DISTORTION_INTENSITY_MULT.put(HullSize.DESTROYER, 0.065f);
        LIGHTNING_COUNT_SCALE.put(HullSize.DESTROYER, 16.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.DESTROYER, 130.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.DESTROYER, 12f);
        JITTER_INTENSITY_SCALE.put(HullSize.DESTROYER, 0.8f);
        SOUND_PITCH_SCALE.put(HullSize.DESTROYER, 1.05f);
        SOUND_VOLUME_SCALE.put(HullSize.DESTROYER, 0.9f);
        PARTICLE_SPRITE_SCALE.put(HullSize.DESTROYER, 0.8f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.DESTROYER, -15f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.DESTROYER, 45f);

        DISTORTION_RADIUS_SCALE.put(HullSize.CRUISER, 400f);
        DISTORTION_INTENSITY_MULT.put(HullSize.CRUISER, 0.075f);
        LIGHTNING_COUNT_SCALE.put(HullSize.CRUISER, 28.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.CRUISER, 180.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.CRUISER, 15f);
        JITTER_INTENSITY_SCALE.put(HullSize.CRUISER, 1.0f);
        SOUND_PITCH_SCALE.put(HullSize.CRUISER, 1.0f);
        SOUND_VOLUME_SCALE.put(HullSize.CRUISER, 1.0f);
        PARTICLE_SPRITE_SCALE.put(HullSize.CRUISER, 1.0f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.CRUISER, 10f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.CRUISER, 40f);

        DISTORTION_RADIUS_SCALE.put(HullSize.CAPITAL_SHIP, 650f);
        DISTORTION_INTENSITY_MULT.put(HullSize.CAPITAL_SHIP, 0.09f);
        LIGHTNING_COUNT_SCALE.put(HullSize.CAPITAL_SHIP, 42.0f);
        LIGHTNING_SIZE_SCALE.put(HullSize.CAPITAL_SHIP, 240.0f);
        LIGHTNING_THICKNESS_SCALE.put(HullSize.CAPITAL_SHIP, 20f);
        JITTER_INTENSITY_SCALE.put(HullSize.CAPITAL_SHIP, 1.2f);
        SOUND_PITCH_SCALE.put(HullSize.CAPITAL_SHIP, 0.9f);
        SOUND_VOLUME_SCALE.put(HullSize.CAPITAL_SHIP, 1.2f);
        PARTICLE_SPRITE_SCALE.put(HullSize.CAPITAL_SHIP, 1.2f);
        MAX_SPEED_BONUS_FLAT.put(HullSize.CAPITAL_SHIP, 20f);
        DIRECTIONAL_SPEED_PENALTY_MAP.put(HullSize.CAPITAL_SHIP, 45f);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
        } else {
            return;
        }

        String id_suffix_time = "_time";
        String id_suffix_boost = "_boost";
        String id_suffix_penalty = "_penalty";

        String id_time = id + "_" + ship.getId() + id_suffix_time;
        String id_boost = id + "_" + ship.getId() + id_suffix_boost;
        String id_penalty = id + "_" + ship.getId() + id_suffix_penalty;

        HullSize hullSize = ship.getHullSize();

        float distortionRadius = DISTORTION_RADIUS_SCALE.getOrDefault(hullSize, 400f);
        float distortionIntensityMult = DISTORTION_INTENSITY_MULT.getOrDefault(hullSize, 0.075f);
        float particleScaleMult = PARTICLE_SPRITE_SCALE.getOrDefault(hullSize, 1.0f);
        float soundPitch = SOUND_PITCH_SCALE.getOrDefault(hullSize, 1.0f);
        float soundVolume = SOUND_VOLUME_SCALE.getOrDefault(hullSize, 1.0f);


        if (!runOnce) {
            runOnce = true;
            Vector2f loc = ship.getLocation();

            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"),
                    loc,
                    new Vector2f(),
                    new Vector2f(325 * particleScaleMult, 325 * particleScaleMult),
                    new Vector2f(185 * particleScaleMult, 185 * particleScaleMult),
                    360 * (float) Math.random(),
                    100 * particleScaleMult,
                    new Color(255, 228, 184, 121),
                    true,
                    0.07f,
                    0.0f,
                    0.55f
            );

            wave = new WaveDistortion();
            wave.setLocation(loc);
            wave.setSize(distortionRadius);
            wave.setIntensity(distortionRadius * distortionIntensityMult);
            wave.fadeInSize(1.2f);
            wave.fadeOutIntensity(0.9f);
            wave.setSize(distortionRadius * 0.25f);
            DistortionShader.addDistortion(wave);

            Global.getSoundPlayer().playSound("bt_gestalt_dash", soundPitch, soundVolume, ship.getLocation(), ship.getVelocity());
        }

        float lightningSize = LIGHTNING_SIZE_SCALE.getOrDefault(hullSize, 80f);
        float lightningCount = LIGHTNING_COUNT_SCALE.getOrDefault(hullSize, 6.0f);
        float lightningThickness = LIGHTNING_THICKNESS_SCALE.getOrDefault(hullSize, 8f);

        if (effectLevel >= 0.1f) {
            if (!HAS_FIRED_LIGHTNING) {
                HAS_FIRED_LIGHTNING = true;
                float tempCounter = 0;
                while (tempCounter <= lightningCount) {
                    Global.getCombatEngine().spawnEmpArc(ship,
                            new Vector2f(ship.getLocation().x + MathUtils.getRandomNumberInRange(-lightningSize, lightningSize), ship.getLocation().y + MathUtils.getRandomNumberInRange(-lightningSize, lightningSize)),
                            null, ship,
                            DamageType.ENERGY, 0f, 0f, 100000f, "bt_gestalt_arc",
                            lightningThickness, JITTER_COLOR, JITTER_UNDER_COLOR
                    );
                    tempCounter++;
                }
            }
        } else {
            HAS_FIRED_LIGHTNING = false;
        }

        float jitterLevel = effectLevel;
        float jitterRangeBonus = 0f;
        float maxRangeBonus = 15f;
        float jitterScale = JITTER_INTENSITY_SCALE.getOrDefault(hullSize, 1.0f);

        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;

            Global.getSoundPlayer().playSound("bt_gestaltdash_out", soundPitch, soundVolume, ship.getLocation(), ship.getVelocity());

            Vector2f loc = ship.getLocation();
            wave = new WaveDistortion();
            wave.setLocation(loc);
            wave.setSize(distortionRadius);
            wave.setIntensity(distortionRadius * distortionIntensityMult * 0.7f);
            wave.fadeInSize(0.6f);
            wave.fadeOutIntensity(0.7f);
            wave.setSize(distortionRadius * 0.1f);
            DistortionShader.addDistortion(wave);
        }

        float finalJitterLevel = (float) Math.sqrt(jitterLevel) * jitterScale;
        float finalJitterRangeBonus = jitterRangeBonus * jitterScale;

        ship.setJitter(this, JITTER_COLOR, finalJitterLevel, 1, 0, 0 + finalJitterRangeBonus);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, finalJitterLevel, 25, 0f, 7f + finalJitterRangeBonus);

        float currentEffectLevel = effectLevel * effectLevel;

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        arcTimer.advance(amount);
        if (arcTimer.intervalElapsed()){
            arcOnBounds(ship, Global.getCombatEngine());
        }

        afterImageTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (afterImageTimer.intervalElapsed()) {
            bultach_utils.afterimage(ship, AFTERIMAGE_COLOR, AFTERIMAGE_DURATION);
        }

        float shipTimeMult = 1f + (SHIP_TIME_MULT_FLAT - 1f) * currentEffectLevel;
        stats.getTimeMult().modifyMult(id_time, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id_time, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id_time);
        }

        float firingSpeedDebuff = 0.6f;
        stats.getBallisticRoFMult().modifyMult(id, firingSpeedDebuff);
        stats.getEnergyRoFMult().modifyMult(id, firingSpeedDebuff);
        stats.getMissileRoFMult().modifyMult(id, firingSpeedDebuff);

        float baseSpeedBonus = MAX_SPEED_BONUS_FLAT.getOrDefault(hullSize, 0f);
        float actualSpeedBonus = baseSpeedBonus * currentEffectLevel;
        stats.getMaxSpeed().modifyFlat(id_boost, actualSpeedBonus);

        stats.getMaxSpeed().unmodify(id_penalty);
        Vector2f shipVelocity = ship.getVelocity();
        if (shipVelocity.lengthSquared() > 0.001f) {
            float facing = ship.getFacing();
            float moveAngle = VectorUtils.getAngle(new Vector2f(0,0), shipVelocity);
            float angleDiff = MathUtils.getShortestRotation(facing, moveAngle);

            if (Math.abs(angleDiff) > BACKWARD_MOVEMENT_THRESHOLD_DEGREES) {
                float directionalPenaltyBase = DIRECTIONAL_SPEED_PENALTY_MAP.getOrDefault(hullSize, 0f);
                float directionalPenaltyAmount = directionalPenaltyBase * currentEffectLevel;
                stats.getMaxSpeed().modifyFlat(id_penalty, -directionalPenaltyAmount);
            }
        }

        ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0,0,0,0), effectLevel, 0.5f);
        ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        String id_suffix_time = "_time";
        String id_suffix_boost = "_boost";
        String id_suffix_penalty = "_penalty";

        String id_time = id + "_" + ship.getId() + id_suffix_time;
        String id_boost = id + "_" + ship.getId() + id_suffix_boost;
        String id_penalty = id + "_" + ship.getId() + id_suffix_penalty;

        runOnce = false;
        HAS_FIRED_LIGHTNING = false;

        Global.getCombatEngine().getTimeMult().unmodify(id_time);
        stats.getTimeMult().unmodify(id_time);
        stats.getMaxSpeed().unmodify(id_boost);
        stats.getMaxSpeed().unmodify(id_penalty);

        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);
    }

    public void arcOnBounds(ShipAPI target, CombatEngineAPI engine){
        target.getExactBounds().update(target.getLocation(), target.getFacing());
        List<BoundsAPI.SegmentAPI> Segments = target.getExactBounds().getSegments();
        if (Segments.isEmpty()) return;

        int firstBound = MathUtils.getRandomNumberInRange(0, Segments.size() - 1);
        int secondBound = firstBound + MathUtils.getRandomNumberInRange(2, 4);
        secondBound = secondBound % Segments.size();

        Vector2f firstBoundLoc = Segments.get(firstBound).getP1();
        Vector2f secondBoundLoc = Segments.get(secondBound).getP1();

        float thicknessScale = LIGHTNING_THICKNESS_SCALE.getOrDefault(target.getHullSize(), 8f) * 0.5f;

        engine.spawnEmpArcVisual(firstBoundLoc, target, secondBoundLoc, target, thicknessScale, arcFringe, arcCore);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            if (state == State.IN) {
                return new StatusData("Grasping threads", false);
            } else if (state == State.ACTIVE) {
                return new StatusData("Time is your birthright", false);
            } else if (state == State.OUT) {
                return new StatusData("Releasing threads", false);
            }
        }
        return null;
    }
}