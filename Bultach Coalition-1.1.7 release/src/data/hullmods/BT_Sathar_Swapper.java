package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// this should work properly now, but is a bit scuffed
public class BT_Sathar_Swapper extends BaseHullMod {

    public static final String WEAPON_SLOT = "WS0001";
    public static final String WEAPON_SLOT_2 = "WS0002";
    public static final String WEAPON_PREFIX = "ork_sathar_";
    public static final String HULLMOD_PREFIX = "bt_sathar_mode_";
    public static final String TAG_PREFIX = "bt_sathar_tag_";

    public static final Map<String, String> LOADOUT_CYCLE = new HashMap<>();
    static {
        LOADOUT_CYCLE.put("siege_laser", "gigashotgun");
        LOADOUT_CYCLE.put("gigashotgun", "emp_nuke");
        LOADOUT_CYCLE.put("emp_nuke", "doom_laser");
        LOADOUT_CYCLE.put("doom_laser", "siege_laser");
    }

    public static final String[] WEAPON_POOL = { "siege_laser", "gigashotgun", "emp_nuke", "doom_laser" };
    private static final Random rand = new Random();

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getEntity() == null) {
            return;
        }

        ShipVariantAPI variant = stats.getVariant();
        String currentMode = findCurrentModeFromHullmods(variant);

        if (currentMode != null) {
            updateTags(variant, currentMode);
            return;
        }

        String lastMode = findCurrentModeFromTags(variant);
        String newMode;

        if (lastMode != null && LOADOUT_CYCLE.containsKey(lastMode)) {
            newMode = LOADOUT_CYCLE.get(lastMode);
        } else {
            newMode = WEAPON_POOL[rand.nextInt(WEAPON_POOL.length)];
        }

        if (newMode != null) {
            updateTags(variant, newMode);
            variant.addMod(HULLMOD_PREFIX + newMode);
            variant.clearSlot(WEAPON_SLOT);
            variant.clearSlot(WEAPON_SLOT_2);
            variant.addWeapon(WEAPON_SLOT, WEAPON_PREFIX + newMode);
            variant.addWeapon(WEAPON_SLOT_2, WEAPON_PREFIX + newMode);
        }
    }

    private void updateTags(ShipVariantAPI variant, String newMode) {
        List<String> toRemove = new ArrayList<>();
        for (String tag : variant.getTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            variant.removeTag(tag);
        }
        variant.addTag(TAG_PREFIX + newMode);
    }

    private String findCurrentModeFromTags(ShipVariantAPI variant) {
        for (String tag : variant.getTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                return tag.replace(TAG_PREFIX, "");
            }
        }
        return null;
    }

    private String findCurrentModeFromHullmods(ShipVariantAPI variant) {
        for (String mode : LOADOUT_CYCLE.keySet()) {
            if (variant.hasHullMod(HULLMOD_PREFIX + mode)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getOriginalOwner() < 0) {
            if (Global.getSector() != null &&
                    Global.getSector().getPlayerFleet() != null &&
                    Global.getSector().getPlayerFleet().getCargo() != null &&
                    Global.getSector().getPlayerFleet().getCargo().getStacksCopy() != null &&
                    !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()) {

                for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
                    if (s.isWeaponStack() && s.getWeaponSpecIfWeapon().getWeaponId().startsWith(WEAPON_PREFIX)) {
                        Global.getSector().getPlayerFleet().getCargo().removeStack(s);
                    }
                }
            }
        }
    }

    @Override
    public int getDisplayCategoryIndex() {
        return 2;
    }
}