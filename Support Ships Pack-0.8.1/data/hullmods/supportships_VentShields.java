package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.util.MagicIncompatibleHullmods;

public class supportships_VentShields extends BaseHullMod {
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
		if(stats.getVariant().getHullMods().contains("extendedshieldemitter")){
            MagicIncompatibleHullmods.removeHullmodWithWarning(stats.getVariant(), "extendedshieldemitter", "supportships_vent_shields");
           }
		if(stats.getVariant().getHullMods().contains("adaptiveshields")){
            MagicIncompatibleHullmods.removeHullmodWithWarning(stats.getVariant(), "adaptiveshields", "supportships_vent_shields");
           }
	}	
	@Override
}