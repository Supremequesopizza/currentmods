package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class bt_gestalt_symbol_sequence extends BaseEveryFrameCombatPlugin {

    // Toggle this to true to force the plugin to run for testing purposes
    public static final boolean forceRun = false;

    public static final HashSet<String> activateForFactions = new HashSet<>();
    static {
        activateForFactions.add("gestalt");
    }

    private final List<String> symbolPaths = new ArrayList<>();
    private final List<SpriteAPI> symbols = new ArrayList<>();
    private float timer = 0f;
    private int currentSymbolIndex = 0;
    private int previousSymbolIndex = -1;
    private boolean finishing = false;
    private final String symbolSoundId = "bt_gestalt_symbol_appear";
    private final float symbolDuration = 1.5f;
    private final float fadeDuration = 0.9f;
    private boolean initialized = false;
    private boolean active = true;

    private void init() {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().getContext() == null) {
            active = false;
            return;
        }

        if (!forceRun) {
            CampaignFleetAPI otherFleet = Global.getCombatEngine().getContext().getOtherFleet();
            if (otherFleet == null || !activateForFactions.contains(otherFleet.getFaction().getId())) {
                active = false;
                return;
            }
        }

        // I know what these mean even if you dont!
        symbolPaths.add("graphics/fx/symbols/bt_symbol_le.png");
        symbolPaths.add("graphics/fx/symbols/bt_symbol_uy.png");
        symbolPaths.add("graphics/fx/symbols/bt_symbol_ee.png");

        for (String path : symbolPaths) {
            symbols.add(Global.getSettings().getSprite(path));
        }

        initialized = true;
        Global.getSoundPlayer().playUISound(symbolSoundId, 1.0f, 0.7f);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (!initialized) init();
        if (!active || symbols.isEmpty()) return;

        timer += amount;

        if (!finishing) {
            if (timer >= symbolDuration) {
                timer = 0f;
                previousSymbolIndex = currentSymbolIndex;
                if (currentSymbolIndex < symbols.size() - 1) {
                    currentSymbolIndex++;
                    Global.getSoundPlayer().playUISound(symbolSoundId, 1.0f, 0.7f);

                } else {
                    // Last symbol done, deploy the fucked up bandaid
                    finishing = true;
                    timer = 0f;
                }
            }
        } else {
            if (timer >= fadeDuration) {
                active = false;
                Global.getCombatEngine().removePlugin(this);
            }
        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (!initialized || !active || symbols.isEmpty()) return;

        float screenWidth = Global.getSettings().getScreenWidth();
        float screenHeight = Global.getSettings().getScreenHeight();
        float baseX = screenWidth / 2f;
        float baseY = screenHeight - 100f;

        if (!finishing) {
            float alphaIn = Math.min(1f, timer / fadeDuration);
            float alphaOut = 1f - alphaIn;

            renderSymbolLayer(symbols.get(currentSymbolIndex), baseX, baseY, alphaIn);

            if (previousSymbolIndex >= 0) {
                renderSymbolLayer(symbols.get(previousSymbolIndex), baseX, baseY, alphaOut);
            }
        } else {
            // Fade out the last symbol because idfk it wont work
            float alphaOut = 1f - Math.min(1f, timer / fadeDuration);
            renderSymbolLayer(symbols.get(currentSymbolIndex), baseX, baseY, alphaOut);
        }
    }

    private void renderSymbolLayer(SpriteAPI sprite, float baseX, float baseY, float alpha) {
        float centerX = baseX;
        float centerY = baseY;

        // Base symbol
        int coreLayers = 3;
        float coreMaxJitter = 1.5f;
        float coreAlpha = alpha * 0.5f;

        for (int i = 0; i < coreLayers; i++) {
            float jitterX = (float) ((Math.random() - 0.5f) * 2f * coreMaxJitter);
            float jitterY = (float) ((Math.random() - 0.5f) * 2f * coreMaxJitter);
            sprite.setAlphaMult(coreAlpha);
            sprite.renderAtCenter(centerX + jitterX, centerY + jitterY);
        }

        // Faking a glow
        int glowLayers = 6;
        float glowMaxJitter = 18f;
        float glowAlphaBase = alpha * 0.15f;

        for (int i = 0; i < glowLayers; i++) {
            float jitterX = (float) ((Math.random() - 0.5f) * 2f * glowMaxJitter);
            float jitterY = (float) ((Math.random() - 0.5f) * 2f * glowMaxJitter);
            float layerAlpha = glowAlphaBase * (1f - (i * 0.2f));
            sprite.setAlphaMult(layerAlpha);
            sprite.renderAtCenter(centerX + jitterX, centerY + jitterY);
        }
    }

}
