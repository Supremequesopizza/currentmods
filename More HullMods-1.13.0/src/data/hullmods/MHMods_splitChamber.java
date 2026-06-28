package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;

import static Utilities.mhmods_eneableSmod.getEnable;

public class MHMods_splitChamber extends mhmods_baseSHmod {

    final float fireRate = 100f;
    final float damage = 40f;
    final float flux = 50f;

    final float fireRateSmod = 150f;
    final float damageSmod = 50f;
    final float fluxSmod = 60f;

    {
        id = "mhmods_splitChamber";
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float fireRate = this.fireRate;
        float damage = this.damage;
        float flux = this.flux;
        //Smod stats part
        if (stats.getVariant().getSMods().contains(id) && getEnable()) {
            fireRate = fireRateSmod;
            damage = damageSmod;
            flux = fluxSmod;
        }

        stats.getEnergyRoFMult().modifyMult(id, 1 + fireRate * 0.01f);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1 - flux * 0.01f);
        stats.getEnergyWeaponDamageMult().modifyMult(id, 1 - damage * 0.01f);
        stats.getEnergyAmmoBonus().modifyMult(id, 1 + fireRate * 0.01f);
        stats.getEnergyAmmoRegenMult().modifyMult(id, 1 + fireRate * 0.01f);

        stats.getBallisticRoFMult().modifyMult(id, 1 + fireRate * 0.01f);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1 - flux * 0.01f);
        stats.getBallisticWeaponDamageMult().modifyMult(id, 1 - damage * 0.01f);
        stats.getBallisticAmmoBonus().modifyMult(id, 1 + fireRate * 0.01f);
        stats.getBallisticAmmoRegenMult().modifyMult(id, 1 + fireRate * 0.01f);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(fireRate) + "%";
        if (index == 1) return Math.round(flux) + "%";
        if (index == 2) return Math.round(damage) + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (!getEnable()) return;
        if (!Keyboard.isKeyDown(Keyboard.getKeyIndex("F1")) && (ship == null || !ship.getVariant().getSMods().contains(id))){
            tooltip.addPara("Hold F1 to show S-mod effect info", Misc.getGrayColor(), 10);
            return;
        }
        Color labelColor = Misc.getTextColor();
        Color s = Misc.getStoryOptionColor();
        if (ship == null || !ship.getVariant().getSMods().contains(id)) {
            tooltip.addSectionHeading("Effect if S-modded", Alignment.MID, pad);
            labelColor = Misc.getGrayColor();
        }
        HullModSpecAPI hullmod = Global.getSettings().getHullModSpec(id);
        LabelAPI label = tooltip.addPara(hullmod.getDescriptionFormat(), pad, labelColor, labelColor, Math.round(fireRateSmod) + "%", Math.round(fluxSmod) + "%", Math.round(damageSmod) + "%");
        label.setHighlight(Math.round(fireRateSmod) + "%", Math.round(fluxSmod) + "%", Math.round(damageSmod) + "%");
        label.setHighlightColors(s, s, s);
        //tooltip.addPara(hullmod.getDescriptionFormat(), 10, Misc.getGrayColor(), Misc.getPositiveHighlightColor(), Math.round(fireRateSmod) + "%", Math.round(fluxSmod) + "%", Math.round(damageSmod) + "%");
    }
}
