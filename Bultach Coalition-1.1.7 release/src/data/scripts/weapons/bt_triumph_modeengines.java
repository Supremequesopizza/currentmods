package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.hullmods.bt_triumph_combatmodes;

import java.util.ArrayList;
import java.util.List;

public class bt_triumph_modeengines implements EveryFrameWeaponEffectPlugin {

    private boolean initialized = false;
    private final List<ShipEngineControllerAPI.ShipEngineAPI> decoEngines = new ArrayList<>();
    private float flameLevel = 0f;

    private static final String ENGINE_ID_PREFIX = "deco_travel";
    private static final float FADE_SPEED = 1.5f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon.getShip() == null) return;

        ShipAPI ship = weapon.getShip();
        ShipSystemAPI system = ship.getSystem();
        if (system == null) return;

        if (!initialized) {
            for (ShipEngineControllerAPI.ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                if (e.getStyleId() != null && e.getStyleId().startsWith(ENGINE_ID_PREFIX)) {
                    decoEngines.add(e);
                }
            }
            initialized = true;
        }

        boolean cruiseActive = (system.getAmmo() == 1);

        float target = cruiseActive ? 1f : 0f;
        if (flameLevel < target) {
            flameLevel = Math.min(target, flameLevel + FADE_SPEED * amount);
        } else if (flameLevel > target) {
            flameLevel = Math.max(target, flameLevel - FADE_SPEED * amount);
        }

        for (ShipEngineControllerAPI.ShipEngineAPI e : decoEngines) {
            ship.getEngineController().setFlameLevel(e.getEngineSlot(), flameLevel);
        }
    }
}
