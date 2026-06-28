package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.util.MagicIncompatibleHullmods;

public class supportships_CrampedMissileRacks extends BaseHullMod {

	public static final float AMMO_BONUS = -33f;
	public static final float AMMO_BONUS_STRING = 33f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMissileAmmoBonus().modifyPercent(id, AMMO_BONUS);
		//stats.getMissileWeaponDamageMult().modifyPercent(id, 1000f);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) AMMO_BONUS_STRING + "%";
		return null;
	}


}
