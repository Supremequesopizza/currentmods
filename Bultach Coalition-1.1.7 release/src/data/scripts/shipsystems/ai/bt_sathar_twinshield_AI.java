package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.hullmods.bt_sathar_linkedshields;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class bt_sathar_twinshield_AI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private final IntervalUtil checkInterval = new IntervalUtil(0.2f, 0.3f);
    private static final float THREAT_EVALUATION_RANGE = 3000f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed() || engine.isCombatOver()) {
            return;
        }

        String dataKey = bt_sathar_linkedshields.DATA_KEY_PREFIX + ship.getId();
        Object rawData = engine.getCustomData().get(dataKey);

        if (!(rawData instanceof bt_sathar_linkedshields.LinkedShieldsData)) {
            return;
        }
        bt_sathar_linkedshields.LinkedShieldsData moduleData = (bt_sathar_linkedshields.LinkedShieldsData) rawData;

        boolean bothOnline = moduleData.leftModuleOnline && moduleData.rightModuleOnline;
        boolean oneOnline = moduleData.singleSurvivor != null;

        if (!bothOnline && !oneOnline) {
            return;
        }

        boolean parentIsVenting = ship.getFluxTracker().isVenting();
        boolean modulesAreVenting = moduleData.moduleLeftIsVenting || moduleData.moduleRightIsVenting;

        if (!parentIsVenting && modulesAreVenting) {
            return;
        }

        Object systemState = ship.getCustomData().get(data.scripts.shipsystems.bt_SatharShieldCommandSystem.SATHAR_SHIELD_COMMAND_STATE_KEY);
        boolean systemIsActive = Boolean.TRUE.equals(systemState);

        float currentFluxLevel;
        if (bothOnline) {
            currentFluxLevel = moduleData.currentSharedFluxLevel;
        } else {
            currentFluxLevel = moduleData.singleSurvivor.getFluxTracker().getFluxLevel();
        }

        if (currentFluxLevel > 0.95f) {
            if (systemIsActive) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            }
            return;
        }

        float threat = 0f;
        List<DamagingProjectileAPI> projectiles = CombatUtils.getProjectilesWithinRange(ship.getLocation(), THREAT_EVALUATION_RANGE);
        for (DamagingProjectileAPI proj : projectiles) {
            if (proj.getOwner() == ship.getOwner() || proj.isFading() || proj.didDamage()) continue;
            float damageMultiplier = 1f;
            switch (proj.getDamageType()) {
                case HIGH_EXPLOSIVE: damageMultiplier = 1.5f; break;
                case KINETIC: damageMultiplier = 0.5f; break;
                case FRAGMENTATION: damageMultiplier = 0.25f; break;
            }
            threat += (proj.getDamageAmount() + proj.getEmpAmount()) * damageMultiplier;
        }

        List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, THREAT_EVALUATION_RANGE);
        for (ShipAPI enemy : enemies) {
            float proximity = 1f - (MathUtils.getDistance(ship, enemy) / THREAT_EVALUATION_RANGE);
            float dp = enemy.getHullSpec().getSuppliesToRecover();
            float hullSizeThreat = 0f;
            switch (enemy.getHullSize()) {
                case FIGHTER: hullSizeThreat = 3f; break;
                case FRIGATE: hullSizeThreat = 5f; break;
                case DESTROYER: hullSizeThreat = 10f; break;
                case CRUISER: hullSizeThreat = 20f; break;
                case CAPITAL_SHIP: hullSizeThreat = 40f; break;
            }
            threat += (dp + hullSizeThreat) * proximity * 6f;
        }

        ShipwideAIFlags flags = ship.getAIFlags();
        if (flags != null) {
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)) threat += 500f;
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) threat += 150f;
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)) threat += 200f;
            if (flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING) || flags.hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET)) {
                threat *= 0.6f;
            }
        }

        if (ship.getHullLevel() < 0.4f) {
            threat += 400f;
        }

        boolean desireSystem = false;
        if (threat > 1000) {
            desireSystem = true;
        } else if (threat > 600 && currentFluxLevel < 0.5f) {
            desireSystem = true;
        } else if (threat > 300 && currentFluxLevel < 0.25f) {
            desireSystem = true;
        }

        if (parentIsVenting) {
            desireSystem = true;
        }

        if (desireSystem && !systemIsActive) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else if (!desireSystem && systemIsActive) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }
}