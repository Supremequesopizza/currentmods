package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

import static data.scripts.utils.bultach_utils.lerp;


public class bultach_orbbeam implements EveryFrameWeaponEffectPlugin, BeamEffectPlugin {

    static final Color TRANSITION_PARTICLE_COLOR = new Color(255, 241, 223, 255);
    static final Color DISTORTION_FLASH_COLOR = new Color(255, 250, 244, 157);
    static final float TIER_TIME = 0.65f;
    static final float T1_WIDTH = 5f;
    static final float T2_WIDTH = 10f;
    static final float T3_WIDTH = 15f;
    static final float T2_DAMAGE_MULT = 1.5f;
    static final float T3_DAMAGE_MULT = 2.0f;
    static final IntervalUtil t2_arc_interval = new IntervalUtil(0.25f, 0.33f);
    static final IntervalUtil t3_arc_interval = new IntervalUtil(0.15f, 0.25f);
    static final float ARC_DAMAGE = 100f;
    static final float ARC_EMP = 200f;

    private final IntervalUtil particle_interval = new IntervalUtil(0.05f, 0.1f);
    private boolean did_t2 = false;
    private boolean did_t3 = false;
    private boolean wasZero = false;

    float time_firing = 0f;

    public static final String RIFT_HULL_ID_FOR_BEAM_EFFECT = "ork_vortex_drone";


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null || engine.isPaused() || weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        boolean isRiftEntity = ship.getHullSpec().getHullId().equals(RIFT_HULL_ID_FOR_BEAM_EFFECT);

        if (isRiftEntity) {
            ship.getVelocity().set(0, 0);
            ship.setAngularVelocity(0);

            if (weapon.isFiring()) {
                time_firing += amount;
                if (ship.getShipTarget() == null || !ship.getShipTarget().isAlive() || ship.getShipTarget().isPhased()) {
                    ShipAPI newTarget = findClosestEnemy(ship, weapon, weapon.getRange());
                    if (newTarget != null) {
                        ship.setShipTarget(newTarget);
                    }
                }
            } else {
                time_firing = 0f;
                did_t2 = false;
                did_t3 = false;
            }
            applyTierVisualAndWidthEffects(engine, weapon, ship, time_firing);
        } else {
            if (!ship.hasListenerOfClass(bultach_orbbeam_listener.class)) {
                ship.addListener(new bultach_orbbeam_listener());
            }
            WaveDistortion wave = new WaveDistortion();
            wave.setLocation(weapon.getLocation());
            wave.setIntensity(10f * weapon.getChargeLevel() + 5f);
            wave.setSize(10f * weapon.getChargeLevel() + 10f);
            wave.setLifetime(0.03f);
            DistortionShader.addDistortion(wave);

            if (weapon.isFiring()) {
                time_firing += amount;
                particle_interval.advance(amount);
                if (particle_interval.intervalElapsed()) {
                    addRadialParticles(engine, weapon.getLocation(), ship.getVelocity(), DISTORTION_FLASH_COLOR, 50, 300, 0.2f, 10f, 2);
                }
            } else {
                did_t2 = false;
                did_t3 = false;
                time_firing = 0f;
            }
            applyTierVisualAndWidthEffects(engine, weapon, ship, time_firing);
        }
    }

    private void applyTierVisualAndWidthEffects(CombatEngineAPI engine, WeaponAPI weapon, ShipAPI ship, float current_time_firing) {
        if (current_time_firing > TIER_TIME && !did_t2) {
            did_t2 = true;
            addRadialParticles(engine, weapon.getLocation(), ship.getVelocity(), TRANSITION_PARTICLE_COLOR, 50, 300, 0.5f, 15f, 30);
        } else if (current_time_firing > 2 * TIER_TIME && !did_t3) {
            did_t3 = true;
            addRadialParticles(engine, weapon.getLocation(), ship.getVelocity(), TRANSITION_PARTICLE_COLOR, 100, 500, 0.5f, 20f, 30);
        }

        if (weapon.getChargeLevel() > 0 || weapon.isFiring()) {
            if (weapon.getBeams() == null || weapon.getBeams().isEmpty()) return;

            BeamAPI beam = weapon.getBeams().get(0);
            if (current_time_firing < TIER_TIME - 0.25f) {
                beam.setWidth(T1_WIDTH);
            } else if (current_time_firing >= (TIER_TIME - 0.25f) && current_time_firing < (TIER_TIME + 0.25f)) {
                beam.setWidth(lerp(T1_WIDTH, T2_WIDTH, (current_time_firing - (TIER_TIME - 0.25f)) / 0.5f));
            } else if (current_time_firing >= (TIER_TIME + 0.25f) && current_time_firing < (TIER_TIME * 2f - 0.25f)) {
                beam.setWidth(T2_WIDTH);
            } else if (current_time_firing >= (TIER_TIME * 2f - 0.25f) && current_time_firing < (TIER_TIME * 2f + 0.25f)) {
                beam.setWidth(lerp(T2_WIDTH, T3_WIDTH, (current_time_firing - (TIER_TIME * 2f - 0.25f)) / 0.5f));
            } else if (current_time_firing >= (TIER_TIME * 2f + 0.25f)) {
                beam.setWidth(T3_WIDTH);
            }
        } else {
            if (weapon.getBeams() != null && !weapon.getBeams().isEmpty()) {
                weapon.getBeams().get(0).setWidth(T1_WIDTH);
            }
        }
    }

    private ShipAPI findClosestEnemy(ShipAPI riftShip, WeaponAPI weapon, float range) {
        ShipAPI closest = null;
        float minDistanceSq = range * range;

        List<ShipAPI> allShips = AIUtils.getEnemiesOnMap(riftShip);
        for (ShipAPI potentialTarget : allShips) {
            if (potentialTarget == null || !potentialTarget.isAlive() || potentialTarget.isPhased() || potentialTarget.isFighter() || potentialTarget.isDrone()) {
                continue;
            }
            float distSq = MathUtils.getDistanceSquared(weapon.getLocation(), potentialTarget.getLocation());
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                closest = potentialTarget;
            }
        }
        return closest;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f && beam.getWeapon() != null) {
            ShipAPI sourceShip = beam.getWeapon().getShip();
            boolean isRiftEntity = sourceShip.getHullSpec().getHullId().equals(RIFT_HULL_ID_FOR_BEAM_EFFECT);

            float dur = beam.getDamage().getDpsDuration();
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;

            IntervalUtil fireInterval;
            if (beam.getWidth() >= T3_WIDTH - 1f) {
                fireInterval = t3_arc_interval;
            } else if (beam.getWidth() >= T2_WIDTH - 1f) {
                fireInterval = t2_arc_interval;
            } else {
                return;
            }

            fireInterval.advance(dur);
            if (fireInterval.intervalElapsed()) {
                ShipAPI shipHitByBeam = (ShipAPI) target;
                boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
                float pierceChance = shipHitByBeam.getHardFluxLevel() - 0.1f;
                pierceChance *= shipHitByBeam.getMutableStats().getDynamic().getValue(com.fs.starfarer.api.impl.campaign.ids.Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = hitShield && (float) Math.random() < pierceChance;

                if (!hitShield || piercedShield) {
                    Vector2f point = beam.getRayEndPrevFrame();
                    engine.spawnEmpArcPierceShields(
                            beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                            DamageType.ENERGY,
                            ARC_DAMAGE,
                            ARC_EMP,
                            100000f,
                            "tachyon_lance_emp_impact",
                            beam.getWidth() + 5f,
                            beam.getFringeColor(),
                            beam.getCoreColor()
                    );
                }
            }
        }
    }

    private static class bultach_orbbeam_listener implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param instanceof BeamAPI && ((BeamAPI) param).getWeapon() != null) {
                BeamAPI beam = (BeamAPI) param;
                WeaponAPI wep = ((BeamAPI) param).getWeapon();
                if (wep.getSpec().getWeaponId().equals("bultach_orbbeam") ||
                        wep.getSpec().getWeaponId().equals("bultach_orbbeam") ||
                        (wep.getShip() != null && wep.getShip().getHullSpec().getHullId().equals(RIFT_HULL_ID_FOR_BEAM_EFFECT) && wep.getSlot().getId().equals("WS0000")))
                {
                    if (beam.getWidth() > T2_WIDTH - 1f && beam.getWidth() < T3_WIDTH -1f) {
                        damage.getModifier().modifyMult("orbbeam_tier2", T2_DAMAGE_MULT);
                        return "orbbeam_tier2";
                    } else if (beam.getWidth() >= T3_WIDTH - 1f) {
                        damage.getModifier().modifyMult("orbbeam_tier3", T3_DAMAGE_MULT);
                        return "orbbeam_tier3";
                    }
                }
            }
            return null;
        }
    }

    private static void addRadialParticles(CombatEngineAPI engine, Vector2f loc, Vector2f baseVel, Color color, float minvel, float maxvel, float dur, float size, int num) {
        for (int i = 0; i < num; i++) {
            Vector2f vel = MathUtils.getRandomPointOnCircumference(baseVel, lerp(minvel, maxvel, Misc.random.nextFloat()));
            engine.addHitParticle(
                    loc,
                    vel,
                    size,
                    1f,
                    dur,
                    color
            );
        }
    }
}