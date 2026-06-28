package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.*;
import java.util.Map;

public class MHMods_HyperEngineUpgrade extends mhmods_baseSHmod {

    //public final float MANEUVER_BONUS = 25f;
    public final Color Engines_color = new Color(27, 238, 178, 255);
    final float fluxThreshold = 0.05f;

    {
        id = "mhmods_hyperengineupgrade";
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (isSMod(stats)) stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);
    }

    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return Math.round(fluxThreshold * 100) + "%";
        if (index == 1) return "";
        if (index == 2) {
            if (ship == null) {
                return "zero flux boost - ship base speed/3 + 20 /n (exact number on install)";
            } else {
                return String.valueOf(Math.round(calculateSpeedBonus(ship)));
            }
        }
        return null;
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        String id = ship.getId();
        float speedBoost;

        if (customCombatData.get("MHMods_HyperEngineUpgrade" + id) instanceof Float) {
            speedBoost = (float) customCombatData.get("MHMods_HyperEngineUpgrade" + id);
        } else {
            speedBoost = calculateSpeedBonus(ship);
            customCombatData.put("MHMods_HyperEngineUpgrade" + id, speedBoost);
        }

        if (ship.getFluxTracker().isEngineBoostActive() && ship.getFluxLevel() <= fluxThreshold) {
            ship.getMutableStats().getZeroFluxSpeedBoost().modifyFlat(id, speedBoost);
            ship.getEngineController().fadeToOtherColor(this, Engines_color, null, 1f, 0.6f);
            ship.getEngineController().extendFlame(this, 0.4f, 0.4f, 0.4f);
        } else {
            ship.getMutableStats().getZeroFluxSpeedBoost().modifyFlat(id, 0);
        }
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return super.shouldAddDescriptionToTooltip(hullSize, ship, isForModSpec);
    }

    float calculateSpeedBonus(ShipAPI ship) {
        return 20f + ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue() - ship.getMutableStats().getMaxSpeed().getModifiedValue() / 3f;
    }
}

			
			