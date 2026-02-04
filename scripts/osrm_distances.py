import time
import math
import requests
import numpy as np
import pandas as pd

base_url = "http://localhost:5001"

# df = pd.read_excel(
#     "data/inputs/raw/Data_POH_5WK_REINIGEN_ABRI_EN_HEKWERK.xlsx", 
#     sheet_name=["Dagroutes", "Nachtroutes", "Halteinfo"])

# df_stops = df["Halteinfo"]
# coordinates = list(zip(df_stops.latitude, df_stops.longitude))
# coordinates = [(52.113567, 4.283832), (52.110579, 4.290129), (52.110126, 4.296792), (52.110579, 4.290129)]

df = pd.read_csv("data/inputs/cleaned/HTM_CollapsedData.csv")
coordinates = list(zip(df.longitude, df.latitude))
coords_string = ";".join([f"{lon},{lat}" for lon, lat in coordinates])
params = {"annotations": "duration,distance"}

url = f"{base_url}/table/v1/driving/{coords_string}"

r = requests.get(url, params=params, timeout=60)
r.raise_for_status()
data = r.json()

distances =  np.array(data["distances"], dtype=np.float64)
durations =  np.array(data["durations"], dtype=np.float64)
mask = durations > 0
speed = distances[mask] / durations[mask]
print(np.mean(speed))



np.savetxt("data/inputs/cleaned/distances_collapsed.txt", distances)
np.savetxt("data/inputs/cleaned/travel_times_collapsed.txt", durations)