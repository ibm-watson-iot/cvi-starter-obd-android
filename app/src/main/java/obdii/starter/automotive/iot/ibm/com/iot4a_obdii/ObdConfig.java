package obdii.starter.automotive.iot.ibm.com.iot4a_obdii;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.util.ArrayList;

/* https://github.com/pires/android-obd-reader/edit/master/src/main/java/com/github/pires/obd/reader/config/ObdConfig.java */

public final class ObdConfig {

    public static ArrayList<ObdCommand> getCommands() {
        ArrayList<ObdCommand> cmds = new ArrayList<>();

        cmds.add(new FuelLevelCommand());
        cmds.add(new EngineCoolantTemperatureCommand());
        cmds.add(new SpeedCommand());

        return cmds;
    }

}