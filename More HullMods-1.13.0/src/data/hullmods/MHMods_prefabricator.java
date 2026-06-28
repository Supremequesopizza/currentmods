package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MHMods_prefabricator extends mhmods_baseSHmod {

    private final Map<HullSize, Integer> fabricationTime = new HashMap<>();

    float ammoregenSmod = 0.35f;

    {
        fabricationTime.put(HullSize.FIGHTER, 155);
        fabricationTime.put(HullSize.FRIGATE, 155);
        fabricationTime.put(HullSize.DESTROYER, 255);
        fabricationTime.put(HullSize.CRUISER, 330);
        fabricationTime.put(HullSize.CAPITAL_SHIP, 510);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return String.valueOf(fabricationTime.get(HullSize.FRIGATE));
        if (index == 1) return String.valueOf(fabricationTime.get(HullSize.DESTROYER));
        if (index == 2) return String.valueOf(fabricationTime.get(HullSize.CRUISER));
        if (index == 3) return String.valueOf(fabricationTime.get(HullSize.CAPITAL_SHIP));
        if (index == 4) return "removes";
        return null;
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round((1 - ammoregenSmod) * 100) + "%";
        return null;
    }

    boolean makeCurrentRed = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        /*
        if (!ship.isAlive()) return;
        if (ship.getFullTimeDeployed() >= 0.5f) return;

        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        String id = ship.getId();

        if (customCombatData.get("MHMods_prefabricator" + id) instanceof Boolean) return;

        for (WeaponAPI w : ship.getAllWeapons()) {
            float reloadRate = w.getAmmoPerSecond();
            if (w.getType() == WeaponType.MISSILE && !w.getSlot().isBuiltIn() && w.usesAmmo() && reloadRate > 0) {
                float ammo = w.getSpec().getAmmoPerSecond() * fabricationTime.get(ship.getHullSize()) * ship.getMutableStats().getMissileAmmoRegenMult().getModifiedValue();
                w.setMaxAmmo(w.getMaxAmmo() + Math.round(ammo));
                w.getAmmoTracker().setAmmo(w.getAmmo() + Math.round(ammo));

                if (isSMod(ship)) w.getAmmoTracker().setAmmoPerSecond(w.getSpec().getAmmoPerSecond() * ammoregenSmod);
                else w.getAmmoTracker().setAmmoPerSecond(0);
            }
        }

        customCombatData.put("MHMods_prefabricator" + id, true);

         */
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        makeCurrentRed = true;
        if (ship.getOriginalOwner() == -1) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (isWeaponLegible(w)) {
                    makeCurrentRed = false;
                }
            }
        }
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (isWeaponLegible(w)) {
                float ammo = calculateAdditionalAmmo(w, ship);
                w.setMaxAmmo(w.getMaxAmmo() + Math.round(ammo));
                w.getAmmoTracker().setAmmo(w.getAmmo() + Math.round(ammo));

                if (isSMod(ship)) w.getAmmoTracker().setAmmoPerSecond(w.getSpec().getAmmoPerSecond() * ammoregenSmod);
                else w.getAmmoTracker().setAmmoPerSecond(0);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship != null) {
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

            weapons.sort((o1, o2) -> {
                float c1 = calculateAdditionalAmmo(o1, ship);
                float c2 = calculateAdditionalAmmo(o2, ship);
                return (int) Math.signum(c1 - c2);
            });
            float costW = 100f;
            float nameW = width - costW - 5f;
            tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                    20f, true, true,
                    new Object [] {"Affected weapon", nameW, "Ammo added", costW});

            for (WeaponSpecAPI w : weapons) {
                Global.getLogger(MHMods_prefabricator.class).info(w.getWeaponName());
                int cost = calculateAdditionalAmmo(w, ship);
                String name = tooltip.shortenString(w.getWeaponName(), nameW - 20f);
                tooltip.addRow(Alignment.LMID, Misc.getTextColor(), name,
                        Alignment.MID, h, Misc.getRoundedValueOneAfterDecimalIfNotWhole(cost));
                tooltip.addSpacer(1);
            }
            tooltip.addTable("No affected weapons mounted", 0, opad);
        }
    }

    int calculateAdditionalAmmo(WeaponAPI w, ShipAPI ship) {
        float ammo = w.getSpec().getAmmoPerSecond() * fabricationTime.get(ship.getHullSize()) * ship.getMutableStats().getMissileAmmoRegenMult().getModifiedValue();
        ammo = (float) Math.ceil(ammo / w.getSpec().getReloadSize()) * w.getSpec().getReloadSize();
        return Math.round(ammo);
    }

    int calculateAdditionalAmmo(WeaponSpecAPI w, ShipAPI ship) {
        float ammo = w.getAmmoPerSecond() * fabricationTime.get(ship.getHullSize()) * ship.getMutableStats().getMissileAmmoRegenMult().getModifiedValue();
        ammo = (float) Math.ceil(ammo / w.getReloadSize()) * w.getReloadSize();
        return Math.round(ammo);
    }

    boolean isWeaponLegible(WeaponAPI w){
        return (w.getType() == WeaponType.MISSILE && !w.getSlot().isBuiltIn() && w.usesAmmo() && w.getSpec().getAmmoPerSecond() > 0);
    }

    @Override
    public Color getNameColor() {
        if (makeCurrentRed) return Color.RED;
        return super.getNameColor();
    }
}
