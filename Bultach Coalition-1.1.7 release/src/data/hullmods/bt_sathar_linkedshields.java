package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;

public class bt_sathar_linkedshields extends BaseHullMod {

    private static final String LEFT_MODULE_HULL_ID = "sgr_engine_left";
    private static final String RIGHT_MODULE_HULL_ID = "sgr_engine_right";
    private static final String HULLMOD_ID = "bt_sathar_linkedshields";
    public static final String DATA_KEY_PREFIX = "bt_sathar_linkedshields_";
    private static final float FORCED_OVERLOAD_DURATION_SECONDS = 8f;
    private static final float OVERLOAD_RETRIGGER_COOLDOWN_SECONDS = 15f;
    private static final String SHIELD_DMG_TAKEN_MULT_ID = "_shield_eff_sync";
    private static final String SHIELD_UPKEEP_MULT_ID = "_upkeep_sync";
    private static final float TRANSITION_TIME = 4f;

    private static final float ARC_RAMP_UP_TIME = 6f;
    private static final float ARC_MAX_THICKNESS = 10f;
    private static final Color ARC_FRINGE_COLOR = new Color(255, 0, 0, 255);
    private static final Color ARC_CORE_COLOR = new Color(0, 255, 207, 255);
    private static final String ARC_DATA_KEY_PREFIX = "bt_shieldArching_";
    private final float maxAngle = 40f;
    private final float minAngle = 10f;

    public static class LinkedShieldsData {
        public ShipAPI moduleLeft = null;
        public ShipAPI moduleRight = null;
        public boolean leftModuleOnline = false;
        public boolean rightModuleOnline = false;
        public ShipAPI singleSurvivor = null;
        public float emergencyModeTransitionProgress = 0f;
        public float initialSurvivorAngle = 0f;
        float originalLeftMaxFlux = 0f;
        float originalRightMaxFlux = 0f;
        public float combinedMaxFlux = 1f;
        String leaderModuleId = null;
        boolean synchronizedOverloadActive = false;
        float syncOverloadReTriggerCooldown = 0f;
        public float currentSharedFluxLevel = 0f;
        public boolean moduleLeftIsVenting = false;
        public boolean moduleRightIsVenting = false;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        String shipHullId = ship.getHullSpec().getBaseHullId();
        if (!LEFT_MODULE_HULL_ID.equals(shipHullId) && !RIGHT_MODULE_HULL_ID.equals(shipHullId)) {
            return;
        }
        ShipAPI parentShip = ship.getParentStation();
        if (parentShip != null && parentShip.isAlive()) {
            MutableShipStatsAPI parentStats = parentShip.getMutableStats();
            MutableShipStatsAPI moduleStats = ship.getMutableStats();
            String damageTakenModId = id + SHIELD_DMG_TAKEN_MULT_ID; String upkeepModId = id + SHIELD_UPKEEP_MULT_ID;
            moduleStats.getShieldDamageTakenMult().unmodify(damageTakenModId); moduleStats.getShieldUpkeepMult().unmodify(upkeepModId);
            float parentEffectiveShieldDamageTakenMult = parentStats.getShieldDamageTakenMult().getModifiedValue();
            float currentModuleShieldDamageTakenMult = moduleStats.getShieldDamageTakenMult().getModifiedValue();
            if (currentModuleShieldDamageTakenMult > 0.001f) moduleStats.getShieldDamageTakenMult().modifyMult(damageTakenModId, parentEffectiveShieldDamageTakenMult / currentModuleShieldDamageTakenMult);
            else if (parentEffectiveShieldDamageTakenMult <= 0.001f && currentModuleShieldDamageTakenMult > 0f) moduleStats.getShieldDamageTakenMult().modifyMult(damageTakenModId, 0.001f / currentModuleShieldDamageTakenMult);
            float parentEffectiveShieldUpkeepMult = parentStats.getShieldUpkeepMult().getModifiedValue();
            float currentModuleShieldUpkeepMult = moduleStats.getShieldUpkeepMult().getModifiedValue();
            if (currentModuleShieldUpkeepMult > 0.001f) moduleStats.getShieldUpkeepMult().modifyMult(upkeepModId, parentEffectiveShieldUpkeepMult / currentModuleShieldUpkeepMult);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || engine.isCombatOver()) return;

        if (ship.getParentStation() != null) {
            String dataKey = DATA_KEY_PREFIX + ship.getParentStation().getId();
            LinkedShieldsData data = (LinkedShieldsData) engine.getCustomData().get(dataKey);
            if (data == null) { data = new LinkedShieldsData(); engine.getCustomData().put(dataKey, data); }

            if (data.moduleLeft == null && LEFT_MODULE_HULL_ID.equals(ship.getHullSpec().getBaseHullId())) {
                data.moduleLeft = ship;
                data.originalLeftMaxFlux = ship.getFluxTracker().getMaxFlux();
            }
            if (data.moduleRight == null && RIGHT_MODULE_HULL_ID.equals(ship.getHullSpec().getBaseHullId())) {
                data.moduleRight = ship;
                data.originalRightMaxFlux = ship.getFluxTracker().getMaxFlux();
            }

            data.leftModuleOnline = data.moduleLeft != null && data.moduleLeft.isAlive();
            data.rightModuleOnline = data.moduleRight != null && data.moduleRight.isAlive();

            if (data.leftModuleOnline) data.moduleLeftIsVenting = data.moduleLeft.getFluxTracker().isVenting();
            if (data.rightModuleOnline) data.moduleRightIsVenting = data.moduleRight.getFluxTracker().isVenting();

            boolean bothOnline = data.leftModuleOnline && data.rightModuleOnline;
            boolean oneOnline = data.leftModuleOnline ^ data.rightModuleOnline;

            if (oneOnline) {
                data.leaderModuleId = null;
                if (data.singleSurvivor == null) {
                    data.singleSurvivor = data.leftModuleOnline ? data.moduleLeft : data.moduleRight;
                    data.initialSurvivorAngle = data.singleSurvivor.getFacing();
                }
                if (data.emergencyModeTransitionProgress < 1f) {
                    data.emergencyModeTransitionProgress += amount / TRANSITION_TIME;
                    if (data.emergencyModeTransitionProgress > 1f) data.emergencyModeTransitionProgress = 1f;
                }
            } else {
                if (data.emergencyModeTransitionProgress > 0f) {
                    data.emergencyModeTransitionProgress -= amount / TRANSITION_TIME;
                    if (data.emergencyModeTransitionProgress < 0f) data.emergencyModeTransitionProgress = 0f;
                }
                if (data.emergencyModeTransitionProgress <= 0f) {
                    data.singleSurvivor = null;
                }
            }

            if (bothOnline) {
                data.moduleLeft.getShield().setCenter(0, 0);
                data.moduleRight.getShield().setCenter(0, 0);

                if (data.leaderModuleId == null) {
                    data.leaderModuleId = data.moduleLeft.getId();
                    data.combinedMaxFlux = Math.max(1f, data.originalLeftMaxFlux + data.originalRightMaxFlux);
                }

                if (ship.getId().equals(data.leaderModuleId)) {
                    if (data.syncOverloadReTriggerCooldown > 0f) {
                        data.syncOverloadReTriggerCooldown -= amount;
                        if (data.syncOverloadReTriggerCooldown < 0f) {
                            data.syncOverloadReTriggerCooldown = 0f;
                        }
                    }

                    float combinedCurrentFlux = data.moduleLeft.getFluxTracker().getCurrFlux() + data.moduleRight.getFluxTracker().getCurrFlux();
                    float fluxLevelUnclamped = (data.combinedMaxFlux > 0) ? combinedCurrentFlux / data.combinedMaxFlux : 0f;
                    float fluxLevelClamped = Math.max(0f, Math.min(1f, fluxLevelUnclamped));
                    data.currentSharedFluxLevel = fluxLevelClamped;
                    data.moduleLeft.getFluxTracker().setCurrFlux(fluxLevelClamped * data.originalLeftMaxFlux);
                    data.moduleRight.getFluxTracker().setCurrFlux(fluxLevelClamped * data.originalRightMaxFlux);

                    boolean lIsAlive = data.moduleLeft != null && data.moduleLeft.isAlive();
                    boolean rIsAlive = data.moduleRight != null && data.moduleRight.isAlive();
                    boolean lIsCurrentlyOV = lIsAlive && data.moduleLeft.getFluxTracker().isOverloaded();
                    boolean rIsCurrentlyOV = rIsAlive && data.moduleRight.getFluxTracker().isOverloaded();

                    boolean shouldOverloadByFlux = fluxLevelUnclamped >= 1f;
                    boolean outOfSync = lIsCurrentlyOV != rIsCurrentlyOV;
                    boolean canTriggerNewOverload = data.syncOverloadReTriggerCooldown <= 0f;

                    if (!data.synchronizedOverloadActive && canTriggerNewOverload && (shouldOverloadByFlux || outOfSync)) {
                        data.synchronizedOverloadActive = true;
                        if (lIsAlive) {
                            data.moduleLeft.getFluxTracker().forceOverload(FORCED_OVERLOAD_DURATION_SECONDS);
                        }
                        if (rIsAlive) {
                            data.moduleRight.getFluxTracker().forceOverload(FORCED_OVERLOAD_DURATION_SECONDS);
                        }
                    } else if (data.synchronizedOverloadActive) {
                        if (lIsCurrentlyOV && rIsCurrentlyOV) {
                            float lTimer = data.moduleLeft.getFluxTracker().getOverloadTimeRemaining();
                            float rTimer = data.moduleRight.getFluxTracker().getOverloadTimeRemaining();
                            float maxTimer = Math.max(lTimer, rTimer);
                            if (lTimer != maxTimer) {
                                data.moduleLeft.getFluxTracker().setOverloadDuration(maxTimer);
                            }
                            if (rTimer != maxTimer) {
                                data.moduleRight.getFluxTracker().setOverloadDuration(maxTimer);
                            }
                        } else if (outOfSync) {
                            if (lIsCurrentlyOV) data.moduleLeft.getFluxTracker().stopOverload();
                            if (rIsCurrentlyOV) data.moduleRight.getFluxTracker().stopOverload();
                            data.synchronizedOverloadActive = false;
                            data.syncOverloadReTriggerCooldown = OVERLOAD_RETRIGGER_COOLDOWN_SECONDS;
                        } else {
                            data.synchronizedOverloadActive = false;
                            data.syncOverloadReTriggerCooldown = OVERLOAD_RETRIGGER_COOLDOWN_SECONDS;
                        }
                    }
                }
            }

            if (data.emergencyModeTransitionProgress > 0f && data.singleSurvivor != null && ship == data.singleSurvivor) {
                ShipAPI parent = data.singleSurvivor.getParentStation();
                if (parent != null) {
                    ShieldAPI survivorShield = data.singleSurvivor.getShield();
                    float progress = data.emergencyModeTransitionProgress;
                    float shortestRotation = MathUtils.getShortestRotation(data.initialSurvivorAngle, parent.getFacing());
                    float interpolatedAngle = data.initialSurvivorAngle + (shortestRotation * progress);
                    survivorShield.forceFacing(interpolatedAngle);
                    Vector2f parentShieldWorldLoc = parent.getShieldCenterEvenIfNoShield();
                    Vector2f moduleWorldLoc = data.singleSurvivor.getLocation();
                    Vector2f targetRelativeOffset = Vector2f.sub(parentShieldWorldLoc, moduleWorldLoc, new Vector2f());
                    VectorUtils.rotate(targetRelativeOffset, -data.singleSurvivor.getFacing(), targetRelativeOffset);
                    float interX = targetRelativeOffset.x * progress;
                    float interY = targetRelativeOffset.y * progress;
                    survivorShield.setCenter(interX, interY);
                }
            } else if (data.singleSurvivor != null && oneOnline && ship == data.singleSurvivor) {
                data.singleSurvivor.getShield().setCenter(0,0);
            }
        }

        if (ship.getShield() != null) {
            String id = ship.getId();
            Map<String, Object> customCombatData = engine.getCustomData();
            String effectLevelKey = ARC_DATA_KEY_PREFIX + "effectLevel_" + id;
            String timerKey = ARC_DATA_KEY_PREFIX + "timer_" + id;
            float effectLevel = 0f;
            if (customCombatData.containsKey(effectLevelKey)) {
                effectLevel = (float) customCombatData.get(effectLevelKey);
            }
            if (ship.getShield().isOn()) {
                if (effectLevel < 1f) {
                    effectLevel += amount / ARC_RAMP_UP_TIME;
                    if (effectLevel > 1f) effectLevel = 1f;
                }
            } else {
                if (effectLevel > 0f) {
                    effectLevel -= amount / ARC_RAMP_UP_TIME;
                    if (effectLevel < 0f) effectLevel = 0f;
                }
            }
            customCombatData.put(effectLevelKey, effectLevel);

            if (effectLevel > 0f) {
                IntervalUtil timer = (IntervalUtil) customCombatData.get(timerKey);
                if (timer == null) {
                    timer = new IntervalUtil(0.5f, 0.5f);
                    customCombatData.put(timerKey, timer);
                }
                timer.advance(amount);
                if (timer.intervalElapsed()) {
                    if (ship.getShield().getActiveArc() < minAngle) return;
                    float arcDistance = MathUtils.getRandomNumberInRange(minAngle, maxAngle);
                    float startAngleOffset = MathUtils.getRandomNumberInRange(0, ship.getShield().getActiveArc() - arcDistance);
                    float shieldFacing = ship.getShield().getFacing();
                    float shieldStartAngle = shieldFacing + (ship.getShield().getActiveArc() * 0.5f);
                    float arcStartAngle = shieldStartAngle - startAngleOffset;
                    Vector2f shieldCenter = ship.getShield().getLocation();
                    Vector2f arcStart = MathUtils.getPointOnCircumference(shieldCenter, ship.getShield().getRadius(), arcStartAngle);
                    Vector2f arcEnd = MathUtils.getPointOnCircumference(shieldCenter, ship.getShield().getRadius(), arcStartAngle - arcDistance);
                    float currentThickness = ARC_MAX_THICKNESS * effectLevel;
                    int currentAlpha = (int)(255 * effectLevel);
                    engine.spawnEmpArcVisual(arcStart, ship, arcEnd, ship, currentThickness,
                            new Color(ARC_FRINGE_COLOR.getRed(), ARC_FRINGE_COLOR.getGreen(), ARC_FRINGE_COLOR.getBlue(), currentAlpha),
                            new Color(ARC_CORE_COLOR.getRed(), ARC_CORE_COLOR.getGreen(), ARC_CORE_COLOR.getBlue(), currentAlpha));
                }
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return HULLMOD_ID.equals(this.spec.getId()) ||
                (ship.getVariant() != null && ship.getVariant().hasHullMod(HULLMOD_ID));
    }
}