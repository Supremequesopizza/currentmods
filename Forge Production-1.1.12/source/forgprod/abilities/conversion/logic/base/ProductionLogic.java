package forgprod.abilities.conversion.logic.base;

import java.util.*;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import forgprod.abilities.conversion.logic.*;
import forgprod.abilities.conversion.support.checks.FleetwideProductionChecks;
import forgprod.abilities.conversion.support.ProductionType;
import forgprod.abilities.conversion.support.checks.ItemBonusesChecks;
import forgprod.abilities.modules.FleetwideModuleManager;
import forgprod.abilities.modules.dataholders.ProductionModule;
import forgprod.crewReplacer_Combatability.Forgeprod_DefaultCargo;

/**
 * New iteration of commodity production logic.
 * Singleton-pattern class.
 * @author Ontheheavens
 * @since 03.12.2022
 */

public abstract class ProductionLogic {

    protected ProductionType productionType;
    protected boolean hasSecondaryInput;
    protected float cyclePrimaryInputAmount;
    protected float cycleSecondaryInputAmount;
    protected float cycleOutputAmount;
    protected float cycleMachineryUse;
    protected String primaryInputType;
    protected String secondaryInputType;
    protected String outputType;

    // To be overridden by all inheritors.
    protected ProductionLogic() {
        productionType = null;
        hasSecondaryInput = false;
        cyclePrimaryInputAmount = 0f;
        cycleSecondaryInputAmount = 0f;
        cycleOutputAmount = 0f;
        cycleMachineryUse = 0f;
        primaryInputType = "";
        secondaryInputType = "";
        outputType = "";
    }

    public ProductionType getProductionType(){
        return this.productionType;
    }
    public ArrayList<String> getRequired(){
        ArrayList<String> out = new ArrayList<>();
        if (getPrimaryInputType() != null && !getPrimaryInputType().equals("")){
            out.add(getPrimaryInputType());
        }
        if (getSecondaryInputType() != null && !getSecondaryInputType().equals("")){
            out.add(getSecondaryInputType());
        }

        return out;
    }
    public boolean hasSecondaryInput() {
        return hasSecondaryInput;
    }

    public String getPrimaryInputType() {
        return primaryInputType;
    }

    public String getSecondaryInputType() {
        return secondaryInputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public float getCyclePrimaryInputAmount() {
        return cyclePrimaryInputAmount;
    }

    public float getCycleSecondaryInputAmount() {
        return cycleSecondaryInputAmount;
    }

    public float getCycleOutputAmount() {
        return cycleOutputAmount;
    }

    public float getCycleMachineryUse() {
        return cycleMachineryUse;
    }

    public void initializeProduction(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        if (this.productionType == null) return;
        if (FleetwideProductionChecks.getFleetCapacityForType(fleet, productionType) < 1) return;
        if (!FleetwideProductionChecks.hasMinimumMachinery(fleet)) return;
        if (hasMinimumCargo(fleet)) {
            commenceProduction(fleet);
        }
    }

    protected boolean hasMinimumCargo(CampaignFleetAPI fleet) {
        if (hasSecondaryInput) {
            return (getAvailablePrimaryInputs(fleet) > cyclePrimaryInputAmount &&
                    getAvailableSecondaryInputs(fleet) > cycleSecondaryInputAmount);
        } else {
            return (getAvailablePrimaryInputs(fleet) > cyclePrimaryInputAmount);
        }
    }

    protected float getAvailablePrimaryInputs(CampaignFleetAPI fleet) {
        return Forgeprod_DefaultCargo.active.getCommodityAmount(this.productionType,primaryInputType,fleet.getCargo());//fleet.getCargo().getCommodityQuantity(primaryInputType);
    }

    protected float getAvailableSecondaryInputs(CampaignFleetAPI fleet) {
        return Forgeprod_DefaultCargo.active.getCommodityAmount(this.productionType,secondaryInputType,fleet.getCargo());//fleet.getCargo().getCommodityQuantity(secondaryInputType);
    }

    // Takes inputs actually available in cargo holds into account.
    protected int getMinimalCapacity(CampaignFleetAPI fleet) {
        int possibleCapacityFleet = FleetwideProductionChecks.getFleetCapacityForType(fleet, productionType);
        int possibleCapacityCargo = getPossibleCapacityByCargo(fleet);
        return Math.min(possibleCapacityFleet, possibleCapacityCargo);
    }

    protected int getPossibleCapacityByCargo(CampaignFleetAPI fleet) {
        if (hasSecondaryInput) {
            return (int) Math.min((getAvailablePrimaryInputs(fleet) / cyclePrimaryInputAmount),
                    (getAvailableSecondaryInputs(fleet) / cycleSecondaryInputAmount));
        } else {
            return (int) (getAvailablePrimaryInputs(fleet) / cyclePrimaryInputAmount);
        }
    }

    public float getCycleOutputBonus(CampaignFleetAPI fleet) {
        switch (productionType) {
            case METALS_PRODUCTION:
            case TRANSPLUTONICS_PRODUCTION:
                return ItemBonusesChecks.getRefiningBonus(fleet);
            case FUEL_PRODUCTION:
                return ItemBonusesChecks.getFuelProductionBonus(fleet);
            case SUPPLIES_PRODUCTION:
            case MACHINERY_PRODUCTION:
                return ItemBonusesChecks.getHeavyIndustryBonus(fleet);
        }
        return 0f;
    }

    protected void commenceProduction(CampaignFleetAPI fleet) {
        int cycles = getMinimalCapacity(fleet);
        float dailyPrimaryInputsSpent = cyclePrimaryInputAmount * cycles;
        float dailySecondaryInputsSpent = 0;
        if (hasSecondaryInput) {
            dailySecondaryInputsSpent = cycleSecondaryInputAmount * cycles;
        }
        float dailyOutputProduced = (cycleOutputAmount + getCycleOutputBonus(fleet)) * cycles;
        float dailyMachineryUsed = cycleMachineryUse * cycles;

        Forgeprod_DefaultCargo.active.removeCommodity(this.productionType,primaryInputType,dailyPrimaryInputsSpent,fleet.getCargo());//fleet.getCargo().removeCommodity(primaryInputType, dailyPrimaryInputsSpent);
        if (hasSecondaryInput) {
            Forgeprod_DefaultCargo.active.removeCommodity(this.productionType,secondaryInputType,dailySecondaryInputsSpent,fleet.getCargo());//fleet.getCargo().removeCommodity(secondaryInputType, dailySecondaryInputsSpent);
        }
        fleet.getCargo().addCommodity(outputType, dailyOutputProduced);
        this.setReportFlags(dailyOutputProduced);
        this.addMachineryUsed(dailyMachineryUsed);
        this.setModuleFlags();
    }

    // Needs to be overridden to set specific production report flags.
    protected void setReportFlags(float amountProduced) {}

    protected void addMachineryUsed(float amountUsed) {
        BreakdownLogic.getInstance().addToTotalDailyMachineryUsed(amountUsed);
    }

    protected void setModuleFlags() {
        FleetwideModuleManager managerInstance = FleetwideModuleManager.getInstance();
        Collection<ArrayList<ProductionModule>> fleetModules = managerInstance.getModuleIndex().values();
        for (ArrayList<ProductionModule> a : fleetModules) for (ProductionModule module : a){
            if (module.hasSpecificActiveCapacity(productionType)) {
                module.setProducedToday(true);
            }
        }
    }
}
