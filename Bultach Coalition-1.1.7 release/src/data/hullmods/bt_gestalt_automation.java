package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lwjgl.util.vector.Vector2f;

public class bt_gestalt_automation extends BaseHullMod {

	private static final float REPAIR_BONUS = 75f;
	private static final float FLUX_RESISTANCE = 75f;
	public static final float COST_REDUCTION_MEDIUM = 6;
	public static final float COST_REDUCTION_SMALL = 3;
	private static final float VENT_RATE_BONUS = 35f;
	private static final float MAX_CR_PENALTY = 1f;

	private static final String GESTALT_CORE_ID = "bt_gestalt_core";
	private static final String GESTALT_FACTION_ID = "gestalt";

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getCombatEngineRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS * 0.01f);
		stats.getCombatWeaponRepairTimeMult().modifyMult(id, 1f - REPAIR_BONUS * 0.01f);
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - FLUX_RESISTANCE * 0.01f);
		stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);

		boolean conditionsMetForOpBonus = false;
		boolean hasGestaltCore = false;
		FleetMemberAPI member = stats.getFleetMember();

		if (member != null) {
			PersonAPI captain = member.getCaptain();
			if (captain != null && !captain.isDefault() && captain.isAICore() && GESTALT_CORE_ID.equals(captain.getAICoreId())) {
				hasGestaltCore = true;
				conditionsMetForOpBonus = true;
			}

			if (!conditionsMetForOpBonus && member.getFleetData() != null) {
				CampaignFleetAPI fleet = member.getFleetData().getFleet();
				if (fleet != null && fleet.getFaction() != null) {
					String memberFleetFactionId = fleet.getFaction().getId();
					if (GESTALT_FACTION_ID.equals(memberFleetFactionId)) {
						conditionsMetForOpBonus = true;
					}
				}
			}
		}

		if (conditionsMetForOpBonus) {
			stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_MEDIUM);
			stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD).modifyFlat(id, -COST_REDUCTION_MEDIUM);
			stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_SMALL);
			stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD).modifyFlat(id, -COST_REDUCTION_SMALL);
		}

		if (stats.getVariant() != null) {
			stats.getVariant().addTag(Tags.AUTOMATED);
		}

		stats.getMinCrewMod().modifyMult(id, 0);
		stats.getMaxCrewMod().modifyMult(id, 0);

		if (hasGestaltCore && stats.getVariant() != null) {
			stats.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
		}

		if (isInPlayerFleet(stats) && !isAutomatedNoPenalty(stats)) {
			stats.getMaxCombatReadiness().modifyFlat(id, -MAX_CR_PENALTY, "Automated ship penalty");
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.setInvalidTransferCommandTarget(true);
	}

	public static boolean isAutomatedNoPenalty(MutableShipStatsAPI stats) {
		if (stats == null) return false;
		FleetMemberAPI member = stats.getFleetMember();
		if (member == null) {
			if (stats.getVariant() != null) {
				return stats.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY);
			}
			return false;
		}
		return member.getHullSpec().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY) ||
				(member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY));
	}

	public static boolean isAutomatedNoPenalty(FleetMemberAPI member) {
		if (member == null) return false;
		return member.getHullSpec().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY) ||
				(member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY));
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getOriginalOwner() == -1) return;
		if (Global.getCombatEngine() == null || Global.getCombatEngine().isCombatOver() || Global.getCurrentState().equals(GameState.TITLE)) return;

		if (!ship.hasListenerOfClass(bt_gestalt_dmg_listener.class)) {
			bt_gestalt_dmg_listener listener = new bt_gestalt_dmg_listener();
			listener.ship = ship;
			ship.addListener(listener);
		}
	}

	static class bt_gestalt_dmg_listener implements DamageTakenModifier {
		public ShipAPI ship = null;

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (this.ship == null || target != this.ship) {
				return null;
			}
			String id = "bt_gestalt_automation_dmg_reduction";
			if (damage.getDamage() > 6000f) {
				damage.getModifier().modifyMult(id, 0.10f);
			}
			return id;
		}
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}
}