package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

public class bt_pathertele_deco implements EveryFrameWeaponEffectPlugin {

    private boolean initialized = false;
    private SpriteAPI sprite;
    private ShipAPI ship;
    private int baseRed = 255;
    private int baseGreen = 255;
    private int baseBlue = 255;
    private float currentAlphaNormalized = 0f;

    private static final float FADE_IN_RATE = 1.0f;
    private static final float FADE_OUT_RATE = 0.5f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) {
            return;
        }

        if (!initialized) {
            sprite = weapon.getSprite();
            ship = weapon.getShip();
            if (sprite != null) {
                Color initialColor = sprite.getColor();
                if (initialColor != null) {
                    baseRed = initialColor.getRed();
                    baseGreen = initialColor.getGreen();
                    baseBlue = initialColor.getBlue();
                    currentAlphaNormalized = initialColor.getAlpha() / 255f;
                }
            }
            initialized = true;
        }

        if (sprite == null || ship == null) {
            return;
        }

        if (engine == null || engine.isCombatOver() || ship.getOriginalOwner() == -1) {
            sprite.setColor(new Color(baseRed, baseGreen, baseBlue, 0));
            currentAlphaNormalized = 0f;
            return;
        }

        if (engine.isPaused()) {
            return;
        }

        float targetAlphaGoal = 0f;
        if (engine.isEntityInPlay(ship) && ship.getSystem() != null && ship.getSystem().isActive()) {
            targetAlphaGoal = 1f;
        }

        if (currentAlphaNormalized < targetAlphaGoal) {
            currentAlphaNormalized += FADE_IN_RATE * amount;
            if (currentAlphaNormalized > targetAlphaGoal) {
                currentAlphaNormalized = targetAlphaGoal;
            }
        } else if (currentAlphaNormalized > targetAlphaGoal) {
            currentAlphaNormalized -= FADE_OUT_RATE * amount;
            if (currentAlphaNormalized < targetAlphaGoal) {
                currentAlphaNormalized = targetAlphaGoal;
            }
        }

        currentAlphaNormalized = Math.max(0f, Math.min(1f, currentAlphaNormalized));
        sprite.setColor(new Color(baseRed, baseGreen, baseBlue, (int)(currentAlphaNormalized * 255f)));
    }
}