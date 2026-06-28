package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_divine_redirector extends BaseShipSystemScript {

    private static final float ANGLE_FORCE_MULTIPLIER = 1f;
    private static final Map<DamageType, Float> DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS = new HashMap<>(5);
    private static final float POWER_PER_FRIENDLY_DAMAGE_ABSORBED = 0.003f;
    private static final float POWER_PER_HOSTILE_DAMAGE_ABSORBED = 0.003f;
    private static final float MAX_ABSORBED_POWER_CAP = 15.0f;
    private static final float TEXT_AMOUNT_MULTIPLIER = 1000.0f;
    private static final Color TEXT_COLOR = new Color(255, 251, 180, 0);
    private static final float SPARK_BRIGHTNESS = 0.8f;
    private static final Color SPARK_COLOR = new Color(255, 250, 186);
    private static final float SPARK_DURATION = 0.2f;
    private static final float SPARK_RADIUS = 5f;
    private static final float VELOCITY_FORCE_MULTIPLIER = 2000f;

    private static final Color ARC_CORE_COLOR_SYSTEM_ACTIVE = new Color(255, 253, 220, 203);
    private static final Color ARC_FRINGE_COLOR_SYSTEM_ACTIVE = new Color(255, 235, 200, 100);
    private static final float ARC_THICKNESS_SYSTEM_ACTIVE = 10f;

    private static final float BOUNDS_ARC_INTERVAL_MIN = 0.08f;
    private static final float BOUNDS_ARC_INTERVAL_MAX = 0.15f;
    private static final int BOUNDS_ARCS_PER_INTERVAL = 2;
    private static final float PROJ_ARC_INTERVAL_MIN = 0.25f;
    private static final float PROJ_ARC_INTERVAL_MAX = 0.5f;

    private static final String RIFT_VARIANT_ID = "ork_vortex_drone_standard";

    private static final int MAX_RIFTS_TO_SPAWN = 6;
    private static final float POWER_PER_RIFT = MAX_ABSORBED_POWER_CAP / MAX_RIFTS_TO_SPAWN;

    private static final float RIFT_SPAWN_RANDOM_ARC_DEGREES = 170f;
    private static final float RIFT_SPAWN_MIN_DISTANCE_FACTOR = 0.75f;
    private static final float RIFT_SPAWN_MAX_DISTANCE_FACTOR = 2.35f;
    private static final float RIFT_SPAWN_BASE_OFFSET = 70f;

    private static final Color RIFT_SUMMON_ARC_CORE_COLOR = new Color(255, 248, 220, 255);
    private static final Color RIFT_SUMMON_ARC_FRINGE_COLOR = new Color(255, 211, 142, 71);
    private static final float RIFT_SUMMON_ARC_THICKNESS = 50f;

    private static final float RIFT_SPAWN_RIPPLE_SIZE = 75f;
    private static final float RIFT_SPAWN_RIPPLE_INTENSITY = 15f;
    private static final float RIFT_SPAWN_RIPPLE_DURATION = 0.75f;
    private static final int NUM_ARCS_PER_RIFT = 3;


    static {
        DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.put(DamageType.ENERGY, 1.0f);
        DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.put(DamageType.FRAGMENTATION, 0.4f);
        DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.put(DamageType.HIGH_EXPLOSIVE, 1.0f);
        DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.put(DamageType.KINETIC, 1.0f);
        DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.put(DamageType.OTHER, 1.0f);
    }

    private float absorbedPower = 0;
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private IntervalUtil boundsArcTimer = new IntervalUtil(BOUNDS_ARC_INTERVAL_MIN, BOUNDS_ARC_INTERVAL_MAX);
    private IntervalUtil projArcTimer = new IntervalUtil(PROJ_ARC_INTERVAL_MIN, PROJ_ARC_INTERVAL_MAX);
    private boolean riftsSpawnedThisCycle = false;
    private List<ShipAPI> spawnedRiftsList = new ArrayList<>();

    private int riftsLeftToSpawnThisCycle = 0;
    private IntervalUtil riftSpawnIntervalTimer = new IntervalUtil(0.3f, 0.35f);
    private static final int MAX_SPAWN_LOCATION_ATTEMPTS = 15;
    private static final float MIN_RIFT_SEPARATION_FACTOR = 2f;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }

        this.ship = (ShipAPI) stats.getEntity();
        if (this.ship == null) return;

        this.engine = Global.getCombatEngine();
        if (this.engine == null || this.engine.isPaused()) return;

        float amount = this.engine.getElapsedInLastFrame();

        if (state == State.IN) {
            if (!this.riftsSpawnedThisCycle) {
                this.spawnedRiftsList.clear();
            }
            this.riftsLeftToSpawnThisCycle = 0;
            this.riftsSpawnedThisCycle = false;
        }

        if (state == State.IN || state == State.ACTIVE) {
            this.boundsArcTimer.advance(amount);
            if (this.boundsArcTimer.intervalElapsed()) {
                for (int i = 0; i < BOUNDS_ARCS_PER_INTERVAL; i++) {
                    Vector2f point1 = MathUtils.getRandomPointOnCircumference(this.ship.getLocation(), this.ship.getCollisionRadius());
                    Vector2f point2 = MathUtils.getRandomPointOnCircumference(this.ship.getLocation(), this.ship.getCollisionRadius());
                    this.engine.spawnEmpArcVisual(point1, this.ship, point2, this.ship, ARC_THICKNESS_SYSTEM_ACTIVE, ARC_FRINGE_COLOR_SYSTEM_ACTIVE, ARC_CORE_COLOR_SYSTEM_ACTIVE);
                }
            }
            this.projArcTimer.advance(amount);
            boolean canSpawnProjArc = this.projArcTimer.intervalElapsed();
            List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(this.ship.getLocation(), this.ship.getCollisionRadius() * 2.5f);
            for (CombatEntityAPI entity : entities) {
                if (!(entity instanceof DamagingProjectileAPI)) {
                    continue;
                }
                DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
                float angleDiff = Math.abs(MathUtils.getShortestRotation(this.ship.getFacing(), VectorUtils.getAngle(this.ship.getLocation(), proj.getLocation())));

                if (MathUtils.getDistance(this.ship, proj) <= this.ship.getCollisionRadius() && angleDiff <= 95f) {
                    absorbProjectile(proj);
                } else {
                    suckInProjectile(proj, state, effectLevel, canSpawnProjArc);
                }
            }
            this.ship.setPhased(true);
        }


        if (state == State.OUT) {
            this.ship.setPhased(false);

            if (!this.riftsSpawnedThisCycle && this.absorbedPower > 0) {
                int numTotalRifts = (int) (this.absorbedPower / POWER_PER_RIFT);
                this.riftsLeftToSpawnThisCycle = Math.min(numTotalRifts, MAX_RIFTS_TO_SPAWN);
                if (this.riftsLeftToSpawnThisCycle > 0) {
                    Global.getSoundPlayer().playSound("system_temporalshell", 1.0f, 1.0f, this.ship.getLocation(), this.ship.getVelocity());
                }
                this.riftsSpawnedThisCycle = true;
            }

            if (this.riftsLeftToSpawnThisCycle > 0) {
                this.riftSpawnIntervalTimer.advance(amount);
                if (this.riftSpawnIntervalTimer.intervalElapsed()) {
                    spawnOneRiftWithFXAndArcs();
                    this.riftsLeftToSpawnThisCycle--;
                }
            }
        }
    }

    private void spawnOneRiftWithFXAndArcs() {
        if (this.ship == null || this.engine == null) return;

        ShipVariantAPI riftBaseVariant = Global.getSettings().getVariant(RIFT_VARIANT_ID);
        if (riftBaseVariant == null) {
            Global.getLogger(this.getClass()).error("Could not load RIFT_VARIANT_ID: " + RIFT_VARIANT_ID);
            return;
        }
        ShipHullSpecAPI riftHullSpec = riftBaseVariant.getHullSpec();
        float riftCollisionRadius = (riftHullSpec != null) ? riftHullSpec.getCollisionRadius() : 30f;

        Vector2f spawnLocation = null;
        float spawnAngle = 0f;

        for (int attempt = 0; attempt < MAX_SPAWN_LOCATION_ATTEMPTS; attempt++) {
            float shipFacing = this.ship.getFacing();
            float arcCenterDegrees = shipFacing + 180f;
            float arcHalfWidth = RIFT_SPAWN_RANDOM_ARC_DEGREES / 2f;
            float randomAngleOffset = MathUtils.getRandomNumberInRange(-arcHalfWidth, arcHalfWidth);
            float currentSpawnAngle = MathUtils.clampAngle(arcCenterDegrees + randomAngleOffset);

            float randomDistanceFactor = MathUtils.getRandomNumberInRange(RIFT_SPAWN_MIN_DISTANCE_FACTOR, RIFT_SPAWN_MAX_DISTANCE_FACTOR);
            float currentSpawnDistance = (this.ship.getCollisionRadius() * randomDistanceFactor) + RIFT_SPAWN_BASE_OFFSET;
            Vector2f potentialLocation = MathUtils.getPointOnCircumference(this.ship.getLocation(), currentSpawnDistance, currentSpawnAngle);

            boolean overlaps = false;
            for (ShipAPI existingRift : this.spawnedRiftsList) {
                float combinedRadii = riftCollisionRadius + existingRift.getCollisionRadius();
                if (MathUtils.getDistance(potentialLocation, existingRift.getLocation()) < combinedRadii * MIN_RIFT_SEPARATION_FACTOR) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                spawnLocation = potentialLocation;
                spawnAngle = currentSpawnAngle;
                break;
            }
            if (attempt == MAX_SPAWN_LOCATION_ATTEMPTS - 1) {
                spawnLocation = potentialLocation;
                spawnAngle = currentSpawnAngle;
            }
        }

        if (spawnLocation == null) return;

        ShipVariantAPI riftVariantToSpawn = riftBaseVariant.clone();
        ShipAPI spawnedRift = this.engine.createFXDrone(riftVariantToSpawn);

        if (spawnedRift != null) {
            spawnedRift.setOwner(this.ship.getOwner());
            spawnedRift.getLocation().set(spawnLocation);
            spawnedRift.setFacing(spawnAngle);

            spawnedRift.setCollisionClass(CollisionClass.FIGHTER);
            spawnedRift.setDrone(true);
            if (this.ship.getFleetMember() != null && this.ship.getFleetMember().getFleetCommander() != null) {
                spawnedRift.setCaptain(this.ship.getFleetMember().getFleetCommander());
            }

            this.engine.addEntity(spawnedRift);
            this.spawnedRiftsList.add(spawnedRift);
            Global.getSoundPlayer().playSound("bt_vortex_summon", 0.6f, 1.3f, this.ship.getLocation(), this.ship.getVelocity());

            if (spawnedRift.getShipAI() != null) {
                spawnedRift.getShipAI().forceCircumstanceEvaluation();
            }

            RippleDistortion ripple = new RippleDistortion(spawnLocation, new Vector2f());
            ripple.setSize(RIFT_SPAWN_RIPPLE_SIZE);
            ripple.setIntensity(RIFT_SPAWN_RIPPLE_INTENSITY);
            ripple.setFrameRate(60f / RIFT_SPAWN_RIPPLE_DURATION);
            ripple.fadeInSize(RIFT_SPAWN_RIPPLE_DURATION * 0.25f);
            ripple.fadeOutIntensity(RIFT_SPAWN_RIPPLE_DURATION);
            DistortionShader.addDistortion(ripple);

            float angleToRiftCenter = VectorUtils.getAngle(this.ship.getLocation(), spawnedRift.getLocation());
            for (int arcNum = 0; arcNum < NUM_ARCS_PER_RIFT; arcNum++) {
                float angleVariance = (NUM_ARCS_PER_RIFT > 1) ? MathUtils.getRandomNumberInRange(-10f, 10f) : 0f;
                float finalArcAngle = MathUtils.clampAngle(angleToRiftCenter + angleVariance);
                Vector2f shipBoundPoint = MathUtils.getPointOnCircumference(this.ship.getLocation(), this.ship.getCollisionRadius(), finalArcAngle);

                Vector2f riftBoundPointOffset = MathUtils.getPointOnCircumference(null, spawnedRift.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.5f, 0.8f), MathUtils.getRandomNumberInRange(0, 360f));
                Vector2f riftHitLocation = Vector2f.add(spawnedRift.getLocation(), riftBoundPointOffset, new Vector2f());

                this.engine.spawnEmpArcVisual(shipBoundPoint, this.ship, riftHitLocation, spawnedRift,
                        RIFT_SUMMON_ARC_THICKNESS, RIFT_SUMMON_ARC_FRINGE_COLOR, RIFT_SUMMON_ARC_CORE_COLOR);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        this.absorbedPower = 0;
        this.riftsLeftToSpawnThisCycle = 0;
        this.riftsSpawnedThisCycle = false;
        if (this.ship != null) {
            this.ship.setPhased(false);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        int numRiftsPossible = (int) (this.absorbedPower / POWER_PER_RIFT);
        numRiftsPossible = Math.min(numRiftsPossible, MAX_RIFTS_TO_SPAWN);

        if (index == 0) {
            if (this.riftsLeftToSpawnThisCycle > 0 && state == State.OUT) {
                int totalToSpawn = Math.min((int)(this.absorbedPower / POWER_PER_RIFT), MAX_RIFTS_TO_SPAWN);
                if (totalToSpawn <=0 && !this.spawnedRiftsList.isEmpty() && this.riftsSpawnedThisCycle) {
                    totalToSpawn = this.spawnedRiftsList.size() + this.riftsLeftToSpawnThisCycle;
                } else if (totalToSpawn <= 0 && this.absorbedPower <=0) {
                    Object potentialMaxObj = null;
                    if (this.ship != null) { // Null check for ship before getCustomData
                        potentialMaxObj = this.ship.getCustomData().get("bt_divine_redirector_potential_max_rifts");
                    }
                    if (potentialMaxObj instanceof Integer) {
                        totalToSpawn = (Integer) potentialMaxObj;
                    } else {
                        totalToSpawn = MAX_RIFTS_TO_SPAWN;
                    }
                } else if (totalToSpawn <=0) {
                    totalToSpawn = MAX_RIFTS_TO_SPAWN;
                }

                int currentNum = totalToSpawn - this.riftsLeftToSpawnThisCycle +1;
                if (currentNum > totalToSpawn && totalToSpawn > 0) currentNum = totalToSpawn;
                else if (currentNum <=0 && totalToSpawn > 0) currentNum = 1;

                if (totalToSpawn > 0) {
                    return new StatusData(String.format("Summoning Rift (%d/%d)", currentNum, totalToSpawn ), false);
                } else {
                    return new StatusData("Summoning Rifts...", false);
                }
            } else if (state == State.IN || state == State.ACTIVE) {
                int potentialMax = (int)(MAX_ABSORBED_POWER_CAP / POWER_PER_RIFT);
                potentialMax = Math.min(potentialMax, MAX_RIFTS_TO_SPAWN);
                if (this.ship != null) { // Null check before setCustomData
                    this.ship.setCustomData("bt_divine_redirector_potential_max_rifts", potentialMax);
                }
                return new StatusData(String.format("Absorbing Power (%d units)", (int) (this.absorbedPower * 10)), false);
            } else if (state == State.OUT && this.riftsSpawnedThisCycle && !this.spawnedRiftsList.isEmpty() && this.riftsLeftToSpawnThisCycle == 0) {
                return new StatusData(String.format("Summoned %d Rifts", this.spawnedRiftsList.size()), false);
            } else if (state == State.OUT && !this.riftsSpawnedThisCycle && numRiftsPossible > 0){
                return new StatusData(String.format("Preparing to summon %d Rifts...", numRiftsPossible), false);
            } else {
                return new StatusData("Ready to absorb", false);
            }
        }
        return null;
    }

    private void absorbProjectile(DamagingProjectileAPI proj) {
        if (this.ship == null || this.engine == null || this.engine.isPaused()) return;

        this.engine.spawnEmpArcVisual(this.ship.getLocation(), this.ship, proj.getLocation(), proj, ARC_THICKNESS_SYSTEM_ACTIVE * 1.5f, ARC_FRINGE_COLOR_SYSTEM_ACTIVE, ARC_CORE_COLOR_SYSTEM_ACTIVE);

        float powerGained = proj.getDamageAmount();
        powerGained *= (proj.getOwner() == this.ship.getOwner()) ? POWER_PER_FRIENDLY_DAMAGE_ABSORBED : POWER_PER_HOSTILE_DAMAGE_ABSORBED;
        Float damageTypeMult = DAMAGE_TYPE_POWER_ABSORBTION_MULTIPLIERS.get(proj.getDamageType());
        if (damageTypeMult != null) {
            powerGained *= damageTypeMult;
        }

        this.engine.addFloatingDamageText(this.ship.getLocation(), powerGained * TEXT_AMOUNT_MULTIPLIER, TEXT_COLOR, this.ship, proj);
        this.absorbedPower += powerGained;
        this.absorbedPower = Math.min(this.absorbedPower, MAX_ABSORBED_POWER_CAP);

        float sparkAngle = VectorUtils.getAngle(proj.getLocation(), this.ship.getLocation());
        Vector2f sparkVel = Misc.getUnitVectorAtDegreeAngle(sparkAngle);
        float distance = MathUtils.getDistance(proj, this.ship);
        float visualEffect = (float) Math.sqrt(powerGained * 1000);
        visualEffect = Math.max(0.5f, Math.min(visualEffect, 3f));
        sparkVel.scale(3 * distance / SPARK_DURATION);
        this.engine.addHitParticle(proj.getLocation(), sparkVel, SPARK_RADIUS * visualEffect + SPARK_RADIUS, SPARK_BRIGHTNESS, SPARK_DURATION, SPARK_COLOR);

        this.engine.removeEntity(proj);
    }

    private void suckInProjectile(DamagingProjectileAPI proj, State state, float effectLevel, boolean spawnArc) {
        if (this.ship == null || this.engine == null || this.engine.isPaused()) return;

        float fromToAngle = VectorUtils.getAngle(this.ship.getLocation(), proj.getLocation());
        float angleDif = MathUtils.getShortestRotation(fromToAngle, this.ship.getFacing());
        float amount = this.engine.getElapsedInLastFrame();
        float distance = MathUtils.getDistance(this.ship.getLocation(), proj.getLocation());
        if (distance < 1f) distance = 1f;
        float force = (this.ship.getCollisionRadius() / distance) * effectLevel * ANGLE_FORCE_MULTIPLIER;

        if (Math.abs(angleDif) >= 70f) return;
        else if (Math.abs(angleDif) >= 60f) force *= (70f - Math.abs(angleDif)) / 10f;

        if (proj instanceof MissileAPI && proj.getOwner() != this.ship.getOwner()) {
            ((MissileAPI) proj).flameOut();
        }

        float dAngle = angleDif * amount * force;
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(fromToAngle);
        direction.scale(VELOCITY_FORCE_MULTIPLIER * amount * effectLevel);

        Vector2f.add(proj.getVelocity(), (Vector2f) direction.negate(), proj.getVelocity());
        proj.setFacing(MathUtils.clampAngle(proj.getFacing() - dAngle));

        if (spawnArc) {
            Vector2f boundsPoint = MathUtils.getPointOnCircumference(this.ship.getLocation(), this.ship.getCollisionRadius(), VectorUtils.getAngle(this.ship.getLocation(), proj.getLocation()));
            this.engine.spawnEmpArcVisual(boundsPoint, this.ship, proj.getLocation(), proj, ARC_THICKNESS_SYSTEM_ACTIVE, ARC_FRINGE_COLOR_SYSTEM_ACTIVE, ARC_CORE_COLOR_SYSTEM_ACTIVE);
        }
    }
}