package data.scripts.shipsystems.convergence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.hullmods.bt_drone_construction_fx;
import data.scripts.shipsystems.convergence.ConvergenceSwarmEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ConvergenceShipConstructionScript extends BaseEveryFrameCombatPlugin {

    public static String SWARM_CONSTRUCTING_SHIP_TAG = "convergence_swarm_constructing_ship_drone";
    public static String SHIP_UNDER_CONSTRUCTION_TAG = "convergence_ship_under_construction";
    public static final String CONSTRUCTOR_FACTION_ID = "gestalt";
    public static float FADE_IN_RATE_MULT_WHEN_DESTROYED = 10f;

    protected float elapsed = 0f;
    protected ShipAPI shipToConstruct = null;
    protected CollisionClass finalCollisionClass;
    protected String variantIdToConstruct;
    protected ShipAPI constructorDroneSource;
    protected float constructionDelay;
    protected float constructionFadeInTime;
    protected List<ShipAPI> explodedPieces = new ArrayList<>();
    protected IntervalUtil particleStreamInterval;
    protected IntervalUtil tentacleSpawnInterval;
    protected ShipAPI mothershipRef;
    protected String preferredOriginSlotID;

    private static final Color DIVINE_GOLD_BRIGHT = new Color(255, 242, 195, 220);
    private static final Color DIVINE_GOLD_MEDIUM = new Color(255, 243, 195, 124);
    private static final Color DIVINE_GOLD_ETHEREAL_CORE = new Color(255, 239, 210, 158);
    private static final Color DIVINE_GOLD_ETHEREAL_FRINGE = new Color(255, 245, 223, 47);
    private static final Color DIVINE_GOLD_TENTACLE_MAIN = new Color(255, 233, 205, 180);
    private static final Color DIVINE_GOLD_TENTACLE_HIGHLIGHT = new Color(255, 244, 212, 136);
    private static final Color DIVINE_GOLD_TENTACLE_FADE = new Color(255, 239, 189, 30);

    public static final String TENTACLE_SPAWN_SOUND_ID = "bt_convergence_tentacle_spawn";

    public ConvergenceShipConstructionScript(String variantId, ShipAPI constructorDrone, float delay, float fadeInTime,
                                             String preferredOriginSlotID, ShipAPI mothership) {
        this.variantIdToConstruct = variantId;
        this.constructorDroneSource = constructorDrone;
        this.constructionDelay = delay;
        this.constructionFadeInTime = fadeInTime;
        this.mothershipRef = mothership;
        this.preferredOriginSlotID = preferredOriginSlotID;
        this.particleStreamInterval = new IntervalUtil(0.03f, 0.06f);
        this.tentacleSpawnInterval = new IntervalUtil(1f, 1.8f);
        spawnShipToConstruct();
    }

    private Vector2f updateAndGetMothershipParticleOrigin() {
        if (mothershipRef == null || !mothershipRef.isAlive()) {
            return constructorDroneSource != null ? new Vector2f(constructorDroneSource.getLocation()) : new Vector2f();
        }
        if (this.preferredOriginSlotID != null && !this.preferredOriginSlotID.isEmpty()) {
            for (WeaponSlotAPI slot : mothershipRef.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.getId().equals(this.preferredOriginSlotID)) {
                    return slot.computePosition(mothershipRef);
                }
            }
        }
        return new Vector2f(mothershipRef.getLocation());
    }

    protected void spawnShipToConstruct() {
        if (constructorDroneSource == null || !constructorDroneSource.isAlive()) {
            cleanupAndRemovePlugin();
            return;
        }

        Vector2f spawnLoc = new Vector2f(constructorDroneSource.getLocation());
        float spawnFacing = constructorDroneSource.getFacing();

        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManager = engine.getFleetManager(constructorDroneSource.getOwner());
        boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
        fleetManager.setSuppressDeploymentMessages(true);

        shipToConstruct = fleetManager.spawnShipOrWing(variantIdToConstruct, spawnLoc, spawnFacing, 0f, null);

        fleetManager.setSuppressDeploymentMessages(wasSuppressed);

        if (shipToConstruct == null) {
            cleanupAndRemovePlugin();
            return;
        }

        if (constructorDroneSource != null) {
            float fxScale = 1f;
            switch (shipToConstruct.getHullSize()) {
                case FIGHTER:       fxScale = 0.6f; break;
                case FRIGATE:       fxScale = 1.35f; break;
                case DESTROYER:     fxScale = 1.4f; break;
                case CRUISER:       fxScale = 1.65f; break;
                case CAPITAL_SHIP:  fxScale = 1.5f; break;
                default:            fxScale = 1f;   break;
            }
            constructorDroneSource.setCustomData(bt_drone_construction_fx.DRONE_FX_SCALE_FACTOR_KEY, fxScale);
        }

        if (Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().isInCampaignSim()) {
            FactionAPI faction = Global.getSector().getFaction(CONSTRUCTOR_FACTION_ID);
            if (faction != null) shipToConstruct.setName(faction.pickRandomShipName());
        }

        finalCollisionClass = shipToConstruct.getCollisionClass();
        shipToConstruct.setCollisionClass(CollisionClass.NONE);
        shipToConstruct.getVelocity().set(0,0);

        ConvergenceSwarmEffect existingSwarmOnConstructedShip = ConvergenceSwarmEffect.getSwarmFor(shipToConstruct);
        if (existingSwarmOnConstructedShip != null) {
            existingSwarmOnConstructedShip.getParams().withInitialMembers = false;
            existingSwarmOnConstructedShip.getParams().withRespawn = false;
        }

        shipToConstruct.addTag(SHIP_UNDER_CONSTRUCTION_TAG);
        constructorDroneSource.addTag(SWARM_CONSTRUCTING_SHIP_TAG);
        if (constructorDroneSource.getMutableStats() != null) {
            constructorDroneSource.getMutableStats().getHullDamageTakenMult().modifyMult(SWARM_CONSTRUCTING_SHIP_TAG, 0f);
        }

        shipToConstruct.setShipAI(null);
        for (WeaponGroupAPI g : shipToConstruct.getWeaponGroupsCopy()) g.toggleOff();
        shipToConstruct.setControlsLocked(true);
        shipToConstruct.setAlphaMult(0f);
    }

    protected float hulkFor = 0f;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        if (shipToConstruct == null || constructorDroneSource == null || mothershipRef == null || !mothershipRef.isAlive()) {
            cleanupAndRemovePlugin();
            return;
        }

        if (!constructorDroneSource.isAlive() || constructorDroneSource.isHulk()) {
            if (shipToConstruct.isAlive() && !shipToConstruct.isHulk()) {
                engine.applyDamage(shipToConstruct, shipToConstruct.getLocation(), shipToConstruct.getMaxHitpoints() * 2f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, shipToConstruct, true);
            }
            cleanupAndRemovePlugin();
            return;
        }

        elapsed += amount;
        if (elapsed < constructionDelay) return;

        float currentConstructionTime = elapsed - constructionDelay;
        float progress = Math.min(1f, currentConstructionTime / constructionFadeInTime);

        shipToConstruct.getLocation().set(constructorDroneSource.getLocation());
        shipToConstruct.getVelocity().set(constructorDroneSource.getVelocity());
        shipToConstruct.setFacing(constructorDroneSource.getFacing());

        if (shipToConstruct.isHulk()) {
            hulkFor += amount;
            float acceleratedTimeContribution = hulkFor * (FADE_IN_RATE_MULT_WHEN_DESTROYED -1f);
            progress = Math.min(1f, (currentConstructionTime + acceleratedTimeContribution) / constructionFadeInTime);
            if (explodedPieces.isEmpty() || hulkFor < 0.25f) {
                explodedPieces.clear();
                for (ShipAPI curr : Global.getCombatEngine().getShips()) {
                    if (curr.getFleetMember() == shipToConstruct.getFleetMember() ||
                            (curr.getParentPieceId() != null && curr.getParentPieceId().equals(shipToConstruct.getId()))) {
                        explodedPieces.add(curr);
                    }
                }
            }
        }

        float shipSpriteAlpha;
        if (progress < 0.85f) {
            shipSpriteAlpha = progress * 0.15f;
        } else {
            shipSpriteAlpha = 0.1275f + ((progress - 0.85f) / 0.15f) * (1f - 0.1275f);
        }
        shipSpriteAlpha = Math.min(shipSpriteAlpha, progress * progress);
        shipToConstruct.setAlphaMult(shipSpriteAlpha);

        for (ShipAPI piece : explodedPieces) piece.setAlphaMult(shipSpriteAlpha);
        shipToConstruct.getMutableStats().getEffectiveArmorBonus().modifyMult(SHIP_UNDER_CONSTRUCTION_TAG, progress * progress);

        Global.getSoundPlayer().playLoop("bt_convergence_loop", shipToConstruct, 0.9f, 0.6f + 0.4f * progress, shipToConstruct.getLocation(), shipToConstruct.getVelocity());

        float jitterLevel = progress < 0.2f ? progress * 15f : (progress > 0.9f ? (1f - progress) * 15f : 1.25f);
        jitterLevel = (float) Math.sqrt(jitterLevel) * 1.1f;

        int jitterColorAlpha = 200 + (int)(progress * 55);
        if (progress > 0.85f) {
            jitterColorAlpha = (int) ( (200 + (0.85f * 55)) * (1f - (progress - 0.85f) / 0.15f) );
            jitterColorAlpha = Math.max(80, jitterColorAlpha);
        }
        Color jitterColor = new Color(DIVINE_GOLD_MEDIUM.getRed(), DIVINE_GOLD_MEDIUM.getGreen(), DIVINE_GOLD_MEDIUM.getBlue(), Math.min(255, jitterColorAlpha));

        float minJitterRange = 5f + progress * 5f;
        float maxJitterRange = 15f + progress * 15f;
        shipToConstruct.setJitter(this, jitterColor, jitterLevel, 3, minJitterRange, maxJitterRange);

        shipToConstruct.getEngineController().fadeToOtherColor(this, new Color(0,0,0,0), new Color(0,0,0,0), 1f, 1f);

        spawnParticleStream(amount, progress);
        tentacleSpawnInterval.advance(amount);
        if (tentacleSpawnInterval.intervalElapsed() && progress > 0.02f && progress < 0.99f ) {
            float sizeMultiplier = 1f;
            if (shipToConstruct.getHullSize() == HullSize.DESTROYER) sizeMultiplier = 1.25f;
            else if (shipToConstruct.getHullSize() == HullSize.CRUISER) sizeMultiplier = 1.5f;
            else if (shipToConstruct.getHullSize() == HullSize.CAPITAL_SHIP) sizeMultiplier = 1.75f;
            int baseNumTentacles = 1;
            int numTentacles = baseNumTentacles;
            if (progress > 0.25f && progress < 0.85f) {
                numTentacles = (int) Math.ceil(baseNumTentacles * sizeMultiplier * (0.75f + Misc.random.nextFloat() * 0.5f));
                numTentacles = Math.max(1, numTentacles);
            }
            for (int i=0; i < numTentacles; i++) {
                spawnTentacleVisual(mothershipRef, shipToConstruct, updateAndGetMothershipParticleOrigin(), progress);
            }
        }

        if (progress >= 1f) {
            if (!shipToConstruct.isHulk()) {
                shipToConstruct.setAlphaMult(1f);
                shipToConstruct.setJitter(this, new Color(0,0,0,0), 0f, 0, 0f,0f);
                shipToConstruct.setShipAI(Global.getSettings().createDefaultShipAI(shipToConstruct, new ShipAIConfig()));
                shipToConstruct.setControlsLocked(false);
                for (WeaponGroupAPI g : shipToConstruct.getWeaponGroupsCopy()) if(g.isAutofiring()) g.toggleOn();
                shipToConstruct.setHoldFire(false);
                shipToConstruct.setCollisionClass(finalCollisionClass);
                shipToConstruct.getVelocity().set(0,0);
                shipToConstruct.setAngularVelocity(0f);
            }
            shipToConstruct.removeTag(SHIP_UNDER_CONSTRUCTION_TAG);
            shipToConstruct.getMutableStats().getEffectiveArmorBonus().unmodify(SHIP_UNDER_CONSTRUCTION_TAG);
            ConvergenceSwarmEffect swarmOnDrone = ConvergenceSwarmEffect.getSwarmFor(constructorDroneSource);
            if (swarmOnDrone != null) swarmOnDrone.setForceDespawn(true);
            cleanupAndRemovePlugin();
        }
    }

    protected void spawnParticleStream(float amount, float overallProgress) {
        particleStreamInterval.advance(amount);
        if (!particleStreamInterval.intervalElapsed()) return;
        Vector2f currentOrigin = updateAndGetMothershipParticleOrigin();
        if (mothershipRef == null || !mothershipRef.isAlive() || constructorDroneSource == null || !constructorDroneSource.isAlive()) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f targetPoint = new Vector2f(constructorDroneSource.getLocation());
        float sizeMultiplier = 1f;
        if (shipToConstruct.getHullSize() == HullSize.DESTROYER) sizeMultiplier = 1.5f;
        else if (shipToConstruct.getHullSize() == HullSize.CRUISER) sizeMultiplier = 2f;
        else if (shipToConstruct.getHullSize() == HullSize.CAPITAL_SHIP) sizeMultiplier = 2.5f;
        int baseParticles = 3;
        int particlesThisFrame = baseParticles + (int) (Math.random() * (4 * sizeMultiplier));
        particlesThisFrame = Math.max(2, particlesThisFrame);
        for (int i = 0; i < particlesThisFrame; i++) {
            Vector2f from = MathUtils.getRandomPointInCircle(currentOrigin, 15f + mothershipRef.getCollisionRadius() * 0.03f);
            float angleToTarget = VectorUtils.getAngle(from, targetPoint);
            float randomAngleOffset = (float) (Math.random() - 0.5) * 10f;
            Vector2f particleVelocity = Misc.getUnitVectorAtDegreeAngle(angleToTarget + randomAngleOffset);
            float particleSpeed = 350f + (float)Math.random() * 450f;
            particleVelocity.scale(particleSpeed);
            Vector2f.add(particleVelocity, (Vector2f)new Vector2f(mothershipRef.getVelocity()).scale(0.5f), particleVelocity);
            float particleSize = (5.0f + (float)Math.random() * 12.0f) * (0.8f + sizeMultiplier * 0.2f);
            int coreAlpha = Math.min(255, 210 + (int)(overallProgress * 45));
            int fringeAlpha = Math.min(220,140 + (int)(overallProgress * 80));
            Color particleCoreColor = new Color(DIVINE_GOLD_ETHEREAL_CORE.getRed(), DIVINE_GOLD_ETHEREAL_CORE.getGreen(), DIVINE_GOLD_ETHEREAL_CORE.getBlue(), coreAlpha);
            Color particleFringeColor = new Color(DIVINE_GOLD_ETHEREAL_FRINGE.getRed(), DIVINE_GOLD_ETHEREAL_FRINGE.getGreen(), DIVINE_GOLD_ETHEREAL_FRINGE.getBlue(), fringeAlpha);
            float distanceToTarget = Misc.getDistance(from, targetPoint);
            float baseParticleDuration = (distanceToTarget / Math.max(1f, particleSpeed));
            float particleDuration = baseParticleDuration * (0.95f + (float)Math.random() * 0.3f);
            particleDuration = Math.max(0.4f, Math.min(particleDuration, 2.5f));
            engine.addSmoothParticle(from, particleVelocity, particleSize * 0.8f, 1f, particleDuration * 0.2f, particleDuration * 0.8f, particleCoreColor);
            engine.addSmoothParticle(from, (Vector2f)new Vector2f(particleVelocity).scale(0.9f), particleSize * 1.5f, 0.7f, particleDuration * 0.25f, particleDuration * 0.75f, particleFringeColor);
        }
    }

    private void spawnTentacleVisual(ShipAPI originShip, ShipAPI targetShip, Vector2f actualDynamicOriginPoint, float overallProgress) {
        if (originShip == null || !originShip.isAlive() || targetShip == null || !targetShip.isAlive()) {
            return;
        }
        Global.getSoundPlayer().playSound(TENTACLE_SPAWN_SOUND_ID, 1f, 0.2f + (float)Math.random()*0.2f, actualDynamicOriginPoint, Misc.ZERO);
        float id = MagicTrailPlugin.getUniqueID();
        Vector2f from = MathUtils.getRandomPointInCircle(actualDynamicOriginPoint, mothershipRef.getCollisionRadius() * 0.08f);
        Vector2f to = MathUtils.getRandomPointInCircle(targetShip.getLocation(), targetShip.getCollisionRadius() * 0.02f);
        float emmitAngle = VectorUtils.getAngle(from, to) + MathUtils.getRandomNumberInRange(-15f, 15f);
        Vector2f point = new Vector2f(from);
        float angle = emmitAngle;
        float waveAmplitude = MathUtils.getRandomNumberInRange(-40f, 40f);
        float waveFrequency = MathUtils.getRandomNumberInRange(8f, 13f);
        float arcMultiplier = MathUtils.getRandomNumberInRange(-0.5f, 0.5f);
        float distance = MathUtils.getDistance(from, to);
        float sizeMultiplier = 1f;
        if (targetShip.getHullSize() == HullSize.DESTROYER) sizeMultiplier = 1.35f;
        else if (targetShip.getHullSize() == HullSize.CRUISER) sizeMultiplier = 1.65f;
        else if (targetShip.getHullSize() == HullSize.CAPITAL_SHIP) sizeMultiplier = 2.25f;
        int segments = (int)((45 + (int)(Math.random() * 10)) * sizeMultiplier);
        float timeSinceConstructionStart = elapsed - constructionDelay;
        float tentacleLifetimeFactor = (constructionFadeInTime - timeSinceConstructionStart) / constructionFadeInTime;
        float baseTentacleLifetime = Math.max(1.5f, tentacleLifetimeFactor * (constructionFadeInTime * 0.6f) + 2.0f);
        baseTentacleLifetime = Math.min(baseTentacleLifetime, 5.0f);
        float fadeInSegment = 0.25f + overallProgress * 0.15f;
        float fullOpacitySegment = 0.2f + overallProgress * 0.25f;
        float fadeOutParameter = baseTentacleLifetime * 0.7f;
        for (Integer i = 0; i < segments; i++) {
            float waveSine = (float)Math.sin(i / waveFrequency);
            float waveOffsetSine = (float)Math.sin((i / waveFrequency) + MathUtils.FPI / 2);
            float normalizedProgressSegment = (float)i / (float)(segments -1);
            float arcIntensity = (float)(1.0 - Math.pow(2.0 * normalizedProgressSegment - 1.0, 2.0));
            point = MathUtils.getPoint(point, distance / (float)(segments -1), angle + waveSine * waveAmplitude * arcIntensity);
            Vector2f dirToTarget = new Vector2f();
            Vector2f.sub(to, point, dirToTarget);
            if (dirToTarget.lengthSquared() > 0.001f) {
                angle += MathUtils.getShortestRotation(angle, VectorUtils.getFacing(dirToTarget)) / (18f + i * 0.35f) ;
            }
            angle = MathUtils.clampAngle(angle);
            int alpha = 150 + (int)(arcIntensity * 90) - (int)(normalizedProgressSegment * 80);
            alpha = MathUtils.clamp((int)(alpha * (0.75f + overallProgress * 0.25f)), 60, 230);
            Color trailColor = new Color(DIVINE_GOLD_TENTACLE_HIGHLIGHT.getRed(), DIVINE_GOLD_TENTACLE_HIGHLIGHT.getGreen(), DIVINE_GOLD_TENTACLE_HIGHLIGHT.getBlue(), alpha);
            Color endTrailColor = new Color(DIVINE_GOLD_TENTACLE_FADE.getRed(), DIVINE_GOLD_TENTACLE_FADE.getGreen(), DIVINE_GOLD_TENTACLE_FADE.getBlue(), Math.max(40, alpha/3));
            MagicTrailPlugin.addTrailMemberAdvanced(
                    originShip, id, Global.getSettings().getSprite("fx", "bt_convergence_trail"), point, 0f, 0f,
                    angle + waveOffsetSine * waveAmplitude * arcIntensity, 0f, 0f,
                    (MathUtils.getRandomNumberInRange(26f, 40f) + (18f * arcMultiplier * arcIntensity) * (0.6f + overallProgress * 0.4f)) * sizeMultiplier,
                    (MathUtils.getRandomNumberInRange(2f, 8f)) * sizeMultiplier,
                    trailColor, endTrailColor,
                    0.6f + (0.2f * arcIntensity) - (normalizedProgressSegment * 0.25f),
                    fadeInSegment, fullOpacitySegment, fadeOutParameter, true, 256f, -80f - (float)Math.random() * 80f,
                    new Vector2f(), null, null, 1f
            );
        }
    }

    private void cleanupAndRemovePlugin() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (constructorDroneSource != null && constructorDroneSource.isAlive()) {
            constructorDroneSource.setCustomData(bt_drone_construction_fx.DRONE_SHOULD_FADE_OUT_KEY, true);
            constructorDroneSource.setCollisionClass(CollisionClass.NONE);
            if (constructorDroneSource.getShipAI() != null) {
                constructorDroneSource.setShipAI(null);
            }
            constructorDroneSource.setControlsLocked(true);

            Vector2f droneLocation = new Vector2f(constructorDroneSource.getLocation());
            Vector2f droneVelocity = new Vector2f(constructorDroneSource.getVelocity());
            float fxScaleMultiplier = 1f;
            int particleCountMultiplier = 1;
            if (shipToConstruct != null) {
                switch (shipToConstruct.getHullSize()) {
                    case FIGHTER: fxScaleMultiplier = 0.75f; break;
                    case FRIGATE: fxScaleMultiplier = 1f; break;
                    case DESTROYER: fxScaleMultiplier = 1.5f; particleCountMultiplier = (int) (1 * 1.5f); break;
                    case CRUISER: fxScaleMultiplier = 2.15f; particleCountMultiplier = (int) (1 * 2f); break;
                    case CAPITAL_SHIP: fxScaleMultiplier = 2.25f; particleCountMultiplier = (int) (1 * 2.5f); break;
                    default: fxScaleMultiplier = 1f; break;
                }
            }

            if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
                MagicRender.battlespace(
                        Global.getSettings().getSprite("fx", "bultach_holy_explosion_shockwave"),
                        droneLocation, new Vector2f(droneVelocity),
                        new Vector2f(50 * fxScaleMultiplier, 50 * fxScaleMultiplier),
                        new Vector2f(300 * fxScaleMultiplier, 300 * fxScaleMultiplier),
                        360 * (float) Math.random(), 30,
                        new Color(255, 242, 195, 100), true, 0, 0.1f, 0.8f
                );
                MagicRender.battlespace(
                        Global.getSettings().getSprite("fx", "bt_cleave_cloud"),
                        droneLocation, new Vector2f(droneVelocity),
                        new Vector2f(80 * fxScaleMultiplier, 80 * fxScaleMultiplier),
                        new Vector2f(180 * fxScaleMultiplier, 180 * fxScaleMultiplier),
                        360 * (float) Math.random(), 10,
                        new Color(255, 239, 210, 180), true, 0.05f, 0.15f, 1.1f
                );
                int baseParticleCount = 20;
                for (int i = 0; i < baseParticleCount * particleCountMultiplier; i++) {
                    engine.addHitParticle(
                            droneLocation,
                            MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(75f * fxScaleMultiplier, 200f * fxScaleMultiplier), (float) Math.random() * 360f),
                            MathUtils.getRandomNumberInRange(3 * fxScaleMultiplier, 7 * fxScaleMultiplier),
                            0.8f, MathUtils.getRandomNumberInRange(0.3f, 0.7f),
                            new Color(DIVINE_GOLD_ETHEREAL_FRINGE.getRed(), DIVINE_GOLD_ETHEREAL_FRINGE.getGreen(), DIVINE_GOLD_ETHEREAL_FRINGE.getBlue(), 150 + (int)(Math.random() * 100))
                    );
                }
                Global.getSoundPlayer().playSound("bt_convergence_end", 1.1f, 2f + (fxScaleMultiplier -1f) * 0.2f, droneLocation, droneVelocity);
            }
        }

        if (constructorDroneSource != null) {
            if (constructorDroneSource.getMutableStats() != null) {
                constructorDroneSource.getMutableStats().getHullDamageTakenMult().unmodify(SWARM_CONSTRUCTING_SHIP_TAG);
            }
            constructorDroneSource.removeTag(SWARM_CONSTRUCTING_SHIP_TAG);
        }
        if (shipToConstruct != null) {
            shipToConstruct.removeTag(SHIP_UNDER_CONSTRUCTION_TAG);
            if (shipToConstruct.getMutableStats() != null) {
                shipToConstruct.getMutableStats().getEffectiveArmorBonus().unmodify(SHIP_UNDER_CONSTRUCTION_TAG);
            }
            if (finalCollisionClass != null && shipToConstruct.getCollisionClass() == CollisionClass.NONE && shipToConstruct.isAlive() && !shipToConstruct.isHulk()) {
                shipToConstruct.setCollisionClass(finalCollisionClass);
            }
        }
        Global.getCombatEngine().removePlugin(this);
    }
}