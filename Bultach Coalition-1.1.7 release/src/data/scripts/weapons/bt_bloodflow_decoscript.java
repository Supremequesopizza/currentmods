package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

public class bt_bloodflow_decoscript implements EveryFrameWeaponEffectPlugin {

    private AnimationAPI animation;
    private SpriteAPI weaponSprite;
    private boolean initialized = false;
    private float frameTimer = 0f;
    private static final float FRAME_RATE = 12.0f;
    private static final float FRAME_DURATION = 1.0f / FRAME_RATE;

    private int baseRed = 200, baseGreen = 30, baseBlue = 205;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) {
            return;
        }
        ShipAPI ship = weapon.getShip();

        if (!initialized) {
            this.weaponSprite = weapon.getSprite();
            this.animation = weapon.getAnimation();
            if (this.weaponSprite != null) {
                Color currentColor = this.weaponSprite.getColor();
                if (currentColor != null) {
                    this.baseRed = currentColor.getRed();
                    this.baseGreen = currentColor.getGreen();
                    this.baseBlue = currentColor.getBlue();
                }
            }
            initialized = true;
        }

        if (this.weaponSprite == null || this.animation == null) {
            return;
        }


        if (ship != null && ship.getOriginalOwner() == -1) {
            this.weaponSprite.setColor(new Color(this.baseRed, this.baseGreen, this.baseBlue, 0));
            this.weaponSprite.setNormalBlend();
            this.animation.setAlphaMult(0f);
            if (this.animation.getNumFrames() > 0) {
                this.animation.setFrame(0);
            }
            frameTimer = 0f;
            return;
        }

        if (engine == null || engine.isCombatOver()) {
            this.weaponSprite.setColor(new Color(this.baseRed, this.baseGreen, this.baseBlue, 0));
            this.weaponSprite.setNormalBlend();
            this.animation.setAlphaMult(0f);
            if (this.animation.getNumFrames() > 0) {
                this.animation.setFrame(0);
            }
            frameTimer = 0f;
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        if (ship == null) return;


        boolean effectShouldBeActive = ship.isAlive() &&
                engine.isEntityInPlay(ship) &&
                ship.getSystem() != null &&
                ship.getSystem().isActive();

        if (effectShouldBeActive) {
            this.weaponSprite.setColor(new Color(this.baseRed, this.baseGreen, this.baseBlue, 255));
            this.weaponSprite.setAdditiveBlend();
            this.animation.setAlphaMult(3.5f);

            frameTimer += amount;
            while (frameTimer >= FRAME_DURATION) {
                int nextFrame = this.animation.getFrame() + 1;
                int totalFrames = this.animation.getNumFrames();

                if (totalFrames > 0) {
                    if (nextFrame >= totalFrames) {
                        nextFrame = 0;
                    }
                    this.animation.setFrame(nextFrame);
                    if (nextFrame == 0) {
                        Global.getSoundPlayer().playSound(
                                "bt_bloodflow_single",
                                1.0f,
                                0.6f,
                                weapon.getLocation(),
                                ship.getVelocity()
                        );
                    }
                }
                frameTimer -= FRAME_DURATION;
            }
        } else {
            this.weaponSprite.setColor(new Color(this.baseRed, this.baseGreen, this.baseBlue, 0));
            this.weaponSprite.setNormalBlend();
            this.animation.setAlphaMult(0f);
            if (this.animation.getNumFrames() > 0) {
                this.animation.setFrame(0);
            }
            frameTimer = 0f;
        }
    }
}