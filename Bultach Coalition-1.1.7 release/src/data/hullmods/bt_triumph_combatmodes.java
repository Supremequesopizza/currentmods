package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

//stolen and modified from HTE

public class bt_triumph_combatmodes extends BaseHullMod {

    private static final Map<ShipAPI, ShipTracker> shipTrackerMap = new HashMap<>();
    private static final float EFFECT_RAMP_TIME = 2.0f;
    private static final String ID = "combat_modes";

    // CRUISE Mode
    public static final String CRUISE_MODE_NAME = "Cruise Mode";
    private static final float CRUISE_SPEED_BONUS = 50f;
    private static final float CRUISE_MANEUVER_BONUS = 50f;
    private static final float CRUISE_SHIELD_TURN_RATE_PENALTY = 0.65f;
    private static final float CRUISE_SHIELD_EFFICIENCY_DEBUFF = 0.3f;
    private static final float CRUISE_ARMOR_VULNERABILITY = 1.35f;
    private static final float CRUISE_RANGE_REDUCTION_MULT = 0.65f;
    private static final float CRUISE_FIGHTER_REPLACEMENT_MULT = 2.0f;

    // BATTLE Mode
    public static final String BATTLE_MODE_NAME = "Battle Mode";
    private static final float BATTLE_ARMOR_RESISTANCE = 0.85f;
    private static final float BATTLE_SHIELD_EFFICIENCY_BUFF = 0.1f;
    private static final float BATTLE_SHIELD_TURN_RATE_BUFF = 1.5f;
    private static final float BATTLE_ROF_MULT = 1.75f;
    private static final float BATTLE_FLUX_DISCOUNT_MULT = 0.75f;

    private static int storedHashCode = 0;

    private enum CombatMode {
        CRUISE,
        BATTLE
    }

    private static class ShipTracker {
        public float cruiseEffectLevel = 0f;
        public float battleEffectLevel = 0f;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused() || ship.isHulk()) {
            return;
        }

        if (Global.getCombatEngine().hashCode() != storedHashCode) {
            shipTrackerMap.clear();
            storedHashCode = Global.getCombatEngine().hashCode();
        }

        shipTrackerMap.computeIfAbsent(ship, k -> new ShipTracker());

        ShipSystemAPI system = ship.getSystem();
        MutableShipStatsAPI stats = ship.getMutableStats();
        ShipTracker tracker = shipTrackerMap.get(ship);

        if (system == null) return;

        CombatMode currentMode = getMode(system);
        if (currentMode == null) return;

        float ramp = amount / EFFECT_RAMP_TIME;
        if (currentMode == CombatMode.CRUISE) {
            tracker.cruiseEffectLevel = Math.min(1f, tracker.cruiseEffectLevel + ramp);
            tracker.battleEffectLevel = Math.max(0f, tracker.battleEffectLevel - ramp);
        } else {
            tracker.cruiseEffectLevel = Math.max(0f, tracker.cruiseEffectLevel - ramp);
            tracker.battleEffectLevel = Math.min(1f, tracker.battleEffectLevel + ramp);
        }

        float cruiseEffect = tracker.cruiseEffectLevel;
        float battleEffect = tracker.battleEffectLevel;


        if (cruiseEffect > 0) {
            stats.getMaxSpeed().modifyFlat(ID, CRUISE_SPEED_BONUS * cruiseEffect);
            stats.getAcceleration().modifyPercent(ID, CRUISE_MANEUVER_BONUS * cruiseEffect);
            stats.getDeceleration().modifyPercent(ID, CRUISE_MANEUVER_BONUS * cruiseEffect);
            stats.getTurnAcceleration().modifyPercent(ID, CRUISE_MANEUVER_BONUS * cruiseEffect);
            stats.getMaxTurnRate().modifyPercent(ID, CRUISE_MANEUVER_BONUS * cruiseEffect);

            stats.getShieldTurnRateMult().modifyMult(ID, CRUISE_SHIELD_TURN_RATE_PENALTY + (1f - CRUISE_SHIELD_TURN_RATE_PENALTY) * (1f - cruiseEffect));
            stats.getShieldDamageTakenMult().modifyMult(ID + "_cruise", 1f + CRUISE_SHIELD_EFFICIENCY_DEBUFF * cruiseEffect);
            stats.getArmorDamageTakenMult().modifyMult(ID + "_cruise", 1f + (CRUISE_ARMOR_VULNERABILITY - 1f) * cruiseEffect);

            stats.getBallisticWeaponRangeBonus().modifyMult(ID, 1f - (1f - CRUISE_RANGE_REDUCTION_MULT) * cruiseEffect);
            stats.getEnergyWeaponRangeBonus().modifyMult(ID, 1f - (1f - CRUISE_RANGE_REDUCTION_MULT) * cruiseEffect);

            stats.getFighterRefitTimeMult().modifyMult(ID, 1f + (CRUISE_FIGHTER_REPLACEMENT_MULT - 1f) * cruiseEffect);
        }


        if (battleEffect > 0) {
            stats.getArmorDamageTakenMult().modifyMult(ID + "_battle", 1f - (1f - BATTLE_ARMOR_RESISTANCE) * battleEffect);
            stats.getShieldDamageTakenMult().modifyMult(ID + "_battle", 1f - BATTLE_SHIELD_EFFICIENCY_BUFF * battleEffect);
            stats.getShieldTurnRateMult().modifyMult(ID + "_battle", 1f + (BATTLE_SHIELD_TURN_RATE_BUFF - 1f) * battleEffect);

            stats.getBallisticRoFMult().modifyMult(ID, 1f + (BATTLE_ROF_MULT - 1f) * battleEffect);
            stats.getEnergyRoFMult().modifyMult(ID, 1f + (BATTLE_ROF_MULT - 1f) * battleEffect);

            stats.getBallisticWeaponFluxCostMod().modifyMult(ID, 1f - (1f - BATTLE_FLUX_DISCOUNT_MULT) * battleEffect);
            stats.getEnergyWeaponFluxCostMod().modifyMult(ID, 1f - (1f - BATTLE_FLUX_DISCOUNT_MULT) * battleEffect);
        }


        if (ship == Global.getCombatEngine().getPlayerShip()) {
            String modeName = (currentMode == CombatMode.CRUISE) ? CRUISE_MODE_NAME : BATTLE_MODE_NAME;
            String statusText = (currentMode == CombatMode.CRUISE) ? "Mobility systems prioritized" : "Combat systems amplified";
            Global.getCombatEngine().maintainStatusForPlayerShip(ID, "graphics/icons/hullsys/maneuvering_jets.png", modeName, statusText, false);
        }
    }

    private CombatMode getMode(ShipSystemAPI system) {
        return (system.getAmmo() == 1) ? CombatMode.CRUISE : CombatMode.BATTLE;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 10f;
        tooltip.addSectionHeading("Modes", Alignment.MID, pad);

        tooltip.addPara("• " + CRUISE_MODE_NAME, Color.CYAN, pad);
        tooltip.addPara("  +%s top speed, +%s maneuverability", 0, Color.GREEN,
                (int) CRUISE_SPEED_BONUS + "", (int) CRUISE_MANEUVER_BONUS + "%%");
        tooltip.addPara("  -35%% shield turn rate, +30%% shield damage taken, +35%% armor damage taken", 3f, Color.RED,
                "35%%", "30%%", "35%%");
        tooltip.addPara("  -35%% weapon range, +100%% fighter replacement time", 3f, Color.RED,
                "35%%", "100%%");

        tooltip.addPara("• " + BATTLE_MODE_NAME, Color.ORANGE, pad);
        tooltip.addPara("  -15%% armor damage taken, -10%% shield damage taken, +50%% shield turn rate", 0, Color.GREEN,
                "15%%", "10%%", "50%%");
        tooltip.addPara("  +75%% rate of fire (ballistic & energy), -25%% flux cost", 3f, Color.GREEN,
                "75%%", "25%%");
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return (int) EFFECT_RAMP_TIME + " seconds";
        return null;
    }
}
