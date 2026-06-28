package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;

import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.utils.bultach_utils;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;
import org.magiclib.util.MagicRender;

//As you may have guessed, this is from templars. Original author is DarkRevenant. Heavily modified (fucked up and ruined) by Noof/Pogre
public class bt_ferventcharge extends BaseShipSystemScript {

    private static final Color COLOR1 = new Color(255, 253, 248, 128);
    private static final Color COLOR2 = new Color(255, 251, 237, 69);
    private static final Color AFTERIMAGE_COLOR = new Color(255, 255, 255, 19);
    public static final float AFTERIMAGE_DURATION = 2.22f;
    private static final float FLARE_LIGHT_FADEOUT = 0.15f;
    private static final String ACTIVATION_FX_ID = "bultach_ferventcharge_shockwave";
    private static final float ACTIVATION_FX_OFFSET_1 = -40f;
    private static final float ACTIVATION_FX_SIZE_START_1 = 50f;
    private static final float ACTIVATION_FX_SIZE_END_1 = 550f;
    private static final Color ACTIVATION_FX_COLOR_1 = new Color(255, 251, 245, 208);
    private static final float ACTIVATION_FX_DURATION_1 = 1.2f;
    private static final float ACTIVATION_FX_FADEIN_1 = 0.05f;
    private static final float ACTIVATION_FX_OFFSET_2 = -110f;
    private static final float ACTIVATION_FX_SIZE_START_2 = 40f;
    private static final float ACTIVATION_FX_SIZE_END_2 = 450f;
    private static final Color ACTIVATION_FX_COLOR_2 = new Color(255, 250, 243, 160);
    private static final float ACTIVATION_FX_DURATION_2 = 1.1f;
    private static final float ACTIVATION_FX_FADEIN_2 = 0.08f;
    private static final float ACTIVATION_FX_OFFSET_3 = -160f;
    private static final float ACTIVATION_FX_SIZE_START_3 = 30f;
    private static final float ACTIVATION_FX_SIZE_END_3 = 350f;
    private static final Color ACTIVATION_FX_COLOR_3 = new Color(255, 252, 241, 120);
    private static final float ACTIVATION_FX_DURATION_3 = 1.0f;
    private static final float ACTIVATION_FX_FADEIN_3 = 0.1f;
    private static final String DATA_KEY = "bt_ferventcharge";
    private static final Vector2f ZERO = new Vector2f();
    private static final Color ARC_CORE_COLOR_IN = new Color(255, 238, 194, 120);
    private static final Color ARC_FRINGE_COLOR_IN = new Color(255, 250, 189, 80);
    private static final float ARC_THICKNESS_IN = 15f;
    private static final Color ARC_CORE_COLOR_ACTIVE = new Color(255, 251, 243, 218);
    private static final Color ARC_FRINGE_COLOR_ACTIVE = new Color(255, 252, 241, 181);
    private static final float ARC_THICKNESS_ACTIVE = 35f;
    private static final float DAMAGE_RESISTANCE_PERCENT = 75f;
    private static final float SPEED_BOOST_FLAT = 300f;
    private static final float SPEED_BOOST_PERCENT = 300f;


    public static float cooldownTime(ShipAPI ship) {
        final LocalData localData = (LocalData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (localData == null) return 0f;
        final Map<ShipAPI, Float> coolingDown = localData.coolingDown;
        if (coolingDown.containsKey(ship)) return Global.getCombatEngine().getTotalElapsedTime(false) - coolingDown.get(ship);
        else return 0f;
    }
    public static float effectLevel(ShipAPI ship) {
        final LocalData localData = (LocalData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (localData == null) return 0f;
        final Map<ShipAPI, Float> acting = localData.acting;
        if (acting.containsKey(ship)) return acting.get(ship);
        else return 0f;
    }
    public static void removeFromCooldown(ShipAPI ship) {
        final LocalData localData = (LocalData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (localData == null) return;
        final Map<ShipAPI, Float> coolingDown = localData.coolingDown;
        coolingDown.remove(ship);
    }

    private boolean activated = false;
    private boolean ended = false;
    private final IntervalUtil forceInterval = new IntervalUtil(0.035f, 0.035f);
    private final IntervalUtil interval = new IntervalUtil(0.05f, 0.1f);
    private StandardLight light = null;
    private boolean started = false;
    private IntervalUtil afterImageTimer = new IntervalUtil(0.15f, 0.25f);
    private Map<ShipEngineAPI, StandardLight> activeFlares = new HashMap<>();


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.getCustomData().containsKey(DATA_KEY)) {
            engine.getCustomData().put(DATA_KEY, new LocalData());
        }

        float amount = engine.getElapsedInLastFrame();

        interval.advance(amount);
        forceInterval.advance(amount);
        boolean intervalElapsed = interval.intervalElapsed();
        boolean forceElapsed = forceInterval.intervalElapsed();

        final LocalData localData = (LocalData) engine.getCustomData().get(DATA_KEY);
        final Map<ShipAPI, Float> acting = localData.acting;
        final Map<ShipAPI, Float> coolingDown = localData.coolingDown;

        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        acting.put(ship, effectLevel);
        String resistanceId = id + "_resistance";
        String speedId = id + "_speed";

        if (state == State.IN) {
            activated = false;
            if (light == null) {
                light = new StandardLight(ship.getLocation(), ZERO, ZERO, null);
                light.setColor(COLOR1);
                light.setIntensity(0.2f);
                light.setSize(600f);
                light.fadeIn(2f);
                LightShader.addLight(light);
            } else {
                light.setLocation(ship.getLocation());
            }

            if (!started) {
                started = true;
            }

            if (intervalElapsed) {
                ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
                List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();

                if (segments != null && !segments.isEmpty()) {
                    int numSegments = segments.size();
                    if (numSegments > 0) {
                        int firstBoundIndex = MathUtils.getRandomNumberInRange(0, numSegments - 1);
                        int secondBoundIndex = (firstBoundIndex + MathUtils.getRandomNumberInRange(2, 5)) % numSegments;

                        if (firstBoundIndex < numSegments && secondBoundIndex < numSegments) {
                            Vector2f localPoint1 = segments.get(firstBoundIndex).getP1();
                            Vector2f localPoint2 = segments.get(secondBoundIndex).getP1();

                            if (localPoint1 != null && localPoint2 != null) {
                                engine.spawnEmpArcVisual(localPoint1, ship, localPoint2, ship,
                                        ARC_THICKNESS_IN, ARC_FRINGE_COLOR_IN, ARC_CORE_COLOR_IN);
                            }
                        }
                    }
                }
            }

            float chargeLevel = Math.min(effectLevel * 2f, 1f);
            stats.getAcceleration().modifyMult(id, 1f - chargeLevel);
            stats.getDeceleration().modifyMult(id, 1f - chargeLevel);
            stats.getTurnAcceleration().modifyMult(id, 1f - chargeLevel);
            stats.getMaxSpeed().modifyMult(id, 1f - (chargeLevel * 0.5f));

        } else if (state == State.ACTIVE) {
            if (light != null) {
                light.setLocation(ship.getLocation());
                light.setIntensity(0.15f + (float) Math.random() * 0.05f);
                light.setSize(500f + (float) Math.random() * 100f);
            }

            float area = 100f;
            float force = 100f * forceInterval.getIntervalDuration();

            if (!activated) {
                activated = true;
                Global.getSoundPlayer().playSound("bt_gestalt_dash", 1.4f, 1f, ship.getLocation(), ZERO);


                engine.spawnExplosion(ship.getLocation(), ZERO, COLOR2, area * 1.5f, 0.2f);
                engine.addHitParticle(ship.getLocation(), ZERO, area * 3f, 0.75f, 0.5f, COLOR1);

                Vector2f shockwaveLoc1 = MathUtils.getPoint(ship.getLocation(), ACTIVATION_FX_OFFSET_1, ship.getFacing());
                MagicRender.battlespace( Global.getSettings().getSprite("fx", ACTIVATION_FX_ID), shockwaveLoc1, ZERO,
                        new Vector2f(ACTIVATION_FX_SIZE_START_1, ACTIVATION_FX_SIZE_START_1), new Vector2f(ACTIVATION_FX_SIZE_END_1, ACTIVATION_FX_SIZE_END_1),
                        ship.getFacing() - 180f, 0f, ACTIVATION_FX_COLOR_1, true, ACTIVATION_FX_FADEIN_1, 0f, ACTIVATION_FX_DURATION_1 - ACTIVATION_FX_FADEIN_1);

                Vector2f shockwaveLoc2 = MathUtils.getPoint(ship.getLocation(), ACTIVATION_FX_OFFSET_2, ship.getFacing());
                MagicRender.battlespace( Global.getSettings().getSprite("fx", ACTIVATION_FX_ID), shockwaveLoc2, ZERO,
                        new Vector2f(ACTIVATION_FX_SIZE_START_2, ACTIVATION_FX_SIZE_START_2), new Vector2f(ACTIVATION_FX_SIZE_END_2, ACTIVATION_FX_SIZE_END_2),
                        ship.getFacing() - 180f, 0f, ACTIVATION_FX_COLOR_2, true, ACTIVATION_FX_FADEIN_2, 0f, ACTIVATION_FX_DURATION_2 - ACTIVATION_FX_FADEIN_2);

                Vector2f shockwaveLoc3 = MathUtils.getPoint(ship.getLocation(), ACTIVATION_FX_OFFSET_3, ship.getFacing());
                MagicRender.battlespace( Global.getSettings().getSprite("fx", ACTIVATION_FX_ID), shockwaveLoc3, ZERO,
                        new Vector2f(ACTIVATION_FX_SIZE_START_3, ACTIVATION_FX_SIZE_START_3), new Vector2f(ACTIVATION_FX_SIZE_END_3, ACTIVATION_FX_SIZE_END_3),
                        ship.getFacing() - 180f, 0f, ACTIVATION_FX_COLOR_3, true, ACTIVATION_FX_FADEIN_3, 0f, ACTIVATION_FX_DURATION_3 - ACTIVATION_FX_FADEIN_3);


                Vector2f vel = new Vector2f(1f, 0f);
                VectorUtils.rotate(vel, ship.getFacing(), vel);
                vel.scale(350f);
                ship.getVelocity().set(vel);
                force /= forceInterval.getIntervalDuration();
                coolingDown.put(ship, engine.getTotalElapsedTime(false));
            } else if (!forceElapsed) {
                force = 0f;
            }

            Global.getSoundPlayer().playLoop("system_plasma_burn_loop", ship, 4f, 2f, ship.getLocation(), ZERO);

            if (intervalElapsed) {
                ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
                List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();

                if (segments != null && !segments.isEmpty()) {
                    int numSegments = segments.size();
                    if (numSegments > 0) {
                        int firstBoundIndex = MathUtils.getRandomNumberInRange(0, numSegments - 1);
                        int secondBoundIndex = (firstBoundIndex + MathUtils.getRandomNumberInRange(2, 5)) % numSegments; // Use modulo

                        if (firstBoundIndex < numSegments && secondBoundIndex < numSegments) {
                            Vector2f localPoint1 = segments.get(firstBoundIndex).getP1();
                            Vector2f localPoint2 = segments.get(secondBoundIndex).getP1();

                            if (localPoint1 != null && localPoint2 != null) {
                                engine.spawnEmpArcVisual(localPoint1, ship, localPoint2, ship,
                                        ARC_THICKNESS_ACTIVE, ARC_FRINGE_COLOR_ACTIVE, ARC_CORE_COLOR_ACTIVE);
                            }
                        }
                    }
                }
            }

            stats.getAcceleration().modifyMult(id, 1f);
            stats.getAcceleration().modifyFlat(id, effectLevel * 100f);
            stats.getAcceleration().modifyPercent(id, effectLevel * 100f);
            stats.getMaxSpeed().modifyFlat(speedId, effectLevel * SPEED_BOOST_FLAT);
            stats.getMaxSpeed().modifyPercent(speedId, effectLevel * SPEED_BOOST_PERCENT);
            stats.getDeceleration().modifyMult(id, 0f);
            stats.getTurnAcceleration().modifyMult(id, 1f);
            stats.getTurnAcceleration().modifyFlat(id, effectLevel * 40f);
            stats.getTurnAcceleration().modifyPercent(id, effectLevel * 100f);
            stats.getMaxTurnRate().modifyFlat(id, effectLevel * 15f);
            stats.getMaxTurnRate().modifyPercent(id, effectLevel * 50f);

            float resistanceMult = 1f - (DAMAGE_RESISTANCE_PERCENT * 0.01f * effectLevel);
            stats.getArmorDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getHullDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getShieldDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getEmpDamageTakenMult().modifyMult(resistanceId, resistanceMult);


            afterImageTimer.advance(amount);
            if (afterImageTimer.intervalElapsed()) {
                bultach_utils.afterimage(ship, AFTERIMAGE_COLOR, AFTERIMAGE_DURATION);
            }

            if (force > 0f) {
                List<ShipAPI> nearbyEnemies = CombatUtils.getShipsWithinRange(ship.getLocation(), area);
                for (ShipAPI thisEnemy : nearbyEnemies) {
                    if (thisEnemy.getCollisionClass() == CollisionClass.NONE || thisEnemy == ship) continue;

                    float falloff = 1f - MathUtils.getDistance(ship, thisEnemy) / area;
                    Vector2f dir = VectorUtils.getDirectionalVector(ship.getLocation(), thisEnemy.getLocation());

                    CombatUtils.applyForce(thisEnemy, dir, force * falloff);
                }

                List<CombatEntityAPI> nearbyAsteroids = CombatUtils.getAsteroidsWithinRange(ship.getLocation(), area);
                for (CombatEntityAPI asteroid : nearbyAsteroids) {
                    float falloff = 1f - MathUtils.getDistance(ship, asteroid) / area;
                    Vector2f dir = VectorUtils.getDirectionalVector(ship.getLocation(), asteroid.getLocation());

                    CombatUtils.applyForce(asteroid, dir, force * falloff);
                }
            }

            List<ShipEngineAPI> engines = ship.getEngineController().getShipEngines();
            List<ShipEngineAPI> enginesToRemoveFromMap = new ArrayList<>();
            for (Map.Entry<ShipEngineAPI, StandardLight> entry : activeFlares.entrySet()) {
                ShipEngineAPI eng = entry.getKey();
                StandardLight flare = entry.getValue();
                boolean engineFoundAndActive = false;
                for (ShipEngineAPI currentEng : engines) {
                    if (currentEng == eng && currentEng.isActive()) {
                        engineFoundAndActive = true;
                        flare.setLocation(currentEng.getLocation());
                        break;
                    }
                }
                if (!engineFoundAndActive) {
                    flare.fadeOut(FLARE_LIGHT_FADEOUT);
                    enginesToRemoveFromMap.add(eng);
                }
            }
            for (ShipEngineAPI eng : enginesToRemoveFromMap) {
                activeFlares.remove(eng);
            }

            for (ShipEngineAPI engineSlot : engines) {
                boolean isActive = engineSlot.isActive();
                if (isActive && !activeFlares.containsKey(engineSlot)) {
                    if (MagicRender.screenCheck(0.5f, engineSlot.getLocation())) {
                        MagicLensFlare.createSharpFlare(engine, ship, engineSlot.getLocation(), 2, 150, 0, AFTERIMAGE_COLOR, Color.white);

                    }
                }
            }


        } else {
            if (light != null) {
                light.fadeOut(1f);
                light = null;
            }

            if (!ended && activated) {
                Global.getSoundPlayer().playSound("system_damper_omega_off", 0.8f, 0.4f, ship.getLocation(), ZERO);
                ended = true;
            }

            float resistanceMult = 1f - (DAMAGE_RESISTANCE_PERCENT * 0.01f * effectLevel);
            stats.getArmorDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getHullDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getShieldDamageTakenMult().modifyMult(resistanceId, resistanceMult);
            stats.getEmpDamageTakenMult().modifyMult(resistanceId, resistanceMult);

            stats.getMaxSpeed().unmodifyPercent(speedId);
            stats.getMaxSpeed().modifyFlat(speedId, effectLevel * SPEED_BOOST_FLAT);

            stats.getMaxTurnRate().unmodifyPercent(id);
            stats.getMaxTurnRate().modifyFlat(id, effectLevel * 15f);

            stats.getDeceleration().modifyMult(id, effectLevel);
            stats.getAcceleration().modifyFlat(id, effectLevel * 100f);
            stats.getAcceleration().modifyPercent(id, effectLevel * 100f);
            stats.getTurnAcceleration().modifyFlat(id, effectLevel * 40f);
            stats.getTurnAcceleration().modifyPercent(id, effectLevel * 100f);

            Iterator<Map.Entry<ShipEngineAPI, StandardLight>> iter = activeFlares.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ShipEngineAPI, StandardLight> entry = iter.next();
                entry.getValue().fadeOut(FLARE_LIGHT_FADEOUT);
                iter.remove();
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.IN) {
            if (index == 0) return new StatusData("charging", true);
        } else if (state == State.ACTIVE || state == State.OUT) {
            float resPercent = DAMAGE_RESISTANCE_PERCENT * effectLevel;
            if (index == 0) {
                return new StatusData("increased engine power", false);
            } else if (index == 1) {
                return new StatusData((int)resPercent + "% damage resistance", false);
            }
        }
        return null;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        }

        activated = false;
        started = false;
        ended = false;
        if (light != null) {
            light.fadeOut(0.5f);
            light = null;
        }

        for (StandardLight flare : activeFlares.values()) {
            flare.fadeOut(FLARE_LIGHT_FADEOUT);
        }
        activeFlares.clear();

        stats.getAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxSpeed().unmodify(id + "_speed");
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        String resistanceId = id + "_resistance";
        stats.getArmorDamageTakenMult().unmodify(resistanceId);
        stats.getHullDamageTakenMult().unmodify(resistanceId);
        stats.getShieldDamageTakenMult().unmodify(resistanceId);
        stats.getEmpDamageTakenMult().unmodify(resistanceId);


        if (ship != null) {
            if (!Global.getCombatEngine().getCustomData().containsKey(DATA_KEY)) {
                return;
            }
            final LocalData localData = (LocalData) Global.getCombatEngine().getCustomData().get(DATA_KEY);
            if (localData != null) {
                final Map<ShipAPI, Float> acting = localData.acting;
                acting.remove(ship);
            }
        }
    }

    private static final class LocalData {
        final Map<ShipAPI, Float> acting = new HashMap<>(20);
        final Map<ShipAPI, Float> coolingDown = new HashMap<>(20);
    }
}