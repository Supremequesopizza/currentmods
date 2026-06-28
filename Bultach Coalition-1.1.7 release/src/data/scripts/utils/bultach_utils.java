package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import java.awt.Color;
public class bultach_utils
{
    // idk why this doesn't have an overload for floats
    public static float toRad(float degrees)
    {
        return degrees * 0.017453292519943295f;
    }

    // linear interpolation, useful for a wide variety of things
    public static float lerp(float from, float to, float amount)
    {
        return from * (1f-amount) + to * amount;
    }

    // linear interpolation, for vectors. useful for getting a point somewhere on a line
    public static Vector2f lerp(Vector2f from, Vector2f to, float amount)
    {
        return new Vector2f(
                lerp(from.x, to.x, amount),
                lerp(from.y, to.y, amount)
        );
    }

    public static float random_between(float from, float to)
    {
        return Misc.random.nextFloat() * (to - from) + from;
    }

    // kind of stolen from nia but honestly it's not like there's more than one way to do this
    public static void afterimage(ShipAPI ship, Color color, float duration)
    {
        SpriteAPI origSprite = ship.getSpriteAPI();
        float xoffset = origSprite.getWidth() / 2f - origSprite.getCenterX();
        float yoffset = origSprite.getHeight() / 2f - origSprite.getCenterY();

        float facing = ship.getFacing();
        float height = ship.getSpriteAPI().getHeight();
        float width = ship.getSpriteAPI().getWidth();

        xoffset = (float)(FastTrig.cos(toRad(facing-90f))*xoffset - FastTrig.sin(toRad(facing-90f)*yoffset));
        yoffset = (float)(FastTrig.sin(toRad(facing-90f))*xoffset - FastTrig.cos(toRad(facing-90f)*yoffset));

        MagicRender.battlespace(
                Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                Vector2f.add(ship.getLocation(), new Vector2f(xoffset, yoffset), null),
                Misc.ZERO,
                new Vector2f(width, height),
                Misc.ZERO,
                facing - 90f,
                0f,
                color,
                true,
                0f,
                0f,
                0f,
                0f,
                0f,
                0.1f,
                duration * 0.5f,
                duration * 0.5f,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }

    // sharp lens flare with anchor
    public static void createSharpFlare(CombatEngineAPI engine, ShipAPI origin, CombatEntityAPI anchor, Vector2f point, float thickness, float length, float angle, Color fringeColor, Color coreColor) {

        Vector2f offset = new Vector2f(0, thickness);
        VectorUtils.rotate(offset, MathUtils.clampAngle(angle), offset);
        Vector2f.add(offset, point, offset);

        engine.spawnEmpArcVisual(
                point,
                anchor,
                offset,
                anchor,
                length,
                fringeColor,
                coreColor
        );
    }
}
