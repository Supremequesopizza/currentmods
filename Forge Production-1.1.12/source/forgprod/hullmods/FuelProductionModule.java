package forgprod.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import forgprod.abilities.conversion.support.ProductionConstants;
import forgprod.abilities.conversion.support.ProductionType;
import forgprod.abilities.modules.FleetwideModuleManager;
import forgprod.abilities.modules.dataholders.ProductionCapacity;
import forgprod.abilities.modules.dataholders.ProductionModule;
import forgprod.hullmods.base.BaseProductionHullmod;

/**
 * @author Ontheheavens
 * @since 06.12.2022
 */

@SuppressWarnings("unused")
public class FuelProductionModule extends BaseProductionHullmod {

    @Override
    public String modID() {
        return "Production logistics Fuel";
    }

    @Override
    public void addCapacitySection(TooltipMakerAPI tooltip, ShipAPI ship) {
        if (!super.addAvailableCapacities(tooltip, ship)) {
            super.addCapacityPanels(tooltip, ship, false, ProductionType.FUEL_PRODUCTION, null);
        }
    }


    @Override
    public ProductionModule getSpecificModule(FleetMemberAPI member) {
        for (ProductionModule a : FleetwideModuleManager.getInstance().getSpecificModule(member)){
            if (a.getHullmodId().equals(ProductionConstants.FUEL_PRODUCTION_MODULE)) return a;
        }
        return null;
    }

}
