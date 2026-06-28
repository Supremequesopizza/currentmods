// 
// Decompiled by Procyon v0.5.36
// 
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class BrutePhaseStats extends BaseShipSystemScript {

    public static Color FRINGE_COLOR = new Color(164, 197, 219, 175);
    public static Color JITTER = new Color(242, 85, 85, 160);
    public static Color JITTER2 = new Color(242, 85, 85, 15);
    public static Color FRINGE_COLOR2 = new Color(255, 255, 200, 175);

    private float angle = 0;
    private boolean runonce = true;

    protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) {
            cloak = playerShip.getSystem();
        }
        if (cloak == null) {
            return;
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        if (state == State.COOLDOWN || state == State.IDLE || state == State.OUT) {

            float mult = ship.getMutableStats().getTimeMult().getMult();
            ship.setJitterUnder(this, JITTER, mult - 1f, 25, 0f, 7f + (mult * 40f - 2f));

        }
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);

            return;
        }
        if (state == State.IN) {
            float TimeLevel = ship.getMutableStats().getTimeMult().getMult() / 3.6f;
            ship.setJitterUnder(ship, Color.RED, 1f - TimeLevel, 7, 10f, 15f);
            return;
        }

        float level = effectLevel;
        //float f = VULNERABLE_FRACTION;


        float jitterLevel = 0f;
        float jitterRangeBonus = 0f;
        float levelForAlpha = level;
        float LevelwithTime = levelForAlpha + (ship.getMutableStats().getTimeMult().getMult() / 15f);
        float TimeLevel = ship.getMutableStats().getTimeMult().getMult() / 2f;

        if (effectLevel == 1) {
            stats.getFluxDissipation().modifyMult(id, 0.23f);
            ship.setJitterUnder(ship, JITTER, 1f - TimeLevel, 7, 10f, 15f);
            if (player) {
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / TimeLevel);
            }

            ShipEngineControllerAPI sengine = ship.getEngineController();

            if(runonce) {
                if (sengine.isAccelerating() && sengine.isStrafingLeft()) {
                    angle = 45;
                } else if (sengine.isAccelerating() && sengine.isStrafingRight()) {
                    angle = 315;
                } else if (sengine.isAccelerating()) {
                    angle = 0;
                } else if (sengine.isAcceleratingBackwards() && sengine.isStrafingLeft()) {
                    angle = 135;
                } else if (sengine.isAcceleratingBackwards() && sengine.isStrafingRight()) {
                    angle = 225;
                } else if (sengine.isAcceleratingBackwards()) {
                    angle = 180;
                } else if (sengine.isStrafingLeft()) {
                    angle = 90;
                } else if (sengine.isStrafingRight()) {
                    angle = 270;
                }

                runonce = false;
            }

            ship.getVelocity().set(VectorUtils.rotate(new Vector2f(150f,0f),angle+ship.getFacing()));

            if(Math.random()>0.75) Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+0f),ship,10f,Color.RED,Color.orange);

            ship.setAlphaMult(0.25f);
            ship.setPhased(true);
            ship.setCollisionClass(CollisionClass.NONE);
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        runonce = true;
        ShipAPI ship = null;

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        for(ShipAPI e : AIUtils.getNearbyEnemies(ship,1000f)) {
            Global.getCombatEngine().spawnEmpArc(ship, ship.getLocation(), ship, e, DamageType.ENERGY,250f,250f,2000f,null,10f,Color.RED,Color.orange);
        }
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);
        Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(),ship, MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()+200f),ship,10f,Color.RED,Color.orange);


        ship.setPhased(false);
        ship.setAlphaMult(1f);
        ship.setCollisionClass(CollisionClass.SHIP);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}
