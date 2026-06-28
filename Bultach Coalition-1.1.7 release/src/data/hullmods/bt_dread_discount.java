package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashMap;
import java.util.Map;

public class bt_dread_discount extends BaseHullMod {

    public static final Map<ShipAPI.HullSize, Float> DEPLOYDISC = new HashMap<>();
    static {
        DEPLOYDISC.put(ShipAPI.HullSize.CAPITAL_SHIP, -70f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        if (ship == null || ship.getFleetMember() == null) return;

        // Check if the ship belongs to the player fleet
        FleetMemberAPI fleetMember = ship.getFleetMember();
        if (fleetMember.getFleetData() != null &&
                fleetMember.getFleetData().getFleet() == Global.getSector().getPlayerFleet()) {
            return; // Do nothing if it's in the player fleet
        }

        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, DEPLOYDISC.get(ship.getHullSize()));
    }
}
