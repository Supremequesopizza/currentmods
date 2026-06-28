package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class MHMods_ArmorPiercingAmmunition extends BaseHullMod {

    final float DMGBonus = 25f;
    final float shieldMalus = 5f;

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHitStrengthBonus().modifyPercent(id, DMGBonus);
        stats.getDamageToTargetShieldsMult().modifyMult(id, 1 - shieldMalus * 0.01f);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(DMGBonus) + "%";
        if (index == 1) return Math.round(shieldMalus) + "%";
        return null;
    }
}