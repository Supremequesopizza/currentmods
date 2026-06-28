package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.bt_phase_lash_system;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class bt_phase_lash_systemAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private final IntervalUtil tracker = new IntervalUtil(0.25f, 0.5f);
    private static final String PHASE_TAG = "isPhaseCloak";

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        tracker.advance(amount);

        if (tracker.intervalElapsed()) {
            if (system.isActive() || system.getCooldownRemaining() > 0 || ship.getFluxTracker().isOverloadedOrVenting()) {
                return;
            }

            ShipAPI bestTarget = findBestTarget();
            if (bestTarget != null) {
                ship.setShipTarget(bestTarget);
                ship.useSystem();
            }
        }
    }

    private ShipAPI findBestTarget() {
        List<ShipAPI> enemies = AIUtils.getEnemiesOnMap(ship);
        ShipAPI bestTarget = null;
        float highestScore = 0f;

        for (ShipAPI enemy : enemies) {
            if (!bt_phase_lash_system.isValidTarget(ship, enemy) || MathUtils.getDistance(ship, enemy) > bt_phase_lash_system.MAX_RANGE) {
                continue;
            }

            float currentScore = 0f;
            float dist = MathUtils.getDistance(ship, enemy);
            boolean isPriority = false;

            currentScore += 500f - dist;


            if (enemy.getHullSpec().isPhase()) {
                currentScore += 10000f;
                isPriority = true;
            } else if ("phasecloak".equals(enemy.getHullSpec().getShipDefenseId())) {
                currentScore += 9000f;
                isPriority = true;
            } else if (enemy.getPhaseCloak() != null) {
                currentScore += 8000f;
                isPriority = true;
            } else if (enemy.getHullSpec().hasTag(PHASE_TAG)) {
                currentScore += 1000f;
                isPriority = true;
            }

            if (isPriority && enemy.isPhased()) {
                currentScore += 20000f;
            }

            if (currentScore > highestScore) {
                highestScore = currentScore;
                bestTarget = enemy;
            }
        }

        if (bestTarget != null) {
            boolean isBestTargetAPriority = bestTarget.getHullSpec().isPhase() ||
                    "phasecloak".equals(bestTarget.getHullSpec().getShipDefenseId()) ||
                    bestTarget.getPhaseCloak() != null ||
                    bestTarget.getHullSpec().hasTag(PHASE_TAG);

            if (isBestTargetAPriority && !bestTarget.isPhased()) {
                return null;
            }
        }

        return bestTarget;
    }
}