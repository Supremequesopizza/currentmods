package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.dark.shaders.util.ShaderLib;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;

//non functional rn

public class bt_FrenziedFlamePlugin extends BaseEveryFrameCombatPlugin {

    public static final String TARGET_HULL_ID = "ork_feregether_corrupted";
    private static final float FORWARD_OFFSET = 20f;
    private static final float EFFECT_RADIUS = 200f;

    private ShipAPI targetShip = null;
    private int shaderID = 0;
    private boolean shaderLoaded = false;

    @Override
    public void advance(float amount, java.util.List<com.fs.starfarer.api.input.InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (!shaderLoaded) {
            try {
                if (ShaderLib.areShadersAllowed()) {
                    ShaderLib.init();
                    String vert = Global.getSettings().loadText("graphics/shaders/bt_frenziedflame.vsh");
                    String frag = Global.getSettings().loadText("graphics/shaders/bt_frenziedflame.fs");
                    this.shaderID = ShaderLib.loadShader(vert, frag);
                }
            } catch (IOException e) {
                this.shaderID = 0;
            }
            shaderLoaded = true;
        }

        if (targetShip == null || !targetShip.isAlive() || !engine.isEntityInPlay(targetShip)) {
            targetShip = null;
            for (ShipAPI ship : engine.getShips()) {
                if (!ship.isFighter() && ship.getHullSpec().getHullId().equals(TARGET_HULL_ID)) {
                    targetShip = ship;
                    break;
                }
            }
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        if (shaderID == 0 || targetShip == null) {
            return;
        }

        Vector2f baseLoc = targetShip.getLocation();
        float facing = targetShip.getFacing();
        Vector2f offset = new Vector2f(FORWARD_OFFSET, 0f);
        VectorUtils.rotate(offset, facing, offset);
        Vector2f finalLoc = Vector2f.add(baseLoc, offset, new Vector2f());

        float x1 = finalLoc.x - EFFECT_RADIUS;
        float y1 = finalLoc.y - EFFECT_RADIUS;
        float x2 = finalLoc.x + EFFECT_RADIUS;
        float y2 = finalLoc.y + EFFECT_RADIUS;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL20.glUseProgram(shaderID);

        GL20.glUniform2f(GL20.glGetUniformLocation(shaderID, "u_viewport_pos"), viewport.getLLX(), viewport.getLLY());
        GL20.glUniform2f(GL20.glGetUniformLocation(shaderID, "u_viewport_size"), viewport.getVisibleWidth(), viewport.getVisibleHeight());


        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(x1, y1);
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(x2, y1);
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(x2, y2);
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(x1, y2);
        GL11.glEnd();

        GL20.glUseProgram(0);

        GL11.glPopAttrib();
    }
}