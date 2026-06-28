package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.bultach_utils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class bultach_dash extends BaseShipSystemScript
{
    public static float SPEED_BONUS = 300f;
    private static final Color AFTERIMAGE_COLOR = new Color(255, 249, 182, 118);
    public static final float AFTERIMAGE_DURATION = 0.66f;

    // this is a subtractive color- it's getting subtracted from every other color on the screen where it renders
    // ie, if you make this 255,255,0, it'll look blue in-game because it's subtracting all the non-blue colors
    public static final Color PARTICLE_COLOR = new Color(70, 101, 255, 126);
    private IntervalUtil afterImageTimer = new IntervalUtil(0.06f, 0.06f);
    private boolean didVFX = false;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (!(stats.getEntity() instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
        stats.getMaxTurnRate().modifyMult(id, 0f);
        stats.getTurnAcceleration().modifyMult(id, 100f);

        afterImageTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (afterImageTimer.intervalElapsed())
        {
            bultach_utils.afterimage(ship, AFTERIMAGE_COLOR, AFTERIMAGE_DURATION);
        }

        if (state == State.IN) {
            stats.getAcceleration().modifyMult(id, 500f);
            stats.getDeceleration().modifyMult(id, 500f);
        }
        if (state == State.ACTIVE) {
            if (ship.getVelocity().length() < ship.getMaxSpeed())
            {
                ship.getVelocity().scale(100f);
                VectorUtils.clampLength(ship.getVelocity(), ship.getMaxSpeed(), ship.getVelocity());
            }
            stats.getAcceleration().modifyMult(id, 0f);
            stats.getDeceleration().modifyMult(id, 0f);
        }
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().modifyMult(id, 500f);
            stats.getDeceleration().modifyMult(id, 500f);
        }
        if (!didVFX && effectLevel >= 1)
        {
            didVFX = true;
            for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines())
            {
                if (!eng.isActive()) continue;
                Vector2f spawnLoc = new Vector2f(eng.getEngineSlot().getWidth() * 0.66f, 0f);
                VectorUtils.rotate(spawnLoc, eng.getEngineSlot().getAngle() + ship.getFacing());
                Vector2f.add(eng.getLocation(), spawnLoc, spawnLoc);
                Global.getCombatEngine().addNegativeParticle(
                        spawnLoc,
                        ship.getVelocity(),
                        eng.getEngineSlot().getWidth() * 1.5f,
                        0.0F,
                        ship.getSystem().getChargeActiveDur(),
                        PARTICLE_COLOR);
                Global.getCombatEngine().addNegativeNebulaParticle(
                        spawnLoc,
                        ship.getVelocity(),
                        eng.getEngineSlot().getWidth() * 1.5f,
                        1.75F,
                        0.1F,
                        0.8F,
                        ship.getSystem().getChargeActiveDur(),
                        PARTICLE_COLOR);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        didVFX = false;
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }
}
