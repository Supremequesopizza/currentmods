package data.scripts.shipsystems;

import java.util.HashMap;										//ADDED
import java.util.Map;											//ADDED

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;				//ADDED
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;	//ADDED

import com.fs.starfarer.api.Global;								//ADDED
import com.fs.starfarer.api.combat.ShipAPI;						//ADDED
import com.fs.starfarer.api.combat.ShipAPI.HullSize;			//ADDED



public class RamDriveStats extends BaseShipSystemScript {

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 250f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 170f * effectLevel);
			//stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
		}
	{
		effectLevel = 1f;
		
		float mult = (Float) mag.get(HullSize.CRUISER);
		if (stats.getVariant() != null) {
			mult = (Float) mag.get(stats.getVariant().getHullSize());
		}
		stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
		stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - mult) * effectLevel);
		
		
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
		}
		if (player) {
			ShipSystemAPI system = ship.getSystem();
			if (system != null) {
				float percent = (1f - mult) * effectLevel * 100;
				Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
					system.getSpecAPI().getIconSpriteName(), system.getDisplayName(),
					(int) Math.round(percent) + "% less damage taken", false);
			}
		}
	}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		return null;
	}
	
		private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FIGHTER, 0.33f);
		mag.put(HullSize.FRIGATE, 0.33f);
		mag.put(HullSize.DESTROYER, 0.33f);
		mag.put(HullSize.CRUISER, 0.5f);
		mag.put(HullSize.CAPITAL_SHIP, 0.5f);
	}
	
	protected Object STATUSKEY1 = new Object();
	
	//public static final float INCOMING_DAMAGE_MULT = 0.25f;
	public static final float INCOMING_DAMAGE_CAPITAL = 0.5f;
	

	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}
}
