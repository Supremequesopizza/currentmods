package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static Utilities.MHMods_utilities.floatToString;

public class MHMods_FluxBooster extends BaseHullMod {

    final float
            vent_bonus = 25f,
            RFCMult = 0.5f,
            boostDuration = 3f,
            boostDurationSMod = 4f;

    private final Map<HullSize, Integer> maneuverBonusMap = new HashMap<>();

    {
        maneuverBonusMap.put(HullSize.FIGHTER, 50);
        maneuverBonusMap.put(HullSize.FRIGATE, 50);
        maneuverBonusMap.put(HullSize.DESTROYER, 75);
        maneuverBonusMap.put(HullSize.CRUISER, 100);
        maneuverBonusMap.put(HullSize.CAPITAL_SHIP, 125);

    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats.getVariant();
        float mult = 1f;
        if (variant != null && variant.hasHullMod("fluxbreakers")) {
            mult = RFCMult;
        }
        stats.getVentRateMult().modifyPercent(id, vent_bonus * mult);
    }

    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return Math.round(vent_bonus) + "%";
        if (index == 1) return floatToString(vent_bonus * RFCMult) + "%";
        if (index == 2) return maneuverBonusMap.get(HullSize.FRIGATE) + "%";
        if (index == 3) return maneuverBonusMap.get(HullSize.DESTROYER) + "%";
        if (index == 4) return maneuverBonusMap.get(HullSize.CRUISER) + "%";
        if (index == 5) return maneuverBonusMap.get(HullSize.CAPITAL_SHIP) + "%";
        if (index == 6) return String.valueOf(Math.round(boostDuration));
        return null;
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return String.valueOf(Math.round(boostDurationSMod));
        return null;
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;

        float boostDuration = this.boostDuration;
        if (isSMod(ship)) boostDuration = boostDurationSMod;

        float maneuverBonus = maneuverBonusMap.get(ship.getHullSize());
        float timeLeft = boostDuration;
        float effectPower = 0f;


        if (Global.getCombatEngine().getCustomData().get("MHMods_FB_timeLeft" + ship.getId()) instanceof Float)
            timeLeft = (float) Global.getCombatEngine().getCustomData().get("MHMods_FB_timeLeft" + ship.getId());
        if (Global.getCombatEngine().getCustomData().get("MHMods_FB_effectPower" + ship.getId()) instanceof Float)
            effectPower = (float) Global.getCombatEngine().getCustomData().get("MHMods_FB_effectPower" + ship.getId());


        MutableShipStatsAPI stats = ship.getMutableStats();


        if (ship.getFluxLevel() != 0 && ship.getFluxTracker().isVenting()) {
            if (timeLeft > 0) {
                timeLeft -= amount;
                stats.getMaxSpeed().modifyPercent("MHMods_FluxBuster", 1f + maneuverBonus * effectPower);
                stats.getAcceleration().modifyPercent("MHMods_FluxBuster", 1f + maneuverBonus * 2f * effectPower);
                stats.getDeceleration().modifyPercent("MHMods_FluxBuster", 1f + maneuverBonus * 2f * effectPower);
                stats.getTurnAcceleration().modifyPercent("MHMods_FluxBuster", 1f + maneuverBonus * 2f * effectPower);
                stats.getMaxTurnRate().modifyPercent("MHMods_FluxBuster", 1f + maneuverBonus * effectPower);
                ship.getEngineController().fadeToOtherColor(this, new Color(153, 12, 184, 255), null, 0.75f, 0.75f);
                ship.getEngineController().extendFlame(this, 1f, 0, 0.2f);
                if (ship == Global.getCombatEngine().getPlayerShip())
                    Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_FluxBuster", "graphics/icons/hullsys/mhmods_fluxbooster.png", "Booster Power", Math.round(timeLeft) + "", false);
            } else {
                stats.getMaxSpeed().unmodify("MHMods_FluxBuster");
                stats.getAcceleration().unmodify("MHMods_FluxBuster");
                stats.getDeceleration().unmodify("MHMods_FluxBuster");
                stats.getTurnAcceleration().unmodify("MHMods_FluxBuster");
                stats.getMaxTurnRate().unmodify("MHMods_FluxBuster");
            }
        } else {
            timeLeft = boostDuration;
            effectPower = ship.getFluxLevel();
            stats.getMaxSpeed().unmodify("MHMods_FluxBuster");
            stats.getAcceleration().unmodify("MHMods_FluxBuster");
            stats.getDeceleration().unmodify("MHMods_FluxBuster");
            stats.getTurnAcceleration().unmodify("MHMods_FluxBuster");
            stats.getMaxTurnRate().unmodify("MHMods_FluxBuster");
        }

        Global.getCombatEngine().getCustomData().put("MHMods_FB_timeLeft" + ship.getId(), timeLeft);
        Global.getCombatEngine().getCustomData().put("MHMods_FB_effectPower" + ship.getId(), effectPower);

    }
}