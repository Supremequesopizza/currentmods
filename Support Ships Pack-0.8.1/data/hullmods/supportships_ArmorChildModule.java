package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;

public class supportships_ArmorChildModule extends BaseHullMod {
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        ShipAPI parent = ship.getParentStation();
        if(!ship.isAlive() || parent==null || !parent.isAlive()){
            return;
        }
    }
    
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}