package data.hullmods;

import Utilities.MHMods_utilities;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;

public class MHMods_ExplorationRefit extends BaseLogisticsHullMod {

    float
            FuelUse = 0.25f,
            SupUse = 0.25f,
            SensorStrength = 50f,
            CR = 30f,
            CRRecovery = 0.25f,
            MaxCargoFuelMulti = 0.85f,
            SmodCoronaEffect = 0.5f;


    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        boolean sMod = isSMod(stats);
        stats.getFuelUseMod().modifyMult(id, 1 - FuelUse);
        stats.getSuppliesPerMonth().modifyMult(id,  1 - SupUse);
        stats.getSensorStrength().modifyPercent(id, SensorStrength);
        stats.getMaxCombatReadiness().modifyFlat(id, -CR / 100);
        stats.getBaseCRRecoveryRatePercentPerDay().modifyMult(id, 1 - CRRecovery);
        stats.getCargoMod().modifyMult(id, MaxCargoFuelMulti);
        stats.getFuelMod().modifyMult(id, MaxCargoFuelMulti);

        if (sMod){
            stats.getDynamic().getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, SmodCoronaEffect);
        }
    }

    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return MHMods_utilities.floatToString(FuelUse * 100f) + "%";
        if (index == 1) return MHMods_utilities.floatToString(SupUse * 100f) + "%";
        if (index == 2) return MHMods_utilities.floatToString(SensorStrength) + "%";
        if (index == 3) return MHMods_utilities.floatToString(CR) + "%";
        if (index == 4) return MHMods_utilities.floatToString(CRRecovery * 100f) + "%";
        if (index == 5) return MHMods_utilities.floatToString(100 - MaxCargoFuelMulti * 100f) + "%";
        return null;
    }

    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round((1f - SmodCoronaEffect) * 100f) + "%";
        return null;
    }
}