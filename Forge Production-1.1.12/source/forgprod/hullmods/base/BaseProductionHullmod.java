package forgprod.hullmods.base;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import forgprod.abilities.conversion.support.ProductionConstants;
import forgprod.abilities.conversion.support.ProductionType;
import forgprod.abilities.modules.FleetwideModuleManager;
import forgprod.abilities.modules.dataholders.ProductionCapacity;
import forgprod.abilities.modules.dataholders.ProductionModule;
import forgprod.hullmods.checks.HullmodStateChecks;
import forgprod.hullmods.tooltip.TooltipCapacitySection;
import forgprod.hullmods.tooltip.TooltipModuleSection;
import forgprod.settings.SettingsHolder;

import java.util.ArrayList;

import static forgprod.abilities.conversion.support.ProductionConstants.SHIPSIZE_CAPACITY;
import static forgprod.hullmods.checks.HullmodStateChecks.*;

/**
 * @author Ontheheavens
 * @since 05.12.2022
 */

public abstract class BaseProductionHullmod extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        int shipCapacity = SHIPSIZE_CAPACITY.get(hullSize);
        int crewIncrease = (int) (SettingsHolder.CREW_REQ_PER_CAPACITY * shipCapacity);
        float supplyIncrease = SettingsHolder.SUPPLY_REQ_PER_CAPACITY * shipCapacity;
        stats.getMaxCombatReadiness().modifyFlat(modID(), -SettingsHolder.MAXIMUM_CR_DECREASE);
        stats.getSuppliesPerMonth().modifyFlat(modID(), supplyIncrease);
        stats.getMinCrewMod().modifyFlat(modID(), crewIncrease);
    }
    public abstract String modID();

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        float crewPerCapacity = SettingsHolder.CREW_REQ_PER_CAPACITY;
        float supplyPerCapacity = SettingsHolder.SUPPLY_REQ_PER_CAPACITY;
        int cruiserCapacity = SHIPSIZE_CAPACITY.get(ShipAPI.HullSize.CRUISER);
        int capitalCapacity = SHIPSIZE_CAPACITY.get(ShipAPI.HullSize.CAPITAL_SHIP);
        int combatReadinessDecrease = (int) (SettingsHolder.MAXIMUM_CR_DECREASE * 100f);
        if (index == 0) return ((int)(crewPerCapacity * cruiserCapacity)) + "/" + ((int)(crewPerCapacity * capitalCapacity));
        if (index == 1) return (supplyPerCapacity * cruiserCapacity) + "/" + (supplyPerCapacity * capitalCapacity);
        if (index == 2) return combatReadinessDecrease + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship,
                                          float width, boolean isForModSpec) {
        boolean expanded = Keyboard.isKeyDown(Keyboard.getKeyIndex("F1"));
        if (!expanded && ship != null) {
            String key = "F1";
            tooltip.addPara("Press %s for more info.", 10,
                    Misc.getGrayColor(), Misc.getHighlightColor(), key);
        }
        tooltip.addSectionHeading(ProductionConstants.HULLMOD_NAMES.get(this.spec.getId()), Alignment.MID, 8f);
        TooltipModuleSection.addModulePanel(tooltip, ship, this.spec.getId(), width, expanded);
        if (ship != null && isApplicableToShip(ship) && expanded) {
            addCapacitySection(tooltip, ship);
        }
    }

    public void addCapacitySection(TooltipMakerAPI tooltip, ShipAPI ship) {}

    public void addCapacityPanels(TooltipMakerAPI tooltip, ShipAPI ship, boolean hasSecondary,
                                  ProductionType primaryType, ProductionType secondaryType) {
        tooltip.addSectionHeading("Production capacities", Alignment.MID, 8f);
        FleetMemberAPI member = ship.getFleetMember();
        FleetwideModuleManager manager = FleetwideModuleManager.getInstance();
        ProductionCapacity primaryCapacity = getCapacity(member,0);//manager.getSpecificCapacity(member, 0);
        boolean installed = ship.getVariant().hasHullMod(this.spec.getId());
        TooltipCapacitySection.addCapacityPanel(tooltip, ship, primaryType, primaryCapacity, installed);
        if (hasSecondary && secondaryType != null) {
            ProductionCapacity secondaryCapacity = getCapacity(member,1);//manager.getSpecificCapacity(member, 1);
            TooltipCapacitySection.addCapacityPanel(tooltip, ship, secondaryType, secondaryCapacity, installed);
        }
    }
    public ProductionCapacity getCapacity(FleetMemberAPI member, int index) {
        if (member == null) { return null; }
        ProductionModule module = getSpecificModule(member);
        if (module == null) { return null; }
        if ((index + 1) >  module.getModuleCapacities().size()) {
            return null;
        }
        return module.getModuleCapacities().get(index);
    }

    protected boolean addAvailableCapacities(TooltipMakerAPI tooltip, ShipAPI ship) {
        CampaignFleetAPI fleet = HullmodStateChecks.getFleetOfShip(ship);
        if (fleet == null) {
            return false;
        }
        FleetwideModuleManager manager = FleetwideModuleManager.getInstance();
        ProductionModule module = getSpecificModule(ship.getFleetMember());//manager.getSpecificModule(ship.getFleetMember());
        if (module == null) {
            return false;
        } else {
            boolean hasSecondary = module.getModuleCapacities().size() > 1;
            ProductionType primary = module.getModuleCapacities().get(0).getProductionType();
            if (primary == null) return false;
            ProductionType secondary = null;
            if (hasSecondary) {
                secondary = module.getModuleCapacities().get(1).getProductionType();
            }
            this.addCapacityPanels(tooltip, ship, hasSecondary, primary, secondary);
            return true;
        }
    }
    public abstract ProductionModule getSpecificModule(FleetMemberAPI member);

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (isForgeHullmodInstallValid(ship) && !hasOtherModules(ship, this.spec.getId()));
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship != null && !isValidHullsize(ship.getHullSpec())) {
            ArrayList<String> sizes = SettingsHolder.getAllowedHullmodNames();
            String a = "";
            for (int b = 0; b < sizes.size(); b++){
                a+=sizes.get(b);
                if (b != 0 && b == sizes.size() - 2) a+=", and ";
                else if (b < sizes.size()-2) a+=", ";
            }
            return "Can only be installed on "+a+" hulls";
        }
        if (ship != null && isModule(ship)) {
            return "Can not be installed on modules";
        }
        if (hasOtherModules(ship, this.spec.getId())) {
            return "Can only install one Production Forge per hull";
        }
        return null;
    }

}
