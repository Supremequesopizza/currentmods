package data.hullmods;

import Utilities.MHMods_utilities;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.HashMap;
import java.util.Map;

public class MHMods_hullfoam extends BaseHullMod {

    final Map<HullSize, Float> repairSpeed = new HashMap<>();
    final float maxFoam = 1f;
    final float maxRepairReductionPerUsed = 0.5f;
    {
        repairSpeed.put(HullSize.FIGHTER, 1f);
        repairSpeed.put(HullSize.FRIGATE, 1f);
        repairSpeed.put(HullSize.DESTROYER, 0.8f);
        repairSpeed.put(HullSize.CRUISER, 0.6f);
        repairSpeed.put(HullSize.CAPITAL_SHIP, 0.4f);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return MHMods_utilities.floatToString(repairSpeed.get(HullSize.FRIGATE)) + "%";
        if (index == 1) return MHMods_utilities.floatToString(repairSpeed.get(HullSize.DESTROYER)) + "%";
        if (index == 2) return MHMods_utilities.floatToString(repairSpeed.get(HullSize.CRUISER)) + "%";
        if (index == 3) return MHMods_utilities.floatToString(repairSpeed.get(HullSize.CAPITAL_SHIP)) + "%";
        if (index == 4) return MHMods_utilities.floatToString(maxFoam * 100) + "%";
        if (index == 5) return MHMods_utilities.floatToString((1 - maxRepairReductionPerUsed) * 100) + "%";
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        String id = ship.getId();
        float foamLeft = maxFoam;

        if (customCombatData.get("MHMods_hullfoam" + id) instanceof Float)
            foamLeft = (float) customCombatData.get("MHMods_hullfoam" + id);

        float currentHP = ship.getHitpoints();
        float repairReduction = maxRepairReductionPerUsed * (1 - foamLeft);
        float missingHP = Math.max(0f,1f - repairReduction - ship.getHullLevel());
        if (missingHP > 0){
            float repairThatFrame = repairSpeed.get(ship.getHullSize()) * 0.01f * amount;
            if (missingHP < repairThatFrame) repairThatFrame = missingHP;

            float hullToRepair = ship.getMaxHitpoints() * repairThatFrame;
            float percentRepaired = ship.getMaxHitpoints() * repairThatFrame / ship.getHullSpec().getHitpoints();
            if (percentRepaired > foamLeft) {
                percentRepaired = foamLeft;
                hullToRepair = ship.getHullSpec().getHitpoints() * percentRepaired;
            }
            ship.setHitpoints(currentHP + hullToRepair);

            foamLeft -= percentRepaired;
        }

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            if (foamLeft != 0) {
                Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_hullfoam", "graphics/icons/hullsys/mhmods_hullfoam.png", "Hullfoam Left", (float) Math.round(foamLeft * 1000) / 10 + "%", false);
            } else {
                Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_hullfoam", "graphics/icons/hullsys/mhmods_hullfoam.png", "Hullfoam Left", "OUT OF FOAM", true);
            }
        }

        customCombatData.put("MHMods_hullfoam" + id, foamLeft);
    }
}
