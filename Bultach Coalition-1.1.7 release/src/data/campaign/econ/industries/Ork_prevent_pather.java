package data.campaign.econ.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;

public class Ork_prevent_pather extends BaseIndustry {

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public boolean canBeDisrupted() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public boolean isFunctional() {
        return market.getFactionId().contentEquals("orks");
    }

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public float getPatherInterest() {
        if (market.getFactionId().contentEquals("orks")) {
            return -1000f;
        }
        else {
            return 0;
        }
    }
}