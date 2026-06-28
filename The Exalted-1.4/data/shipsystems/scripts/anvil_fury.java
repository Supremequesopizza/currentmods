package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

import java.util.HashMap;
import java.util.Map;

public class anvil_fury extends BaseShipSystemScript {
	private static Map mag = new HashMap();
	protected Object STATUSKEY1 = new Object();

	

	static {
		mag.put(ShipAPI.HullSize.FIGHTER, 0.70F);
		mag.put(ShipAPI.HullSize.FRIGATE, 0.70F);
		mag.put(ShipAPI.HullSize.DESTROYER, 0.70F);
		mag.put(ShipAPI.HullSize.CRUISER, 0.70F);
		mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.70F);
	}


	public anvil_fury() {
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 150f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 150f * effectLevel);
			//stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
			effectLevel = 1.0F;
			float mult = (Float)mag.get(ShipAPI.HullSize.CRUISER);
			if (stats.getVariant() != null) {
				mult = (Float)mag.get(stats.getVariant().getHullSize());
			}

			stats.getHullDamageTakenMult().modifyMult(id, 1.0F - (1.0F - mult) * effectLevel);
			stats.getArmorDamageTakenMult().modifyMult(id, 1.0F - (1.0F - mult) * effectLevel);
			stats.getEmpDamageTakenMult().modifyMult(id, 1.0F - (1.0F - mult) * effectLevel);
			ShipAPI ship = null;
			boolean player = false;
			if (stats.getEntity() instanceof ShipAPI) {
				ship = (ShipAPI)stats.getEntity();
				player = ship == Global.getCombatEngine().getPlayerShip();
			}

			if (player) {
				ShipSystemAPI system = ship.getSystem();
				if (system != null) {
					float percent = (1.0F - mult) * effectLevel * 100.0F;
					Global.getCombatEngine().maintainStatusForPlayerShip(this.STATUSKEY1, system.getSpecAPI().getIconSpriteName(), system.getDisplayName(), Math.round(percent) + "% less damage taken", false);
				}
			}
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		return null;
	}
}
