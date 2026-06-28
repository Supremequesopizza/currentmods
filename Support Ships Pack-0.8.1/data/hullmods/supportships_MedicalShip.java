package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.EnumSet;

public class supportships_MedicalShip extends BaseHullMod {
    public static final int COSTREDUCE = -6;


    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyFlat(id,COSTREDUCE);
        stats.getDynamic().getMod("deployment_points_mod").modifyFlat(id, COSTREDUCE);
        stats.getNumFighterBays().modifyFlat(id,-2);
        stats.removeListenerOfClass(supportships_MedicalShip.MedicalShipListener.class);
        stats.addListener(new supportships_MedicalShip.MedicalShipListener());
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "Installed";
        } else if (index == 1) {
            return "Point Defence";
        } else if (index == 2) {
            return "Ambulance wings";
        } else if (index == 3) {
            return "6";
        } else {
            return index == 4 ? "Remove this hullmod to uninstall the chip." : null;
        }
    }
    public static class MedicalShipListener implements WeaponOPCostModifier {
        public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
            String role = weapon.getPrimaryRoleStr();
            if (role == null || role.length() <= 0 || !role.contains("Point Defense") && !role.contains("Anti Fighter") && !role.contains("Anti Small Craft")) {
                EnumSet<AIHints> hints = weapon.getAIHints();
                return hints == null || hints.size() <= 0 || !hints.contains(AIHints.PD) && !hints.contains(AIHints.ANTI_FTR) ? 99999 : currCost;
            } else {
                return currCost;
            }
        }
    }
    @Override
    public boolean affectsOPCosts() {
        return true;
    }
}
