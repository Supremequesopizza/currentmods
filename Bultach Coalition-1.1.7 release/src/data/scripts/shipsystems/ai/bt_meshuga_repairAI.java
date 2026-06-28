package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class bt_meshuga_repairAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private final IntervalUtil timer = new IntervalUtil(0.5f, 1.5f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (ship.getShipAI() == null || system == null) {
            return;
        }

        if (system.isActive() || system.isCoolingDown()) {
            return;
        }
        
        timer.advance(amount);

        if (timer.intervalElapsed()) {
            boolean shouldUseSystem = false;

            float hullLevel = ship.getHitpoints() / ship.getMaxHitpoints();
            if (hullLevel < 0.85f) {
                shouldUseSystem = true;
            }

            if (!shouldUseSystem) {
                float maxArmor = ship.getHullSpec().getArmorRating() * 10f;
                if (maxArmor > 0) {
                    float currentArmor = 0f;
                    ArmorGridAPI armorGrid = ship.getArmorGrid();
                    for (int i = 0; i < armorGrid.getGrid().length; i++) {
                        for (int j = 0; j < armorGrid.getGrid()[i].length; j++) {
                            currentArmor += armorGrid.getArmorValue(i, j);
                        }
                    }
                    float armorLevel = currentArmor / maxArmor;
                    if (armorLevel < 0.85f) {
                        shouldUseSystem = true;
                    }
                }
            }
            
            if (!shouldUseSystem && !ship.getDisabledWeapons().isEmpty()) {
                shouldUseSystem = true;
            }

            if (!shouldUseSystem && ship.getEngineController().isFlamedOut()) {
                shouldUseSystem = true;
            }

            if (shouldUseSystem) {
                ship.useSystem();
            }
        }
    }
}