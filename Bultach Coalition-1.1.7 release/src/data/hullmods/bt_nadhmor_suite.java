package data.hullmods;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.Iterator;
import java.util.List;

public class bt_nadhmor_suite extends BaseHullMod {
    float ballisticfluxbonus = 0;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        List<WeaponAPI> weapons = ship.getAllWeapons();
        Iterator weaponchecker = weapons.iterator();
        ballisticfluxbonus = 0f;
        while (weaponchecker.hasNext()) {
            WeaponAPI wepn = (WeaponAPI) weaponchecker.next();
            if (wepn.getSlot().getSlotSize().equals(WeaponAPI.WeaponSize.LARGE) && wepn.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.COMPOSITE)) {
                if (wepn.getType().equals(WeaponAPI.WeaponType.BALLISTIC)) {
                    ballisticfluxbonus = ballisticfluxbonus + wepn.getOriginalSpec().getOrdnancePointCost((MutableCharacterStatsAPI) null, stats);
                }
                if (wepn.getType().equals(WeaponAPI.WeaponType.MISSILE)) {
                    wepn.setMaxAmmo(wepn.getMaxAmmo() / 1);
                }
            }
        }
        stats.getFluxDissipation().modifyFlat(id, ballisticfluxbonus * 8);
    }
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "Composite";
        if (index == 1) return "Ballistic";
        if (index == 2) return (int)ballisticfluxbonus + "";
        if (index == 3) return (int)ballisticfluxbonus*8 + "";
        return null;
    }
}
