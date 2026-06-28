package data.campaign.procgen;

import java.util.Random;

import com.fs.starfarer.campaign.ai.CampaignFleetAI;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

public class GestaltAssignmentAI implements EveryFrameScript {

	protected StarSystemAPI homeSystem;
	protected CampaignFleetAPI fleet;
	protected SectorEntityToken source;

	public GestaltAssignmentAI(CampaignFleetAPI fleet, StarSystemAPI homeSystem, SectorEntityToken source) {
		this.fleet = fleet;
		this.homeSystem = homeSystem;
		this.source = source;
		giveInitialAssignments();
	}

	protected void giveInitialAssignments() {
		boolean playerInSameLocation = fleet.getContainingLocation() == Global.getSector().getCurrentLocation();
		if (playerInSameLocation && (float) Math.random() < 0.1f && source != null) {
			fleet.setLocation(source.getLocation().x, source.getLocation().y);
			fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, source, 3f + (float) Math.random() * 2f, "Whispers in the Void");
		} else {
			SectorEntityToken target = GestaltSeededFleetManager.pickEntityToGuard(new Random(), homeSystem, fleet);
			if (target != null) {
				Vector2f loc = Misc.getPointAtRadius(target.getLocation(), target.getRadius() + 100f);
				fleet.setLocation(loc.x, loc.y);
			} else {
				Vector2f loc = Misc.getPointAtRadius(new Vector2f(), 5000f);
				fleet.setLocation(loc.x, loc.y);
			}
			pickNext();
		}
	}

	protected void pickNext() {
		boolean standDown = source != null && (float) Math.random() < 0.2f;

		if (!standDown) {
			SectorEntityToken target = GestaltSeededFleetManager.pickEntityToGuard(new Random(), homeSystem, fleet);
			if (target != null) {
				float speed = Misc.getSpeedForBurnLevel(8);
				float dist = Misc.getDistance(fleet.getLocation(), target.getLocation());
				float seconds = dist / speed;
				float days = seconds / Global.getSector().getClock().getSecondsPerDay();
				days += 15f + 5f * (float) Math.random();
				fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, days, "Tending the Veil");
				return;
			} else {
				if (source != null) {
					standDown = true;
				} else {
					float days = 15f + 5f * (float) Math.random();
					fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, null, days, "Listening");
					return;
				}
			}
		}

		if (source != null) {
			CampaignFleetAI.FleetAssignmentData currentAssignment = (CampaignFleetAI.FleetAssignmentData) fleet.getCurrentAssignment();
			if (currentAssignment != null && currentAssignment.getTarget() == source) {
				FleetAssignment assignmentType = currentAssignment.getAssignment();
				if (assignmentType == FleetAssignment.GO_TO_LOCATION ||
						assignmentType == FleetAssignment.ORBIT_PASSIVE ||
						assignmentType == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN) {
					return;
				}
			}

			fleet.clearAssignments();
			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, source, 30f, "Returning");
			fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, source, 2f + (float) Math.random() * 2f, "Slipping from the Void");
			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 10f, "Becoming Naught");
		}
	}

	public void advance(float amount) {
		if (fleet.getCurrentAssignment() == null) {
			pickNext();
		}
	}

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}
}

