package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class bt_oaf extends BaseShipSystemScript {
    public static final float FLUX_REDUCTION = 35f;
    public static final float ROF_BONUS = 3f;

    public static final float MALFUNCTION_CHANCE = 0.15f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getWeaponMalfunctionChance().modifyFlat(id, MALFUNCTION_CHANCE);

        float mult = 1f + ROF_BONUS * effectLevel;
        stats.getBallisticRoFMult().modifyMult(id, mult);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getWeaponMalfunctionChance().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float mult = 1f + ROF_BONUS * effectLevel;
        float bonusPercent = (int) ((mult - 1f) * 100f);
        if (index == 0) {
            return new StatusData("ballistic rate of fire +" + (int) bonusPercent + "%", false);
        }
        if (index == 1) {
            return new StatusData("ballistic flux use -" + (int) FLUX_REDUCTION + "%", false);
        }
        if (index == 2) {
            return new StatusData("EAT LEAD", false);
        }
        return null;
    }
}
