package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

// MOSTLY just a "dummy" system
public class bt_SatharShieldCommandSystem extends BaseShipSystemScript {

    public static final String SATHAR_SHIELD_COMMAND_STATE_KEY = "bt_sathar_shield_command_active_state";

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        }
        if (ship == null) return;

        if (state == State.IN || state == State.ACTIVE) {
            ship.getCustomData().put(SATHAR_SHIELD_COMMAND_STATE_KEY, true);
        } else {
            ship.getCustomData().put(SATHAR_SHIELD_COMMAND_STATE_KEY, false);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        }
        if (ship != null) {
            ship.getCustomData().put(SATHAR_SHIELD_COMMAND_STATE_KEY, false);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        boolean systemEffectivelyOn = (state == State.IN || state == State.ACTIVE);
        if (index == 0) {
            if (systemEffectivelyOn) {
                return new StatusData("Shield Command: MODULES ENGAGED", false);
            } else {
                return new StatusData("Shield Command: Modules Standby", true);
            }
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.getState() == ShipSystemAPI.SystemState.ACTIVE || system.getState() == ShipSystemAPI.SystemState.IN) {
            return "MODULE SHIELDS ACTIVE";
        }
        return "MODULE SHIELDS OFFLINE";
    }
}