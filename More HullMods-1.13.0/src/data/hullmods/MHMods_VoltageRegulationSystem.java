package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class MHMods_VoltageRegulationSystem extends BaseHullMod {

    public final float RangeBonus = 0.9f;
    public final float FluxCost = 0.8f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBeamWeaponFluxCostMult().modifyMult(id , FluxCost);
		stats.getBeamWeaponRangeBonus().modifyMult(id, RangeBonus);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round((1f - FluxCost) * 100) + "%";
        if (index == 1) return Math.round((1f - RangeBonus) * 100) + "%";
        return null;
    }
}