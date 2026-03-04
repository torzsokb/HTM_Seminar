import pandas as pd
import numpy as np
import json

distances = np.loadtxt("data/inputs/cleaned/distances_collapsedv2.txt", dtype=np.float64)
durations_osrm = np.loadtxt("data/inputs/cleaned/travel_times_collapsedv2.txt", dtype=np.float64)
speed_night = np.loadtxt("data/inputs/cleaned/speed_night_collapsedv2.txt", dtype=np.float64)
speed_day = np.loadtxt("data/inputs/cleaned/speed_day_collapsedv2.txt", dtype=np.float64)
df = pd.read_csv("data/inputs/HTM_Data_abriTypeStop.csv")
locations = list(df["ID_MAXIMO"])

distance_info = {}

for i, origin in enumerate(locations):
    distances_from_origin = {}
    for j, destination in enumerate(locations):
        
        distance = distances[i,j]
        osrm_travel_time = durations_osrm[i,j] 
        speed_osrm = distance / osrm_travel_time if osrm_travel_time > 0 else 0

        speed_n = speed_night[i,j] 
        speed_d = speed_day[i,j] 

        night_time = distance / speed_n if speed_n > 0 else 0
        day_time = distance / speed_d if speed_d > 0 else 0

        distances_from_origin[destination] = {
            "dist": distance, 
            "speed_osrm": speed_osrm, 
            "speed_day": speed_d, 
            "speed_night": speed_n,
            "time_osrm" : osrm_travel_time,
            "time_night": night_time,
            "time_day": day_time
        }

    distance_info[origin] = distances_from_origin

with open(f"data/inputs/cleaned/distance_info_cleanedv2.json", "w") as f:
    json.dump(distance_info, f, indent=4)