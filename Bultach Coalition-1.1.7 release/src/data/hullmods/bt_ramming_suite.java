package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class bt_ramming_suite extends BaseHullMod {

    public static final float CASUALTY_REDUCTION = 60f;
    public static final float DMOD_AVOID_CHANCE = 50f;
    public static final float DEPLOYMENT_COST_MULT = 0.5f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getCrewLossMult().modifyMult(id, 1f - CASUALTY_REDUCTION * 0.01f);

        stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD).modifyMult(id, (1f - DMOD_AVOID_CHANCE * 0.01f));
        stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
        stats.getSuppliesToRecover().modifyMult(id, DEPLOYMENT_COST_MULT);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) CASUALTY_REDUCTION + "%";
        if (index == 1) return "" + (int) DMOD_AVOID_CHANCE + "%";
        if (index == 2) return "" + (int) (100f - (DEPLOYMENT_COST_MULT * 100f)) + "%";
        return null;
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }
}
