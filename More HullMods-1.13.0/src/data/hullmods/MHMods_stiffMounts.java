package data.hullmods;

import Utilities.MHMods_utilities;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class MHMods_stiffMounts extends BaseHullMod {

    final float recoilReduction = 35f;
    final float turnSpeedReduction = 10f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxRecoilMult().modifyMult(id, 1f - (0.01f * recoilReduction));
        stats.getRecoilPerShotMult().modifyMult(id, 1f - (0.01f * recoilReduction));
        stats.getRecoilDecayMult().modifyMult(id, 1f - (0.01f * recoilReduction));
        stats.getWeaponTurnRateBonus().modifyMult(id, 1f - (0.01f * turnSpeedReduction));
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return MHMods_utilities.floatToString(recoilReduction) + "%";
        if (index == 1) return MHMods_utilities.floatToString(turnSpeedReduction) + "%";
        return null;
    }
}
