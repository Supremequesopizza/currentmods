package forgprod.crewReplacer_Combatability;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import forgprod.abilities.conversion.support.ProductionType;

public class Forgeprod_DefaultCargo {

    public static Forgeprod_DefaultCargo active;
    public static final String TYPE_MS_Event = "MS_Event";
    public static final String TYPE_BASE_REQUIRED = "BASE_REQUIRED";
    public Forgeprod_DefaultCargo(){
        active = this;
    }
    public float getCommodityAmount(String TYPE, String commodity,CargoAPI cargo){
        //ProductionType.values();
        return cargo.getCommodityQuantity(commodity);
    }
    public void removeCommodity(String TYPE, String commodity, float amount,CargoAPI cargo){
        cargo.removeCommodity(commodity,amount);
    }
    public float getCommodityAmount(ProductionType TYPE, String commodity, CargoAPI cargo){
        return getCommodityAmount("TYPE_"+TYPE.name(),commodity,cargo);
    }
    public void removeCommodity(ProductionType TYPE, String commodity, float amount,CargoAPI cargo){
        removeCommodity("TYPE_"+TYPE.name(),commodity,amount,cargo);
    }
}
