package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class MHMods_WeaponInhibitor extends BaseHullMod {

	private final float ROF = 0.9f;
	private final float FluxUsage = 0.8f;
	private final float damage = 0.9f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().modifyMult(id, ROF);
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, FluxUsage);
		stats.getBallisticWeaponDamageMult().modifyMult(id, damage);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round((1f - FluxUsage) * 100) + "%";
        if (index == 1) return Math.round((1f - damage) * 100) + "%";
		if (index == 2) return Math.round((1f - ROF) * 100) + "%";
        return null;
    }
}