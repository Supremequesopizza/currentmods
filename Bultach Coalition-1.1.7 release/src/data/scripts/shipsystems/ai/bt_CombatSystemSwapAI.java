package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_CombatSystemSwapAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private IntervalUtil timer = new IntervalUtil(1f, 2f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (ship == null || !ship.isAlive() || Global.getCombatEngine().isPaused()) return;

        timer.advance(amount);
        if (!timer.intervalElapsed()) return;

        int currentMode = system.getAmmo();
        int desiredMode;


        float threat = getThreatWeight(ship, 1200f);
        if (ship.getFluxLevel() < 0.3f && threat < 20f) {
            desiredMode = 1;
        } else {
            desiredMode = 2;
        }

        if (currentMode != desiredMode && system.getState() == ShipSystemAPI.SystemState.IDLE) {
            ship.useSystem();
        }
    }

    private float getThreatWeight(ShipAPI ship, float range) {
        float total = 0f;
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
            if (enemy == null || !enemy.isAlive()) continue;
            float weight = enemy.getFleetMember() != null ? enemy.getFleetMember().getDeploymentCostSupplies() : 5f;
            if (enemy.getFluxTracker().isOverloadedOrVenting()) weight *= 0.5f;
            if (enemy.getFluxLevel() > 0.7f) weight *= 0.75f;
            total += weight;
        }
        return total;
    }
}
