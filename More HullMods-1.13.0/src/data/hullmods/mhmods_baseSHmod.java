package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

import static Utilities.mhmods_eneableSmod.getEnable;

public class mhmods_baseSHmod extends BaseHullMod {

    String id;

    Color
            s = Misc.getStoryOptionColor(),
            h = Misc.getHighlightColor();

    float
            padS = 3f,
            pad = 10f;

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        //if (id == null) return true;
        if (getEnable()) {
            if (ship != null)
                return !ship.getVariant().getSMods().contains(id);
            else return true;
        }
        return true;
    }
}
