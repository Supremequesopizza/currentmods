package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;

public class MHMods_FuelAdditive extends BaseLogisticsHullMod {

	public final float Fuel_Cons = 0.75f;
	public final float BURN_LEVEL_BONUS = 1f;
	public final float MAINTENANCE_MULT = 1.15f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);

		stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
		stats.getFuelUseMod().modifyMult(id, Fuel_Cons);

		if (!sMod) {
			stats.getSuppliesPerMonth().modifyMult(id, MAINTENANCE_MULT);
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
		if (index == 0) return Math.round((1f - Fuel_Cons) * 100f) + "%";
		if (index == 1) return String.valueOf(Math.round(BURN_LEVEL_BONUS));
		if (index == 2) return Math.round((MAINTENANCE_MULT - 1f) * 100f) + "%";
		return null;
	}
}