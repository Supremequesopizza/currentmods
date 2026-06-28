package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import java.awt.Color;

public class bt_holydem_wep_EveryFrameEffect implements EveryFrameWeaponEffectPlugin {

    private static final Color CHARGEUP_PARTICLE_COLOR = new Color(255, 50, 112, 100);
    private static final Color MUZZLE_FLASH_COLOR = new Color(255, 75, 102, 255);
    private static final Color IDLE_FLARE_COLOR = new Color(255, 100, 128, 150);
    private static final float MUZZLE_FLASH_DURATION = 0.15f;
    private static final float MUZZLE_FLASH_SIZE = 200.0f;
    private static final float MUZZLE_OFFSET_HARDPOINT_END = 15f;
    private static final float MUZZLE_OFFSET_HARDPOINT_START = 15f;
    private static final float MUZZLE_OFFSET_TURRET_END = 10f;
    private static final float MUZZLE_OFFSET_TURRET_START = 10f;

    private final IntervalUtil interval = new IntervalUtil(0.015f, 0.015f);
    private final IntervalUtil idleInterval = new IntervalUtil(0.5f, 1f);
    private float lastChargeLevel = 0.0f;
    private float lastCooldownRemaining = 0.0f;
    private boolean shot = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        float chargeLevel = weapon.getChargeLevel();
        float cooldownRemaining = weapon.getCooldownRemaining();

        Vector2f weaponLocation = weapon.getLocation();
        ShipAPI ship = weapon.getShip();
        float shipFacing = weapon.getCurrAngle();
        Vector2f shipVelocity = ship.getVelocity();
        float along = (float) Math.random();
        Vector2f muzzleLocation = MathUtils.getPointOnCircumference(weaponLocation,
                weapon.getSlot().isHardpoint() ? MUZZLE_OFFSET_HARDPOINT_START * along + MUZZLE_OFFSET_HARDPOINT_END * (1f - along)
                : MUZZLE_OFFSET_TURRET_START * along + MUZZLE_OFFSET_TURRET_END * (1f - along), shipFacing);

        idleInterval.advance(amount);
        if (idleInterval.intervalElapsed() && weapon.getCooldownRemaining() <= 0f && weapon.getChargeLevel() <= 0f) {
            MagicLensFlare.createSharpFlare(engine, ship, muzzleLocation, 2f, 100f, 0f, IDLE_FLARE_COLOR, IDLE_FLARE_COLOR);
        }

        if ((chargeLevel > lastChargeLevel) || (lastCooldownRemaining < cooldownRemaining)) {
            interval.advance(amount);
            if (interval.intervalElapsed() && weapon.isFiring() && weapon.getAmmo() > 0) {
                Vector2f point1 = MathUtils.getRandomPointInCircle(muzzleLocation, (float) Math.random() * weapon.getChargeLevel() * 75f + 25f);
                engine.spawnEmpArc(ship, muzzleLocation, new SimpleEntity(muzzleLocation), new SimpleEntity(point1),
                        DamageType.ENERGY, 0f, 0f, 1000f, null, weapon.getChargeLevel() * 5f + 5f, CHARGEUP_PARTICLE_COLOR, CHARGEUP_PARTICLE_COLOR);
            }

            if (!shot && ((lastCooldownRemaining < cooldownRemaining) || ((chargeLevel >= 1f) && (lastChargeLevel < 1f)))) {
                along = 0.9f;
                muzzleLocation = MathUtils.getPointOnCircumference(weaponLocation,
                        weapon.getSlot().isHardpoint() ? MUZZLE_OFFSET_HARDPOINT_START * along + MUZZLE_OFFSET_HARDPOINT_END * (1f - along)
                        : MUZZLE_OFFSET_TURRET_START * along + MUZZLE_OFFSET_TURRET_END * (1f - along), shipFacing);
                
                MagicLensFlare.createSharpFlare(engine, ship, muzzleLocation, 4f, 200f, 0f, MUZZLE_FLASH_COLOR, MUZZLE_FLASH_COLOR);
                engine.addSmoothParticle(muzzleLocation, shipVelocity, MUZZLE_FLASH_SIZE * 4f, 1f, MUZZLE_FLASH_DURATION * 3f, MUZZLE_FLASH_COLOR);
            } else {
                shot = false;
            }
        } else {
            shot = false;
        }

        lastChargeLevel = chargeLevel;
        lastCooldownRemaining = cooldownRemaining;
    }
}