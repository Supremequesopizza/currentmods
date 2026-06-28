package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class bt_ramDrive extends BaseShipSystemScript {

    private static final float BASE_EXPLOSION_SIZE = 450f;
    private static final float BASE_EXPLOSION_DURATION = 2f;
    private static final float BASE_SOUND_PITCH = 1.0f;
    private static final float BASE_SOUND_VOLUME = 3.0f;
    private static final float BASE_SPEED_BOOST = 750f;
    private static final float BASE_MASS_SCALAR = 1.25f;
    private static final float BASE_TURN_BOOST_MULT = 1.5f;

    private static final Map<ShipAPI.HullSize, Float> EXPLOSION_SIZE_MAP = new HashMap<>();
    static {
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_EXPLOSION_SIZE * 0.4f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_EXPLOSION_SIZE * 0.6f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.CRUISER, BASE_EXPLOSION_SIZE * 0.8f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_EXPLOSION_SIZE);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_EXPLOSION_SIZE * 0.7f);
    }

    private static final Map<ShipAPI.HullSize, Float> SOUND_PITCH_MAP = new HashMap<>();
    static {
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SOUND_PITCH * 1.15f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SOUND_PITCH * 1.05f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SOUND_PITCH * 0.95f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SOUND_PITCH);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SOUND_PITCH);
    }

    private static final Map<ShipAPI.HullSize, Float> SOUND_VOLUME_MAP = new HashMap<>();
    static {
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SOUND_VOLUME * 0.6f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SOUND_VOLUME * 0.7f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SOUND_VOLUME * 0.8f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SOUND_VOLUME);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SOUND_VOLUME * 0.8f);
    }

    private static final Map<ShipAPI.HullSize, Float> SPEED_BOOST_MAP = new HashMap<>();
    static {
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SPEED_BOOST * 1.5f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SPEED_BOOST * 1.4f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SPEED_BOOST * 1.3f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SPEED_BOOST);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SPEED_BOOST * 1.15f);
    }

    private static final Map<ShipAPI.HullSize, Float> MASS_SCALAR_MAP = new HashMap<>();
    static {
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_MASS_SCALAR * 2f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_MASS_SCALAR * 1.6f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.CRUISER, BASE_MASS_SCALAR * 1.35f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_MASS_SCALAR);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_MASS_SCALAR * 1.2f);
    }

    private static final Map<ShipAPI.HullSize, Float> TURN_BOOST_MULT_MAP = new HashMap<>();
    static {
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_TURN_BOOST_MULT * 1.25f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_TURN_BOOST_MULT * 1.2f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.CRUISER, BASE_TURN_BOOST_MULT * 1.15f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_TURN_BOOST_MULT);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_TURN_BOOST_MULT * 1.2f);
    }

    public static final float RESIST_MULT = 0.5f;
    public static final float TURN_REDUCTION = 0.1f;
    public static final float SELF_DAMAGE = 0f;
    public static final float CHARGEUP_MAX_SPEED_MULT = 0.2f;
    public static final float CHARGEUP_ACCEL_MULT = 0.1f;

    public static final String RAM_DRIVE_CHARGE_SOUND_ID = "bt_rammingdrive_startup";
    public static final String RAM_DRIVE_ACTIVE_SOUND_ID = "bt_rammingdrive_blast";

    private float currentMassScalar = BASE_MASS_SCALAR;

    public static Vector2f calculateHeader(float facing) {
        double angle = Math.toRadians(facing);
        Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
        if (dir.lengthSquared() > 0f) dir.normalise();
        return dir;
    }

    private boolean primed = false;
    private boolean upMassed = false;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        ShipAPI.HullSize hullSize = ship.getHullSize();

        float explosionSize = EXPLOSION_SIZE_MAP.getOrDefault(hullSize, EXPLOSION_SIZE_MAP.get(ShipAPI.HullSize.DEFAULT));
        float soundPitch = SOUND_PITCH_MAP.getOrDefault(hullSize, SOUND_PITCH_MAP.get(ShipAPI.HullSize.DEFAULT));
        float soundVolume = SOUND_VOLUME_MAP.getOrDefault(hullSize, SOUND_VOLUME_MAP.get(ShipAPI.HullSize.DEFAULT));
        float speedBoost = SPEED_BOOST_MAP.getOrDefault(hullSize, SPEED_BOOST_MAP.get(ShipAPI.HullSize.DEFAULT));
        currentMassScalar = MASS_SCALAR_MAP.getOrDefault(hullSize, MASS_SCALAR_MAP.get(ShipAPI.HullSize.DEFAULT));
        float turnBoostMult = TURN_BOOST_MULT_MAP.getOrDefault(hullSize, TURN_BOOST_MULT_MAP.get(ShipAPI.HullSize.DEFAULT));

        Color explosionColor = new Color(255, 163, 135, 255);
        Color jitterColor = new Color(255, 84, 84, 64);

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getDeceleration().unmodify(id);
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue())
                ship.getVelocity().scale(0.95f);

        } else if (state == State.ACTIVE) {
            if (primed) {
                primed = false;

                stats.getMaxSpeed().unmodify(id);
                stats.getAcceleration().unmodify(id);
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);

                Vector2f header = calculateHeader(ship.getFacing());
                ship.getVelocity().set(header);
                ship.getVelocity().scale(speedBoost);

                float baseMaxSpeed = stats.getMaxSpeed().getBaseValue();
                stats.getMaxSpeed().modifyFlat(id, speedBoost - baseMaxSpeed);

                Vector2f behindShip = new Vector2f(header);
                behindShip.scale(-180f);
                Vector2f explosionLocation = Vector2f.add(ship.getLocation(), behindShip, new Vector2f());
                behindShip.normalise().scale(165f);
                Vector2f damageLocation = Vector2f.add(ship.getLocation(), behindShip, new Vector2f());

                Global.getCombatEngine().spawnExplosion(explosionLocation, new Vector2f(), explosionColor, explosionSize, BASE_EXPLOSION_DURATION);

                ship.setMass(ship.getMass() * currentMassScalar);
                for (ShipAPI s : ship.getChildModulesCopy()) {
                    if (s.isAlive()) s.setMass(s.getMass() * currentMassScalar);
                }

                Global.getSoundPlayer().playSound(RAM_DRIVE_ACTIVE_SOUND_ID, soundPitch, soundVolume, explosionLocation, ship.getVelocity());

                stats.getEngineDamageTakenMult().modifyMult(id, 0f);
                stats.getArmorDamageTakenMult().unmodify(id);
                stats.getHullDamageTakenMult().unmodify(id);
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (module.isAlive()) {
                        module.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                        module.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    }
                }

                Global.getCombatEngine().applyDamage(ship, damageLocation, SELF_DAMAGE, DamageType.HIGH_EXPLOSIVE, 0f, true, false, null);

                stats.getEngineDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (module.isAlive()) {
                        module.getMutableStats().getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                        module.getMutableStats().getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                    }
                }
                upMassed = true;
            }

            stats.getDeceleration().modifyMult(id, 0f);
            stats.getMaxTurnRate().modifyMult(id, TURN_REDUCTION);
            stats.getTurnAcceleration().modifyMult(id, TURN_REDUCTION);
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);

        } else if (state == State.IN) {
            if (!primed) {
                Global.getSoundPlayer().playSound(RAM_DRIVE_CHARGE_SOUND_ID, soundPitch * 0.9f, soundVolume * 0.8f, ship.getLocation(), ship.getVelocity());
            }
            primed = true;

            stats.getMaxSpeed().modifyMult(id, CHARGEUP_MAX_SPEED_MULT);
            stats.getAcceleration().modifyMult(id, CHARGEUP_ACCEL_MULT);

            stats.getMaxTurnRate().modifyMult(id, turnBoostMult);
            stats.getTurnAcceleration().modifyMult(id, turnBoostMult * 2f);

            stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
            stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
            for (ShipAPI module : ship.getChildModulesCopy()) {
                if (module.isAlive()) {
                    module.getMutableStats().getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                    module.getMutableStats().getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                }
            }

            ship.setJitter(id, jitterColor, effectLevel, 5, 50f * effectLevel);
            for (ShipAPI s : ship.getChildModulesCopy()) {
                if (s.isAlive()) s.setJitter(id + s.getId(), jitterColor, effectLevel, 5, 50f * effectLevel);
            }
        }

        if (state != State.IN) {
            stats.getMissileMaxSpeedBonus().modifyFlat(id, ship.getVelocity().length());
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);

        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        for (ShipAPI module : ship.getChildModulesCopy()) {
            module.getMutableStats().getArmorDamageTakenMult().unmodify(id);
            module.getMutableStats().getHullDamageTakenMult().unmodify(id);
        }

        stats.getMissileMaxSpeedBonus().unmodify(id);

        if (upMassed) {
            float massUnscalar = 1f / currentMassScalar;
            ship.setMass(ship.getMass() * massUnscalar);
            for (ShipAPI s : ship.getChildModulesCopy()) {
                if (s.isAlive() && s.getShipTarget() == null && s.getParentStation() == ship) {
                    s.setMass(s.getMass() * massUnscalar);
                }
            }
            upMassed = false;
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("" + (int)(100f - (RESIST_MULT * 100f)) + "% damage reduction", false);
        }
        if (index == 1 && state != State.IN) {
            float actualBonusPercent = (currentMassScalar - 1f) * 100f;
            return new StatusData("MASS BONUS: " + (int)actualBonusPercent + "%", false);
        }
        if (index == 2 && state != State.IN) {
            return new StatusData("RAMMING SPEED", false);
        }
        if (index == 3 && state == State.ACTIVE) {
            return new StatusData("turning heavily reduced", true);
        }
        return null;
    }
}