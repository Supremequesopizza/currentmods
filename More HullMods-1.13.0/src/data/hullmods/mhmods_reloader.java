package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class mhmods_reloader extends mhmods_baseSHmod {

    float
            regen = 25f,
            regenSMod = 100f,
            ammoSMod = 0.5f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyAmmoRegenMult().modifyPercent(id, regen);
        stats.getBallisticAmmoRegenMult().modifyPercent(id, regen);

        if (isSMod(stats)) {
            stats.getEnergyAmmoRegenMult().modifyPercent(id, regenSMod);
            stats.getEnergyAmmoBonus().modifyMult(id, ammoSMod);
            stats.getBallisticAmmoRegenMult().modifyPercent(id, regenSMod);
            stats.getBallisticAmmoBonus().modifyMult(id, ammoSMod);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(regen) + "%";
        return null;
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(regenSMod) + "%";
        if (index == 1) return Math.round((1 - ammoSMod) * 100) + "%";
        return null;
    }
}
