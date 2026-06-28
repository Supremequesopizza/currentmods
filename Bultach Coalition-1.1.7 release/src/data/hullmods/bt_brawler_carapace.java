package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class bt_brawler_carapace extends BaseHullMod {

    public static final float SUBSYSTEM_HEALTH_BONUS = 100f;
    public static final float EMP_TAKEN_MULT = 0.65f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>(1);

    static {
        BLOCKED_HULLMODS.add("advancedshieldemitter");
        BLOCKED_HULLMODS.add("extendedshieldemitter");
        BLOCKED_HULLMODS.add("frontshield");
		BLOCKED_HULLMODS.add("hardenedshieldemitter");
		BLOCKED_HULLMODS.add("adaptiveshields");
        BLOCKED_HULLMODS.add("stabilizedshieldemitter");
		BLOCKED_HULLMODS.add("shield_shunt");
        BLOCKED_HULLMODS.add("swp_shieldbypass");
    }

	private float check=0;
    private String id, ERROR="IncompatibleHullmodWarning";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEmpDamageTakenMult().modifyMult(id, EMP_TAKEN_MULT);
        stats.getWeaponHealthBonus().modifyPercent(id, SUBSYSTEM_HEALTH_BONUS);
        stats.getEngineHealthBonus().modifyPercent(id, SUBSYSTEM_HEALTH_BONUS);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id){

        if (check>0) {
            check-=1;
            if (check<1){
                ship.getVariant().removeMod(ERROR);
            }
        }

        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                ship.getVariant().addMod(ERROR);
                check=3;
            }
        }
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + "EMP weapons";
        if (index == 1) return "" + "65%";
        if (index == 2) return "" + "shield generators cannot be installed on the vessel";
        return null;
    }
}
