package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class bt_tempHPAI implements ShipSystemAIScript {

    private final IntervalUtil timer = new IntervalUtil(0.5f, 1f);
    private ShipAPI ship;
    private CombatEngineAPI engine;

    private static final float MAX_RANGE = 3000f;
    private static final float FIRE_THRESHOLD = 350f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused() || ship == null || !ship.isAlive()) return;

        timer.advance(amount);
        if (!timer.intervalElapsed()) return;

        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, MAX_RANGE);
        ShipAPI bestTarget = null;
        float bestWeight = 0f;

        for (ShipAPI ally : allies) {
            if (ally == ship || ally.isFighter() || ally.isHulk()) continue;

            float dist = Misc.getDistance(ship.getLocation(), ally.getLocation());
            if (dist > MAX_RANGE) continue;

            float armorDamageFrac = getArmorDamageFraction(ally);
            if (armorDamageFrac < 0.05f) continue;

            float sizeWeight = getSizeWeight(ally.getHullSize());
            float fluxWeight = ally.getFluxLevel() * 0.5f;
            float proximityBonus = (1f - (dist / MAX_RANGE)) * 0.3f;

            float weight = (armorDamageFrac * 1000f + fluxWeight * 300f) * sizeWeight * (1f + proximityBonus);

            if (weight > bestWeight) {
                bestWeight = weight;
                bestTarget = ally;
            }
        }

        if (bestTarget != null && bestWeight >= FIRE_THRESHOLD) {
            ShipSystemAPI system = ship.getSystem();
            if (system != null && system.getState() == ShipSystemAPI.SystemState.IDLE) {
                ship.useSystem();


            }
        }
    }

    private float getArmorDamageFraction(ShipAPI ally) {
        ArmorGridAPI grid = ally.getArmorGrid();
        float totalArmor = 0f;
        float currentArmor = 0f;
        int width = grid.getGrid().length;
        int height = grid.getGrid()[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                currentArmor += grid.getArmorValue(x, y);
                totalArmor += grid.getMaxArmorInCell();
            }
        }

        float armorFrac = 1f - (currentArmor / Math.max(1f, totalArmor));
        return Math.max(0f, armorFrac);
    }

    private float getSizeWeight(ShipAPI.HullSize size) {
        switch (size) {
            case CAPITAL_SHIP: return 2.0f;
            case CRUISER: return 1.85f;
            case DESTROYER: return 1.65f;
            case FRIGATE: return 1.4f;
            default: return 1f;
        }
    }
}
