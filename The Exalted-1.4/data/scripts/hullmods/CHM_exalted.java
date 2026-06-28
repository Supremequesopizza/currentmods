package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.SurveyPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashMap;
import java.util.Map;

public class CHM_exalted extends BaseHullMod {

	private static final Map mag = new HashMap();
	static {
		mag.put(HullSize.FRIGATE, 2f);
		mag.put(HullSize.DESTROYER, 3f);
		mag.put(HullSize.CRUISER, 4f);
		mag.put(HullSize.CAPITAL_SHIP, 5f);
	}
	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getArmorBonus().modifyFlat(id, (Float) mag.get(hullSize));
		stats.getDynamic().getMod(Stats.getSurveyCostReductionId(Commodities.HEAVY_MACHINERY)).modifyFlat(id, (Float) mag.get(hullSize));
		stats.getDynamic().getMod(Stats.getSurveyCostReductionId(Commodities.SUPPLIES)).modifyFlat(id, (Float) mag.get(hullSize));
	}
	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod("CHM_commission")) {
			ship.getVariant().removeMod("CHM_commission");
		}
	}
	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + ((Float) mag.get(HullSize.FRIGATE)).intValue();
		if (index == 1) return "" + ((Float) mag.get(HullSize.DESTROYER)).intValue();
		if (index == 2) return "" + ((Float) mag.get(HullSize.CRUISER)).intValue();
		if (index == 3) return "" + ((Float) mag.get(HullSize.CAPITAL_SHIP)).intValue();
		if (index == 4) return "" + (int) SurveyPluginImpl.MIN_SUPPLIES_OR_MACHINERY;
		return null;
	}
}
