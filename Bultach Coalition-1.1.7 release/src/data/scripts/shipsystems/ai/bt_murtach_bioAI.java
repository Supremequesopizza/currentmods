package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.BrutePhaseStatsAlt;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class bt_murtach_bioAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private IntervalUtil decisionInterval;

    public static final String ACTIVE_TIME_KEY = "bt_brute_phase_active_time";
    public static final String SHIP_TARGET_KEY = "bt_brute_phase_target";

    private static final float DEACTIVATE_BUFFER = 0.2f;
    private static final float BOOST_SPEED_THRESHOLD = 350f;
    private static final float BRAVE_DECLOAK_TIMER_RATIO = 0.4f;
    private static final float GAMBLE_CHANCE_ROF = 0.30f;
    private static final float GAMBLE_CHANCE_ARMOR = 0.15f;
    private static final float THREAT_CHECK_RANGE = 2000f;
    private static final float REAR_ARC_DEGREES = 120f;
    private static final float RETREAT_HULL_LEVEL = 0.5f;
    private static final float RETREAT_FLUX_LEVEL = 0.8f;
    private static final float OFFENSIVE_BLAST_RADIUS_MULT = 0.75f;

    private enum AIState {
        EVALUATING,
        ENGAGING,
        FLANKING,
        BOOSTING,
        GAMBLING,
        HUNTING_PHASED,
        RETREATING
    }
    private AIState currentState = AIState.EVALUATING;

    private static final Map<ShipAPI.HullSize, Float> mults = new HashMap<>();
    static {
        mults.put(ShipAPI.HullSize.FIGHTER, 0.5f);
        mults.put(ShipAPI.HullSize.FRIGATE, 1f);
        mults.put(ShipAPI.HullSize.DESTROYER, 2f);
        mults.put(ShipAPI.HullSize.CRUISER, 4f);
        mults.put(ShipAPI.HullSize.CAPITAL_SHIP, 8f);
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
        this.system = system;
        this.flags = flags;
        this.decisionInterval = new IntervalUtil(0.1f, 0.2f);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused() || !ship.isAlive()) return;

        if (system.isOn()) {
            handleSystemOn(missileDangerDir, collisionDangerDir);
        }

        decisionInterval.advance(amount);
        if (!decisionInterval.intervalElapsed()) return;

        if (system.isOn()) return;

        currentState = getNextState(missileDangerDir, collisionDangerDir);
        setManeuverFlags(currentState);

        if (system.canBeActivated()) {
            switch (currentState) {
                case ENGAGING:
                case HUNTING_PHASED:
                case BOOSTING:
                case RETREATING:
                    ship.useSystem();
                    break;
                case FLANKING:
                    ShipAPI currentTarget = getTarget();
                    if (currentTarget != null && isBehind(currentTarget)) {
                        ship.useSystem();
                    }
                    break;
            }
        }
    }

    private void setManeuverFlags(AIState state) {
        ShipAPI target = getTarget();
        flags.unsetFlag(AIFlags.DO_NOT_BACK_OFF);
        flags.unsetFlag(AIFlags.PURSUING);

        Vector2f retreatDest = findRetreatDestination();

        switch (state) {
            case ENGAGING:
            case HUNTING_PHASED:
                if (target != null) {
                    flags.setFlag(AIFlags.MOVEMENT_DEST, 1f, target.getLocation());
                    flags.setFlag(AIFlags.MANEUVER_RANGE_FROM_TARGET, 1f, ship.getCollisionRadius() * OFFENSIVE_BLAST_RADIUS_MULT * 0.5f);
                    flags.setFlag(AIFlags.PURSUING, 1f);
                    flags.setFlag(AIFlags.DO_NOT_BACK_OFF, 1f);
                }
                break;
            case FLANKING:
                if (target != null) {
                    flags.setFlag(AIFlags.MOVEMENT_DEST, 1f, findFlankDestination(target));
                    flags.setFlag(AIFlags.PURSUING, 1f);
                    flags.setFlag(AIFlags.DO_NOT_BACK_OFF, 1f);
                }
                break;
            case RETREATING:
                if (retreatDest != null) {
                    flags.setFlag(AIFlags.MOVEMENT_DEST, 1f, retreatDest);
                }
                break;
        }
    }

    private AIState getNextState(Vector2f missileDangerDir, Vector2f collisionDangerDir) {
        float threat = getThreatWeight(THREAT_CHECK_RANGE, ship);

        if ((ship.getFluxLevel() > RETREAT_FLUX_LEVEL || ship.getHullLevel() < RETREAT_HULL_LEVEL) && threat > ship.getFleetMember().getDeploymentCostSupplies() * 0.5f) {
            return AIState.RETREATING;
        }

        ShipAPI phasedTarget = findPhasedEnemy();
        if (phasedTarget != null) {
            flags.setFlag(AIFlags.MANEUVER_TARGET, 1f, phasedTarget);
            return AIState.HUNTING_PHASED;
        }

        if (missileDangerDir != null || collisionDangerDir != null) {
            return AIState.ENGAGING;
        }

        ShipAPI currentTarget = getTarget();
        if (currentTarget != null) {
            boolean isLargeTarget = currentTarget.getHullSize() == ShipAPI.HullSize.CRUISER || currentTarget.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP;
            if (isLargeTarget && isAlone(currentTarget) && !isBehind(currentTarget)) {
                return AIState.FLANKING;
            }
            return AIState.ENGAGING;
        }

        if (flags.hasFlag(AIFlags.MOVEMENT_DEST)) {
            return AIState.BOOSTING;
        }

        return AIState.EVALUATING;
    }

    private void handleSystemOn(Vector2f missileDangerDir, Vector2f collisionDangerDir) {
        float activeTime = getActiveTime();
        float maxTime = BrutePhaseStatsAlt.MAX_ACTIVE_TIME_FOR_SCALING;
        float timeRatio = (maxTime > 0) ? activeTime / maxTime : 0f;

        if (activeTime >= (maxTime - DEACTIVATE_BUFFER)) {
            ship.useSystem();
            currentState = AIState.EVALUATING;
            return;
        }

        if (currentState == AIState.GAMBLING) {
            float targetRatio = 0f;
            Object gambleData = flags.getCustom(AIFlags.CUSTOM1);
            if (gambleData instanceof Float) {
                targetRatio = (Float) gambleData;
            }
            if (timeRatio >= targetRatio) {
                ship.useSystem();
                currentState = AIState.EVALUATING;
                flags.removeFlag(AIFlags.CUSTOM1);
            }
            return;
        }

        boolean immediateThreat = missileDangerDir != null || collisionDangerDir != null;
        ShipAPI currentTarget = getTarget();
        boolean hasTarget = currentTarget != null && currentTarget.isAlive() && currentTarget.getOwner() != ship.getOwner();

        if (currentState == AIState.BOOSTING) {
            CombatEntityAPI maneuverTarget = (flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI) ? (CombatEntityAPI) flags.getCustom(AIFlags.MANEUVER_TARGET) : null;
            if (maneuverTarget != null && MathUtils.getDistance(ship, maneuverTarget) < 2500f) {
                ship.useSystem();
            } else if (ship.getVelocity().length() >= BOOST_SPEED_THRESHOLD || immediateThreat || hasTarget) {
                ship.useSystem();
                currentState = AIState.EVALUATING;
            }
            return;
        }

        if (currentState == AIState.RETREATING) {
            Vector2f retreatDest = findRetreatDestination();
            if (retreatDest != null && MathUtils.getDistance(ship, retreatDest) < 200f) {
                ship.useSystem();
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                currentState = AIState.EVALUATING;
            } else if (!immediateThreat) {
                ship.useSystem();
                currentState = AIState.EVALUATING;
            }
            return;
        }

        float blastRadius = ship.getCollisionRadius() * OFFENSIVE_BLAST_RADIUS_MULT;
        if (hasTarget && MathUtils.getDistance(ship, currentTarget) <= blastRadius) {
            boolean shouldDecloak = false;
            if (currentState == AIState.HUNTING_PHASED && currentTarget.isPhased()) {
                shouldDecloak = true;
            } else if (currentState != AIState.HUNTING_PHASED) {
                boolean isSmallTarget = currentTarget.getHullSize() == ShipAPI.HullSize.FRIGATE || currentTarget.getHullSize() == ShipAPI.HullSize.DESTROYER;
                boolean isLargeTarget = currentTarget.getHullSize() == ShipAPI.HullSize.CRUISER || currentTarget.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP;
                if (isSmallTarget || (isLargeTarget && isAlone(currentTarget) && isBehind(currentTarget))) {
                    shouldDecloak = true;
                }
            }
            if (shouldDecloak) {
                float roll = (float) Math.random();
                if (roll < GAMBLE_CHANCE_ARMOR) {
                    currentState = AIState.GAMBLING;
                    flags.setFlag(AIFlags.CUSTOM1, 2f, 0.96f);
                } else if (roll < GAMBLE_CHANCE_ROF) {
                    currentState = AIState.GAMBLING;
                    flags.setFlag(AIFlags.CUSTOM1, 2f, 0.91f);
                } else {
                    ship.useSystem();
                    currentState = AIState.EVALUATING;
                }
                return;
            }
        }

        if (timeRatio >= BRAVE_DECLOAK_TIMER_RATIO && !hasTarget) {
            ship.useSystem();
            currentState = AIState.EVALUATING;
            return;
        }

        if (!immediateThreat && !hasTarget && !flags.hasFlag(AIFlags.MOVEMENT_DEST)) {
            ship.useSystem();
            currentState = AIState.EVALUATING;
        }
    }

    private ShipAPI findPhasedEnemy() {
        List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, THREAT_CHECK_RANGE);
        ShipAPI closest = null;
        float minDistance = Float.MAX_VALUE;
        for(ShipAPI enemy : enemies) {
            if (enemy.isPhased() && !enemy.isHulk()) {
                float distance = MathUtils.getDistance(ship, enemy);
                if(distance < minDistance){
                    closest = enemy;
                    minDistance = distance;
                }
            }
        }
        return closest;
    }

    private Vector2f findFlankDestination(ShipAPI target) {
        float distance = target.getCollisionRadius() + ship.getCollisionRadius() + 200f;
        Vector2f direction = VectorUtils.getDirectionalVector(target.getLocation(), ship.getLocation());
        direction = VectorUtils.rotate(direction, 180f);
        return Vector2f.add(target.getLocation(), (Vector2f) direction.scale(distance), null);
    }

    private Vector2f findRetreatDestination() {
        Vector2f away = new Vector2f();
        float totalThreat = 0f;
        List<ShipAPI> threats = AIUtils.getNearbyEnemies(ship, THREAT_CHECK_RANGE * 2);
        for (ShipAPI threat : threats) {
            if (threat.getFleetMember() == null) {
                continue;
            }
            float dp = threat.getFleetMember().getDeploymentCostSupplies();
            totalThreat += dp;
            Vector2f dir = VectorUtils.getDirectionalVector(threat.getLocation(), ship.getLocation());
            Vector2f.add(away, (Vector2f) dir.scale(dp), away);
        }
        if (totalThreat > 0) {
            return Vector2f.add(ship.getLocation(), (Vector2f) away.normalise().scale(2000f), null);
        }
        return null;
    }

    private boolean isAlone(ShipAPI target) {
        if (target == null) return false;
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(target, THREAT_CHECK_RANGE);
        int frigateEscorts = 0;
        int destroyerEscorts = 0;

        for (ShipAPI enemy : nearbyEnemies) {
            if (enemy == target || enemy.getHullSize() == ShipAPI.HullSize.FIGHTER) continue;
            if (enemy.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                frigateEscorts++;
            } else if (enemy.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                destroyerEscorts++;
            } else {
                return false;
            }
        }
        if (destroyerEscorts > 1) return false;
        if (frigateEscorts > 2) return false;
        return !(destroyerEscorts > 0 && frigateEscorts > 0);
    }

    private float getActiveTime() {
        Object data = ship.getCustomData().get(ACTIVE_TIME_KEY);
        if (data instanceof Float) {
            return (Float) data;
        }
        return 0f;
    }

    private ShipAPI getTarget() {
        if(flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof ShipAPI){
            ShipAPI t = (ShipAPI) flags.getCustom(AIFlags.MANEUVER_TARGET);
            if(t.isAlive() && !t.isHulk()) return t;
        }
        Object data = ship.getCustomData().get(SHIP_TARGET_KEY);
        if (data instanceof ShipAPI) {
            ShipAPI potentialTarget = (ShipAPI) data;
            if (potentialTarget.isAlive() && !potentialTarget.isHulk()) {
                return potentialTarget;
            }
        }
        return AIUtils.getNearestEnemy(ship);
    }

    private boolean isBehind(ShipAPI target) {
        if (target == null) return false;
        float angleToTarget = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
        float angleDiff = MathUtils.getShortestRotation(target.getFacing(), angleToTarget);
        return Math.abs(angleDiff) > (180f - (REAR_ARC_DEGREES / 2f));
    }

    private float getThreatWeight(float range, ShipAPI ship) {
        float threatWeightTotal = 0f;
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
            if (enemy == null || enemy.getFleetMember() == null) continue;

            float weight = enemy.getFleetMember().getDeploymentCostSupplies();
            if (mults.containsKey(enemy.getHullSize())) {
                weight *= mults.get(enemy.getHullSize());
            }
            if (enemy.getFluxTracker().isOverloadedOrVenting()) {
                weight *= 0.75f;
            }
            if (enemy.getHullLevel() < 0.4f) {
                weight *= 0.5f;
            }
            if (enemy.getFluxLevel() > 0.5f) {
                weight *= 0.5f;
            }
            if (enemy.getEngineController().isFlamedOut()) {
                weight *= 0.5f;
            }

            threatWeightTotal += weight;
        }
        return threatWeightTotal;
    }
}