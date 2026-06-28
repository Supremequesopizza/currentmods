package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class ReactorHeatEffect implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        float effect_level = (ship.getFluxTracker().getFluxLevel());

        if (effect_level == 0) {
            weapon.getAnimation().setAlphaMult(0f);
            return;
        }


        float wave = (float) Math.cos(engine.getTotalElapsedTime(false) * Math.PI);
        wave *= (float) Math.cos(engine.getTotalElapsedTime(false) * Math.E / 3);

        weapon.getAnimation().setAlphaMult(effect_level * (wave / 4 + 0.66f));
    }
}
