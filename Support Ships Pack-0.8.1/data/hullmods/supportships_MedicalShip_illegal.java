package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class supportships_MedicalShip_illegal extends BaseHullMod {
    //Yes, you are reading this right. It literally does nothing at all other than pop some yellow text in the description.
    //Also, ping me on discord if you find this text
    //I'm ForestFighters#3760
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) {
            return "Not Installed";
        } else if (index == 1) {
            return "Any Weapons";
        } else if (index == 2) {
            return "Any Fighters";
        } else {
            return index == 3 ? "Remove this hullmod to install the chip." : null;
        }
    }
}
