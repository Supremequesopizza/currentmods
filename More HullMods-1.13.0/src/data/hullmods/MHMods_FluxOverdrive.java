package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;

public class MHMods_FluxOverdrive extends BaseHullMod {

	public final float BonusDMG = 30f;
	public final float FluxCost = 30f;

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return Math.round(BonusDMG) + "%";
		if (index == 1) return Math.round(FluxCost) + "%";
		return null;
    }

	public void advanceInCombat(ShipAPI ship, float amount){
		if (!ship.isAlive()) return;
		MutableShipStatsAPI stats = ship.getMutableStats();
		stats.getEnergyWeaponDamageMult().modifyMult(spec.getId(), 1 + BonusDMG * 0.01f * ship.getFluxLevel());
		stats.getBallisticWeaponDamageMult().modifyMult(spec.getId(), 1 + BonusDMG * 0.01f * ship.getFluxLevel());
		
		stats.getEnergyWeaponFluxCostMod().modifyMult(spec.getId(), 1 + FluxCost * 0.01f * ship.getFluxLevel());
		stats.getBallisticWeaponFluxCostMod().modifyMult(spec.getId(), 1 + FluxCost * 0.01f * ship.getFluxLevel());

		if (ship == Global.getCombatEngine().getPlayerShip())
			Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_FluxOverdrive", "graphics/icons/hullsys/ammo_feeder.png", "Overcharge boost", Math.round(BonusDMG * ship.getFluxLevel()) + "%", false);
	}
}