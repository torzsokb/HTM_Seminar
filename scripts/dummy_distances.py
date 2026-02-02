import numpy as np
import geopy.distance
import pandas as pd


df = pd.read_excel("data/inputs/raw/Data_POH_5WK_REINIGEN_ABRI_EN_HEKWERK.xlsx", sheet_name=["Dagroutes", "Nachtroutes", "Halteinfo"])
df_stops = df["Halteinfo"]
coordinates = list(zip(df_stops.latitude, df_stops.longitude))
n = len(df_stops)
dummy_distances = np.zeros((n,n), dtype=np.float64)


for i in range(n):
    for j in range(i + 1, n):
        dist = geopy.distance.geodesic(coordinates[i], coordinates[j]).km * 1000
        dummy_distances[i,j] = dist
        dummy_distances[j,i] = dist
        

np.savetxt("data/inputs/dummy_data/euclidean_dist.txt", dummy_distances)
