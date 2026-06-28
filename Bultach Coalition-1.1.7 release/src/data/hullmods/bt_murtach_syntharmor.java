package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicUI;

import java.util.HashSet;
import java.util.Set;
import java.awt.Color;

public class bt_murtach_syntharmor extends BaseHullMod {

    public static final float SUBSYSTEM_HEALTH_BONUS = 100f;
    public static final float EMP_TAKEN_MULT = 0.65f;
    public static final float BEAM_DAMAGE_TAKEN_MULT = 0.70f;
    public static final float WEAPON_REPAIR_RATE_BONUS_PERCENT = 25f;
    private static final float WEAPON_REPAIR_TIME_MULT = 1f / (1f + WEAPON_REPAIR_RATE_BONUS_PERCENT / 100f);
    public static final float HULL_REPAIR_PER_SECOND = 125f;
    public static final float ARMOR_REPAIR_FRACTION_OF_TOTAL_MAX_PER_SECOND = 0.01f;

    public static final float MAX_HULL_REPAIR_CAP_PERCENTAGE = 1f;
    public static final float MAX_ARMOR_REPAIR_CAP_PERCENTAGE = 1f;

    private static final float REPAIR_INTERVAL = 0.2f;
    private float repairIntervalTimer = 0f;

    private static final String HULLMOD_ID_PREFIX = "bt_murtach_syntharmor_";
    private static final String HULL_REPAIRED_KEY = HULLMOD_ID_PREFIX + "hull_repaired_this_combat";
    private static final String ARMOR_REPAIRED_KEY = HULLMOD_ID_PREFIX + "armor_repaired_this_combat";
    private static final String COMBAT_INIT_KEY = HULLMOD_ID_PREFIX + "combat_initialized";

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static {
        BLOCKED_HULLMODS.add("advancedshieldemitter");
        BLOCKED_HULLMODS.add("extendedshieldemitter");
        BLOCKED_HULLMODS.add("frontshield");
        BLOCKED_HULLMODS.add("hardenedshieldemitter");
        BLOCKED_HULLMODS.add("adaptiveshields");
        BLOCKED_HULLMODS.add("stabilizedshieldemitter");
        BLOCKED_HULLMODS.add("shield_shunt");
        BLOCKED_HULLMODS.add("swp_shieldbypass");
        BLOCKED_HULLMODS.add("frontconversion");
        BLOCKED_HULLMODS.add("omnishield");
        BLOCKED_HULLMODS.add("safetyoverrides");
        BLOCKED_HULLMODS.add("fragment_swarm");
        BLOCKED_HULLMODS.add("secondary_fabricator");
        BLOCKED_HULLMODS.add("fragment_coordinator");
        BLOCKED_HULLMODS.add("shrouded_mantle");
        BLOCKED_HULLMODS.add("shrouded_thunderhead");
        BLOCKED_HULLMODS.add("phase_anchor");
        BLOCKED_HULLMODS.add("shrouded_lens");
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEmpDamageTakenMult().modifyMult(id, EMP_TAKEN_MULT);
        stats.getWeaponHealthBonus().modifyPercent(id, SUBSYSTEM_HEALTH_BONUS);
        stats.getEngineHealthBonus().modifyPercent(id, SUBSYSTEM_HEALTH_BONUS);
        stats.getBeamDamageTakenMult().modifyMult(id, BEAM_DAMAGE_TAKEN_MULT);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, WEAPON_REPAIR_TIME_MULT);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine == null || ship == null || !ship.isAlive()) {
            return;
        }

        if (engine.isPaused() || engine.isCombatOver()){
            return;
        }

        if (!ship.getCustomData().containsKey(COMBAT_INIT_KEY)) {
            ship.getCustomData().put(COMBAT_INIT_KEY, true);
            ship.getCustomData().put(HULL_REPAIRED_KEY, 0f);
            ship.getCustomData().put(ARMOR_REPAIRED_KEY, 0f);
        }

        Float hullRepairedFloat = (Float) ship.getCustomData().get(HULL_REPAIRED_KEY);
        float hullRepairedThisCombat = (hullRepairedFloat != null) ? hullRepairedFloat : 0f;

        Float armorRepairedFloat = (Float) ship.getCustomData().get(ARMOR_REPAIRED_KEY);
        float armorRepairedThisCombat = (armorRepairedFloat != null) ? armorRepairedFloat : 0f;

        repairIntervalTimer += amount;
        if (repairIntervalTimer >= REPAIR_INTERVAL) {
            float effectiveInterval = repairIntervalTimer;
            repairIntervalTimer = 0f;

            float maxHullRepairThisCombatCap = ship.getMaxHitpoints() * MAX_HULL_REPAIR_CAP_PERCENTAGE;
            if (hullRepairedThisCombat < maxHullRepairThisCombatCap && ship.getHitpoints() < ship.getMaxHitpoints()) {
                float potentialHullRepair = HULL_REPAIR_PER_SECOND * effectiveInterval;
                float allowedHullRepair = maxHullRepairThisCombatCap - hullRepairedThisCombat;
                float actualHullRepair = Math.min(potentialHullRepair, allowedHullRepair);
                actualHullRepair = Math.min(actualHullRepair, ship.getMaxHitpoints() - ship.getHitpoints());

                if (actualHullRepair > 0) {
                    ship.setHitpoints(ship.getHitpoints() + actualHullRepair);
                    hullRepairedThisCombat += actualHullRepair;
                    ship.getCustomData().put(HULL_REPAIRED_KEY, hullRepairedThisCombat);
                }
            }

            ArmorGridAPI armorGrid = ship.getArmorGrid();
            float maxArmorInCell = armorGrid.getMaxArmorInCell();
            int gridWidth = armorGrid.getGrid().length;
            int gridHeight = armorGrid.getGrid()[0].length;
            float totalMaxArmorOnGrid = (float)gridWidth * gridHeight * maxArmorInCell;
            float maxArmorRepairThisCombatCap = totalMaxArmorOnGrid * MAX_ARMOR_REPAIR_CAP_PERCENTAGE;

            if (armorRepairedThisCombat < maxArmorRepairThisCombatCap && totalMaxArmorOnGrid > 0) {
                float potentialTotalArmorToRepairThisInterval = totalMaxArmorOnGrid * ARMOR_REPAIR_FRACTION_OF_TOTAL_MAX_PER_SECOND * effectiveInterval;
                float allowedTotalArmorToRepair = maxArmorRepairThisCombatCap - armorRepairedThisCombat;
                float actualTotalArmorToDistributeThisInterval = Math.min(potentialTotalArmorToRepairThisInterval, allowedTotalArmorToRepair);

                float armorActuallyRepairedThisPass = 0f;
                int cellsToAttemptRepair = 5;

                for (int i = 0; i < cellsToAttemptRepair && actualTotalArmorToDistributeThisInterval > 0.01f; i++) {
                    int x = MathUtils.getRandom().nextInt(gridWidth);
                    int y = MathUtils.getRandom().nextInt(gridHeight);

                    float currentCellArmor = armorGrid.getArmorValue(x, y);
                    if (currentCellArmor < maxArmorInCell) {
                        float maxPossibleRepairForCell = maxArmorInCell - currentCellArmor;
                        float budgetPerCellAttempt = actualTotalArmorToDistributeThisInterval / Math.max(1, (cellsToAttemptRepair - i));
                        float repairAmountForCell = Math.min(budgetPerCellAttempt, maxArmorInCell * 0.05f);
                        repairAmountForCell = Math.min(repairAmountForCell, maxPossibleRepairForCell);

                        if (repairAmountForCell > 0.01f) {
                            armorGrid.setArmorValue(x, y, currentCellArmor + repairAmountForCell);
                            actualTotalArmorToDistributeThisInterval -= repairAmountForCell;
                            armorActuallyRepairedThisPass += repairAmountForCell;
                        }
                    }
                }
                if (armorActuallyRepairedThisPass > 0) {
                    armorRepairedThisCombat += armorActuallyRepairedThisPass;
                    ship.getCustomData().put(ARMOR_REPAIRED_KEY, armorRepairedThisCombat);
                }
            }
        }

        if (ship == engine.getPlayerShip() && !engine.isCombatOver()) {
            ArmorGridAPI armorGrid = ship.getArmorGrid();
            float totalMaxArmor = armorGrid.getMaxArmorInCell() * armorGrid.getGrid().length * armorGrid.getGrid()[0].length;
            float maxArmorCap = totalMaxArmor * MAX_ARMOR_REPAIR_CAP_PERCENTAGE;
            float armorReserveFill = (maxArmorCap > 0) ? 1f - (armorRepairedThisCombat / maxArmorCap) : 0f;
            int armorReservePercent = (int) (armorReserveFill * 100f);

            MagicUI.drawInterfaceStatusBar(
                    ship,
                    "syntharmor_armor_bar",
                    armorReserveFill,
                    new Color(51, 255, 68, 200),
                    null,
                    0f,
                    "RG-CAP",
                    armorReservePercent
            );
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String blockedModId : BLOCKED_HULLMODS) {
            if (ship.getVariant().hasHullMod(blockedModId)) {
                ship.getVariant().removeMod(blockedModId);
                Global.getLogger(bt_murtach_syntharmor.class).info(String.format(
                        "bt_murtach_syntharmor removed incompatible hullmod '%s' from ship '%s'",
                        blockedModId, ship.getHullSpec().getHullName())
                );
            }
        }
    }

    private static final Color POSITIVE_COLOR = Misc.getPositiveHighlightColor();
    private static final Color NEGATIVE_COLOR = Misc.getNegativeHighlightColor();
    private static final Color GRAY_COLOR = new Color(255, 210, 0, 255);
    private static final Color QUOTE_COLOR = new Color(190, 89, 255, 255);

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) ((1f - EMP_TAKEN_MULT) * 100f) + "%";
        if (index == 1) return "" + (int) SUBSYSTEM_HEALTH_BONUS + "%";
        if (index == 2) return "" + (int) ((1f - BEAM_DAMAGE_TAKEN_MULT) * 100f) + "%";
        if (index == 3) return "" + (int) WEAPON_REPAIR_RATE_BONUS_PERCENT + "%";
        if (index == 4) return "" + (int) HULL_REPAIR_PER_SECOND;
        if (index == 5) return "" + (int)(MAX_HULL_REPAIR_CAP_PERCENTAGE * 100f) + "%";
        if (index == 6) return "" + (int)(MAX_ARMOR_REPAIR_CAP_PERCENTAGE * 100f) + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;

        tooltip.addPara("Advanced bio-synthetic plating provides exceptional resilience and allows for self repair in combat.", opad);

        tooltip.addSectionHeading("Shelter", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), com.fs.starfarer.api.ui.Alignment.MID, opad);

        tooltip.addPara("Reduces incoming EMP damage by %s.", pad, POSITIVE_COLOR,
                (int) ((1f - EMP_TAKEN_MULT) * 100f) + "%");
        tooltip.addPara("Reduces damage taken from beam weapons by %s.", pad, POSITIVE_COLOR,
                (int) ((1f - BEAM_DAMAGE_TAKEN_MULT) * 100f) + "%");
        tooltip.addPara("Increases weapon and engine hitpoints by %s.", pad, POSITIVE_COLOR,
                (int) SUBSYSTEM_HEALTH_BONUS + "%");

        tooltip.addSectionHeading("Bio-synthetic Regrowth", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), com.fs.starfarer.api.ui.Alignment.MID, opad);

        tooltip.addPara("Increases the combat repair rate of disabled weapon systems by %s.", pad, POSITIVE_COLOR,
                (int) WEAPON_REPAIR_RATE_BONUS_PERCENT + "%");
        tooltip.addPara("Slowly regenerates hull integrity at a rate of %s points per second in combat.", pad, POSITIVE_COLOR,
                String.valueOf((int) HULL_REPAIR_PER_SECOND));

        String repairPercentStr = String.format("%.2f%%", ARMOR_REPAIR_FRACTION_OF_TOTAL_MAX_PER_SECOND * 100f);
        tooltip.addPara("Gradually restores damaged armor plating across the grid in combat, repairing approximately %s of the ship's total maximum armor per second.",
                pad, POSITIVE_COLOR, repairPercentStr);

        String hullCapStr = String.format("%d%%", (int)(MAX_HULL_REPAIR_CAP_PERCENTAGE * 100f));
        String armorCapStr = String.format("%d%%", (int)(MAX_ARMOR_REPAIR_CAP_PERCENTAGE * 10f));
        tooltip.addPara("Total armor and hull integrity regenerated is capped at %s of maximum values per engagement.", opad, GRAY_COLOR, hullCapStr);
        tooltip.addPara("The armor capacity can be refreshed %s through perfectly timing system exit.", pad, GRAY_COLOR, armorCapStr);

        tooltip.addSectionHeading("System Limitations", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), com.fs.starfarer.api.ui.Alignment.MID, opad);
        tooltip.addPara("The integrated bio-mechanical systems and unique phase technology are fundamentally incompatible with all forms of shield generation technology. Any shield emitters or converters will be actively disabled.", pad, NEGATIVE_COLOR);

        tooltip.addPara("Attempting to install Safety Overrides is outright rejected by the vessel's bio-intelligence.", opad, NEGATIVE_COLOR);

        String playerName = Global.getSector().getPlayerPerson().getNameString();
        if (playerName == null || playerName.isEmpty()) {
            playerName = "Pilot";
        }

        String personalizedQuote = String.format("\"As much as I care for your judgement, %s, I suspect you too would find an unending heart attack unenjoyable.\"", playerName);
        tooltip.addPara(personalizedQuote, QUOTE_COLOR, opad);
    }
}