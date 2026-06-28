package data.hullmods;

import Utilities.MHMods_utilities;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class MHMods_fluxGenerator extends BaseHullMod {

    float multi = 2f;
    float threshold = 0.5f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return MHMods_utilities.floatToString(threshold * 100) + "%";
        if (index == 1) return MHMods_utilities.floatToString(multi);
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().isVenting()) return;
        if (ship.getFluxLevel() <= threshold){
            float fluxToGenerate = ship.getMutableStats().getFluxDissipation().getModifiedValue() * multi * amount;
            /*
            if (fluxToGenerate > ship.getMaxFlux() * threshold) fluxToGenerate = ship.getMaxFlux() * threshold - ship.getCurrFlux();
            ship.getFluxTracker().increaseFlux(fluxToGenerate - 1, false);

             */
            ship.getFluxTracker().setCurrFlux(Math.max(Math.min(fluxToGenerate + ship.getCurrFlux(), ship.getMaxFlux() * threshold), 0));
        }
    }
}
