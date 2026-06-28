package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;

public class MHMods_EmergencyVentingSystemV2 extends mhmods_baseSHmod {

    final float maxOverload = 2f,
            ventBonusSMod = 40f;

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0 || index == 1) return Math.round(maxOverload) + "";
        if (index == 2) return "";
        return null;
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(ventBonusSMod) + "%";
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        if (!ship.isAlive()) return;

        boolean ShouldVent = false;
        boolean ventFromSystem = false;

        if (Global.getCombatEngine().getCustomData().get("MHM_EVS_ShouldVent" + ship.getId()) instanceof Boolean)
            ShouldVent = (boolean) Global.getCombatEngine().getCustomData().get("MHM_EVS_ShouldVent" + ship.getId());

        if (Global.getCombatEngine().getCustomData().get("MHM_EVS_ventFromSystem" + ship.getId()) instanceof Boolean)
            ventFromSystem = (boolean) Global.getCombatEngine().getCustomData().get("MHM_EVS_ventFromSystem" + ship.getId());

        if (ship.getFluxTracker().getOverloadTimeRemaining() >= maxOverload && ship.getMutableStats().getVentRateMult().getModifiedValue() > 0 && !ShouldVent) {
            ship.getFluxTracker().stopOverload();
            ship.getFluxTracker().beginOverloadWithTotalBaseDuration(maxOverload);
            ShouldVent = true;
        }
        if (ventFromSystem && ship.getFluxTracker().isVenting() && ship.getVariant().getSMods().contains(id)) {
            ship.getMutableStats().getVentRateMult().modifyPercent(id, ventBonusSMod);
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/hullsys/infernium_injector.png", "Emergency Venting System", "Venting speed increased by " + Math.round(ventBonusSMod) + "%", false);
            }
        } else {
            ship.getMutableStats().getVentRateMult().unmodify(id);
            ventFromSystem = false;
        }
        if (ShouldVent && !ship.getFluxTracker().isOverloadedOrVenting()) {
            ship.giveCommand(ShipCommand.VENT_FLUX, true, 1);
            ShouldVent = false;
            ventFromSystem = true;
        }

        Global.getCombatEngine().getCustomData().put("MHM_EVS_ShouldVent" + ship.getId(), ShouldVent);
        Global.getCombatEngine().getCustomData().put("MHM_EVS_ventFromSystem" + ship.getId(), ventFromSystem);
    }
}
