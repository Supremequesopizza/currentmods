package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.campaign.SectorEntityToken;
//import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.CoreScript;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableLuddicPathFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.DisposablePirateFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.MercFleetManagerV2;
import com.fs.starfarer.api.impl.campaign.events.CoreEventProbabilityManager;
import com.fs.starfarer.api.impl.campaign.CoreScript;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
import java.util.ArrayList;
import exerelin.campaign.SectorManager;


//import data.hullmods.HeavyArmor;
import data.scripts.world.Auroria;


public class UAFGen extends BaseModPlugin {

   
    @Override
    public void onNewGame() {
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getCorvusMode()){
            SectorAPI sector = Global.getSector();

            new Auroria().generate(sector);

            FactionAPI uaf = sector.getFaction("uaf");
            FactionAPI player = sector.getFaction(Factions.PLAYER);
            
            player.setRelationship("uaf", 10);
            
        }
        // new Sol().generate(sector);
        // new Qonos().generate(sector);
        // new Eridani().generate(sector);
        // new Romulus().generate(sector);
        // new System1().generate(sector);
        // new Cerberi().generate(sector);
        // new Andoria().generate(sector);
   

//        FactionAPI ufp = sector.getFaction("united_federation");
//        FactionAPI andorian = sector.getFaction("andorian");
//        FactionAPI vulcan = sector.getFaction("vulcan");
//        
//        ufp.setRelationship(Factions.PLAYER, 0.35f);
//
//        ufp.setRelationship("andorian", 1f);
//        ufp.setRelationship("vulcan", 1f);
//        ufp.setRelationship(Factions.HEGEMONY, 0.45f);
//        ufp.setRelationship(Factions.TRITACHYON, 0.25f);
//        ufp.setRelationship(Factions.PIRATES, -1f);
//        ufp.setRelationship(Factions.INDEPENDENT, RepLevel.FAVORABLE);
//        ufp.setRelationship(Factions.LUDDIC_CHURCH, RepLevel.INHOSPITABLE);
//        ufp.setRelationship(Factions.KOL, RepLevel.HOSTILE);
//        ufp.setRelationship(Factions.LUDDIC_PATH, RepLevel.VENGEFUL);
//        ufp.setRelationship(Factions.PERSEAN, RepLevel.FAVORABLE);
//
//        andorian.setRelationship(Factions.PLAYER, 0.35f);
//
//        andorian.setRelationship(Factions.HEGEMONY, 0.2f);
//        andorian.setRelationship("united_federation", 1f);
//        andorian.setRelationship("vulcan", -0.3f);
//        andorian.setRelationship(Factions.TRITACHYON, 0f);
//        andorian.setRelationship(Factions.PIRATES, -1f);
//        andorian.setRelationship(Factions.INDEPENDENT, 0.3f);
//        andorian.setRelationship(Factions.LUDDIC_CHURCH, -0.2f);
//        andorian.setRelationship(Factions.KOL, 0.09f);
//        andorian.setRelationship(Factions.LUDDIC_PATH, -1f);
//        andorian.setRelationship(Factions.PERSEAN, 0.154f);
//
//        vulcan.setRelationship(Factions.PLAYER, 0.2f);
//
//        vulcan.setRelationship(Factions.HEGEMONY, 0.1f);
//        vulcan.setRelationship("united_federation", 1f);
//        vulcan.setRelationship("andorian", -0.3f);
//        vulcan.setRelationship(Factions.TRITACHYON, 0f);
//        vulcan.setRelationship(Factions.PIRATES, -1f);
//        vulcan.setRelationship(Factions.INDEPENDENT, 0.3f);
//        vulcan.setRelationship(Factions.LUDDIC_CHURCH, 0f);
//        vulcan.setRelationship(Factions.KOL, 0.09f);
//        vulcan.setRelationship(Factions.LUDDIC_PATH, -1f);
//        vulcan.setRelationship(Factions.PERSEAN, 0.154f);


        
//        List<FactionAPI> factions = sector.getAllFactions();
//        for (FactionAPI faction: factions){
//            if (!faction.isShowInIntelTab()){
//                neutrinocorp.setRelationship(faction.getId(), RepLevel.NEUTRAL);
//            }
//        }

		
	}
}