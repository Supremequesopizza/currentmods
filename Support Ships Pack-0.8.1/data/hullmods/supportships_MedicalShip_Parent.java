package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.Map;

//more hidden text woooooooo
//Fun fact, this is a large part of why 0.7.0 took a long time.
//I am bad at coding


public class supportships_MedicalShip_Parent extends BaseHullMod {

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        if (!ship.getVariant().hasHullMod("supportships_MedicalShip")&&!ship.getVariant().hasHullMod("supportships_MedicalShip_illegal")) {
            if(memory.get("$nextmod")==null||memory.get("$nextmod")=="supportships_MedicalShip_illegal"){
                memory.set("$nextmod","supportships_MedicalShip");
            }else if(memory.get("$nextmod")=="supportships_MedicalShip"){
                memory.set("$nextmod","supportships_MedicalShip_illegal");
            }else{
                memory.set("$nextmod","supportships_MedicalShip");
            }
            ship.getVariant().getHullMods().add((String) memory.get("$nextmod"));
        }
    }
}