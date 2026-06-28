package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_safetyoverrides_toggle extends BaseShipSystemScript {

    private static final float BALLISTIC_ROF_MULT = 1.5f;
    private static final float ENERGY_DAMAGE_MULT = 1.25f;
    private static final float FLUX_DISSIPATION_MULT = 2f;
    private static final float REVERSE_SPEED_PENALTY = 0.5f;
    private static final float REVERSE_ANGLE_THRESHOLD = 120f;

    private static final float RANGE_THRESHOLD = 450f;
    private static final float RANGE_MULT = 0.25f;

    private static final float RAMPING_CR_DRAIN_BASE = 0.4f;
    private static final float CR_DRAIN_CAP = 5f;

    private static final Map<HullSize, Float> SPEED_BONUS = new HashMap<>();
    static {
        SPEED_BONUS.put(HullSize.FRIGATE, 50f);
        SPEED_BONUS.put(HullSize.DESTROYER, 30f);
        SPEED_BONUS.put(HullSize.CRUISER, 20f);
        SPEED_BONUS.put(HullSize.CAPITAL_SHIP, 35f);
    }

    private final Color color = new Color(255, 100, 255, 255);
    private float timeActive = 0f;
    private String uniqueID_CR;
    private String uniqueID_Reverse;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        uniqueID_CR = id + "_cr_drain";
        uniqueID_Reverse = id + "_reverse_penalty";

        float speedBonus = SPEED_BONUS.getOrDefault(ship.getHullSize(), 10f);
        stats.getMaxSpeed().modifyFlat(id, speedBonus * effectLevel);
        stats.getAcceleration().modifyFlat(id, speedBonus * 2f * effectLevel);
        stats.getDeceleration().modifyFlat(id, speedBonus * 2f * effectLevel);

        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
        stats.getFluxDissipation().modifyMult(id, FLUX_DISSIPATION_MULT);
        stats.getBallisticRoFMult().modifyMult(id, BALLISTIC_ROF_MULT);
        stats.getEnergyWeaponDamageMult().modifyMult(id, ENERGY_DAMAGE_MULT);
        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);

        ship.getEngineController().fadeToOtherColor(this, color, null, effectLevel, 0.4f);
        ship.getEngineController().extendFlame(this, 0.25f * effectLevel, 0.25f * effectLevel, 0.25f * effectLevel);

        if (state == State.ACTIVE) {
            float amount = com.fs.starfarer.api.Global.getCombatEngine().getElapsedInLastFrame();
            timeActive += amount;
            float currentCRDrain = Math.min(RAMPING_CR_DRAIN_BASE * timeActive, CR_DRAIN_CAP);
            stats.getCRLossPerSecondPercent().modifyFlat(uniqueID_CR, currentCRDrain);

            float facing = ship.getFacing();
            float moveAngle = VectorUtils.getAngle(new Vector2f(0, 0), ship.getVelocity());
            float angleDiff = MathUtils.getShortestRotation(facing, moveAngle);

            if (Math.abs(angleDiff) > REVERSE_ANGLE_THRESHOLD && ship.getVelocity().length() > 5) {
                stats.getMaxSpeed().modifyMult(uniqueID_Reverse, REVERSE_SPEED_PENALTY);
            } else {
                stats.getMaxSpeed().unmodify(uniqueID_Reverse);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        timeActive = 0f;

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getZeroFluxMinimumFluxLevel().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getWeaponRangeThreshold().unmodify(id);
        stats.getWeaponRangeMultPastThreshold().unmodify(id);

        stats.getCRLossPerSecondPercent().unmodify(uniqueID_CR);
        stats.getMaxSpeed().unmodify(uniqueID_Reverse);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        float currentCRDrain = Math.min(RAMPING_CR_DRAIN_BASE * timeActive, CR_DRAIN_CAP);

        if (index == 0) {
            return new StatusData("ballistic rate of fire +" + (int) ((BALLISTIC_ROF_MULT - 1f) * 100) + "%", false);
        }
        if (index == 1) {
            return new StatusData("energy weapon damage +" + (int) ((ENERGY_DAMAGE_MULT - 1f) * 100) + "%", false);
        }
        if (index == 2 && timeActive > 0.1f) {
            return new StatusData("CR Drain: x" + String.format("%.1f", currentCRDrain), true);
        }
        return null;
    }
}