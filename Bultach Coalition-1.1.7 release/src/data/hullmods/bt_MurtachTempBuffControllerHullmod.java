package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class bt_MurtachTempBuffControllerHullmod extends BaseHullMod {

    public static final String HULLMOD_ID = "bt_murtach_tempbuff";
    public static final String HULLMOD_ACTIVATION_SIGNAL = "bt_MTBCH_Activate";
    public static final String UI_SIGNAL_ROF_ACTIVE = "bt_MTBCH_UI_RofActive";
    public static final String UI_SIGNAL_ARMOR_CAP_REDUCED = "bt_MTBCH_UI_ArmorCapReduced";

    private static final float ROF_BUFF_MULT = 2f;
    private static final float ARMOR_CAP_REDUCTION_PERCENT = 0.10f;
    private static final float BUFF_MAX_DURATION_FALLBACK = 8.4f;


    private String activeRofBuffId = null;
    private boolean secondBuffActive = false;
    private String mainSystemToMonitorId = null;

    private boolean isBuffSequenceActive = false;
    private boolean isTaperingVisuals = false;
    private float taperTimeRemaining = 0f;
    private float buffDurationRemaining = 0f;

    private transient IntervalUtil arcTimer;

    private static final String SYNTHARMOR_HULLMOD_ID = "bt_murtach_syntharmor";
    private static final String SYNTHARMOR_REPAIR_KEY = "bt_murtach_syntharmor_armor_repaired_this_combat";

    private static final Color BUFF_JITTER_UNDER_COLOR = new Color(133, 70, 200, 120);
    private static final float BUFF_JITTER_BASE_INTENSITY = 0.2f;
    private static final int BUFF_JITTER_COPIES = 6;
    private static final float BUFF_JITTER_MIN_RANGE = 1f;
    private static final float BUFF_JITTER_RANGE = 6f;

    private static final Color BUFF_ARC_CORE_COLOR = new Color(255, 176, 221, 150);
    private static final Color BUFF_ARC_FRINGE_COLOR = new Color(255, 156, 179, 75);
    private static final float BUFF_ARC_THICKNESS_BASE = 6f;
    private static final float BUFF_ARC_SPAWN_MIN_INTERVAL = 0.7f;
    private static final float BUFF_ARC_SPAWN_MAX_INTERVAL = 1.1f;
    private static final float VISUAL_FADE_OUT_DURATION = 0.75f;

    private Object readResolve() {
        if (arcTimer == null) {
            arcTimer = new IntervalUtil(BUFF_ARC_SPAWN_MIN_INTERVAL, BUFF_ARC_SPAWN_MAX_INTERVAL);
        }
        return this;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) {
            if (isBuffSequenceActive) deactivateAndCleanup(ship);
            return;
        }

        if (arcTimer == null) readResolve();

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        Map<String, Object> customData = ship.getCustomData();

        if (!isBuffSequenceActive) {
            if (customData.containsKey(HULLMOD_ACTIVATION_SIGNAL)) {
                MutableShipStatsAPI stats = ship.getMutableStats();
                mainSystemToMonitorId = (String) customData.get("SYSTEM_ID");

                boolean rofBuffApplied = false;

                if (customData.containsKey("ROF_ID")) {
                    activeRofBuffId = (String) customData.get("ROF_ID");
                    stats.getBallisticRoFMult().modifyMult(activeRofBuffId, ROF_BUFF_MULT);
                    stats.getEnergyRoFMult().modifyMult(activeRofBuffId, ROF_BUFF_MULT);
                    stats.getMissileRoFMult().modifyMult(activeRofBuffId, ROF_BUFF_MULT);
                    customData.put(UI_SIGNAL_ROF_ACTIVE, ROF_BUFF_MULT);
                    rofBuffApplied = true;
                }

                if (customData.containsKey("FLUX_ID")) {
                    if (ship.getVariant().hasHullMod(SYNTHARMOR_HULLMOD_ID)) {
                        Float armorRepairedSoFar = (Float) customData.get(SYNTHARMOR_REPAIR_KEY);

                        if (armorRepairedSoFar != null && armorRepairedSoFar > 0) {
                            ArmorGridAPI armorGrid = ship.getArmorGrid();
                            float totalMaxArmor = armorGrid.getMaxArmorInCell() * armorGrid.getGrid().length * armorGrid.getGrid()[0].length;
                            float maxArmorRepairCap = totalMaxArmor * bt_murtach_syntharmor.MAX_ARMOR_REPAIR_CAP_PERCENTAGE;

                            float reductionAmount = maxArmorRepairCap * ARMOR_CAP_REDUCTION_PERCENT;
                            float newArmorRepairedAmount = Math.max(0, armorRepairedSoFar - reductionAmount);

                            customData.put(SYNTHARMOR_REPAIR_KEY, newArmorRepairedAmount);
                            customData.put(UI_SIGNAL_ARMOR_CAP_REDUCED, ARMOR_CAP_REDUCTION_PERCENT);
                            secondBuffActive = true;
                        }
                    }
                }

                if (rofBuffApplied || secondBuffActive) {
                    isBuffSequenceActive = true;
                    buffDurationRemaining = BUFF_MAX_DURATION_FALLBACK;
                }

                customData.remove(HULLMOD_ACTIVATION_SIGNAL);
                customData.remove("SYSTEM_ID");
                customData.remove("ROF_ID");
                customData.remove("FLUX_ID");
                customData.remove("VISUAL_SCALE");
            }
        }

        if (isBuffSequenceActive) {
            float visualScale = 1f;
            if (customData.containsKey(UI_SIGNAL_ROF_ACTIVE) && secondBuffActive) {
                visualScale = 1.5f;
            }

            if (isTaperingVisuals) {
                taperTimeRemaining -= amount;
                if (taperTimeRemaining <= 0) {
                    deactivateAndCleanup(ship);
                    return;
                }
                applyVisuals(ship, engine, amount, (taperTimeRemaining / VISUAL_FADE_OUT_DURATION) * visualScale);
            } else {
                buffDurationRemaining -= amount;
                boolean shouldStartTaper = buffDurationRemaining <= 0;

                ShipSystemAPI systemToMonitor = null;
                if (mainSystemToMonitorId != null) {
                    if (ship.getSystem() != null && systemIdBaseForSystem(ship.getSystem(), ship).equals(mainSystemToMonitorId)) {
                        systemToMonitor = ship.getSystem();
                    } else if (ship.getPhaseCloak() != null && systemIdBaseForSystem(ship.getPhaseCloak(), ship).equals(mainSystemToMonitorId)) {
                        systemToMonitor = ship.getPhaseCloak();
                    }
                }

                if (systemToMonitor != null && systemToMonitor.getState() == ShipSystemAPI.SystemState.IDLE && systemToMonitor.getCooldownRemaining() <= 0f) {
                    shouldStartTaper = true;
                }

                if (shouldStartTaper) {
                    isTaperingVisuals = true;
                    taperTimeRemaining = VISUAL_FADE_OUT_DURATION;
                }

                applyVisuals(ship, engine, amount, 1f * visualScale);
            }
        }
    }

    private void deactivateAndCleanup(ShipAPI ship) {
        if (ship == null) return;

        isBuffSequenceActive = false;
        isTaperingVisuals = false;

        MutableShipStatsAPI stats = ship.getMutableStats();
        if (activeRofBuffId != null) {
            stats.getBallisticRoFMult().unmodify(activeRofBuffId);
            stats.getEnergyRoFMult().unmodify(activeRofBuffId);
            stats.getMissileRoFMult().unmodify(activeRofBuffId);
        }

        ship.getCustomData().remove(UI_SIGNAL_ROF_ACTIVE);
        ship.getCustomData().remove(UI_SIGNAL_ARMOR_CAP_REDUCED);

        ship.setJitterUnder(this, BUFF_JITTER_UNDER_COLOR, 0f, 0, 0, 0);

        activeRofBuffId = null;
        secondBuffActive = false;
        mainSystemToMonitorId = null;
    }

    private String systemIdBaseForSystem(ShipSystemAPI system, ShipAPI ship) {
        if (system == null || ship == null || system.getSpecAPI() == null) return "";
        return system.getSpecAPI().getId() + "_" + ship.getId();
    }

    private void applyVisuals(ShipAPI ship, CombatEngineAPI engine, float amount, float intensityFactor) {
        if (intensityFactor <= 0.01f) return;
        ship.setJitterUnder(this, BUFF_JITTER_UNDER_COLOR, BUFF_JITTER_BASE_INTENSITY * intensityFactor, BUFF_JITTER_COPIES, BUFF_JITTER_MIN_RANGE, BUFF_JITTER_RANGE + (intensityFactor > 1.1f ? 3f : 0f));
        arcTimer.advance(amount);
        if (arcTimer.intervalElapsed()) {
            ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
            List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();
            if (!segments.isEmpty()) {
                int arcsToSpawn = intensityFactor > 1.1f ? 2 : 1;
                for (int i = 0; i < arcsToSpawn; i++) {
                    Vector2f p1 = segments.get(MathUtils.getRandomNumberInRange(0, segments.size() - 1)).getP1();
                    Vector2f p2 = segments.get(MathUtils.getRandomNumberInRange(0, segments.size() - 1)).getP1();
                    engine.spawnEmpArcVisual(p1, ship, p2, ship, BUFF_ARC_THICKNESS_BASE * Math.max(0.5f, intensityFactor), new Color(BUFF_ARC_CORE_COLOR.getRed(), BUFF_ARC_CORE_COLOR.getGreen(), BUFF_ARC_CORE_COLOR.getBlue(), (int) (BUFF_ARC_CORE_COLOR.getAlpha() * Math.min(1f, intensityFactor))), new Color(BUFF_ARC_FRINGE_COLOR.getRed(), BUFF_ARC_FRINGE_COLOR.getGreen(), BUFF_ARC_FRINGE_COLOR.getBlue(), (int) (BUFF_ARC_FRINGE_COLOR.getAlpha() * Math.min(1f, intensityFactor))));
                }
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}