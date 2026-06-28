package com.fs.starfarer.api.impl.campaign;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhostCreator;
import com.fs.starfarer.api.impl.campaign.ghosts.GhostFrequencies;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.util.Misc;

public class bt_sirenghost_creator extends BaseSensorGhostCreator {

	public static float MIN_DISTANCE_FROM_HUBRIS_LY = 1f;


	@Override
	public
	List<SensorGhost> createGhost(SensorGhostManager manager) {
		if (!Global.getSector().getCurrentLocation().isHyperspace()) return null;

		CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
		StarSystemAPI hubris = Global.getSector().getStarSystem("Hubris");
		if (hubris == null) return null;

		float distanceToHubrisLY = Misc.getDistanceLY(pf.getLocationInHyperspace(), hubris.getLocation());
		if (distanceToHubrisLY < MIN_DISTANCE_FROM_HUBRIS_LY) return null;

		List<SensorGhost> result = new ArrayList<SensorGhost>();
		bt_sirenghost g = new bt_sirenghost(manager, hubris.getHyperspaceAnchor());
		if (g.isCreationFailed()) return null;
		result.add(g);
		return result;
	}


	@Override
	public
	float getFrequency(SensorGhostManager manager) {
		CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
		StarSystemAPI hubris = Global.getSector().getStarSystem("Hubris");
		if (hubris == null) return 0f;

		float distanceToHubrisLY = Misc.getDistanceLY(pf.getLocationInHyperspace(), hubris.getLocation());

		if (distanceToHubrisLY <= MIN_DISTANCE_FROM_HUBRIS_LY) {
			return GhostFrequencies.getGuideFrequency(manager) * 3f;
		}

		return GhostFrequencies.getGuideFrequency(manager);
	}
}