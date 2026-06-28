package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class BT_antiNukeHidden extends BaseHullMod {

    public static float
            dmgIncrease = 0.3f,
            dmgTaken = 1.25f,
            cloakCost = 1.25f,
            rangePenalty = 0.85f,
            minRange = 500,
            maxRange = 700;

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }

    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
//        if (index == 0) return Math.round((dmgIncrease) * 100) + "%";
//        if (index == 1) return Math.round(minRange) + "";
//        if (index == 2) return Math.round(maxRange) + "";
//        if (index == 3) return Math.round(((dmgTaken) - 1) * 100) + "%";
//        if (index == 4) return Math.round(((cloakCost) - 1) * 100) + "%";
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getOriginalOwner() == -1) return;
        if (Global.getCurrentState().equals(GameState.TITLE)) return;
        //attach the listener
        if (!ship.hasListenerOfClass(ork_idkname_armor_thingy_listener.class)) {
            ork_idkname_armor_thingy_listener listener = new ork_idkname_armor_thingy_listener();
            listener.ship = ship;
            ship.addListener(listener);
        }

    }

    static class ork_idkname_armor_thingy_listener implements DamageTakenModifier {
        public CombatEntityAPI ship = null;

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

            if (ship == null) return null;

            String id = "BT_antiNukeHidden"; //change this to the hullmod ID you want

            //modify damage
            if(damage.getDamage() > 4000f) {
                damage.getModifier().modifyMult(id, (float) 0.20);
            }
            return id;
        }
    }
}
