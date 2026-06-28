package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class bt_stolen_blastdampeners extends BaseHullMod {

    public static final float RESISTANCE = 0.10f;
    public static final float RESIST_TIME = 0.15f; //in seconds


    private Set<ShipAPI> nearbyShips = new HashSet<>();
    private float resisting = 0f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount){

        if (Global.getCombatEngine() == null)
            return;


        List<ShipAPI> shipPlusModules = ship.getChildModulesCopy();
        shipPlusModules.add(ship);

        resisting -= amount;
        for (ShipAPI s : Global.getCombatEngine().getShips()){
            if (s != ship && s.getHullSize() != HullSize.FIGHTER && s.isAlive()){
                float distance = Vector2f.sub(ship.getLocation(),s.getLocation(),new Vector2f()).length() - ship.getCollisionRadius();
                for (ShipAPI child : ship.getChildModulesCopy()){
                    float newDistance = Vector2f.sub(child.getLocation(),s.getLocation(),new Vector2f()).length() - child.getCollisionRadius();
                    distance = Math.min(distance,newDistance);
                }
                float mult = s.getMutableStats().getDynamic().getValue(Stats.EXPLOSION_RADIUS_MULT);
                float radius = s.getCollisionRadius() + Math.min(200f, s.getCollisionRadius()) * mult;
                if (distance <= radius){
                    nearbyShips.add(s);
                } else {
                    nearbyShips.remove(s);
                }
            }
        }
        Iterator<ShipAPI> iter = nearbyShips.iterator();
        while (iter.hasNext()){
            ShipAPI t = iter.next();
            if (t == null || !t.isAlive()){
                iter.remove();
                for (ShipAPI s : shipPlusModules) {
                    s.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult("bt_stolen_blastdampeners", RESISTANCE);
                    s.getMutableStats().getHighExplosiveShieldDamageTakenMult().modifyMult("bt_stolen_blastdampeners", RESISTANCE);
                }
                resisting = RESIST_TIME;
            }
        }
        if (resisting <= 0f){
            for (ShipAPI s : shipPlusModules) {
                s.getMutableStats().getHighExplosiveDamageTakenMult().unmodify("bt_stolen_blastdampeners");
                s.getMutableStats().getHighExplosiveShieldDamageTakenMult().unmodify("bt_stolen_blastdampeners");
            }
        }

    }
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        float percent = (1f - RESISTANCE) * 100f;
        if (index == 0) return "" + Math.round(percent) + "%";
        return null;
    }
}
