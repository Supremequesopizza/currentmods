package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashSet;
import java.util.Set;

public class bt_divine_core extends BaseHullMod {

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
	static {
		// These hullmods will automatically be removed
		BLOCKED_HULLMODS.add("frontshield");
		BLOCKED_HULLMODS.add("targetingunit");
		BLOCKED_HULLMODS.add("dedicated_targeting_core");
	}

	private float check = 0;
	private String id, ERROR = "IncompatibleHullmodWarning";

	public static final float COST_REDUCTION = 10;
	public static final float MAX_TIME_FLOW_BONUS = 50f;
	public static final float MIN_TIME_FLOW_BONUS = 10f;

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

		if (check > 0) {
			check -= 1;
			if (check < 1) {
				ship.getVariant().removeMod(ERROR);
			}
		}

		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
				ship.getVariant().addMod(ERROR);
				check = 3;
			}
		}
	}

	public static float RANGE_BONUS = 75f;
	public static float PD_MINUS = 45f;

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive() || ship.isHulk()) {
			return;
		}

		// hull integrity
		float hullIntegrity = ship.getHullLevel();
		// Calculate time flow bonus based on it
		float timeFlowBonus = MIN_TIME_FLOW_BONUS + (1f - hullIntegrity) * (MAX_TIME_FLOW_BONUS - MIN_TIME_FLOW_BONUS);
		ship.getMutableStats().getTimeMult().modifyPercent(id, timeFlowBonus);

	}

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);

		stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);
		stats.getBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);

		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION);
		stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD).modifyFlat(id, -COST_REDUCTION);

	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) Math.round(RANGE_BONUS) + "%";
		if (index == 1) return "" + (int) Math.round(RANGE_BONUS - PD_MINUS) + "%";
		if (index == 2) return "" + (int) MIN_TIME_FLOW_BONUS + "%";
		if (index == 3) return "" + (int) MAX_TIME_FLOW_BONUS + "%";
		if (index == 4) return "" + (int) COST_REDUCTION + " OP";
		return null;
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}
}
