package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import data.scripts.utils.bt_SinuousSegment;
import com.fs.starfarer.api.combat.ShipAIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import org.lwjgl.util.vector.Vector2f;

public class bt_puncher extends BaseHullMod {
    // I STOLE THIS FROM KOL BECAUSE IDK HOW TO CODE!!!!

    // LEFT ARM ////////////////////////////////////
    private Map<String, Boolean> leftArmFiring = new HashMap<String, Boolean>();
    private Map<String, PunchState> leftPunchState = new HashMap<String, PunchState>();
    private float leftAngle = 210f;
    private bt_SinuousSegment[] leftArm = new bt_SinuousSegment[2];
    private String[] leftArgs = new String[2];

    public static final float COOLDOWN_L_TO_L = 4f;
    public static final float COOLDOWN_L_TO_R = 2f;

    public static final float ANG_VEL_INCREMENT = 200f;
    public static final float PUNCH_SPEED = 4f;

    public static final float SWING_DURATION = 0.25f;
    public static final float EXTEND_DURATION = 0.45f;
    public static final float RETRACT_DURATION = 0.25f;

    private float swingTime = 0f;
    private float extendTime = 0f;
    private float retractTime = 0f;
    ////////////////////////////////////

    // RIGHT ARM ////////////////////////////////////
    private Map<String, Boolean> rightArmFiring = new HashMap<String, Boolean>();
    private Map<String, PunchState> rightPunchState = new HashMap<String, PunchState>();
    //private PunchState rightPunchState = PunchState.DONE;
    private float rightAngle = 210f;
    private bt_SinuousSegment[] rightArm = new bt_SinuousSegment[2];
    private String[] rightArgs = new String[2];

    public static final float COOLDOWN_R_TO_L = 2f;
    public static final float COOLDOWN_R_TO_R = 4f;

    public static final float ANG_VEL_INCREMENT_R = 200f;
    public static final float PUNCH_SPEED_R = 4f;

    public static final float WINDUP_DURATION_R = 0f;
    public static final float SWING_DURATION_R = 0.25f;
    public static final float EXTEND_DURATION_R = 0.45f;
    public static final float RETRACT_DURATION_R = 0.25f;

    private float windupTime_R = 0f;
    private float swingTime_R = 0f;
    private float extendTime_R = 0f;
    private float retractTime_R = 0f;
    ////////////////////////////////////

    public static final float RESIST_MULT = 0.2f;
    public static final float FLUX_TOLERANCE = 0.5f; // How much it hesitates if it's high flux
    public static final float OFFCENTER_DISTANCE_RATIO = 0.025f;

    private float originalAngVel = 0f;


    public enum PunchState {
        WINDUP, SWINGING, EXTENDED, RETRACTING, DONE;
    }

    // AI scalars
    private float aiTimer = 0f;
    private static Map approachComfort = new HashMap();

    static {

        approachComfort.put(ShipAPI.HullSize.FIGHTER, 0f);
        approachComfort.put(ShipAPI.HullSize.FRIGATE, 0f);
        approachComfort.put(ShipAPI.HullSize.DESTROYER, 0.25f);
        approachComfort.put(ShipAPI.HullSize.CRUISER, 0.5f);
        approachComfort.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.25f);
    }

    private static final float FLANKING_MULT = 3f; // AI will be this many times more likely to use system when flanking
    private static final float FLANKING_RANGE = 360f; // AI will consider this many degrees of a ship's rear exposed to be a flank
    private float extraMad = 0f;

    private boolean aiOverrideInit = false;


    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }


    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }


    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        // Init stuff




        super.advanceInCombat(ship, amount);

        leftArgs[0] = "LEFTARM";
        leftArgs[1] = "LEFTHAND";

        rightArgs[0] = "RIGHTARM";
        rightArgs[1] = "RIGHTHAND";


        List<ShipAPI> children = ship.getChildModulesCopy();


        bt_SinuousSegment.setup(leftArm, children, leftArgs);
        bt_SinuousSegment.setup(rightArm, children, rightArgs);

        WeaponAPI leftGuide = null;
        WeaponAPI rightGuide = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("LEFTGUIDE")) {
                leftGuide = w;
            }
            if (w.getSlot().getId().equals("RIGHTGUIDE"))
                rightGuide = w;
        }
        // framerate check
        float frameRatio = amount / 0.016666668f;

        aiTimer += amount;
        if (aiTimer >= 0.5f) {
            aiTimer = 0f;

            // AI process for closing distance w/system, only called if AI pilot is active & at least 1 melee weapon is available
            if (Global.getCombatEngine().getPlayerShip() != ship || !Global.getCombatEngine().isUIAutopilotOn()) {
                if ((leftArm[1].ship != null && leftArm[1].ship.isAlive()) ||
                        (rightArm[1].ship != null && rightArm[1].ship.isAlive())) {
                    aiHandler(ship);
                }
            }
        }

        // Fire check left
        if (leftGuide != null && leftGuide.isFiring() && leftGuide.getCooldownRemaining() == 0f &&
                leftArm[1].ship != null && leftArm[1].ship.isAlive()) {
            //Global.getCombatEngine().getCombatUI().addMessage(0,"check: " + (!leftArmFiring.containsKey(ship.getId()) || !leftArmFiring.get(ship.getId())));
            if (!leftArmFiring.containsKey(ship.getId()) || !leftArmFiring.get(ship.getId())) {
                leftArmFiring.put(ship.getId(), true);
                //originalAngle = ship.getFacing();
                leftPunchState.put(ship.getId(), PunchState.SWINGING);
                originalAngVel = 0f;//ship.getAngularVelocity();


                leftGuide.setRemainingCooldownTo(COOLDOWN_L_TO_L);
                if (rightGuide != null)
                    rightGuide.setRemainingCooldownTo(Math.max(COOLDOWN_L_TO_R, rightGuide.getCooldownRemaining()));

                if (leftArm[0].ship != null) {
                    leftArm[0].ship.getMutableStats().getArmorDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                    leftArm[0].ship.getMutableStats().getHullDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                }
                if (leftArm[1].ship != null) {
                    leftArm[1].ship.getMutableStats().getArmorDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                    leftArm[1].ship.getMutableStats().getHullDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                }
                //Global.getCombatEngine().getCombatUI().addMessage(0,"success");
            } else {
                //Global.getCombatEngine().getCombatUI().addMessage(0,"failure. !leftArmFiring.containsKey(ship.getId()): " + !leftArmFiring.containsKey(ship.getId()) + ", !leftArmFiring.get(ship.getId()): " + !leftArmFiring.get(ship.getId()));

            }
        }

        // Left punch action
        if (leftArmFiring.containsKey(ship.getId()) && leftArmFiring.get(ship.getId())) {
            float increment = 0f;

            if (leftPunchState.get(ship.getId()) == PunchState.SWINGING) {
                if (swingTime >= SWING_DURATION) {
                    leftPunchState.put(ship.getId(), PunchState.EXTENDED);
                    swingTime = 0f;
                    leftAngle = 0f;
                } else {
                    increment = -PUNCH_SPEED * frameRatio;
                    ship.setAngularVelocity(originalAngVel - ANG_VEL_INCREMENT);
                    swingTime += amount;
                }
            }

            if (leftPunchState.get(ship.getId()) == PunchState.EXTENDED) {
                if (extendTime >= EXTEND_DURATION) {
                    extendTime = 0f;
                    leftPunchState.put(ship.getId(), PunchState.RETRACTING);
                } else {
                    if (extendTime <= 0f)
                        activateSystem(leftArm[1].ship);
                    extendTime += amount;
                    ship.setAngularVelocity(originalAngVel);
                }
            }

            if (leftPunchState.get(ship.getId()) == PunchState.RETRACTING) {
                if (retractTime >= RETRACT_DURATION) {
                    leftPunchState.put(ship.getId(), PunchState.DONE);
                    leftArmFiring.put(ship.getId(), false);
                    ship.setAngularVelocity(originalAngVel);
                    retractTime = 0f;
                    leftAngle = 210f;
                    if (leftArm[0].ship != null) {
                        leftArm[0].ship.getMutableStats().getArmorDamageTakenMult().unmodify("bt_puncher");
                        leftArm[0].ship.getMutableStats().getHullDamageTakenMult().unmodify("bt_puncher");
                    }
                    if (leftArm[1].ship != null) {
                        leftArm[1].ship.getMutableStats().getArmorDamageTakenMult().unmodify("bt_puncher");
                        leftArm[1].ship.getMutableStats().getHullDamageTakenMult().unmodify("bt_puncher");
                    }
                } else {
                    increment = PUNCH_SPEED * frameRatio;
                    ship.setAngularVelocity(originalAngVel + ANG_VEL_INCREMENT);
                    retractTime += amount;
                }
            }

            leftAngle += increment;
            if (leftArm[0].ship != null) {
                leftArm[0].ship.getStationSlot().setAngle(leftAngle);
                if (leftArm[1].ship != null) {
                    leftArm[1].ship.getLocation().set(leftArm[0].ship.getHullSpec().getWeaponSlotAPI("HAND").computePosition(leftArm[0].ship));
                }
            }
        } else {

            if (leftArm[0].ship != null)
                leftArm[0].ship.getStationSlot().setAngle(150f);
        }

        // Fire check right
        if (rightGuide != null && rightGuide.isFiring() && rightGuide.getCooldownRemaining() == 0f &&
                rightArm[1].ship != null && rightArm[1].ship.isAlive()) {
            if (!rightArmFiring.containsKey(ship.getId()) || !rightArmFiring.get(ship.getId())) {
                rightArmFiring.put(ship.getId(), true);
                rightPunchState.put(ship.getId(), PunchState.WINDUP);

                if (leftPunchState.get(ship.getId()) == PunchState.EXTENDED) {
                    windupTime_R = WINDUP_DURATION_R * 0.75f;
                    extendTime = EXTEND_DURATION - (WINDUP_DURATION_R * 0.25f);
                    //} else {
                    //originalAngVel = ship.getAngularVelocity();
                }
                originalAngVel = 0f;

                rightGuide.setRemainingCooldownTo(COOLDOWN_R_TO_R);
                if (leftGuide != null)
                    leftGuide.setRemainingCooldownTo(COOLDOWN_R_TO_L);

                if (rightArm[0].ship != null) {
                    rightArm[0].ship.getMutableStats().getArmorDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                    rightArm[0].ship.getMutableStats().getHullDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                }
                if (rightArm[1].ship != null) {
                    rightArm[1].ship.getMutableStats().getArmorDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                    rightArm[1].ship.getMutableStats().getHullDamageTakenMult().modifyMult("bt_puncher", RESIST_MULT);
                }
            }
        }

        // Right punch action
        if (rightArmFiring.containsKey(ship.getId()) && rightArmFiring.get(ship.getId())) {
            float increment = 0f;

            if (rightPunchState.get(ship.getId()) == PunchState.WINDUP) {
                if (windupTime_R >= WINDUP_DURATION_R) {
                    rightPunchState.put(ship.getId(), PunchState.SWINGING);
                    windupTime_R = 0f;
                } else {
                    if (windupTime_R < WINDUP_DURATION_R * 0.25f)
                        ship.setAngularVelocity(originalAngVel - ANG_VEL_INCREMENT_R);
                    else if (windupTime_R < WINDUP_DURATION_R * 0.75f)
                        ship.setAngularVelocity(originalAngVel);
                    else
                        ship.setAngularVelocity(originalAngVel + ANG_VEL_INCREMENT_R);

                    windupTime_R += amount;
                }
            }

            if (rightPunchState.get(ship.getId()) == PunchState.SWINGING) {
                if (swingTime_R >= SWING_DURATION_R) {
                    rightPunchState.put(ship.getId(), PunchState.EXTENDED);
                    swingTime_R = 0f;
                    rightAngle = 0f;
                } else {
                    increment = PUNCH_SPEED_R * frameRatio;
                    ship.setAngularVelocity(originalAngVel + ANG_VEL_INCREMENT_R);
                    swingTime_R += amount;
                }
            }

            if (rightPunchState.get(ship.getId()) == PunchState.EXTENDED) {
                if (extendTime_R >= EXTEND_DURATION_R) {
                    extendTime_R = 0f;
                    rightPunchState.put(ship.getId(), PunchState.RETRACTING);
                } else {
                    if (extendTime_R <= 0f)
                        activateSystem(rightArm[1].ship);
                    extendTime_R += amount;
                    ship.setAngularVelocity(originalAngVel);
                }
            }

            if (rightPunchState.get(ship.getId()) == PunchState.RETRACTING) {
                if (retractTime_R >= RETRACT_DURATION_R) {
                    rightPunchState.put(ship.getId(), PunchState.DONE);
                    rightArmFiring.put(ship.getId(), false);
                    ship.setAngularVelocity(originalAngVel);
                    retractTime_R = 0f;
                    rightAngle = 210f;
                    if (rightArm[0].ship != null) {
                        rightArm[0].ship.getMutableStats().getArmorDamageTakenMult().unmodify("bt_puncher");
                        rightArm[0].ship.getMutableStats().getHullDamageTakenMult().unmodify("bt_puncher");
                    }
                    if (rightArm[1].ship != null) {
                        rightArm[1].ship.getMutableStats().getArmorDamageTakenMult().unmodify("bt_puncher");
                        rightArm[1].ship.getMutableStats().getHullDamageTakenMult().unmodify("bt_puncher");
                    }
                } else {
                    increment = -PUNCH_SPEED_R * frameRatio;
                    ship.setAngularVelocity(originalAngVel - ANG_VEL_INCREMENT_R);
                    retractTime_R += amount;
                }
            }

            rightAngle += increment;
            if (rightArm[0].ship != null) {
                rightArm[0].ship.getStationSlot().setAngle(rightAngle);
                if (rightArm[1].ship != null) {
                    rightArm[1].ship.getLocation().set(rightArm[0].ship.getHullSpec().getWeaponSlotAPI("HAND").computePosition(rightArm[0].ship));
                    //rightArm[1].ship.getSystem().setAmmo(0);
                }
            }
        } else {

            if (rightArm[0].ship != null)
                rightArm[0].ship.getStationSlot().setAngle(210f);
        }

        if (leftArm[0].ship == null || !leftArm[0].ship.isAlive()) {
            try {
                leftArm[1].ship.setHitpoints(1f);
                leftArm[1].ship.applyCriticalMalfunction(leftArm[1].ship.getAllWeapons().get(0));
            } catch (Exception e) {
            }
        }
        if (rightArm[0].ship == null || !rightArm[0].ship.isAlive()) {
            try {
                rightArm[1].ship.setHitpoints(1f);
                rightArm[1].ship.applyCriticalMalfunction(rightArm[1].ship.getAllWeapons().get(0));
            } catch (Exception e) {
            }
        }

    }

    private float normalizeAngle(float f) {
        if (f < 0f)
            return f + 360f;
        if (f > 360f)
            return f - 360f;
        return f;
    }

    private void activateSystem(ShipAPI ship) {
        try {
            ship.getSystem().setAmmo(1);
            ship.useSystem();
        } catch (Exception e) {
            //Global.getCombatEngine().getCombatUI().addMessage(0, "Exception: " + e);
        }
    }

    // The system uses default plasma burn AI, this appends special melee rules.
    private void aiHandler(ShipAPI ship) {
        // If system isn't ready, abort.
        ShipSystemAPI system = ship.getSystem();
        if (system.getCooldownRemaining() > 0) return;
        if (system.isActive()) return;
        if (!system.getId().equals("microburn")) return;
        if (ship.isRetreating()) return;

        // Set viewfinder
        WeaponAPI viewfinder = null;
        WeaponAPI viewfinderAlly = null;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals("VIEWFINDER"))
                viewfinder = w;
            if (w.getSlot().getId().equals("VIEWFINDER_ALLY"))
                viewfinderAlly = w;
        }
        if (viewfinder == null) return;

        // Get closest enemy target
        ShipAPI target = null;
        float distance = 10000f;
        int enemy = 0;
        if (ship.getOwner() == 0)
            enemy = 1;
        for (ShipAPI f : Global.getCombatEngine().getShips()) {
            if (f.getOwner() == enemy && !f.isPhased()){
                Vector2f dir = Vector2f.sub(f.getLocation(), ship.getLocation(), new Vector2f());
                float newDistance = dir.length() - ship.getShieldRadiusEvenIfNoShield() - f.getShieldRadiusEvenIfNoShield();
                if (viewfinder.distanceFromArc(f.getLocation()) <= newDistance * OFFCENTER_DISTANCE_RATIO) {
                    if (newDistance < distance || target == null) {
                        distance = newDistance;
                        target = f;
                    }
                }
            }
        }
        if (target == null) return;

        // AI trigger procs much more often when flanking enemy
        float flanking = 1f;
        float angle = normalizeAngle(target.getFacing()) - normalizeAngle(ship.getFacing());
        angle = normalizeAngle(angle);
        if (angle <= FLANKING_RANGE * 0.5f || angle >= 360f - (FLANKING_RANGE * 0.5f) || target.getFluxTracker().isOverloaded()) {
            flanking = FLANKING_MULT;
        }

        // Trigger is random chance based on size of enemy, flux level, and flanking status
        if (Math.random() > ((float) approachComfort.get(target.getHullSize()) + extraMad) * flanking * (1 + (FLUX_TOLERANCE * (target.getFluxLevel() - ship.getFluxLevel()))))
            return;

        // Will never proc outside of certain range - more strict vs bigger ships
        if (distance < 50f || distance > 1000f) return;
        if (distance < 50f && (target.getHullSize() == HullSize.CRUISER || target.getHullSize() == HullSize.CAPITAL_SHIP)) {
            extraMad += 1.2f;
            return;
        }

        // Will not activate system if ally is within bump range, and between ship and enemy
        for (ShipAPI f : Global.getCombatEngine().getShips()) {
            if (f.getOwner() == ship.getOwner() || f.isAlly()) {
                if (viewfinderAlly != null && viewfinderAlly.distanceFromArc(f.getLocation()) <= 0f && f != ship && f.getHullSize() != HullSize.FIGHTER && !f.isPhased()) {
                    float allyDistance = Vector2f.sub(f.getLocation(), ship.getLocation(), new Vector2f()).length() - ship.getShieldRadiusEvenIfNoShield() - f.getShieldRadiusEvenIfNoShield();
                    if (allyDistance <= distance)
                        return;
                }
            }
        }
        extraMad = 0f;
        ship.useSystem();

    }

}