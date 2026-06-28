package data.scripts.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class bt_pirate_tempshell_stats extends BaseShipSystemScript {
    public static final float MAX_TIME_MULT = 3f;
    public static final float MIN_TIME_MULT = 0.1f;
    public static final float DAM_MULT = 0.1f;

    public static final Color JITTER_COLOR = new Color(255, 90, 164,55);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 90, 90,155);

    private boolean systemWasUsed = false;


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

        if (state == State.IN || state == State.ACTIVE) {
            systemWasUsed = true;
        }

        float jitterLevel = effectLevel;
        float jitterRangeBonus = 0;
        float maxRangeBonus = 10f;
        if (state == State.IN) {
            jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
            if (jitterLevel > 1) {
                jitterLevel = 1f;
            }
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        } else if (state == State.ACTIVE) {
            jitterLevel = 1f;
            jitterRangeBonus = maxRangeBonus;
        } else if (state == State.OUT) {
            jitterRangeBonus = jitterLevel * maxRangeBonus;
        }
        jitterLevel = (float) Math.sqrt(jitterLevel);
        effectLevel *= effectLevel;

        ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);


        float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0,0,0,0), effectLevel, 0.5f);
        ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        if (systemWasUsed) {
            float overloadDuration = 1f;
            if (ship.getVariant().hasHullMod("safetyoverrides")) {
                overloadDuration = 4f;
            }
            ship.getFluxTracker().beginOverloadWithTotalBaseDuration(overloadDuration);
            systemWasUsed = false;
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("time flow altered", false);
        }
        return null;
    }
}