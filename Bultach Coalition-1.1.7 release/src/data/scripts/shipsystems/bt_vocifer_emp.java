package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class bt_vocifer_emp extends BaseShipSystemScript {

    private static final float FIRE_DELAY = 0.4f;
    private static final float BASE_RANGE = 500f;
    private static final float MAX_ADDITIONAL_RANGE = 600f;
    private static final float BASE_DAMAGE = 100f;
    private static final float MAX_ADDITIONAL_DAMAGE = 350f;
    private static final float EMP_DAMAGE_MULTIPLIER = 2.0f;
    private static final float BASE_THICKNESS = 20f;
    private static final float MAX_ADDITIONAL_THICKNESS = 45f;

    private static final Color MIN_FLUX_COLOR = new Color(239, 166, 255, 200);
    private static final Color MAX_FLUX_COLOR = new Color(255, 24, 143, 255);
    private static final Color FRINGE_COLOR = new Color(255, 141, 141, 85);

    private static final String SOUND_ID = "system_emp_emitter_impact";
    private static final float BASE_PITCH = 1.4f;
    private static final float MAX_PITCH = 0.5f;
    private static final float BASE_VOLUME = 0.5f;
    private static final float MAX_VOLUME = 1f;

    private boolean hasInitializedSlots = false;
    private final List<WeaponSlotAPI> systemSlots = new ArrayList<>();
    private float timer = 0f;

    private Color interpolateColor(Color from, Color to, float progress) {
        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        float r = (from.getRed() + (to.getRed() - from.getRed()) * clampedProgress) / 255f;
        float g = (from.getGreen() + (to.getGreen() - from.getGreen()) * clampedProgress) / 255f;
        float b = (from.getBlue() + (to.getBlue() - from.getBlue()) * clampedProgress) / 255f;
        float a = (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clampedProgress) / 255f;
        return new Color(r, g, b, a);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || engine == null) {
            return;
        }

        if (!hasInitializedSlots) {
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.isSystemSlot()) {
                    systemSlots.add(slot);
                }
            }
            hasInitializedSlots = true;
        }

        timer += engine.getElapsedInLastFrame();

        if (timer >= FIRE_DELAY) {
            timer -= FIRE_DELAY;

            float fluxFraction = ship.getFluxTracker().getFluxLevel();
            float currentRange = BASE_RANGE + (MAX_ADDITIONAL_RANGE * fluxFraction);
            float currentThickness = BASE_THICKNESS + (MAX_ADDITIONAL_THICKNESS * fluxFraction);
            Color currentCoreColor = interpolateColor(MIN_FLUX_COLOR, MAX_FLUX_COLOR, fluxFraction);
            float currentPitch = BASE_PITCH + ((MAX_PITCH - BASE_PITCH) * fluxFraction);
            float currentVolume = BASE_VOLUME + ((MAX_VOLUME - BASE_VOLUME) * fluxFraction);

            List<CombatEntityAPI> validTargets = new ArrayList<>();
            validTargets.addAll(AIUtils.getNearbyEnemies(ship, currentRange));
            validTargets.addAll(CombatUtils.getMissilesWithinRange(ship.getLocation(), currentRange));

            List<CombatEntityAPI> finalTargets = new ArrayList<>();
            for (CombatEntityAPI potentialTarget : validTargets) {
                if (potentialTarget.getOwner() == ship.getOwner()) {
                    continue;
                }
                if (potentialTarget instanceof ShipAPI && ((ShipAPI) potentialTarget).isPhased()) {
                    continue;
                }
                finalTargets.add(potentialTarget);
            }

            List<Vector2f> origins = new ArrayList<>();
            if (systemSlots.isEmpty()) {
                origins.add(ship.getLocation());
            } else {
                for (WeaponSlotAPI slot : systemSlots) {
                    origins.add(slot.computePosition(ship));
                }
            }

            if (finalTargets.isEmpty()) {
                for (Vector2f origin : origins) {
                    fireDecorativeArc(ship, origin, currentThickness, currentCoreColor);
                }
            } else {
                float currentDamage = BASE_DAMAGE + (MAX_ADDITIONAL_DAMAGE * fluxFraction);
                float currentEmp = currentDamage * EMP_DAMAGE_MULTIPLIER;
                for (Vector2f origin : origins) {
                    fireDamagingArc(ship, origin, finalTargets, currentDamage, currentEmp, currentThickness, currentCoreColor);
                }
            }

            Global.getSoundPlayer().playSound(SOUND_ID, currentPitch, currentVolume, ship.getLocation(), ship.getVelocity());
        }
    }

    private void fireDamagingArc(ShipAPI source, Vector2f origin, List<CombatEntityAPI> validTargets, float damage, float emp, float thickness, Color coreColor) {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));

        engine.spawnEmpArc(
                source, origin, source, target,
                DamageType.ENERGY, damage, emp, 100000f, null,
                thickness, FRINGE_COLOR, coreColor
        );
    }

    private void fireDecorativeArc(ShipAPI source, Vector2f origin, float thickness, Color coreColor) {
        CombatEngineAPI engine = Global.getCombatEngine();
        float randomAngle = MathUtils.getRandomNumberInRange(0f, 360f);
        Vector2f targetPoint = MathUtils.getPointOnCircumference(source.getLocation(), source.getCollisionRadius(), randomAngle);

        engine.spawnEmpArcVisual(
                origin,
                source,
                targetPoint,
                null,
                thickness,
                FRINGE_COLOR,
                coreColor
        );
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        timer = 0f;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        float fluxFraction = 0f;
        if (getShip(true) != null) {
            fluxFraction = getShip(true).getFluxTracker().getFluxLevel();
        }
        int bonusPercent = (int) (fluxFraction * 100f);

        if (index == 0) {
            return new StatusData("EMP OVERCHARGE: " + bonusPercent + "%", false);
        }
        return null;
    }

    private ShipAPI getShip(boolean player) {
        if (Global.getCombatEngine() == null) return null;
        if (Global.getCombatEngine().getPlayerShip() == null) return null;
        if (!player) return null;
        return Global.getCombatEngine().getPlayerShip();
    }
}