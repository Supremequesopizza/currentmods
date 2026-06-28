package data.scripts.ai;

import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShieldAPI;
import data.scripts.shipsystems.bt_SatharShieldCommandSystem;

public class bt_sathar_module_AI implements ShipAIPlugin {

    private final ShipAPI ship;
    private final ShipwideAIFlags flags;

    public bt_sathar_module_AI(ShipAPI ship) {
        this.ship = ship;
        this.flags = ship.getAIFlags();
    }

    @Override
    public void advance(float amount) {
        ShipAPI parent = ship.getParentStation();
        if (parent == null || !parent.isAlive() || !ship.isAlive()) {
            return;
        }

        FluxTrackerAPI flux = ship.getFluxTracker();
        if (flux.isOverloadedOrVenting()) {
            return;
        }

        if (parent.getFluxTracker().isVenting()) {
            ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            return;
        }

        ShieldAPI shield = ship.getShield();
        if (shield == null) {
            return;
        }

        Object commandState = parent.getCustomData().get(bt_SatharShieldCommandSystem.SATHAR_SHIELD_COMMAND_STATE_KEY);
        boolean parentWantsShieldsUp = Boolean.TRUE.equals(commandState);

        if (parentWantsShieldsUp) {
            if (!shield.isOn()) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
        } else {
            if (shield.isOn()) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
        }
    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }

    @Override
    public void forceCircumstanceEvaluation() {
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return flags;
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override
    public void cancelCurrentManeuver() {
    }
}