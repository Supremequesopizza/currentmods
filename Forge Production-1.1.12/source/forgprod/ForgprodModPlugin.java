package forgprod;

import forgprod.abilities.modules.FleetwideModuleManager;
import forgprod.crewReplacer_Combatability.Forgeprod_CrewReplacerCargo;
import forgprod.crewReplacer_Combatability.Forgeprod_DefaultCargo;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.FactionAPI;

import forgprod.settings.SettingsDataSupplier;
import forgprod.settings.SettingsHolder;

@SuppressWarnings("unused")
public final class ForgprodModPlugin extends BaseModPlugin {

    public static final String FORGE_SETTINGS = "forge_production_settings.ini";
    public static final String FORGE_PRODUCTION_ABILITY = "forge_production";

    @Override
    public void onApplicationLoad() throws JSONException, IOException {
        SettingsDataSupplier.loadSettings(FORGE_SETTINGS);
        crewReplacerCombatability();
    }
    @Override
    public void onGameLoad(boolean newGame) {
        FleetwideModuleManager.replaceThing();
        if (!SettingsHolder.ENABLE_FEATURE_UNLOCK) {
            return;
        }
        if (!Global.getSector().getPlayerFleet().hasAbility(FORGE_PRODUCTION_ABILITY)) {
            Global.getSector().getCharacterData().addAbility(FORGE_PRODUCTION_ABILITY);
        }
        FactionAPI player = Global.getSector().getPlayerFaction();
        ArrayList<String> forgeHullmods = new ArrayList<>();
        forgeHullmods.add("forgprod_refining_module");
        forgeHullmods.add("forgprod_fuel_production_module");
        forgeHullmods.add("forgprod_heavy_industry_module");
        for (String hullmod : forgeHullmods) {
            if (!player.knowsHullMod(hullmod)) {
                player.addKnownHullMod(hullmod);
            }
        }
    }

    private void crewReplacerCombatability(){
        if (Global.getSettings().getModManager().isModEnabled("aaacrew_replacer")){
            new Forgeprod_CrewReplacerCargo();
        }else{
            new Forgeprod_DefaultCargo();
        }
    }
}
