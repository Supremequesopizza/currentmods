package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_rightclick_flaresAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private final IntervalUtil checkInterval = new IntervalUtil(0.2f, 0.3f);
    private static final float CHECK_RANGE = 1200f;
    private static final float THREAT_THRESHOLD = 5f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed() || system.getCooldownRemaining() > 0) {
            return;
        }

        List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, CHECK_RANGE);
        if (nearbyMissiles.isEmpty()) {
            return;
        }

        float totalThreat = 0f;
        for (MissileAPI missile : nearbyMissiles) {
            if (missile.isFading() || missile.isFizzling() || missile.didDamage()) {
                continue;
            }

            float threat = 1f;
            if (missile.isGuided()) {
                threat *= 3f;
            }
            threat += missile.getDamageAmount() / 250f;
            totalThreat += threat;
        }

        if (totalThreat >= THREAT_THRESHOLD) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }
}