package Utilities;

public class MHMods_utilities {

    static public String floatToString(float number) {
        if (number == Math.round(number)) {
            return Math.round(number) + "";
        } else {
            return number + "";
        }
    }

}
