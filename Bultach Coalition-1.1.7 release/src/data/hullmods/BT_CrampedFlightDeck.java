package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class BT_CrampedFlightDeck extends BaseHullMod {

	public static final float FIGHTER_REFIT_MULT = 20f;

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getFighterRefitTimeMult().modifyPercent(id,FIGHTER_REFIT_MULT);
	}

	//Built-in only
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return false;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)Math.round(FIGHTER_REFIT_MULT) + "%";
		return null;
	}
}
