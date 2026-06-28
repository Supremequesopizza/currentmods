package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class MHMods_TorpedoSpec extends BaseHullMod {

    public final int SpeedBonus = 30;
    public final int MTurn = 25;
    public final int HullBonus = 15;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMissileMaxSpeedBonus().modifyPercent(id, SpeedBonus);
        stats.getMissileAccelerationBonus().modifyPercent(id, SpeedBonus);
        stats.getMissileMaxTurnRateBonus().modifyPercent(id, -MTurn);
        stats.getMissileTurnAccelerationBonus().modifyPercent(id, -MTurn);
        stats.getMissileHealthBonus().modifyPercent(id, HullBonus);
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return SpeedBonus + "%";
        if (index == 1) return HullBonus + "%";
        if (index == 2) return MTurn + "%";
        return null;
    }
}

			
			