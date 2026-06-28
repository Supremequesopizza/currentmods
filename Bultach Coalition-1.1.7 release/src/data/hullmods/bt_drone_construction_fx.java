package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;

public class bt_drone_construction_fx extends BaseHullMod {

    private static final String SPRITE_ID = "bt_summoning_aura";
    private static final String SPRITE_CATEGORY = "fx";
    private static final Vector2f BASE_SPRITE_SIZE = new Vector2f(325f, 325f);
    private static final float FADE_IN_TIME_SECONDS = 4f;
    private static final float ACTIVE_DURATION_SECONDS = 20.0f;
    private static final float FADE_OUT_TIME_SECONDS = 1.0f;
    private static final float SPIN_SPEED_DPS = 15f;
    private static final Color FX_COLOR = new Color(124, 116, 108, 85);

    private static final String DATA_KEY_PREFIX = "bt_drone_fx_";
    public static final String DRONE_FX_SCALE_FACTOR_KEY = "bt_drone_fx_scale_factor";
    public static final String DRONE_SHOULD_FADE_OUT_KEY = "bt_drone_fx_fade_command";

    private enum VisualState {
        FADING_IN,
        ACTIVE,
        FADING_OUT,
        DONE
    }

    private static class VisualData {
        float currentAngle = MathUtils.getRandomNumberInRange(0f, 360f);
        float currentAlpha = 0f;
        float lifetimeCounter = 0f;
        VisualState state = VisualState.FADING_IN;
        float scaleFactor = 1f;
        transient SpriteAPI sprite;
        boolean fadeCommandReceived = false;

        VisualData() {}
    }

    private SpriteAPI getSpriteFX() {
        try {
            return Global.getSettings().getSprite(SPRITE_CATEGORY, SPRITE_ID);
        } catch (Exception e) {
            try {
                return Global.getSettings().getSprite(SPRITE_ID);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        String dataKey = DATA_KEY_PREFIX + ship.getId();
        if (ship.getCustomData().get(dataKey) == null) {
            VisualData vData = new VisualData();
            ship.setCustomData(dataKey, vData);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused() || !ship.isAlive()) {
            return;
        }

        String dataKey = DATA_KEY_PREFIX + ship.getId();
        VisualData visualData = (VisualData) ship.getCustomData().get(dataKey);

        if (visualData == null) {
            visualData = new VisualData();
            ship.setCustomData(dataKey, visualData);
        }

        if (visualData.state == VisualState.DONE) {
            return;
        }

        if (ship.getCustomData().containsKey(DRONE_FX_SCALE_FACTOR_KEY)) {
            Object scaleObj = ship.getCustomData().get(DRONE_FX_SCALE_FACTOR_KEY);
            if (scaleObj instanceof Float) {
                visualData.scaleFactor = (Float) scaleObj;
            }
        }

        if (visualData.sprite == null) {
            visualData.sprite = getSpriteFX();
            if (visualData.sprite == null && ship.getCustomData().get(dataKey + "_logflag_sprite") == null) {
                ship.setCustomData(dataKey + "_logflag_sprite", true);
            }
        }

        visualData.lifetimeCounter += amount;
        visualData.currentAngle += SPIN_SPEED_DPS * amount;
        visualData.currentAngle = MathUtils.clampAngle(visualData.currentAngle);

        if (!visualData.fadeCommandReceived && ship.getCustomData().containsKey(DRONE_SHOULD_FADE_OUT_KEY)) {
            Boolean fadeCommand = (Boolean) ship.getCustomData().get(DRONE_SHOULD_FADE_OUT_KEY);
            if (fadeCommand != null && fadeCommand) {
                if (visualData.state != VisualState.FADING_OUT && visualData.state != VisualState.DONE) {
                    visualData.state = VisualState.FADING_OUT;
                    visualData.lifetimeCounter = 0f;
                    visualData.fadeCommandReceived = true;
                }
                ship.removeCustomData(DRONE_SHOULD_FADE_OUT_KEY);
            }
        }

        switch (visualData.state) {
            case FADING_IN:
                if (visualData.lifetimeCounter >= FADE_IN_TIME_SECONDS) {
                    visualData.currentAlpha = 1f;
                    visualData.state = VisualState.ACTIVE;
                    visualData.lifetimeCounter = 0f;
                } else {
                    visualData.currentAlpha = visualData.lifetimeCounter / FADE_IN_TIME_SECONDS;
                }
                break;
            case ACTIVE:
                visualData.currentAlpha = 1f;
                if (visualData.lifetimeCounter >= ACTIVE_DURATION_SECONDS && !visualData.fadeCommandReceived) {
                    visualData.state = VisualState.FADING_OUT;
                    visualData.lifetimeCounter = 0f;
                }
                break;
            case FADING_OUT:
                if (visualData.lifetimeCounter >= FADE_OUT_TIME_SECONDS) {
                    visualData.currentAlpha = 0f;
                    visualData.state = VisualState.DONE;
                    if (ship.isAlive()) {
                        engine.removeEntity(ship);
                    }
                } else {
                    visualData.currentAlpha = 1f - (visualData.lifetimeCounter / FADE_OUT_TIME_SECONDS);
                }
                break;
            case DONE:
                break;
        }
        visualData.currentAlpha = MathUtils.clamp(visualData.currentAlpha, 0f, 1f);

        if (Global.getSettings().getModManager().isModEnabled("MagicLib")) {
            if (visualData.sprite != null && visualData.state != VisualState.DONE) {
                Vector2f scaledSize = new Vector2f(BASE_SPRITE_SIZE.x * visualData.scaleFactor, BASE_SPRITE_SIZE.y * visualData.scaleFactor);
                float finalSpriteAlphaComponent = visualData.currentAlpha * (FX_COLOR.getAlpha() / 105f); // Your tweaked divisor
                Color renderColor = new Color(FX_COLOR.getRed()/255f,
                        FX_COLOR.getGreen()/255f,
                        FX_COLOR.getBlue()/255f,
                        finalSpriteAlphaComponent);

                MagicRender.battlespace(
                        visualData.sprite,
                        ship.getLocation(),
                        new Vector2f(ship.getVelocity()),
                        scaledSize,
                        new Vector2f(0f, 0f),
                        visualData.currentAngle,
                        0f,
                        renderColor,
                        true,
                        0f,
                        0f,
                        1.25f,
                        3.1f,
                        0f,
                        0.5f,
                        0.05f,
                        0f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER
                );
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "Visual effect for constructing drone.";
        return null;
    }

    @Override
    public boolean affectsOPCosts() {
        return false;
    }
}