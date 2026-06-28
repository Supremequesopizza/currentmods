// By Tartiflette
package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

import org.magiclib.util.MagicFakeBeam;

public class bt_repair_dem_ai implements MissileAIPlugin, GuidedMissileAI {

    private static final float TARGET_FIND_RANGE = 3000f;
    private static final float BEAM_RANGE = 600f;
    private final float repairAmount;
    private static final float ARMOR_REPAIR_FRACTION = 1f;

    private static final Color BEAM_CORE_COLOR = new Color(100, 255, 150, 255);
    private static final Color BEAM_FRINGE_COLOR = new Color(175, 255, 200, 200);
    private static final Color HEAL_EFFECT_COLOR = new Color(100, 255, 150, 150);
    private static final float BEAM_WIDTH = 15f;
    private static final String FIRE_SOUND_ID = "bt_aegis_healerbeam";
    private static final String BEAM_TEXTURE_ID = "Trail_Surge";
    private static final String BEAM_AURA_ID = "doublehelix_trail_beam";

    private final MissileAPI missile;
    private CombatEntityAPI target;
    private final ShipAPI launchingShip;
    private boolean hasSearchedForTarget = false;
    private Vector2f lead = new Vector2f();
    private final float maxSpeed;

    public bt_repair_dem_ai(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        this.launchingShip = launchingShip;
        this.maxSpeed = missile.getMaxSpeed();
        this.repairAmount = missile.getDamageAmount();
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
            return;
        }

        if (!hasSearchedForTarget) {
            hasSearchedForTarget = true;
            setTarget(findBestRepairTarget(missile, launchingShip, TARGET_FIND_RANGE));
        }

        if (target == null || !Global.getCombatEngine().isEntityInPlay(target) ||
                (target instanceof ShipAPI && ((ShipAPI) target).isHulk())) {
            return;
        }

        float distanceToTarget = MathUtils.getDistance(missile.getLocation(), target.getLocation());
        float angleToTarget = VectorUtils.getAngle(missile.getLocation(), target.getLocation());
        boolean isInRange = distanceToTarget <= BEAM_RANGE * 0.9f;
        boolean isFacingTarget = Math.abs(MathUtils.getShortestRotation(missile.getFacing(), angleToTarget)) < 15;

        if (isInRange && isFacingTarget) {
            fireRepairBeam();
            return;
        }

        lead = AIUtils.getBestInterceptPoint(missile.getLocation(), maxSpeed, target.getLocation(), target.getVelocity());
        if (lead == null) {
            lead = target.getLocation();
        }

        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), lead));
        missile.giveCommand(aimAngle < 0 ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);

        if (Math.abs(aimAngle) < 45) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * 0.1f) {
            missile.setAngularVelocity(aimAngle / 0.1f);
        }
    }

    private void fireRepairBeam() {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;

            repairArmor(targetShip);

            if (targetShip.getHullLevel() < 1f) {
                float hullRepair = repairAmount * 2f;
                targetShip.setHitpoints(Math.min(targetShip.getHitpoints() + hullRepair,
                        targetShip.getMaxHitpoints()));
            }
        }

        MagicFakeBeam.spawnFakeBeam(
                engine,
                missile.getLocation(),
                BEAM_RANGE,
                VectorUtils.getAngle(missile.getLocation(), target.getLocation()),
                BEAM_WIDTH,
                0.1f,
                0.2f,
                50f,
                BEAM_CORE_COLOR,
                BEAM_FRINGE_COLOR,
                0f,
                DamageType.ENERGY,
                0f,
                launchingShip
        );

        engine.addHitParticle(target.getLocation(), new Vector2f(), 100f, 1f, 0.5f, HEAL_EFFECT_COLOR);
        engine.addSmoothParticle(target.getLocation(), new Vector2f(), 75f, 1f, 0.25f, HEAL_EFFECT_COLOR);

        Global.getSoundPlayer().playSound(FIRE_SOUND_ID, 1f, 0.8f, missile.getLocation(), missile.getVelocity());
        engine.spawnExplosion(missile.getLocation(), missile.getVelocity(), BEAM_CORE_COLOR, 100f, 1.25f);

        engine.removeEntity(missile);
    }

    private void repairArmor(ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        float[][] armorGrid = grid.getGrid();
        int gridWidth = armorGrid.length;
        int gridHeight = armorGrid[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell();

        if (maxArmorInCell <= 0) return;

        float totalMissingArmor = 0f;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                float missing = maxArmorInCell - armorGrid[x][y];
                if (missing > 0) {
                    totalMissingArmor += missing;
                }
            }
        }

        if (totalMissingArmor <= 0) return;

        float totalRepairAmount = repairAmount * ARMOR_REPAIR_FRACTION;

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                float currentArmor = armorGrid[x][y];
                if (currentArmor < maxArmorInCell) {
                    float missing = maxArmorInCell - currentArmor;
                    float repairFraction = missing / totalMissingArmor;
                    float repairThisCell = totalRepairAmount * repairFraction;
                    float newArmor = Math.min(currentArmor + repairThisCell, maxArmorInCell);
                    grid.setArmorValue(x, y, newArmor);
                }
            }
        }
    }

    private ShipAPI findBestRepairTarget(MissileAPI missile, ShipAPI source, float range) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        List<ShipAPI> allies = AIUtils.getNearbyAllies(missile, range);
        ShipAPI bestTarget = null;
        float bestWeight = 0f;

        for (ShipAPI ally : allies) {
            if (ally.isHulk() || ally.isFighter() || ally == source) {
                continue;
            }

            float armorDeficit = 1f - getAverageArmorFraction(ally);
            if (armorDeficit <= 0.05f) {
                continue;
            }

            float dist = MathUtils.getDistance(missile.getLocation(), ally.getLocation());

            float weight = armorDeficit * 100f;

            switch (ally.getHullSize()) {
                case CAPITAL_SHIP: weight *= 3f; break;
                case CRUISER:      weight *= 2f; break;
                case DESTROYER:    weight *= 1.5f; break;
                default:           break;
            }

            weight += (1f - dist / range) * 50f;

            if (weight > bestWeight) {
                bestWeight = weight;
                bestTarget = ally;
            }
        }

        return bestTarget;
    }

    private float getAverageArmorFraction(ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        float[][] armorGrid = grid.getGrid();
        int gridWidth = armorGrid.length;
        int gridHeight = armorGrid[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell();

        if (maxArmorInCell <= 0) return 1f;

        float maxTotalArmor = maxArmorInCell * gridWidth * gridHeight;
        if (maxTotalArmor <= 0) return 1f;

        float currentTotalArmor = 0f;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                currentTotalArmor += armorGrid[x][y];
            }
        }

        return currentTotalArmor / maxTotalArmor;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    public void init(CombatEngineAPI engine) {}
}