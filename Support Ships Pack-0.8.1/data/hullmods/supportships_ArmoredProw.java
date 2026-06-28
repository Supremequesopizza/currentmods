package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.util.MagicUI;
import java.util.List;

public class supportships_ArmoredProw extends BaseHullMod {


    public void applyEffectsAFterShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.ensureClonedStationSlotSpec();
    }

    String supportships_armoredprow_moduleStatus;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        float ap_childHull = 0;
        float ap_childBaseHullpoints = 0;
        boolean ap_childAlive = false;

        if (ship.isShipWithModules()) {
            ship.ensureClonedStationSlotSpec();
            List<ShipAPI> prow_modules = ship.getChildModulesCopy();
            for (ShipAPI childModule : prow_modules) {
                if (childModule.getVariant().getHullSpec().getHullId().equals("supportships_tercio_armor")) {
                    ap_childBaseHullpoints = childModule.getVariant().getHullSpec().getHitpoints();
                    ap_childHull = childModule.getHullLevel();
                    ap_childAlive = !childModule.isHulk();

                }
				if (childModule.getVariant().getHullSpec().getHullId().equals("supportships_tercio_xiv_armor")) {
                    ap_childBaseHullpoints = childModule.getVariant().getHullSpec().getHitpoints();
                    ap_childHull = childModule.getHullLevel();
                    ap_childAlive = !childModule.isHulk();

                }
            }
        }

        supportships_armoredprow_moduleStatus = "PROW";
        //Color ui = new Color(255, 128, 0);
        if (ap_childAlive){
            MagicUI.drawInterfaceStatusBar(ship, (ap_childHull), null, null, 0, supportships_armoredprow_moduleStatus, (int) (ap_childBaseHullpoints * ap_childHull));
        }

    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {

        if (index == 0) return "3000";
        if (index == 1) return  "1000";
        if (index == 2) return  "1100";
        if (index == 3) return  "Sufficiently large explosions may damage the rest of the ship";
        return null;
    }
}
