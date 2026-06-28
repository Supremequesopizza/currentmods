package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.hullmods.bt_triumph_combatmodes;

public class bt_CombatSystemSwap extends BaseShipSystemScript {
    private boolean firstFrame = true;
    private int mode = 1; // 1 = Cruise, 2 = Battle

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;

        if (firstFrame) {
            firstFrame = false;
            ShipAPI ship = (ShipAPI) stats.getEntity();

            mode = (mode == 1) ? 2 : 1;
            ship.getSystem().setAmmo(mode);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        firstFrame = true;
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            ship.getSystem().setAmmo(mode);
        }
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        switch (system.getAmmo()) {
            case 1: return bt_triumph_combatmodes.CRUISE_MODE_NAME;
            case 2: return bt_triumph_combatmodes.BATTLE_MODE_NAME;
            default: return "Bruh Mode";
        }
    }
}
