package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import milp.*;
import SmartFeas.SmartFeas;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class LetJasperDoHisShit {
        static final double totalShiftLength = 8 * 60;

        public static void listFilesForFolder(final File folder, double[][] nightTT, double[][] dayTT, HTMInstance instance) throws IOException {
                    for (final File fileEntry : folder.listFiles()) {
                            String name = fileEntry.getPath();
                            System.out.println(name);
                            List<Shift> shifts = Utils.readShiftsFromCSVDiffTimes(name, nightTT, dayTT);
                            Utils.checkFeasibility(shifts, instance, totalShiftLength +15.0);
                        }
                    }

            public static void main(String[] args) throws Exception {
                String initSolutionDir = "src/core/scenario_instances/HTM_scenario_csvs/";
        
                String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
                String travelDayPath   = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";
                double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
                double[][] travelTimesDay   = Utils.readTravelTimes(travelDayPath);
        
                File folder = new File(initSolutionDir);
                HTMInstance instance = Utils.readInstance("src/core/scenario_instances/txt_files/summer.txt", "feasible", "Night_shift");
                
                listFilesForFolder(folder, travelTimesNight, travelTimesDay, instance);

    }  
}
