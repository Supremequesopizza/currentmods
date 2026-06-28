package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class anvil_possession extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS_PERCENT = 20f;
	public static final float SPEED_BONUS_PERCENT = 30f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 30f);
			stats.getAcceleration().modifyPercent(id, 100f * effectLevel);
			stats.getDeceleration().modifyPercent(id, 100f * effectLevel);
			stats.getTurnAcceleration().modifyFlat(id, 20f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, 100f * effectLevel);
			stats.getMaxTurnRate().modifyFlat(id, 15f);
			stats.getMaxTurnRate().modifyPercent(id, 100f);
		}

		if (stats.getEntity() instanceof ShipAPI && false) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			String key = ship.getId() + "_" + id;
			Object test = Global.getCombatEngine().getCustomData().get(key);
			if (state == State.IN) {
				if (test == null && effectLevel > 0.2f) {
					Global.getCombatEngine().getCustomData().put(key, new Object());
					ship.getEngineController().getExtendLengthFraction().advance(1f);
					for (ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
						if (engine.isSystemActivated()) {
							ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 1f);
						}
					}
				}
			} else {
				Global.getCombatEngine().getCustomData().remove(key);
			}
		}

		float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		stats.getEnergyWeaponDamageMult().modifyPercent(id, bonusPercent);

	}
	public void unapply(MutableShipStatsAPI stats) {
		String id = null;
		stats.getEnergyWeaponDamageMult().unmodify(id);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
		if (index == 0) {
			return new StatusData("+" + (int) bonusPercent + "% energy weapon damage" , false);
		} else if (index == 1) {
			return null;
		} else if (index == 2) {
			return null;
		}
		if (index == 3) {
			return new StatusData("improved maneuverability", false);
		} else if (index == 4) {
			return new StatusData("+30 top speed", false);
		}
		return null;
	}

}
