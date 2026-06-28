package data.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Pair;


public class bt_algae_farms extends BaseIndustry {

	public void apply() {
		super.apply(true);

		int size = market.getSize();

		//Produces food and minimal organics through algae. Not efficient.
		demand(Commodities.HEAVY_MACHINERY, Math.max(4, size - 1));

		supply(Commodities.FOOD, size-2);
		supply(Commodities.ORGANICS, size-5);

		Pair<String, Integer> deficit = getMaxDeficit(Commodities.HEAVY_MACHINERY);
		applyDeficitToProduction(1, deficit, Commodities.FOOD);
		applyDeficitToProduction(2, deficit, Commodities.ORGANICS);

		if (!isFunctional()) {
			supply.clear();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
	}


	@Override
	public boolean showWhenUnavailable() {
		return false;
	}

	//It's gross
	@Override
	public boolean isAvailableToBuild() {
		if (!super.isAvailableToBuild()) return false;
		if (market.getId().contains("gathrog")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getUnavailableReason() {
		if (!super.isAvailableToBuild()) return super.getUnavailableReason();
		return "Not Available.";
	}


	@Override
	public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
		super.createTooltip(mode, tooltip, expanded);
	}
}







