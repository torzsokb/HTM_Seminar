import pandas as pd
import json
import math
import requests
import os
import time


BATCH_SIZE = 31

key = "8a92140aa65a4f0cbc625c3f4d4bbe29"
url = f"https://api.geoapify.com/v1/routematrix?apiKey={key}"

headers = {
  'Content-Type': 'application/json',
  'Content-Length': ''
}


df = pd.read_csv("src/results/HTM_data_initRes_typeHalte.csv")
locations = list(df["ID_MAXIMO"])
coordinates = list(zip(df.longitude, df.latitude))



n_requests = 0

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

        
        path = f"data/inputs/cleaned/distance_info_{i}_{j}.json"
        
        if os.path.exists(path):
            continue

        if n_requests >= 15:
            print("sleeping")
            time.sleep(3600)
            n_requests = 0

        data = {
            "mode": "drive",
            "traffic": "approximated",
            "units": "metric",
            "sources": [],
            "targets" : []
        }

        for lon, lat in origin_batch_coordinates:
            data["sources"].append(
                {"location": [lon, lat]}
            )

        for lon, lat in destination_batch_coordinates:
            data["targets"].append(
                {"location": [lon, lat]}
            )

        payload = json.dumps(data)

        resp = requests.request(
            method="POST", 
            url=url, 
            headers=headers, 
            data=payload)
        
        info = resp.json()
        info["sources"] = origin_batch_locations
        info["targets"] = destination_batch_locations
    
        with open(path, "w") as f:
            json.dump(info, f, indent=4)

        n_requests += 1

        
