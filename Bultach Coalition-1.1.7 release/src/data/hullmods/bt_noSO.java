package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.HashSet;
import java.util.Set;

public class bt_noSO extends BaseHullMod {

	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
	static
	{
		// These hullmods will automatically be removed
		BLOCKED_HULLMODS.add("safetyoverrides");

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


	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

}





