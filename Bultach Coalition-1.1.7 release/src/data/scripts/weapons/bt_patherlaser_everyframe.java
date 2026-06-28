package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class bt_patherlaser_everyframe implements EveryFrameWeaponEffectPlugin {

    private static final List<String> USED_IDS = new ArrayList<>();
    static {
        USED_IDS.add("SMOKE_ID");
    }

    private static final Map<String, Integer> ON_SHOT_PARTICLE_COUNT = new HashMap<>();
    static {
        ON_SHOT_PARTICLE_COUNT.put("default", 0);
    }
    private static final Map<String, Float> PARTICLES_PER_SECOND = new HashMap<>();
    static {
        PARTICLES_PER_SECOND.put("default", 130f);
        PARTICLES_PER_SECOND.put("SMOKE_ID", 15f);
    }
    private static final Map<String, Boolean> AFFECTED_BY_CHARGELEVEL = new HashMap<>();
    static {
        AFFECTED_BY_CHARGELEVEL.put("default", true);
    }
    private static final Map<String, String> PARTICLE_SPAWN_MOMENT = new HashMap<>();
    static {
        PARTICLE_SPAWN_MOMENT.put("default", "CHARGEUP-FIRING-CHARGEDOWN");
    }
    private static final Map<String, Boolean> SPAWN_POINT_ANCHOR_ALTERNATION = new HashMap<>();
    static {
        SPAWN_POINT_ANCHOR_ALTERNATION.put("default", true);
    }
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_TURRET = new HashMap<>();
    static {
        PARTICLE_SPAWN_POINT_TURRET.put("default", new Vector2f(0f, 0f));
    }
    private static final Map<String, Vector2f> PARTICLE_SPAWN_POINT_HARDPOINT = new HashMap<>();
    static {
        PARTICLE_SPAWN_POINT_HARDPOINT.put("default", new Vector2f(0f, 0f));
    }
    private static final Map<String, String> PARTICLE_TYPE = new HashMap<>();
    static {
        PARTICLE_TYPE.put("default", "SMOKE");
        PARTICLE_TYPE.put("SMOKE_ID", "SMOKE");
    }
    private static final Map<String, Color> PARTICLE_COLOR = new HashMap<>();
    static {
        PARTICLE_COLOR.put("default", new Color(255, 136, 136, 225));
        PARTICLE_COLOR.put("SMOKE_ID", new Color(100, 100, 100, 150));
    }
    private static final Map<String, Float> PARTICLE_SIZE_MIN = new HashMap<>();
    static {
        PARTICLE_SIZE_MIN.put("default", 14f);
        PARTICLE_SIZE_MIN.put("SMOKE_ID", 5f);
    }
    private static final Map<String, Float> PARTICLE_SIZE_MAX = new HashMap<>();
    static {
        PARTICLE_SIZE_MAX.put("default", 26f);
        PARTICLE_SIZE_MAX.put("SMOKE_ID", 10f);
    }
    private static final Map<String, Float> PARTICLE_VELOCITY_MIN = new HashMap<>();
    static {
        PARTICLE_VELOCITY_MIN.put("default", 0.1f);
    }
    private static final Map<String, Float> PARTICLE_VELOCITY_MAX = new HashMap<>();
    static {
        PARTICLE_VELOCITY_MAX.put("default", 60f);
    }
    private static final Map<String, Float> PARTICLE_DURATION_MIN = new HashMap<>();
    static {
        PARTICLE_DURATION_MIN.put("default", 0.45f);
        PARTICLE_DURATION_MIN.put("SMOKE_ID", 1.0f);
    }
    private static final Map<String, Float> PARTICLE_DURATION_MAX = new HashMap<>();
    static {
        PARTICLE_DURATION_MAX.put("default", 0.65f);
        PARTICLE_DURATION_MAX.put("SMOKE_ID", 2.5f);
    }
    private static final Map<String, Float> PARTICLE_OFFSET_MIN = new HashMap<>();
    static {
        PARTICLE_OFFSET_MIN.put("default", -4f);
    }
    private static final Map<String, Float> PARTICLE_OFFSET_MAX = new HashMap<>();
    static {
        PARTICLE_OFFSET_MAX.put("default", 4f);
    }
    private static final Map<String, Float> PARTICLE_ARC = new HashMap<>();
    static {
        PARTICLE_ARC.put("default", 360f);
        PARTICLE_ARC.put("SMOKE_ID", 20f);
    }
    private static final Map<String, Float> PARTICLE_ARC_FACING = new HashMap<>();
    static {
        PARTICLE_ARC_FACING.put("default", 0f);
    }
    private static final Map<String, Float> PARTICLE_SCREENSPACE_CULL_DISTANCE = new HashMap<>();
    static {
        PARTICLE_SCREENSPACE_CULL_DISTANCE.put("default", 450f);
    }

    private static final boolean S_LENS_FLARE_ENABLED = true;
    private static final Color S_LENS_FLARE_COLOR_CORE = new Color(255, 160, 160, 180);
    private static final Color S_LENS_FLARE_COLOR_FRINGE = new Color(255, 22, 84, 130);
    private static final float S_LENS_FLARE_SIZE = 10f;
    private static final float S_LENS_FLARE_BRIGHTNESS = 200f;
    private static final float S_LENS_FLARE_INTERVAL_MIN = 0.15f;
    private static final float S_LENS_FLARE_INTERVAL_MAX = 0.25f;
    private static final boolean S_LENS_FLARE_ONLY_WHILE_FIRING_FOR_INTERVAL = true;
    private static final boolean S_LENS_FLARE_SPAWN_ON_SHOT = true;

    private boolean hasFiredThisCharge = false;
    private int currentBarrel = 0;
    private boolean shouldOffsetBarrelExtra = false;
    private com.fs.starfarer.api.util.IntervalUtil s_flareInterval;

    public bt_patherlaser_everyframe() {
        s_flareInterval = new com.fs.starfarer.api.util.IntervalUtil(S_LENS_FLARE_INTERVAL_MIN, S_LENS_FLARE_INTERVAL_MAX);
    }

    private <T> T getter(Map<String, T> map, String id, T defaultValue) {
        if (map.containsKey(id)) {
            return map.get(id);
        }
        return map.getOrDefault("default", defaultValue);
    }

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || amount <= 0f) {return;}

        float chargeLevel = weapon.getChargeLevel();
        String sequenceState = "READY";
        if (chargeLevel > 0 && (!weapon.isBeam() || weapon.isFiring())) {
            if (chargeLevel >= 1f) {
                sequenceState = "FIRING";
            } else if (!hasFiredThisCharge) {
                sequenceState = "CHARGEUP";
            } else {
                sequenceState = "CHARGEDOWN";
            }
        } else if (weapon.getCooldownRemaining() > 0) {
            sequenceState = "COOLDOWN";
        }

        if (weapon.isBurstBeam() && sequenceState.contains("CHARGEDOWN")) {
            chargeLevel = Math.max(0f, Math.min(Math.abs(weapon.getCooldownRemaining()-weapon.getCooldown()) / weapon.getSpec().getDerivedStats().getBurstFireDuration(), 1f));
        }

        shouldOffsetBarrelExtra = sequenceState.contains("CHARGEDOWN") || sequenceState.contains("COOLDOWN");

        for (String ID : USED_IDS) {
            float screenspaceCullingDistance = getter(PARTICLE_SCREENSPACE_CULL_DISTANCE, ID, 450f);
            if (!engine.getViewport().isNearViewport(weapon.getLocation(), screenspaceCullingDistance)) {continue;}

            boolean p_affectedByChargeLevel = getter(AFFECTED_BY_CHARGELEVEL, ID, true);
            String p_particleSpawnMoment = getter(PARTICLE_SPAWN_MOMENT, ID, "CHARGEUP-FIRING-CHARGEDOWN");
            boolean p_spawnPointAnchorAlternation = getter(SPAWN_POINT_ANCHOR_ALTERNATION, ID, true);
            Vector2f p_baseSpawnPointOffset = getter(PARTICLE_SPAWN_POINT_TURRET, ID, new Vector2f(0f,0f));
            if (weapon.getSlot().isHardpoint()) {
                p_baseSpawnPointOffset = getter(PARTICLE_SPAWN_POINT_HARDPOINT, ID, new Vector2f(0f,0f));
            }
            String p_particleType = getter(PARTICLE_TYPE, ID, "SMOKE");
            Color p_particleColor = getter(PARTICLE_COLOR, ID, new Color(255, 136, 136, 225));
            float p_particleSizeMin = getter(PARTICLE_SIZE_MIN, ID, 14f);
            float p_particleSizeMax = getter(PARTICLE_SIZE_MAX, ID, 26f);
            float p_particleVelocityMin = getter(PARTICLE_VELOCITY_MIN, ID, 0.1f);
            float p_particleVelocityMax = getter(PARTICLE_VELOCITY_MAX, ID, 60f);
            float p_particleDurationMin = getter(PARTICLE_DURATION_MIN, ID, 0.45f);
            float p_particleDurationMax = getter(PARTICLE_DURATION_MAX, ID, 0.65f);
            float p_particleOffsetMin = getter(PARTICLE_OFFSET_MIN, ID, -4f);
            float p_particleOffsetMax = getter(PARTICLE_OFFSET_MAX, ID, 4f);
            float p_particleArc = getter(PARTICLE_ARC, ID, 360f);
            float p_particleArcFacing = getter(PARTICLE_ARC_FACING, ID, 0f);

            Vector2f effectiveMuzzleLocation = new Vector2f(p_baseSpawnPointOffset.y, p_baseSpawnPointOffset.x);
            float effectiveMuzzleAngle = p_particleArcFacing;
            int barrelIdx = currentBarrel;

            if (shouldOffsetBarrelExtra && barrelIdx > 0) {
                barrelIdx--;
            } else if (shouldOffsetBarrelExtra && barrelIdx <= 0) {
                int barrelCount = weapon.getSpec().getTurretAngleOffsets().size();
                if (weapon.getSlot().isHardpoint()) barrelCount = weapon.getSpec().getHardpointAngleOffsets().size();
                else if (weapon.getSlot().isHidden()) barrelCount = weapon.getSpec().getHiddenAngleOffsets().size();
                if (barrelCount > 0) barrelIdx = barrelCount - 1; else barrelIdx = 0;
            }

            if (p_spawnPointAnchorAlternation) {
                List<Vector2f> fireOffsets = weapon.getSpec().getTurretFireOffsets();
                List<Float> angleOffsets = weapon.getSpec().getTurretAngleOffsets();
                if (weapon.getSlot().isHardpoint()) {
                    fireOffsets = weapon.getSpec().getHardpointFireOffsets();
                    angleOffsets = weapon.getSpec().getHardpointAngleOffsets();
                } else if (weapon.getSlot().isHidden()) {
                    fireOffsets = weapon.getSpec().getHiddenFireOffsets();
                    angleOffsets = weapon.getSpec().getHiddenAngleOffsets();
                }

                if (fireOffsets != null && barrelIdx >= 0 && barrelIdx < fireOffsets.size()) {
                    effectiveMuzzleLocation.x += fireOffsets.get(barrelIdx).x;
                    effectiveMuzzleLocation.y += fireOffsets.get(barrelIdx).y;
                }
                if (angleOffsets != null && barrelIdx >= 0 && barrelIdx < angleOffsets.size()) {
                    effectiveMuzzleAngle += angleOffsets.get(barrelIdx);
                }
            }
            effectiveMuzzleAngle += weapon.getCurrAngle();
            effectiveMuzzleLocation = VectorUtils.rotate(effectiveMuzzleLocation, weapon.getCurrAngle(), new Vector2f(0f, 0f));
            effectiveMuzzleLocation.x += weapon.getLocation().x;
            effectiveMuzzleLocation.y += weapon.getLocation().y;

            if (chargeLevel >= 1f && !hasFiredThisCharge) {
                float p_onShotCount = getter(ON_SHOT_PARTICLE_COUNT, ID, 0);
                if (p_onShotCount > 0) {
                    spawnParticles(engine, weapon, p_onShotCount, p_particleType, effectiveMuzzleLocation, effectiveMuzzleAngle, p_particleColor, p_particleSizeMin, p_particleSizeMax, p_particleVelocityMin, p_particleVelocityMax,
                            p_particleDurationMin, p_particleDurationMax, p_particleOffsetMin, p_particleOffsetMax, p_particleArc);
                }
            }
            if (p_particleSpawnMoment.contains(sequenceState)) {
                float p_particlesThisFrame = getter(PARTICLES_PER_SECOND, ID, 0f);
                p_particlesThisFrame *= amount;
                if (p_affectedByChargeLevel && (sequenceState.contains("CHARGEUP") || sequenceState.contains("CHARGEDOWN"))) { p_particlesThisFrame *= chargeLevel; }
                if (p_affectedByChargeLevel && sequenceState.contains("COOLDOWN")) { p_particlesThisFrame *= (weapon.getCooldownRemaining()/weapon.getCooldown()); }

                if (p_particlesThisFrame > 0f) {
                    spawnParticles(engine, weapon, p_particlesThisFrame, p_particleType, effectiveMuzzleLocation, effectiveMuzzleAngle, p_particleColor, p_particleSizeMin, p_particleSizeMax,
                            p_particleVelocityMin, p_particleVelocityMax, p_particleDurationMin, p_particleDurationMax, p_particleOffsetMin, p_particleOffsetMax,
                            p_particleArc);
                }
            }
        }

        if (S_LENS_FLARE_ENABLED) {
            Vector2f flareSpawnLocation = new Vector2f();
            float baseWeaponAngle = weapon.getCurrAngle();

            List<Vector2f> fireOffsetsList = weapon.getSpec().getTurretFireOffsets();
            List<Float> angleOffsetsList = weapon.getSpec().getTurretAngleOffsets();

            if (weapon.getSlot().isHardpoint()) {
                fireOffsetsList = weapon.getSpec().getHardpointFireOffsets();
                angleOffsetsList = weapon.getSpec().getHardpointAngleOffsets();
            } else if (weapon.getSlot().isHidden()) {
                fireOffsetsList = weapon.getSpec().getHiddenFireOffsets();
                angleOffsetsList = weapon.getSpec().getHiddenAngleOffsets();
            }

            Vector2f barrelSpecificOffset = new Vector2f();
            float barrelSpecificAngleOffset = 0f;
            int actualBarrelIndex = currentBarrel;

            if (shouldOffsetBarrelExtra) {
                if (actualBarrelIndex > 0) {
                    actualBarrelIndex--;
                } else if (angleOffsetsList != null && !angleOffsetsList.isEmpty()) {
                    actualBarrelIndex = angleOffsetsList.size() - 1;
                } else {
                    actualBarrelIndex = 0;
                }
            }

            if (fireOffsetsList != null && actualBarrelIndex >= 0 && actualBarrelIndex < fireOffsetsList.size()) {
                barrelSpecificOffset.set(fireOffsetsList.get(actualBarrelIndex));
            }
            if (angleOffsetsList != null && actualBarrelIndex >= 0 && actualBarrelIndex < angleOffsetsList.size()) {
                barrelSpecificAngleOffset = angleOffsetsList.get(actualBarrelIndex);
            }

            flareSpawnLocation.set(barrelSpecificOffset);
            flareSpawnLocation = VectorUtils.rotate(flareSpawnLocation, baseWeaponAngle, new Vector2f(0f,0f));
            flareSpawnLocation.translate(weapon.getLocation().x, weapon.getLocation().y);

            float parallelFlareAngle = baseWeaponAngle + barrelSpecificAngleOffset;
            float perpendicularFlareAngle = parallelFlareAngle + 90f;

            if (S_LENS_FLARE_SPAWN_ON_SHOT) {
                if (chargeLevel >= 1f && !hasFiredThisCharge) {
                    MagicLensFlare.createSharpFlare(engine, weapon.getShip(), flareSpawnLocation, S_LENS_FLARE_SIZE, S_LENS_FLARE_BRIGHTNESS, perpendicularFlareAngle, S_LENS_FLARE_COLOR_CORE, S_LENS_FLARE_COLOR_FRINGE);
                }
            }

            if (S_LENS_FLARE_INTERVAL_MIN > 0f || S_LENS_FLARE_INTERVAL_MAX > 0f) {
                boolean canSpawnIntervalFlare = true;
                if (S_LENS_FLARE_ONLY_WHILE_FIRING_FOR_INTERVAL) {
                    canSpawnIntervalFlare = sequenceState.equals("CHARGEUP") || sequenceState.equals("FIRING") || sequenceState.equals("CHARGEDOWN");
                }
                if (canSpawnIntervalFlare) {
                    s_flareInterval.advance(amount);
                    if (s_flareInterval.intervalElapsed()) {
                        MagicLensFlare.createSharpFlare(engine, weapon.getShip(), flareSpawnLocation, S_LENS_FLARE_SIZE, S_LENS_FLARE_BRIGHTNESS, perpendicularFlareAngle, S_LENS_FLARE_COLOR_CORE, S_LENS_FLARE_COLOR_FRINGE);
                    }
                }
            }
        }

        if (chargeLevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true;
        }

        if (hasFiredThisCharge && (chargeLevel <= 0f || (!weapon.isBeam() && !weapon.isFiring()))) {
            hasFiredThisCharge = false;
            currentBarrel++;
            int barrelCount = 1;
            List<Float> relevantAngleOffsets = weapon.getSpec().getTurretAngleOffsets();
            if (weapon.getSlot().isHardpoint()) {
                relevantAngleOffsets = weapon.getSpec().getHardpointAngleOffsets();
            } else if (weapon.getSlot().isHidden()) {
                relevantAngleOffsets = weapon.getSpec().getHiddenAngleOffsets();
            }
            if (relevantAngleOffsets != null && !relevantAngleOffsets.isEmpty()) {
                barrelCount = relevantAngleOffsets.size();
            }
            if (currentBarrel >= barrelCount) {
                currentBarrel = 0;
            }
        }
    }

    private void spawnParticles (CombatEngineAPI engine, WeaponAPI weapon, float count, String type, Vector2f muzzleLocation, float muzzleFacing, Color color, float sizeMin, float sizeMax,
                                 float velocityMin, float velocityMax, float durationMin, float durationMax,
                                 float offsetMin, float offsetMax, float arcSpread) {
        float counter = count;
        while (Math.random() < counter) {
            counter--;
            float arcPoint = MathUtils.getRandomNumberInRange(muzzleFacing - (arcSpread / 2f), muzzleFacing + (arcSpread / 2f));
            Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(velocityMin, velocityMax), arcPoint);
            Vector2f spawnLocation = MathUtils.getPointOnCircumference(muzzleLocation, MathUtils.getRandomNumberInRange(offsetMin, offsetMax), arcPoint);
            float duration = MathUtils.getRandomNumberInRange(durationMin, durationMax);
            float size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax);

            switch (type) {
                case "SMOOTH":
                    engine.addSmoothParticle(spawnLocation, velocity, size, 1f, duration, color);
                    break;
                case "SMOKE":
                    engine.addNebulaParticle(spawnLocation,velocity,size,1.3f,0.1f,0.3f,duration,color);
                    break;
                default:
                    engine.addHitParticle(spawnLocation, velocity, size, 10f, duration, color);
                    break;
            }
        }
    }
}