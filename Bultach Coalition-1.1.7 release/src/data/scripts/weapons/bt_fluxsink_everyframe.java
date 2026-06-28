package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class bt_fluxsink_everyframe implements EveryFrameWeaponEffectPlugin {


    private static final float USE_THRESHOLD = 0.5f;
    private static final float FLUX_REDUCE = 1000f;

    @Override
    public
    void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();

        // Check if the weapon should fire based on ship's flux level
        if (ship.getFluxTracker().getFluxLevel() >= USE_THRESHOLD && weapon.getCooldownRemaining() <= 0 && weapon.getAmmo() > 0) {
            weapon.setForceFireOneFrame(true);
        } else {
            weapon.setForceNoFireOneFrame(true);
        }
    }
}