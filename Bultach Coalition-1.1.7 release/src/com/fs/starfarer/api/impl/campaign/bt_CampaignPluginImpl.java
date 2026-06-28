package com.fs.starfarer.api.impl.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreAdminPlugin;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

public class bt_CampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "bt_CampaignPlugin";
    }

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        if ("bt_gestalt_core".equals(commodityId)) {
            return new PluginPick<AICoreOfficerPlugin>(new bt_GestaltCore(), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public boolean isTransient() {
        return true;
    }
}