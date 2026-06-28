package forgprod.abilities.conversion.support.checks;

import com.fs.starfarer.api.fleet.FleetMemberAPI;

import forgprod.abilities.modules.FleetwideModuleManager;
import forgprod.abilities.modules.dataholders.ProductionModule;
import forgprod.settings.SettingsHolder;

import java.util.ArrayList;

public class ModuleConditionChecks {

    public static boolean isOperational(FleetMemberAPI member) {
        return !member.getRepairTracker().isMothballed() &&
               !member.getRepairTracker().isSuspendRepairs() &&
                hasMinimumCR(member);
    }

    public static boolean hasMinimumCR(FleetMemberAPI member) {
        return member.getRepairTracker().getCR() > SettingsHolder.MINIMUM_CR_PERCENT;
    }

    public static boolean hasActiveModule(FleetMemberAPI member) {
        //todo: this only has one use.
        //      looking at the use, there seems to be a macanic were the trail increases in size per ship that is forging.
        //      I -should- beable to change this function to handle that. I just need to itterate over all the values.
        //      although, it is also fine to just... keep this as it is?
        ArrayList<ProductionModule> module = FleetwideModuleManager.getInstance().getModuleIndex().get(member);
        if (module == null) return false;
        for (ProductionModule a : module){
            if (a != null && a.hasActiveCapacities()) return true;
        }
        return false;
    }

}
