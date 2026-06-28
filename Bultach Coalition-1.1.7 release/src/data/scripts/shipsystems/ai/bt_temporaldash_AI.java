package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class bt_temporaldash_AI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;

    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.25f);

    private static final float FIGHTER_CRITICAL_RANGE = 500f;
    private static final float FIGHTER_STRIKE_RANGE = 1200f;

    private static final boolean DEBUG = false;
    private final Object STATUSKEY1 = new Object();
    private final Object STATUSKEY2 = new Object();
    private float desireShow = 0f;
    private float targetDesireShow = 0f;
    private float angleToTargetShow = 0f;

    private final Map<ShipAPI.HullSize, Float> strafeMulti = new HashMap<>();
    {
        strafeMulti.put(ShipAPI.HullSize.FIGHTER, 1f);
        strafeMulti.put(ShipAPI.HullSize.FRIGATE, 1f);
        strafeMulti.put(ShipAPI.HullSize.DESTROYER, 0.75f);
        strafeMulti.put(ShipAPI.HullSize.CRUISER, 0.5f);
        strafeMulti.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.25f);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        tracker.advance(amount);

        if (tracker.intervalElapsed()) {
            float desire = 0f;
            CombatEntityAPI immediateTarget = null;

            boolean returning = false;
            if ((ship.getWing() != null) && ship.getWing().isReturning(ship)) {
                returning = true;
            }

            float engageRange;
            if (ship.getWing() != null) {
                if (returning) {
                    engageRange = 500f;
                } else {
                    engageRange = ship.getWing().getSpec().getAttackRunRange();
                }
            } else {
                engageRange = 700f;
                for (WeaponAPI weapon : ship.getUsableWeapons()) {
                    if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                        continue;
                    }
                    if (weapon.getRange() > engageRange * 0.9f) {
                        engageRange = weapon.getRange() * 0.9f;
                    }
                }
            }

            ShipAPI carrier = null;
            if ((ship.getWing() != null) && ship.getWing().getSourceShip() != null) {
                carrier = ship.getWing().getSourceShip();
                if (returning) {
                    immediateTarget = carrier;
                } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI) {
                    immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET);
                } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI) {
                    immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                } else {
                    immediateTarget = carrier.getShipTarget();
                }
            }
            if ((immediateTarget == null) && ship.isFighter() && (flags.getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI)) {
                immediateTarget = (CombatEntityAPI) flags.getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET);
            }
            if ((immediateTarget == null) && (flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI)) {
                immediateTarget = (CombatEntityAPI) flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
            }
            if (immediateTarget == null) {
                immediateTarget = ship.getShipTarget();
            }

            CombatFleetManagerAPI.AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);
            Vector2f targetSpot;
            if (ship.isFighter()) {
                assignment = null;
                targetSpot = null;
            } else {
                if ((assignment != null) && (assignment.getTarget() != null) && (assignment.getType() != CombatAssignmentType.AVOID)) {
                    targetSpot = assignment.getTarget().getLocation();
                } else {
                    targetSpot = null;
                }
            }

            Vector2f newVector = new Vector2f();
            if (ship.getEngineController().isAccelerating()) {
                newVector.y += 1 * ship.getAcceleration();
            }
            if(ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()){
                newVector.y -= 1 * ship.getDeceleration();
            }
            if (ship.getEngineController().isStrafingLeft()) {
                newVector.x -=  1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
            }
            if (ship.getEngineController().isStrafingRight()) {
                newVector.x += 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
            }
            VectorUtils.rotate(newVector, ship.getFacing() - 90);
            if (VectorUtils.isZeroVector(newVector)) newVector = new Vector2f(ship.getVelocity());
            Vector2f direction = newVector;
            if (direction.lengthSquared() == 0 && ship.getVelocity().lengthSquared() > 0) {
                direction = new Vector2f(ship.getVelocity());
            } else if (direction.lengthSquared() == 0) {
                direction = Misc.getUnitVectorAtDegreeAngle(ship.getFacing());
            }
            Misc.normalise(direction);


            float range = (600f + ship.getMaxSpeed()) * system.getChargeActiveDur() * 1.1f;

            float angleToTargetSpot = 0f;
            if (targetSpot != null) {
                float targetSpotDir = VectorUtils.getAngleStrict(ship.getLocation(), targetSpot);
                angleToTargetSpot = MathUtils.getShortestRotation(VectorUtils.getFacing(direction), targetSpotDir);
            }
            float angleToImmediateTarget = 0f;
            if (immediateTarget != null) {
                float immediateTargetDir = VectorUtils.getAngleStrict(ship.getLocation(), immediateTarget.getLocation());
                angleToImmediateTarget = MathUtils.getShortestRotation(VectorUtils.getFacing(direction), immediateTargetDir);
            }
            angleToTargetShow = angleToImmediateTarget;

            float onTargetThreshold;
            if (ship.isFighter()) {
                onTargetThreshold = 45f;
            } else {
                onTargetThreshold = 60f;
            }

            if (immediateTarget != null && (ship.getEngineController().isStrafingLeft() || ship.getEngineController().isStrafingRight())) {
                float angleOfDashToTarget = Math.abs(angleToImmediateTarget);
                if (angleOfDashToTarget > 45f && angleOfDashToTarget < 135f) {
                    desire += 1.0f;
                }
            }

            if (!ship.isFighter()) {
                if (ship.getFluxTracker().getFluxLevel() > 0.65f) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= 75f) {
                            desire += 1.8f;
                        } else if (Math.abs(angleToImmediateTarget) > 75f && Math.abs(angleToImmediateTarget) < 135f) {
                            desire += 1.2f;
                        } else {
                            desire += 0.2f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }

                if (flags.hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) >= 135f) {
                            desire += 0.1f;
                        } else if (Math.abs(angleToImmediateTarget) >= 90f) {
                            desire += 0.3f;
                        } else {
                            desire += 0.5f;
                        }
                    } else {
                        desire += 0.3f;
                    }
                }
                if (flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire += 1.0f;
                        } else if (Math.abs(angleToImmediateTarget) <= 90f) {
                            desire += 0.6f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.7f;
                        }
                    } else {
                        desire += 0.25f;
                    }
                }
                if (flags.hasFlag(ShipwideAIFlags.AIFlags.HARASS_MOVE_IN)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire += 1.2f;
                        } else if (Math.abs(angleToImmediateTarget) <= 90f) {
                            desire += 0.7f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.9f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }
                if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) >= 135f) {
                            desire -= 0.5f;
                        } else if (Math.abs(angleToImmediateTarget) >= 110f) {
                            desire += 0.1f;
                        }
                    } else {
                        desire -= 0.2f;
                    }
                }
                if (ship.getEngineController().isAcceleratingBackwards() &&
                        !ship.getEngineController().isStrafingLeft() &&
                        !ship.getEngineController().isStrafingRight() &&
                        immediateTarget != null && Math.abs(angleToImmediateTarget) > 150f) {
                    desire -= 1.2f;
                }


                if (flags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP) || flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= 45f) {
                            desire -= 1.5f;
                        } else if (Math.abs(angleToImmediateTarget) >= 135f) {
                            desire += 0.8f;
                        } else {
                            desire += 0.6f;
                        }
                    } else {
                        desire += 0.7f;
                    }
                }

                if (flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= 45f) {
                            desire -= 0.5f;
                        } else if (Math.abs(angleToImmediateTarget) >= 135f) {
                            desire += 0.5f;
                        } else {
                            desire += 0.7f;
                        }
                    } else {
                        desire += 0.6f;
                    }
                }
                if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire -= 1.0f;
                        }
                    } else {
                        desire -= 0.5f;
                    }
                }

                if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX)) {
                    desire += 0.5f;
                }

                if (flags.hasFlag(ShipwideAIFlags.AIFlags.TURN_QUICKLY)) {
                    desire += 0.4f;
                }

            } else {
                if (!returning && flags.hasFlag(ShipwideAIFlags.AIFlags.IN_ATTACK_RUN)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire -= 1f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                                // In optimal strike range, maybe don't dash unless it's for a specific reason like dodging
                            } else {
                                desire += 1.25f;
                            }
                        } else if (Math.abs(angleToImmediateTarget) >= (180f - onTargetThreshold)) {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire += 1.25f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                                if (flags.hasFlag(ShipwideAIFlags.AIFlags.POST_ATTACK_RUN)) {
                                    desire += 1f;
                                }
                            } else {
                                desire -= 1.25f;
                            }
                        } else {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire += 1f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                                if (flags.hasFlag(ShipwideAIFlags.AIFlags.POST_ATTACK_RUN)) {
                                    desire += 1.25f;
                                } else {
                                    desire += 0.75f;
                                }
                            }
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 1f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }
                if (flags.hasFlag(ShipwideAIFlags.AIFlags.WANTED_TO_SLOW_DOWN)) {
                    desire -= 0.5f;
                }
            }

            boolean immediateTargetInRange = false;
            if ((immediateTarget != null) && (MathUtils.getDistance(immediateTarget, ship) < (engageRange - ship.getCollisionRadius() - ship.getCollisionRadius()))) {
                immediateTargetInRange = true;
            }

            if (!ship.isFighter() && immediateTarget != null && !immediateTargetInRange) {
                if ((carrier == null) || !carrier.isPullBackFighters() || (immediateTarget == carrier)) {
                    if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                        desire += 0.6f;
                    } else if (Math.abs(angleToImmediateTarget) <= 90f) {
                        desire += 0.3f;
                    }
                }
            }

            if (!ship.isFighter()) {
                float desiredRangeFromAssignment = 500f;
                if ((assignment != null) &&
                        ((assignment.getType() == CombatAssignmentType.ENGAGE) ||
                                (assignment.getType() == CombatAssignmentType.HARASS) ||
                                (assignment.getType() == CombatAssignmentType.INTERCEPT) ||
                                (assignment.getType() == CombatAssignmentType.LIGHT_ESCORT) ||
                                (assignment.getType() == CombatAssignmentType.MEDIUM_ESCORT) ||
                                (assignment.getType() == CombatAssignmentType.HEAVY_ESCORT) ||
                                (assignment.getType() == CombatAssignmentType.STRIKE))) {
                    desiredRangeFromAssignment = engageRange;
                }

                if (targetSpot != null && (MathUtils.getDistance(targetSpot, ship.getLocation()) >= desiredRangeFromAssignment) && !immediateTargetInRange) {
                    if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                        desire += 0.5f;
                    }
                }
            } else {
                if (returning && !immediateTargetInRange) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold + 15f) {
                            desire += 2.5f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold + 15f) {
                            desire += 2.5f;
                        }
                    } else {
                        desire += 1.5f;
                    }
                }
            }

            if (immediateTarget instanceof ShipAPI && !ship.isFighter()) {
                ShipAPI enemyShip = (ShipAPI) immediateTarget;
                if (enemyShip.getFluxTracker().getFluxLevel() > 0.85f || enemyShip.getFluxTracker().isVenting()) {
                    if (Math.abs(angleToImmediateTarget) <= 60f) {
                        desire += 1.5f;
                    } else if (Math.abs(angleToImmediateTarget) <= 90f) {
                        desire += 0.7f;
                    }
                }
            }

            if (!ship.isFighter() && (assignment != null) && (assignment.getType() == CombatAssignmentType.RETREAT)) {
                float retreatDirectionMapping = (ship.getOwner() == 0) ? 270f : 90f;
                float angleToRetreatDir = Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(direction), retreatDirectionMapping));

                if (angleToRetreatDir <= onTargetThreshold) {
                    if (ship.getFluxTracker().getFluxLevel() > 0.85f) {
                        desire += 0.5f;
                    } else {
                        desire -= 0.5f;
                    }
                } else if (Math.abs(angleToImmediateTarget) > 90f && Math.abs(angleToImmediateTarget) < 150f) {
                    desire += 0.3f;
                }
                else {
                    desire -= 2.0f;
                }
            }


            List<ShipAPI> directTargets = CombatUtils.getShipsWithinRange(ship.getLocation(), range);
            if (!directTargets.isEmpty() && !ship.isFighter()) {
                Vector2f endpoint = new Vector2f(direction);
                endpoint.scale(range);
                Vector2f.add(endpoint, ship.getLocation(), endpoint);

                Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(ship.getLocation()));
                ListIterator<ShipAPI> iter = directTargets.listIterator();
                while (iter.hasNext()) {
                    ShipAPI tmp = iter.next();
                    if ((tmp != ship) && (ship.getCollisionClass() != CollisionClass.NONE) && !tmp.isFighter() && !tmp.isDrone()) {
                        Vector2f loc = tmp.getLocation();
                        float collisionRadiusFactor = 0.75f;
                        if (tmp.getOwner() == ship.getOwner()) {
                            collisionRadiusFactor = 1.25f;
                        }

                        if (CollisionUtils.getCollides(ship.getLocation(), endpoint, loc,
                                (tmp.getCollisionRadius() * 0.6f) + (ship.getCollisionRadius() * collisionRadiusFactor))) {

                            float threatLevel = 0f;
                            switch(tmp.getHullSize()){
                                case CAPITAL_SHIP: threatLevel = ship.isCapital() ? 1f : (ship.isCruiser() ? 2f : (ship.isDestroyer() ? 4f : 8f)); break;
                                case CRUISER:    threatLevel = ship.isCapital() ? 2f : (ship.isCruiser() ? 1f : (ship.isDestroyer() ? 2f : 4f)); break;
                                case DESTROYER:  threatLevel = ship.isCapital() ? 4f : (ship.isCruiser() ? 2f : (ship.isDestroyer() ? 1f : 2f)); break;
                                case FRIGATE:    threatLevel = ship.isCapital() ? 8f : (ship.isCruiser() ? 4f : (ship.isDestroyer() ? 2f : 1f)); break;
                            }
                            if(tmp.isHulk()) threatLevel *= 0.5f;
                            if(tmp.getOwner() == ship.getOwner()) threatLevel *= 1.5f;

                            if (tmp == immediateTarget && Math.abs(angleToImmediateTarget) < 45f) {
                                desire -= threatLevel * 0.5f;
                            } else {
                                desire -= threatLevel;
                            }
                        }
                    }
                }
            }

            float targetDesire;
            if (system.getMaxAmmo() <= 2) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.2f;
                } else {
                    targetDesire = 0.7f;
                }
            } else if (system.getMaxAmmo() == 3) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.3f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 0.8f;
                } else {
                    targetDesire = 0.5f;
                }
            } else if (system.getMaxAmmo() == 4) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.4f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 0.9f;
                } else if (system.getAmmo() == 3) {
                    targetDesire = 0.6f;
                } else {
                    targetDesire = 0.45f;
                }
            } else {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.5f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 1.1f;
                } else if (system.getAmmo() == 3) {
                    targetDesire = 0.8f;
                } else if (system.getAmmo() == 4) {
                    targetDesire = 0.6f;
                } else {
                    targetDesire = 0.4f;
                }
            }
            if (ship.getFluxTracker().getFluxLevel() > 0.7f) {
                targetDesire *= 0.8;
            }


            desireShow = desire;
            targetDesireShow = targetDesire;

            if (desire >= targetDesire) {
                ship.useSystem();
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.engine = engine;
    }
}