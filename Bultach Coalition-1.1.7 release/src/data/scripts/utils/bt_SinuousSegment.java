package data.scripts.utils;

import com.fs.starfarer.api.combat.ShipAPI;
import java.util.List;

//I STOLE THIS FROM KOL TOO
public class bt_SinuousSegment {

    public ShipAPI ship = null;
    public bt_SinuousSegment nextSegment = null;
    public bt_SinuousSegment previousSegment = null;

    public static void setup(bt_SinuousSegment[] segments, List<ShipAPI> ships, String[] args){

        for (int f = 0; f < segments.length; f++){
            // Iterates through SinuousSegment array and connects them in order
            segments[f] = new bt_SinuousSegment();
            if (f > 0){
                segments[f].previousSegment = segments[f-1];
                segments[f-1].nextSegment = segments[f];
            }

            // Assigns each module to a segment based on its station slot name
            for (ShipAPI s : ships) {
                s.ensureClonedStationSlotSpec();

                if (s.getStationSlot() != null && s.getStationSlot().getId().equals(args[f])) {
                    segments[f].ship = s;

                    // First module: Assigns mothership as its previousSegment
                    if (f == 0){
                        segments[f].previousSegment = new bt_SinuousSegment();
                        segments[f].previousSegment.ship = s.getParentStation();
                        segments[f].previousSegment.nextSegment = segments[f];
                    }
                }
            }
        }
    }

    public bt_SinuousSegment(){
    }

    public bt_SinuousSegment(ShipAPI newShip) {
        ship = newShip;
        previousSegment = new bt_SinuousSegment();
        previousSegment.ship = ship.getParentStation();
        previousSegment.nextSegment = this;
    }

    public bt_SinuousSegment(ShipAPI newShip, bt_SinuousSegment newPrevious){
        ship = newShip;
        previousSegment = newPrevious;
        previousSegment.nextSegment = this;
    }

}
