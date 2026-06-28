package data.scripts.shipsystems.convergence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.shipsystems.convergence.ConvergenceSwarmEffect;
import data.scripts.shipsystems.convergence.ConvergenceShipConstructionScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class bt_divine_convergence_SystemScript extends BaseShipSystemScript {

    public static final String TEMPORARY_CONSTRUCTOR_DRONE_VARIANT_ID = "ork_convergence_swarm_standard";
    public static final String PREFERRED_PARTICLE_ORIGIN_SLOT_ID = "WS0023";


    public static final float CONSTRUCTION_SPAWN_ARC_CENTER_OFFSET_DEGREES = 180f;
    public static final float CONSTRUCTION_SPAWN_ARC_WIDTH_DEGREES = 240f;

    public static final float MIN_CONSTRUCTION_RANGE_MULT = 1.5f;
    public static final float MAX_CONSTRUCTION_RANGE_MULT = 3.0f;

    public static final float BASE_CONSTRUCTION_DELAY = 0.5f;
    public static final float BASE_CONSTRUCTION_FADE_IN_TIME = 7f;
    public static final int BASE_FRAGMENTS_VISUAL_SCALAR = 75;

    public static List<SwarmConstructableVariant> CONSTRUCTABLE_VARIANTS = new ArrayList<>();
    private static boolean variantsInited = false;

    public static class SwarmConstructableVariant {
        public String variantId;
        public float dp;
        public HullSize size;
        public int requiredFragmentsForVisuals;

        public SwarmConstructableVariant(String variantId) {
            this.variantId = variantId;
            ShipVariantAPI v = Global.getSettings().getVariant(variantId);
            if (v == null) {
                this.dp = 10;
                this.size = HullSize.FRIGATE;
                this.requiredFragmentsForVisuals = BASE_FRAGMENTS_VISUAL_SCALAR;
                Global.getLogger(bt_divine_convergence_SystemScript.class).error("Failed to load constructable variant: " + variantId);
                return;
            }
            this.dp = v.getHullSpec().getSuppliesToRecover();
            this.size = v.getHullSize();
            this.requiredFragmentsForVisuals = Math.max(25, (int) (BASE_FRAGMENTS_VISUAL_SCALAR * (this.dp / 8f)));
        }
    }

    public static void initConstructableVariants() {
        if (variantsInited) return;
        CONSTRUCTABLE_VARIANTS.clear();
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_nasairde_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_ceartas_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_fulang_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_feirge_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_ifrinn_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_basaich_standard"));
        CONSTRUCTABLE_VARIANTS.add(new SwarmConstructableVariant("ork_freiceadan_standard"));
        variantsInited = true;
    }

    private boolean systemChargedAndReady = true;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        initConstructableVariants();

        if (state == BaseShipSystemScript.State.IDLE || state == BaseShipSystemScript.State.COOLDOWN) {
            systemChargedAndReady = true;
        }

        if (effectLevel >= 1f && systemChargedAndReady && ship.isAlive()) {
            systemChargedAndReady = false;

            SwarmConstructableVariant chosenVariantToBuild = pickVariantToConstruct(ship);
            if (chosenVariantToBuild == null) {
                return;
            }

            Vector2f mothershipParticleOrigin = new Vector2f(ship.getLocation());
            if (PREFERRED_PARTICLE_ORIGIN_SLOT_ID != null && !PREFERRED_PARTICLE_ORIGIN_SLOT_ID.isEmpty()) {
                for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (slot.getId().equals(PREFERRED_PARTICLE_ORIGIN_SLOT_ID)) {
                        mothershipParticleOrigin = slot.computePosition(ship);
                        break;
                    }
                }
            }

            float shipFacing = ship.getFacing();
            float arcCenterDegrees = MathUtils.clampAngle(shipFacing + CONSTRUCTION_SPAWN_ARC_CENTER_OFFSET_DEGREES);
            float arcHalfWidth = CONSTRUCTION_SPAWN_ARC_WIDTH_DEGREES / 2f;
            float randomAngleOffset = MathUtils.getRandomNumberInRange(-arcHalfWidth, arcHalfWidth);
            float currentSpawnAngle = MathUtils.clampAngle(arcCenterDegrees + randomAngleOffset);

            float minAbsoluteRange = ship.getCollisionRadius() * MIN_CONSTRUCTION_RANGE_MULT;
            float maxAbsoluteRange = ship.getCollisionRadius() * MAX_CONSTRUCTION_RANGE_MULT;
            float currentSpawnDistance = MathUtils.getRandomNumberInRange(minAbsoluteRange, maxAbsoluteRange);

            Vector2f randomConstructionSite = MathUtils.getPointOnCircumference(ship.getLocation(), currentSpawnDistance, currentSpawnAngle);


            CombatEngineAPI engine = Global.getCombatEngine();
            CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOwner());
            boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
            fleetManager.setSuppressDeploymentMessages(true);

            ShipAPI constructorDrone = fleetManager.spawnShipOrWing(
                    TEMPORARY_CONSTRUCTOR_DRONE_VARIANT_ID,
                    randomConstructionSite,
                    ship.getFacing(),
                    ship.getCRAtDeployment()
            );

            fleetManager.setSuppressDeploymentMessages(wasSuppressed);

            if (constructorDrone == null) {
                Global.getLogger(bt_divine_convergence_SystemScript.class).error("FATAL: Failed to spawn constructor drone " + TEMPORARY_CONSTRUCTOR_DRONE_VARIANT_ID);
                return;
            }

            constructorDrone.getVelocity().set(0, 0);
            constructorDrone.setCollisionClass(CollisionClass.NONE);

            ConvergenceSwarmEffect.ConvergenceSwarmParams rParams = new ConvergenceSwarmEffect.ConvergenceSwarmParams();
            rParams.spriteCat = "misc";
            rParams.spriteKey = "nebula_particles";
            rParams.initialMembers = chosenVariantToBuild.requiredFragmentsForVisuals;
            rParams.baseMembersToMaintain = chosenVariantToBuild.requiredFragmentsForVisuals;
            rParams.withInitialMembers = true;
            rParams.withRespawn = false;
            rParams.minOffset = chosenVariantToBuild.size.ordinal() * 5f + 10f;
            rParams.maxOffset = chosenVariantToBuild.size.ordinal() * 15f + Math.max(50f, chosenVariantToBuild.requiredFragmentsForVisuals * 0.3f);
            rParams.generateOffsetAroundAttachedEntityOval = true;
            rParams.maxSpeed = 100f + (chosenVariantToBuild.size.ordinal() * 20f);
            rParams.baseDur = BASE_CONSTRUCTION_FADE_IN_TIME + BASE_CONSTRUCTION_DELAY + 5f;
            rParams.minFadeoutTime = 1.5f;
            rParams.maxFadeoutTime = 2.5f;
            rParams.despawnSound = null;
            rParams.color = new Color(255, 239, 215, 50);
            rParams.flashFrequency = 2.0f;
            rParams.flashProbability = 0.3f;
            rParams.flashFringeColor = new Color(255, 221, 171, 255);
            rParams.flashCoreColor = Color.WHITE;
            rParams.flashRadius = 40f + chosenVariantToBuild.size.ordinal() * 15f;
            rParams.alphaMultBase = 0.8f;

            new ConvergenceSwarmEffect(constructorDrone, rParams);

            float actualConstructionTime = BASE_CONSTRUCTION_FADE_IN_TIME + (chosenVariantToBuild.dp * 0.1f);

            ConvergenceShipConstructionScript constructionScript = new ConvergenceShipConstructionScript(
                    chosenVariantToBuild.variantId,
                    constructorDrone,
                    BASE_CONSTRUCTION_DELAY,
                    actualConstructionTime,
                    PREFERRED_PARTICLE_ORIGIN_SLOT_ID,
                    ship
            );
            engine.addPlugin(constructionScript);

            Global.getSoundPlayer().playSound("bt_convergence_portal_open", 1f, 0.5f, ship.getLocation(), ship.getVelocity());
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        systemChargedAndReady = true;
    }

    public SwarmConstructableVariant pickVariantToConstruct(ShipAPI motherShip) {
        initConstructableVariants();
        if (CONSTRUCTABLE_VARIANTS.isEmpty()) return null;

        CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(motherShip.getOwner());
        if (manager == null) return null;

        int dpLeft = manager.getMaxStrength() - manager.getCurrStrength();
        if (dpLeft <= 0) return null;

        WeightedRandomPicker<SwarmConstructableVariant> picker = new WeightedRandomPicker<>(Misc.random);

        for (SwarmConstructableVariant potential : CONSTRUCTABLE_VARIANTS) {
            if (potential.dp <= dpLeft) {

                float weight = 1f;
                switch (potential.size) {
                    case FRIGATE:
                        weight = potential.dp * 2f;
                        break;
                    case DESTROYER:
                        weight = potential.dp * 3f;
                        break;
                    case CRUISER:
                        weight = potential.dp * 4f;
                        break;
                    case CAPITAL_SHIP:
                        weight = potential.dp * 1.5f;
                        break;
                    default:
                        weight = potential.dp * 0.5f;
                        break;
                }
                picker.add(potential, Math.max(0.1f, weight));
            }
        }
        return picker.pick();
    }


    private float getMinDPConstructable() {
        if (!variantsInited || CONSTRUCTABLE_VARIANTS.isEmpty()) return 999f;
        float minDP = Float.MAX_VALUE;
        for (SwarmConstructableVariant v : CONSTRUCTABLE_VARIANTS) {
            minDP = Math.min(minDP, v.dp);
        }
        return minDP == Float.MAX_VALUE ? 999f : minDP;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return "DEPLETED";
        if (system.getState() == ShipSystemAPI.SystemState.ACTIVE) return "REACHING INTO WHAT WAS";
        if (system.getState() == ShipSystemAPI.SystemState.IN) return "CONVERGING WHAT IS";
        if (system.getState() == ShipSystemAPI.SystemState.OUT) return "REALITY OBEYS";
        if (system.getCooldownRemaining() > 0) return "COALESCING";
        if (ship != null && ship.isAlive()) {
            initConstructableVariants();
            CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
            if (manager != null && manager.getMaxStrength() - manager.getCurrStrength() < getMinDPConstructable()) {
                return "LOW FLEET CAPACITY";
            }
            if (pickVariantToConstruct(ship) == null) {
                return "NO BLUEPRINTS VALID";
            }
        }
        return "READY TO CONVERGE";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship == null || !ship.isAlive()) return false;
        initConstructableVariants();
        CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
        if (manager == null) return false;
        if (manager.getMaxStrength() - manager.getCurrStrength() < getMinDPConstructable()) {
            return false;
        }
        return pickVariantToConstruct(ship) != null;
    }
}