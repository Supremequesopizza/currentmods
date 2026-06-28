package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.HashMap;
import java.util.Map;

public class EngineBurnStats extends BaseShipSystemScript {

	private final Map<ShipAPI.HullSize, Float> strafeMulti = new HashMap<>();

	{
		strafeMulti.put(ShipAPI.HullSize.FIGHTER, 1f);
		strafeMulti.put(ShipAPI.HullSize.FRIGATE, 0.9f);
		strafeMulti.put(ShipAPI.HullSize.DESTROYER, 0.85f);
		strafeMulti.put(ShipAPI.HullSize.CRUISER, 0.75f);
		strafeMulti.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.65f);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();

		stats.getMaxSpeed().modifyFlat(id, 500f * effectLevel * strafeMulti.get(ship.getHullSize()));
		stats.getAcceleration().modifyPercent(id, 10000f * effectLevel * strafeMulti.get(ship.getHullSize()));
		stats.getDeceleration().modifyPercent(id, 0f * effectLevel * strafeMulti.get(ship.getHullSize()));
		stats.getMaxTurnRate().modifyPercent(id, 500f * effectLevel * strafeMulti.get(ship.getHullSize()));
		stats.getTurnAcceleration().modifyPercent(id, 850f * effectLevel * strafeMulti.get(ship.getHullSize()));
		stats.getTurnAcceleration().modifyFlat(id, 850f * effectLevel * strafeMulti.get(ship.getHullSize()));
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Activating Leap Jets.", false);
		}
		return null;
	}

}

