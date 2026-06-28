package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.HashSet;
import java.util.Set;

public class exalted_subsystems extends BaseHullMod {

	public static final float DEGRADE_INCREASE_PERCENT = 45f;
	public static final float PROFILE_MULT = 0.5f;
	public static final float SHIELD_BONUS_TURN = 30f;
	public static final float SHIELD_BONUS_UNFOLD = 50f;
	private static final Set<String> BLOCKED_HULLMODS = new HashSet(7);

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getCRLossPerSecondPercent().modifyPercent(id, DEGRADE_INCREASE_PERCENT);
		stats.getSensorProfile().modifyMult(id, PROFILE_MULT);
		stats.getShieldTurnRateMult().modifyPercent(id, SHIELD_BONUS_TURN);
		stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_BONUS_UNFOLD);
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp))
				ship.getVariant().removeMod(tmp);
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) DEGRADE_INCREASE_PERCENT + "%";
		if (index == 1) return "" + (int) ((1f - PROFILE_MULT) * 100f) + "%";
		if (index == 2) return "" + (int) SHIELD_BONUS_TURN + "%";
		if (index == 3) return "" + (int) SHIELD_BONUS_UNFOLD + "%";
		return null;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && (ship.getHullSpec().getNoCRLossTime() < 10000 || ship.getHullSpec().getCRLossPerSecond() > 0);
	}

	static {
		BLOCKED_HULLMODS.add("hardenedshieldemitter");
	}

}
