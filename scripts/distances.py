import pandas as pd
import googlemaps
import json
import datetime
import math

REFERENCE_DEPARTURE_TIME = datetime.datetime(2026, 2, 2, 15)
TRAFFIC_MODEL = "pessimistic"
DEPOT_LAT = 0.0
DEPOT_LONG = 0.0
BATCH_SIZE = 10

maps_key = "AIzaSyA7zbsadbUBeP2g2-g4fom1Sl0cVFUPl54"
gmaps = googlemaps.Client(key=maps_key)

df = pd.read_excel("data/inputs/raw/Data_POH_5WK_REINIGEN_ABRI_EN_HEKWERK.xlsx", sheet_name=["Dagroutes", "Nachtroutes", "Halteinfo"])
df_stops = df["Halteinfo"]

locations = list(df_stops["ID_MAXIMO"])
locations.insert(0, "DEPOT")

coordinates = list(zip(df_stops.latitude, df_stops.longitude))
coordinates.insert(0, (DEPOT_LAT, DEPOT_LONG))


for i in range(math.ceil(len(locations) / BATCH_SIZE)):
    if (i + 1) * BATCH_SIZE >= len(locations):
        origin_batch_coordinates = coordinates[i * BATCH_SIZE:]
        origin_batch_locations = locations[i * BATCH_SIZE:]
    else:
        origin_batch_coordinates = coordinates[i * BATCH_SIZE:(i + 1) * BATCH_SIZE]
        origin_batch_locations = locations[i * BATCH_SIZE:(i + 1) * BATCH_SIZE]

    for j in range(math.ceil(len(locations) / BATCH_SIZE)):
        if (j + 1) * BATCH_SIZE >= len(locations):
            destination_batch_coordinates = coordinates[j * BATCH_SIZE:]
            destination_batch_locations = locations[j * BATCH_SIZE:]
            
        else:
            destination_batch_coordinates = coordinates[j * BATCH_SIZE:(j + 1) * BATCH_SIZE]
            destination_batch_locations = locations[j * BATCH_SIZE:(j + 1) * BATCH_SIZE]

        distance_info = gmaps.distance_matrix(
            origin_batch_coordinates,
            destination_batch_coordinates,
            mode="driving",
            departure_time=REFERENCE_DEPARTURE_TIME,
            traffic_model=TRAFFIC_MODEL)
        
        distance_info["destination_addresses"] = destination_batch_locations
        distance_info["origin_addresses"] = origin_batch_locations
        
        with open(f"data/inputs/cleaned/distance_info_{i}_{j}.json", "w") as f:
            json.dump(distance_info, f, indent=4)

        
