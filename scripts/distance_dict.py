import pandas as pd
import numpy as np
import json

distances = np.loadtxt("data/inputs/cleaned/distances_collapsed.txt", dtype=np.float64)
durations = np.loadtxt("data/inputs/cleaned/travel_times_collapsed.txt", dtype=np.float64)
df = pd.read_csv("data/inputs/cleaned/HTM_CollapsedData.csv")
locations = list(df["ID_MAXIMO"])

distance_info = {}

for i, origin in enumerate(locations):
    distances_from_origin = {}
    for j, destination in enumerate(locations):
        distances_from_origin[destination] = {"time": durations[i,j], "dist": distances[i,j]}
    distance_info[origin] = distances_from_origin

with open(f"data/inputs/cleaned/distance_info_cleaned.json", "w") as f:
    json.dump(distance_info, f, indent=4)