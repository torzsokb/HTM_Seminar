import numpy as np
import pandas as pd
from models.route import Route
import math

def get_shift_data(
        instance_data: pd.DataFrame, 
        distances: dict, 
        night: bool, 
        add_end_depot: bool, 
        penalty: float=1.0, 
        service_time_model: str="Service_time",
        convert_service_time_to_seconds: bool=True):
    
    if night:
        df = instance_data[instance_data["Night_shift"] != 0]
    else:
        df = instance_data[instance_data["Night_shift"] != 1]

    stops = list(df["ID_MAXIMO"])
    if convert_service_time_to_seconds:
        df[service_time_model] = df[service_time_model].mul(60)
    route_ids = list(df["Route"].unique())
    service_times = list(df[service_time_model])

    
    routes = []
    for i in range(1, len(route_ids)):
        df_r = df[df["Route"] == route_ids[i]]
        route_stops  = list(df_r["ID_MAXIMO"])
        route = Route(
            stops=route_stops, 
            travel_time=total_travel_time(distances=distances, locations=route_stops),
            cleaning_time=round(df_r[service_time_model].sum()))
        routes.append(route)

    if add_end_depot:
        stops.append("Depot")
        service_times.append(0)

    n = len(stops)
    dist_matrix = np.zeros(shape=(n,n), dtype=np.int16)

    for i in range(n):
        for j in range(n):
            origin = stops[i]
            destination = stops[j]
            dist_matrix[i,j] = round(distances[origin][destination]["time"] * penalty)

    return stops, service_times, dist_matrix, routes
        

    
def total_travel_time(distances: dict, locations: list, round_result: bool=True) -> int:
    total_time = distances["Depot"][locations[0]]["time"]
    for i in range(1, len(locations)):
        total_time += distances[locations[i-1]][locations[i]]["time"]
    total_time += distances[locations[-1]]["Depot"]["time"]
    if round_result:
        return round(total_time)
    else:
        return total_time

    