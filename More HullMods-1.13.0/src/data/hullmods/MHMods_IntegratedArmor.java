package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class MHMods_IntegratedArmor extends mhmods_baseSHmod {

    final float
            minArmorMulti = 5f,
            armorMulti = 0.9f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinArmorFraction().modifyMult(id, minArmorMulti);
        stats.getArmorBonus().modifyMult(id, armorMulti);
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return (Math.round(100 * minArmorMulti) + "%");
        if (index == 1) return (Math.round(100 * (1f - armorMulti)) + "%");
        return null;
    }

}