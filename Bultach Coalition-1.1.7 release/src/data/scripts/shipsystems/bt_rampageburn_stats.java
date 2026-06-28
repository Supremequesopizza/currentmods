package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class bt_rampageburn_stats extends BaseShipSystemScript {

    private static final float BURN_DRIVE_SPEED_BONUS = 200f;
    private static final float BURN_DRIVE_ACCELERATION_BONUS = 200f;

    private static final float AMMO_FEED_ROF_BONUS = 1f;
    private static final float AMMO_FEED_FLUX_REDUCTION = 50f;

    private static final float DAMAGE_REDUCTION_PERCENT = 25f; // Adjust this value as needed

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        // Apply Burn Drive effects
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, BURN_DRIVE_SPEED_BONUS * effectLevel);
            stats.getAcceleration().modifyFlat(id, BURN_DRIVE_ACCELERATION_BONUS * effectLevel);
        }

        // Apply Ammo Feed effects
        float rofMult = 1f + AMMO_FEED_ROF_BONUS * effectLevel;
        stats.getBallisticRoFMult().modifyMult(id, rofMult);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (AMMO_FEED_FLUX_REDUCTION * 0.01f));

        // Apply Damage Reduction
        float damageReduction = DAMAGE_REDUCTION_PERCENT * 0.01f * effectLevel;
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - damageReduction);
        stats.getHullDamageTakenMult().modifyMult(id, 1f - damageReduction);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        // Clean up Burn Drive effects
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);

        // Clean up Ammo Feed effects
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);

        // Clean up Damage Reduction effects
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
    }

    @Override
    public ShipSystemStatsScript.StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new ShipSystemStatsScript.StatusData("Increased engine power", false);
        } else if (index == 1) {
            float rofBonusPercent = (int) ((AMMO_FEED_ROF_BONUS * effectLevel) * 100f);
            return new ShipSystemStatsScript.StatusData("Ballistic rate of fire +" + (int) rofBonusPercent + "%", false);
        } else if (index == 2) {
            return new ShipSystemStatsScript.StatusData("Ballistic flux use -" + (int) AMMO_FEED_FLUX_REDUCTION + "%", false);
        } else if (index == 3) {
            return new ShipSystemStatsScript.StatusData("Incoming damage reduction +" + (int) DAMAGE_REDUCTION_PERCENT + "%", false);
        }
        return null;
    }
}
