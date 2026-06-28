package com.fs.starfarer.api.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.econ.Industry;

import java.awt.*;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl.*;

public class bt_GestaltCore extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin{

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) {
            new Random();
        }
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        boolean GestaltCore = "bt_gestalt_core".equals(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
        int points = 0;
        float mult = 1.0F;
        if (GestaltCore) {
            person.getStats().setLevel(7);
            person.getStats().setSkillLevel("helmsmanship", 2.0F);
            person.getStats().setSkillLevel("target_analysis", 2.0F);
            person.getStats().setSkillLevel("energy_weapon_mastery", 2.0F);
            person.getStats().setSkillLevel("field_modulation", 2.0F);
            person.getStats().setSkillLevel("gunnery_implants", 2.0F);
            person.getStats().setSkillLevel("combat_endurance", 2.0F);
            person.getStats().setSkillLevel("bt_gestalt_influence", 2.0F);
            person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "BTgestaltcore"));

            points = ALPHA_POINTS;
            mult = ALPHA_MULT;
        }
        person.getMemoryWithoutUpdate().set("$autoPointsMult", mult);

        person.setPersonality(Personalities.RECKLESS);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId((String)null);
        person.getStats().setSkipRefresh(false);
        return person;
    }
    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        float opad = 10.0F;
        Color text = person.getFaction().getBaseUIColor();
        Color bg = person.getFaction().getDarkUIColor();
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());
        if (spec.getId().equals("bt_gestalt_core")) {
            tooltip.addSectionHeading("Personality: Devoted", text, bg, Alignment.MID, 20.0F);
            tooltip.addPara("In combat, the " + spec.getName() + " is single-minded and near fervent. " + "This display of zealotry is more akin to fury than fearlessness.", opad);
        }
    }
    public boolean isInstallable(Industry industry) {
        return false; // probably doesn't work
    }
}

