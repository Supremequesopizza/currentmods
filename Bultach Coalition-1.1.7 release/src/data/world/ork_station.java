package data.world;

import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates Anargaia and attaches various extra scripts to it to get a solid all-round experience. Sure wish it showed in the starsystem screen though...
 * @author Nicke535
 */
public class ork_station {
    /* -- Governor/Admin settings -- */
    // The path to the portrait of the AGI admin. I think it also needs to be added to the settings.json file to show up like some other custom sprites, but I'm not sure
    public static final String ADMIN_PORTRAIT_PATH = "graphics/portraits/bt_ai_station.png";

    // The first and last name of the AGI core (last should probably be left empty, unless you want it to have one)
    public static final String ADMIN_FIRST_NAME = "Warmind";
    public static final String ADMIN_LAST_NAME = "";
    
}
