package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class supportships_combatvent_stats extends BaseShipSystemScript {

	public static final float FLUX_REDUCTION = 50f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponFluxCostMod().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("10% total flux vent per second, flux use -" + (int) FLUX_REDUCTION + "%", false);
		}
		if (index == 1) {
			return new StatusData("Engines forced on", false);
		}
		return null;
	}
}