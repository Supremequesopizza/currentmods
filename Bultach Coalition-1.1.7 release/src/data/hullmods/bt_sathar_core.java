package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.magiclib.util.MagicUI;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class bt_sathar_core extends BaseHullMod {

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
	static
	{
		BLOCKED_HULLMODS.add("frontshield");
		BLOCKED_HULLMODS.add("targetingunit");
		BLOCKED_HULLMODS.add("dedicated_targeting_core");
	}
	private float check=0;
	private String id, ERROR="IncompatibleHullmodWarning";

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id){
		if (check>0) {
			check-=1;
			if (check<1){
				ship.getVariant().removeMod(ERROR);
			}
		}

		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
				ship.getVariant().addMod(ERROR);
				check=3;
			}
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || engine.isPaused() || engine.isCombatOver()) {
			return;
		}

		if (ship == engine.getPlayerShip()) {
			String dataKey = bt_sathar_linkedshields.DATA_KEY_PREFIX + ship.getId();
			Object rawData = engine.getCustomData().get(dataKey);

			if (rawData instanceof bt_sathar_linkedshields.LinkedShieldsData) {
				bt_sathar_linkedshields.LinkedShieldsData moduleData = (bt_sathar_linkedshields.LinkedShieldsData) rawData;

				boolean bothOnline = moduleData.leftModuleOnline && moduleData.rightModuleOnline;
				boolean oneOnline = moduleData.singleSurvivor != null;

				float fluxLevel = 0f;
				float maxFlux = 0f;

				if (bothOnline) {
					fluxLevel = moduleData.currentSharedFluxLevel;
					maxFlux = moduleData.combinedMaxFlux;
				} else if (oneOnline) {
					fluxLevel = moduleData.singleSurvivor.getFluxTracker().getFluxLevel();
					maxFlux = moduleData.singleSurvivor.getFluxTracker().getMaxFlux();
				}

				if (bothOnline || oneOnline) {
					Color safeColor = new Color(154, 254, 0);
					Color dangerColor = new Color(255, 88, 50);

					int red = (int) (safeColor.getRed() * (1f - fluxLevel) + dangerColor.getRed() * fluxLevel);
					int green = (int) (safeColor.getGreen() * (1f - fluxLevel) + dangerColor.getGreen() * fluxLevel);
					int blue = (int) (safeColor.getBlue() * (1f - fluxLevel) + dangerColor.getBlue() * fluxLevel);
					Color interpolatedColor = new Color(red, green, blue, 200);

					int currentFlux = (int) (fluxLevel * maxFlux);
					MagicUI.drawInterfaceStatusBar(
							ship,
							"bt_sathar_flux_bar",
							fluxLevel,
							interpolatedColor,
							null,
							0f,
							"M-SHLD",
							currentFlux
					);
				}
			}
		}
	}

	public static float RANGE_BONUS = 75f;
	public static float PD_MINUS = 45f;

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int)Math.round(RANGE_BONUS) + "%";
		if (index == 1) return "" + (int)Math.round(RANGE_BONUS - PD_MINUS) + "%";
		return null;
	}


	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);

		stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);
		stats.getBeamPDWeaponRangeBonus().modifyPercent(id, -PD_MINUS);
	}
}