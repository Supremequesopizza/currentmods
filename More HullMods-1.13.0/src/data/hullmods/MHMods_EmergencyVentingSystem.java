package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MHMods_EmergencyVentingSystem extends BaseHullMod {

	private final float TriggerAt = 0.9f;
	private final float RedPerCell = 0.8f;
	private final float HardFluxVent = 0.5f;
	private final float SoftFluxVent = 0.7f;

	private final float VentTime = 10f;

	private boolean IsActive = false;


	private final Map<HullSize, Integer> mag = new HashMap<>();
	{
		mag.put(HullSize.FIGHTER, 5);
		mag.put(HullSize.FRIGATE, 5);
		mag.put(HullSize.DESTROYER, 4);
		mag.put(HullSize.CRUISER, 3);
		mag.put(HullSize.CAPITAL_SHIP, 3);
	}

	 public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(TriggerAt * 100f) + "%";
		if (index == 1) return Math.round(HardFluxVent * 100f) + "%";
		if (index == 2) return Math.round(SoftFluxVent * 100f) + "%";
		if (index == 3) return VentTime + "";
		if (index == 4) return mag.get(HullSize.FRIGATE) + "";
		if (index == 5) return mag.get(HullSize.DESTROYER) + "";
		if (index == 6) return mag.get(HullSize.CRUISER) + "";
		if (index == 7) return mag.get(HullSize.CAPITAL_SHIP) + "";
		if (index == 8) return Math.round((1f - RedPerCell) * 100f) + "%";
        return null;
    }

    @Override
	public void advanceInCombat(ShipAPI ship, float amount) {

		if (!ship.isAlive()) return;
		int FusionCell_used = 0;

		if (Global.getCombatEngine().getCustomData().get("MHM_FusionCell_used" + ship.getId()) instanceof Integer)
			FusionCell_used = (int) Global.getCombatEngine().getCustomData().get("MHM_FusionCell_used" + ship.getId());

		float HardFluxAtStart = 0f;
		if (Global.getCombatEngine().getCustomData().get("MHM_HardFluxAtStart" + ship.getId()) instanceof Float)
			HardFluxAtStart = (float) Global.getCombatEngine().getCustomData().get("MHM_HardFluxAtStart" + ship.getId());

		float SoftFluxAtStart = 0f;
		if (Global.getCombatEngine().getCustomData().get("MHM_SoftFluxAtStart" + ship.getId()) instanceof Float)
			SoftFluxAtStart = (float) Global.getCombatEngine().getCustomData().get("MHM_SoftFluxAtStart" + ship.getId());

		float TimeVentingPas = 0f;
		if (Global.getCombatEngine().getCustomData().get("MHM_TimeVentingPas" + ship.getId()) instanceof Float)
			TimeVentingPas = (float) Global.getCombatEngine().getCustomData().get("MHM_TimeVentingPas" + ship.getId());

		MutableShipStatsAPI stats = ship.getMutableStats();
		float Flux = ship.getFluxTracker().getFluxLevel();
		float SoftFlux = ship.getFluxTracker().getCurrFlux();
		float HardFlux = ship.getFluxTracker().getHardFlux();

		if (IsActive){
			TimeVentingPas += amount;
			if (TimeVentingPas >= VentTime){
				IsActive = false;

				float VentedHardFlux = HardFluxAtStart * HardFluxVent * (amount - (TimeVentingPas - VentTime)) / VentTime;
				ship.getFluxTracker().setHardFlux(HardFlux - VentedHardFlux);
				ship.getFluxTracker().setCurrFlux((SoftFlux - ((SoftFluxAtStart - HardFluxAtStart) * SoftFluxVent * (amount - (TimeVentingPas - VentTime)) / VentTime)) - VentedHardFlux);
				stats.getFluxCapacity().modifyMult("MHMods_FusionCells", (float) Math.pow(RedPerCell, FusionCell_used));

				TimeVentingPas = 0f;
			}else {
				float VentedHardFlux = (HardFluxAtStart * HardFluxVent * (amount / VentTime));
				ship.getFluxTracker().setHardFlux(HardFlux - VentedHardFlux);
				ship.getFluxTracker().setCurrFlux((SoftFlux - ((SoftFluxAtStart - HardFluxAtStart) * SoftFluxVent * amount / VentTime)) - VentedHardFlux);
				stats.getFluxCapacity().modifyMult("MHMods_FusionCells", (float) Math.pow(RedPerCell, FusionCell_used - 1 + (TimeVentingPas / VentTime)));
			}
			Global.getCombatEngine().getCustomData().put("MHM_TimeVentingPas" + ship.getId(), TimeVentingPas);
		}
		if (FusionCell_used < mag.get(ship.getHullSize())) {
			if (Flux >= TriggerAt && !IsActive) {
				Global.getCombatEngine().getCustomData().put("MHM_HardFluxAtStart" + ship.getId(), HardFlux);
				Global.getCombatEngine().getCustomData().put("MHM_SoftFluxAtStart" + ship.getId(), SoftFlux);

				//ship.getFluxTracker().setHardFlux(HardFlux * HardFluxVent);
				//ship.getFluxTracker().setCurrFlux((SoftFlux - HardFlux) * SoftFluxVent + ship.getFluxTracker().getHardFlux());

				FusionCell_used += 1;
				Global.getCombatEngine().getCustomData().put("MHM_FusionCell_used" + ship.getId(), FusionCell_used);

				IsActive = true;

			}
			if (ship.isPhased())
				ship.setJitterUnder(ship, new Color(0, 255, 255, 20),2f,10,5);
			else
				ship.setJitterUnder(ship, new Color(0, 255, 255, 215),2f,10,5);
			ship.setJitterShields(false);

			if (ship == Global.getCombatEngine().getPlayerShip())
				Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_EmergencyVentingSystem", "graphics/icons/hullsys/emp_emitter.png", "Cells left", Math.round( mag.get(ship.getHullSize()) - FusionCell_used) + " Cells", false);
		}
		else if (ship == Global.getCombatEngine().getPlayerShip())
			Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_EmergencyVentingSystem", "graphics/icons/hullsys/emp_emitter.png", "Cells left", "Out of cells", false);
	}
}








