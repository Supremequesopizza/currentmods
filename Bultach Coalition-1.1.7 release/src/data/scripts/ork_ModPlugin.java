package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.bt_CampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.bt_DerelictSpawner;
import com.fs.starfarer.api.impl.campaign.bt_sirenghost_creator;
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.bt_GestaltCore;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ai.bt_doombeam_autofire;
import data.scripts.ai.bt_patherlaser_autofire;
import data.scripts.ai.bt_repair_dem_ai;
import data.scripts.world.ork_Gen;
import data.scripts.fleets.bt_PersonalFleetAdmiral1;
import data.scripts.utils.bt_relationship_util;
import exerelin.campaign.SectorManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ork_ModPlugin extends BaseModPlugin {

    private static final String BULTACH_HATER_ID = "orks";
    private static final String CORPORTATE_SCUM_ID = "tritachyon";
    public final String DOOMBEAM = "ork_sathar_doom_laser";
    public final String PATHERBEAM = "ork_pather_laser";
    public final String HEALERDEM = "ork_healerdem_proj";

    private static boolean sirenGhostCreatorAdded = false;

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        switch (weapon.getId()) {
            case DOOMBEAM:
                return new PluginPick<AutofireAIPlugin>(new bt_doombeam_autofire(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            case PATHERBEAM:
                return new PluginPick<AutofireAIPlugin>(new bt_patherlaser_autofire(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case HEALERDEM:
                return new PluginPick<MissileAIPlugin>(new bt_repair_dem_ai(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }




    @Override
    public void onNewGame() {
        SectorAPI sector = Global.getSector();

        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new ork_Gen().generate(sector);
        }
        addSirenGhostCreator();
    }

    public static void OrkLoveSettings() {
        FactionAPI orks = Global.getSector().getFaction("orks");

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (orks.equals(faction) || (faction.isPlayerFaction() && orks.getId().equals(Misc.getCommissionFactionId())))
                continue;
            orks.setRelationship(faction.getId(), -0.2f);
        }

        orks.setRelationship(Factions.LUDDIC_CHURCH, 0.2f);
        orks.setRelationship(Factions.LUDDIC_PATH, -0.3f);
        orks.setRelationship(Factions.TRITACHYON, -1f);
        orks.setRelationship(Factions.PERSEAN, -0.3f);
        orks.setRelationship(Factions.PIRATES, -0.5f);
        orks.setRelationship(Factions.INDEPENDENT, 0.6f);
        orks.setRelationship(Factions.DIKTAT, -0.5f);
        orks.setRelationship(Factions.LIONS_GUARD, -0.5f);
        orks.setRelationship(Factions.HEGEMONY, 0.3f);
        orks.setRelationship(Factions.REMNANTS, -1f);

        if (Global.getSettings().getModManager().isModEnabled("blade_breakers")) {
            orks.setRelationship("blade_breakers", -0.8f);
        }
        if (Global.getSettings().getModManager().isModEnabled("exipirated")) {
            orks.setRelationship("exipirated", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("gmda")) {
            orks.setRelationship("gmda", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("gmda_patrol")) {
            orks.setRelationship("gmda_patrol", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("draco")) {
            orks.setRelationship("draco", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("fang")) {
            orks.setRelationship("fang", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("HMI")) {
            orks.setRelationship("mess", -0.8f);
        }
        if (Global.getSettings().getModManager().isModEnabled("pigeonpun_projectsolace")) {
            orks.setRelationship("projectsolace", 0.5f);
        }
        if (Global.getSettings().getModManager().isModEnabled("tahlan")) {
            orks.setRelationship("tahlan_legioinfernalis", -0.7f);
        }
        if (Global.getSettings().getModManager().isModEnabled("diableavionics")) {
            orks.setRelationship("diableavionics", -0.7f);
        }
        if (Global.getSettings().getModManager().isModEnabled("scalartech")) {
            orks.setRelationship("scalartech", -0.7f);
        }
        if (Global.getSettings().getModManager().isModEnabled("HIVER")) {
            orks.setRelationship("HIVER", -0.7f);
        }
    }

    private void setGestaltHostileToAllFactions() {
        FactionAPI gestalt = Global.getSector().getFaction("gestalt");
        if (gestalt == null) return;

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.getId().equals("gestalt")) continue;
            gestalt.setRelationship(faction.getId(), -1f);
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        OrkLoveSettings();
        setGestaltHostileToAllFactions();

        SectorAPI sector = Global.getSector();
        MarketAPI dregruk = Global.getSector().getEconomy().getMarket("dregruk");
        if (dregruk != null) {
            {
                data.campaign.econ.bt_People.create();
            }
        }
        if (!sector.hasScript(bt_PersonalFleetAdmiral1.class)) {
            sector.addScript(new bt_PersonalFleetAdmiral1());
        }
        addSirenGhostCreator();
    }

    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();

        addSirenGhostCreator();

        final String BT_RESTORED_NANO = "bt_repaired_c_nanoforge";
        final String MODIFIER_ID = "bt_nanoforge_effect";
        final float QUALITY_BONUS = ItemEffectsRepo.PRISTINE_NANOFORGE_QUALITY_BONUS;
        final int DEMAND_INCREASE = 1;

        ItemEffectsRepo.ITEM_EFFECTS.put(BT_RESTORED_NANO,
                new BoostIndustryInstallableItemEffect(
                        BT_RESTORED_NANO,
                        0,
                        DEMAND_INCREASE
                ) {
                    public
                    void apply(Industry industry) {
                        if (industry.getMarket() != null && industry.getMarket().getStats() != null) {
                            industry.getMarket().getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                                    .modifyFlat(MODIFIER_ID, QUALITY_BONUS, "Repaired Nanoforge");
                        }
                    }
                    public
                    void unapply(Industry industry) {
                        if (industry.getMarket() != null && industry.getMarket().getStats() != null) {
                            industry.getMarket().getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD)
                                    .unmodifyFlat(MODIFIER_ID);
                        }
                    }
                    protected
                    void addItemDescriptionImpl(Industry industry, TooltipMakerAPI tooltip, SpecialItemData data,
                                                InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, String pre, float pad) {
                        tooltip.addPara(pre + "A Corrupted Nanoforge that has been heavily modified in attempts to restore it. " +
                                        "Increases ship and weapon production quality by %s. " +
                                        "Increases demand for input resources by %s units. Does not increase unit output.",
                                pad,
                                Misc.getHighlightColor(),
                                "" + (int) Math.round(QUALITY_BONUS * 100f) + "%",
                                "" + DEMAND_INCREASE
                        );
                        tooltip.addPara("On habitable worlds, causes pollution which becomes permanent.", Misc.getNegativeHighlightColor(), pad);
                    }
                }
        );
    }

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        try {
            SectorAPI sector = Global.getSector();
            bt_CampaignPluginImpl plugin = new bt_CampaignPluginImpl();
            sector.registerPlugin(plugin);
        } catch (Throwable t) {
        }
        setupReputationLock();
        addSirenGhostCreator();
    }

    private void addSirenGhostCreator() {
        if (!sirenGhostCreatorAdded) {
            boolean alreadyExists = false;
            for (Object creator : SensorGhostManager.CREATORS) {
                if (creator instanceof bt_sirenghost_creator) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                SensorGhostManager.CREATORS.add(new bt_sirenghost_creator());
                sirenGhostCreatorAdded = true;
            }
        }
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        String hullId = ship.getHullSpec().getBaseHullId();

        if ("sgr_engine_left".equals(hullId) || "sgr_engine_right".equals(hullId)) {
            return new PluginPick<ShipAIPlugin>(new data.scripts.ai.bt_sathar_module_AI(ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }

        return null;
    }

    private void setupReputationLock() {
        float maxReputation = -1.0f;

        FactionAPI faction1 = Global.getSector().getFaction(BULTACH_HATER_ID);
        FactionAPI faction2 = Global.getSector().getFaction(CORPORTATE_SCUM_ID);
        if (faction1 != null && faction2 != null) {
            if (faction1.getRelationship(faction2.getId()) > maxReputation) {
                faction1.setRelationship(faction2.getId(), maxReputation);
            }
        }

        boolean alreadyRunning = false;
        for (EveryFrameScript existingScript : Global.getSector().getScripts()) {
            if (existingScript instanceof bt_relationship_util) {
                alreadyRunning = true;
                break;
            }
        }

        if (!alreadyRunning) {
            Global.getSector().addScript(new bt_relationship_util(maxReputation));
        }
    }

    public void onNewGameAfterTimePass(){
        bt_DerelictSpawner.spawnDerelicts();
    }

}
