package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MHMods_HardlightFocus extends BaseHullMod {

    static final Map<WeaponAPI.WeaponSize, Integer> rangeLimitMap = new HashMap<>();
    static final Map<WeaponAPI.WeaponSize, Float> damageBonusMap = new HashMap<>();

    static {
        rangeLimitMap.put(WeaponAPI.WeaponSize.SMALL, 500);
        rangeLimitMap.put(WeaponAPI.WeaponSize.MEDIUM, 600);
        rangeLimitMap.put(WeaponAPI.WeaponSize.LARGE, 700);

        damageBonusMap.put(WeaponAPI.WeaponSize.SMALL, 15f / 10000);
        damageBonusMap.put(WeaponAPI.WeaponSize.MEDIUM, 10f / 10000);
        damageBonusMap.put(WeaponAPI.WeaponSize.LARGE, 7.5f / 10000);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new MHMods_VoltageRegulationSystemRangeMod());
        ship.addListener(new MHMods_VoltageRegulationSystemDamageModifier());
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return String.valueOf(rangeLimitMap.get(WeaponAPI.WeaponSize.SMALL));
        if (index == 1) return String.valueOf(rangeLimitMap.get(WeaponAPI.WeaponSize.MEDIUM));
        if (index == 2) return String.valueOf(rangeLimitMap.get(WeaponAPI.WeaponSize.LARGE));
        if (index == 3)
            return Misc.getRoundedValueOneAfterDecimalIfNotWhole(damageBonusMap.get(WeaponAPI.WeaponSize.SMALL) * 10000) + "%";
        if (index == 4)
            return Misc.getRoundedValueOneAfterDecimalIfNotWhole(damageBonusMap.get(WeaponAPI.WeaponSize.MEDIUM) * 10000) + "%";
        if (index == 5)
            return Misc.getRoundedValueOneAfterDecimalIfNotWhole(damageBonusMap.get(WeaponAPI.WeaponSize.LARGE) * 10000) + "%";
        if (index == 6) return String.valueOf(100);
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship != null || !isForModSpec) {
            float pad = 3f;
            float opad = 10f;
            Color h = Misc.getHighlightColor();
            Color bad = Misc.getNegativeHighlightColor();

            List<WeaponSpecAPI> weapons = new ArrayList<>();

            tooltip.addSectionHeading("Effects", Alignment.MID, pad);
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (isWeaponLegible(w)) {
                    if (weapons.contains(w.getSpec())) continue;
                    weapons.add(w.getSpec());
                }
            }

            float costW = 125f;
            float nameW = width - costW - 5f;
            tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                    20f, true, true,
                    new Object[]{"Affected weapon", nameW, "Damage bonus", costW});

            for (WeaponSpecAPI w : weapons) {
                Global.getLogger(MHMods_prefabricator.class).info(w.getWeaponName());
                float bonus = calculateBonusDamage(w) * 100f;
                String name = tooltip.shortenString(w.getWeaponName(), nameW - 20f);
                tooltip.addRow(Alignment.LMID, Misc.getTextColor(), name,
                        Alignment.MID, h, Misc.getRoundedValueOneAfterDecimalIfNotWhole(bonus) + "%");
                tooltip.addSpacer(1);
            }
            tooltip.addTable("No affected weapons mounted", 0, opad);
        }
    }

    static class MHMods_VoltageRegulationSystemRangeMod implements WeaponBaseRangeModifier {

        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }

        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.getSlot() == null || !weapon.isBeam()) {
                return 0f;
            }
            return -calculateExcessRange(weapon.getSpec());
        }
    }

    static class MHMods_VoltageRegulationSystemDamageModifier implements DamageDealtModifier {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param instanceof BeamAPI) {
                damage.getModifier().modifyMult("MHMods_VoltageRegulation", 1 + calculateBonusDamage(((BeamAPI) param).getWeapon().getSpec()));
                Global.getLogger(MHMods_HardlightFocus.class).info(1 + calculateBonusDamage(((BeamAPI) param).getWeapon().getSpec()));
                return "MHMods_VoltageRegulation";
            }
            return null;
        }
    }


    static float calculateExcessRange(WeaponSpecAPI weapon) {
        return Math.max(0, weapon.getMaxRange() - rangeLimitMap.get(weapon.getSize()));
    }

    static float calculateBonusDamage(WeaponSpecAPI weapon) {
        return calculateExcessRange(weapon) * damageBonusMap.get(weapon.getSize());
    }

    boolean isWeaponLegible(WeaponAPI weapon) {
        return weapon.isBeam() && weapon.getSpec().getMaxRange() > rangeLimitMap.get(weapon.getSize());
    }


}