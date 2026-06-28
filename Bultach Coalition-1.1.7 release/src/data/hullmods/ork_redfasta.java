package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ork_redfasta extends BaseHullMod {
    private static final String id = "ork_redfasta";

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk())
        {
            return;
        }
        //if (ship.getOwner() == 1) {
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        float avgArmor = 3f - (3f * ship.getAverageArmorInSlice(0f, 360f) / armorGrid.getArmorRating());
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getMaxSpeed().modifyFlat(id, stats.getMaxSpeed().base * 0.5f * avgArmor);
        stats.getAcceleration().modifyPercent(id, stats.getAcceleration().base * 2f * avgArmor);
        stats.getDeceleration().modifyPercent(id, stats.getDeceleration().base * 2f * avgArmor);
        stats.getTurnAcceleration().modifyPercent(id, stats.getTurnAcceleration().base * 2f * avgArmor);
        //}
    }

}