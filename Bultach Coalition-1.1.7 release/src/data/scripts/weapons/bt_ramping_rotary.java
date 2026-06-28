package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class bt_ramping_rotary implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private float currentRoFBonus = 0f;
    public static final float MAX_ROF_BONUS = 1.5f;
    public static final String ROF_BONUS_MEM_KEY_PREFIX = "$bt_ramping_RoFBonus_";

    private float timeSinceLastFired = 0f;
    private static final float DECAY_DELAY_SECONDS = 1.5f;
    private static final float DECAY_RATE_PER_SECOND = 0.25f;
    private static final float MIN_SHOTS_TO_REACH_MAX_BONUS = 15f;

    private float originalCooldown = -1f;
    private boolean initialized = false;

    private AnimationAPI weaponAnimation;
    private int animNumFrames;
    private float animBaseFrameDelay;
    private float animTimer = 0f;
    private int currentAnimFrame = 0;

    private String getUniqueMemoryKeyForWeapon(WeaponAPI weapon) {
        if (weapon == null || weapon.getSlot() == null) {
            return null;
        }
        return ROF_BONUS_MEM_KEY_PREFIX + weapon.getSlot().getId();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null) {
            return;
        }
        if (weapon.getSlot().isHidden()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        if (!initialized) {
            this.originalCooldown = weapon.getCooldown();
            if (this.originalCooldown <= 0f) {
                this.originalCooldown = 0.1f;
            }

            this.weaponAnimation = weapon.getAnimation();
            if (this.weaponAnimation != null) {
                this.animNumFrames = this.weaponAnimation.getNumFrames();
                if (this.weaponAnimation.getFrameRate() > 0) {
                    this.animBaseFrameDelay = 1f / this.weaponAnimation.getFrameRate();
                } else {
                    this.animBaseFrameDelay = 0.1f;
                }
            } else {
                this.animNumFrames = 0;
            }
            initialized = true;
        }

        timeSinceLastFired += amount;

        if (timeSinceLastFired > DECAY_DELAY_SECONDS) {
            currentRoFBonus = Math.max(0f, currentRoFBonus - DECAY_RATE_PER_SECOND * amount);
        }

        String uniqueKey = getUniqueMemoryKeyForWeapon(weapon);
        if (uniqueKey != null) {
            ship.getCustomData().put(uniqueKey, currentRoFBonus);
        }

        if (this.weaponAnimation != null && this.animNumFrames > 1) {
            float shipRoFMultiplier = 1f;
            if (ship.getMutableStats() != null) {
                switch (weapon.getSpec().getType()) {
                    case BALLISTIC:
                        shipRoFMultiplier = ship.getMutableStats().getBallisticRoFMult().getModifiedValue();
                        break;
                    case ENERGY:
                        shipRoFMultiplier = ship.getMutableStats().getEnergyRoFMult().getModifiedValue();
                        break;
                    case MISSILE:
                        shipRoFMultiplier = ship.getMutableStats().getMissileRoFMult().getModifiedValue();
                        break;
                    default:
                        break;
                }
            }

            float effectiveAnimSpeedMultiplier = shipRoFMultiplier * (1f + currentRoFBonus);
            if (effectiveAnimSpeedMultiplier <= 0.001f) {
                effectiveAnimSpeedMultiplier = 0.001f;
            }

            float currentFrameTargetDelay = this.animBaseFrameDelay / effectiveAnimSpeedMultiplier;
            if (currentFrameTargetDelay < 0.001f) {
                currentFrameTargetDelay = 0.001f;
            }

            if (weapon.getChargeLevel() > 0 || weapon.isFiring()) {
                animTimer += amount;
                while (animTimer >= currentFrameTargetDelay && currentFrameTargetDelay > 0) {
                    animTimer -= currentFrameTargetDelay;
                    currentAnimFrame++;
                    if (currentAnimFrame >= animNumFrames) {
                        currentAnimFrame = 0;
                    }
                }
            } else {
                animTimer = 0f;
            }
            this.weaponAnimation.setFrame(currentAnimFrame);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!initialized) {
            this.originalCooldown = weapon.getCooldown();
            if (this.originalCooldown <= 0f) {
                this.originalCooldown = 0.1f;
            }

            this.weaponAnimation = weapon.getAnimation();
            if (this.weaponAnimation != null) {
                this.animNumFrames = this.weaponAnimation.getNumFrames();
                if (this.weaponAnimation.getFrameRate() > 0) {
                    this.animBaseFrameDelay = 1f / this.weaponAnimation.getFrameRate();
                } else {
                    this.animBaseFrameDelay = 0.1f;
                }
            } else {
                this.animNumFrames = 0;
            }
            initialized = true;
        }

        if (MIN_SHOTS_TO_REACH_MAX_BONUS > 0) {
            currentRoFBonus += MAX_ROF_BONUS / MIN_SHOTS_TO_REACH_MAX_BONUS;
            currentRoFBonus = Math.min(currentRoFBonus, MAX_ROF_BONUS);
        }

        weapon.setRemainingCooldownTo(originalCooldown / (1f + currentRoFBonus));

        ShipAPI ship = weapon.getShip();
        String uniqueKey = getUniqueMemoryKeyForWeapon(weapon);
        if (uniqueKey != null && ship != null) {
            ship.getCustomData().put(uniqueKey, currentRoFBonus);
        }

        timeSinceLastFired = 0f;
        animTimer = 0f;
    }
}