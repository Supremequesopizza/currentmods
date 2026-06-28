package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class BT_shieldsChanger_Gestalt extends BaseHullMod {

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getShield() != null) {
			ship.getShield().setRadius(ship.getShield().getRadius(), Global.getSettings().getSpriteName("GESTALT_TECH", "BT_shield_inner_gestalt"), Global.getSettings().getSpriteName("GESTALT_TECH", "BT_shield_outer_gestalt"));
		}
	}
}



