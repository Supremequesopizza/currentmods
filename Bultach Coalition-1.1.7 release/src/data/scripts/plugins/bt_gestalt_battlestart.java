package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class bt_gestalt_battlestart extends BaseEveryFrameCombatPlugin {

    //screen-edge static plugin, code by ruddygreat

    //time is all in seconds
    public final float inDur = 0.8f;
    public final float activeDur = 2.8f;
    public final float outDur = 4f;

    //target alpha mult for the colour
    //goes from 0 to this over in & this to 0 over out
    public final float alphaMult = 1;
    //how thick the borders should be, in terms of screen width / height
    public final float borderThickness = 0.45f;

    //the colour of the static around the edge of the screen
    public final Color outerColour = new Color(1, 1, 1, 0.55f);
    //the colour of the static towards the middle of the screen
    public final Color innerColour = new Color(0f, 0f, 0f, 0.05f);

    //set to true for easy testing
    boolean forceRun = false;

    //internal variables
    private float timer = 0;
    private float currAlphaMult = 0;
    boolean doneOnce = false;
    private final String BattleStartSoundId = "bt_gestalt_battle_start";

    private enum staticFaderState {
        IN,
        ACTIVE,
        OUT,
        OFF
    }

    private staticFaderState currState = staticFaderState.IN;
    public static final HashSet<String> activateForFactions = new HashSet<>();
    public static final Logger log = Global.getLogger(bt_gestalt_battlestart.class);

    static {
        activateForFactions.add("gestalt");
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        //check if the plugin should actually run
        if (!doneOnce) {
            makeSureThisShouldActuallyRun();
            doneOnce = true;
        }

        if (currState == staticFaderState.OFF) { //remove the plugin
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        timer += amount; //advance the timer

        switch (currState) {
            case IN:

                currAlphaMult = (alphaMult * (timer / inDur)); //linearly scale static alpha from 0 to alphaMult

                if (timer >= inDur) { //set up stuff for next state
                    currState = staticFaderState.ACTIVE;
                    timer = 0;
                    currAlphaMult = alphaMult;
                    Global.getSoundPlayer().playUISound(BattleStartSoundId, 1.0f, 0.7f);
                }
                break;

            case ACTIVE:

                currAlphaMult = alphaMult; //keep static alpha at alphaMult

                if (timer >= activeDur) { //set up stuff for next state
                    currState = staticFaderState.OUT;
                    timer = 0;
                }
                break;

            case OUT:

                currAlphaMult = (alphaMult * (1 - (timer / outDur))); //linearly scale static alpha from alphaMult to 0

                if (timer >= outDur) { //set up stuff for next state
                    currState = staticFaderState.OFF;
                    timer = 0;
                    currAlphaMult = 0;
                }
                break;
        }
    }

    private void makeSureThisShouldActuallyRun() {

        if (forceRun) {
            log.info("forceRun is true, we're so back");
            return;
        }

        CampaignFleetAPI otherFleet = Global.getCombatEngine().getContext().getOtherFleet(); //get other fleet
        if (otherFleet != null) { // if it's not null
            String otherFactionID = otherFleet.getFaction().getId(); //get the id
            if (!activateForFactions.contains(otherFactionID)) {
                log.info("other fleet faction " + otherFactionID + " is not in list, it's so joever");
                currState = staticFaderState.OFF; //if it's not in the list, jump straight to off
            } else {
                log.info("other fleet faction " + otherFactionID + " is in list, we're so back");
            }
        } else {
            log.info("other fleet is null, it's so joever");
            currState = staticFaderState.OFF;
        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

        if (currState == staticFaderState.OFF) { //double extra make sure the plugin isn't running when it shouldn't be
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        SpriteAPI staticSprite = Global.getSettings().getSprite("graphics/fx/bt_recolored_noise.png");

        float screenTop = Global.getSettings().getScreenHeight();
        float screenRightEdge = Global.getSettings().getScreenWidth();

        float topBottomBorderHeight = screenTop * borderThickness;
        float leftRightBorderThickness = screenRightEdge * borderThickness;

        //gl magic below
        float pixelToTexCoords = staticSprite.getWidth() / screenRightEdge;
        float texXOffset = MathUtils.getRandomNumberInRange(0f, 5f);
        float texYOffest = MathUtils.getRandomNumberInRange(0f, 5f);

        //render the top and bottom bits
        renderTexturedSquareWithVerticalFade(
                0,
                screenTop,
                topBottomBorderHeight,
                screenRightEdge,
                staticSprite,
                (0 * pixelToTexCoords) + texXOffset,
                screenTop * pixelToTexCoords + texYOffest,
                outerColour,
                innerColour,
                currAlphaMult
        );

        renderTexturedSquareWithVerticalFade(
                0,
                topBottomBorderHeight,
                topBottomBorderHeight,
                screenRightEdge,
                staticSprite,
                (0 * pixelToTexCoords) + texXOffset,
                topBottomBorderHeight * pixelToTexCoords + texYOffest,
                innerColour,
                outerColour,
                currAlphaMult
        );

        //render the left and right bits
        renderTexturedSquareWithHorizontalFade(
                0,
                screenTop,
                screenTop,
                leftRightBorderThickness,
                staticSprite,
                (0 * pixelToTexCoords) + texXOffset,
                (screenTop - topBottomBorderHeight) * pixelToTexCoords + texYOffest,
                outerColour,
                innerColour,
                currAlphaMult
        );

        renderTexturedSquareWithHorizontalFade(
                screenRightEdge - leftRightBorderThickness,
                screenTop,
                screenTop,
                leftRightBorderThickness,
                staticSprite,
                ((screenRightEdge - leftRightBorderThickness) * pixelToTexCoords) + texXOffset,
                (screenTop - topBottomBorderHeight) * pixelToTexCoords + texYOffest,
                innerColour,
                outerColour,
                currAlphaMult
        );
    }

    //renders from top left
    private void renderTexturedSquareWithHorizontalFade(float x, float y, float height, float width, SpriteAPI sprite, float texX, float texY, Color leftEdgeColour, Color rightEdgeColour, float alphaMult) {

        Vector2f tl = new Vector2f(x, y);
        Vector2f tr = new Vector2f(x + width, y);
        Vector2f br = new Vector2f(x + width, y - height);
        Vector2f bl = new Vector2f(x, y - height);

        float texWidth = width / sprite.getWidth();
        float texHeight = height / sprite.getHeight();

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        sprite.bindTexture();
        glBegin(GL_QUADS);

        Misc.setColor(leftEdgeColour, alphaMult);
        vector2fTogl(tl, texX, texY);

        Misc.setColor(rightEdgeColour, alphaMult);
        vector2fTogl(tr, texX + texWidth, texY);
        vector2fTogl(br, texX + texWidth, texY - texHeight);

        Misc.setColor(leftEdgeColour, alphaMult);
        vector2fTogl(bl, texX, texY - texHeight);

        glEnd();
        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);

    }

    //renders from top left
    private void renderTexturedSquareWithVerticalFade(float x, float y, float height, float width, SpriteAPI sprite, float texX, float texY, Color topEdgeColour, Color bottomEdgeColour, float alphaMult) {

        Vector2f tl = new Vector2f(x, y);
        Vector2f tr = new Vector2f(x + width, y);
        Vector2f br = new Vector2f(x + width, y - height);
        Vector2f bl = new Vector2f(x, y - height);

        float texWidth = width / sprite.getWidth();
        float texHeight = height / sprite.getHeight();

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        sprite.bindTexture();
        glBegin(GL_QUADS);

        Misc.setColor(topEdgeColour, alphaMult);
        vector2fTogl(tl, texX, texY);
        vector2fTogl(tr, texX + texWidth, texY);

        Misc.setColor(bottomEdgeColour, alphaMult);
        vector2fTogl(br, texX + texWidth, texY - texHeight);
        vector2fTogl(bl, texX, texY - texHeight);

        glEnd();
        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);

    }

    private void vector2fTogl(Vector2f vector, float texX, float texY) {
        glTexCoord2f(texX, texY);
        glVertex2f(vector.x, vector.y);
    }
}
