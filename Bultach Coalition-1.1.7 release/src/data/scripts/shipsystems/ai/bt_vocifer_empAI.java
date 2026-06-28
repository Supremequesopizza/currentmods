package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_vocifer_empAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private ShipSystemAPI system;
    private final IntervalUtil checkInterval = new IntervalUtil(0.25f, 0.4f);

    private static final float BASE_RANGE = 600f;
    private static final float MAX_ADDITIONAL_RANGE = 500f;

    private static final float FLUX_LEVEL_WEIGHT = 10f;
    private static final float SHIP_THREAT = 2f;
    private static final float FIGHTER_THREAT = 1f;
    private static final float MISSILE_THREAT = 0.5f;
    private static final float MISSILE_DAMAGE_DIVISOR = 400f;
    private static final float USE_THRESHOLD = 8f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed() || system.isActive() || system.isCoolingDown()) {
            return;
        }
        
        float fluxLevel = ship.getFluxTracker().getFluxLevel();
        float currentRange = BASE_RANGE + (MAX_ADDITIONAL_RANGE * fluxLevel);
        
        float totalThreat = fluxLevel * FLUX_LEVEL_WEIGHT;
        
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, currentRange);
        for (ShipAPI enemy : nearbyEnemies) {
            if (enemy.isFighter() || enemy.isDrone()) {
                totalThreat += FIGHTER_THREAT * enemy.getHullLevel();
            } else {
                totalThreat += SHIP_THREAT * (enemy.getHullSize().ordinal() + 1);
            }
        }
        
        List<MissileAPI> nearbyMissiles = AIUtils.getNearbyEnemyMissiles(ship, currentRange);
        for (MissileAPI missile : nearbyMissiles) {
            if (missile.isFizzling() || missile.isFading()) continue;
            totalThreat += MISSILE_THREAT + (missile.getDamageAmount() / MISSILE_DAMAGE_DIVISOR);
        }
        
        if (totalThreat >= USE_THRESHOLD) {
            ship.useSystem();
        }
    }
}