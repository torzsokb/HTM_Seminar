from models.tsp_solver import solve_tsp_mtz
import json
import pandas as pd
from tabulate import tabulate

def main():

    # df_old_routes = pd.read_csv("data/outputs/reordered_route_times.csv")
    # df_old_routes.set_index("route_ID", inplace=True, drop=True, append=False)
    # df_old_routes.drop(columns="Unnamed: 0", inplace=True)
    # df_old_routes.rename(
    #     columns={"cleaning_time": "clean_t", "original_time": "old_t", "reordered_time": "new_t", "original_feas": "old_feas", "reordered_feas": "new_feas", "improvement": "impr", "total_time_original": "old_total_t", "total_time_reordered": "new_total_t"},
    #     inplace=True)
    # print(tabulate(df_old_routes.round(2), headers="keys"))
    # print(tabulate(df_old_routes.describe().round(2), headers="keys"))

    # df_clustered_routes = pd.read_csv("data/outputs/clustered_tsp_times.csv")
    # df_clustered_routes.set_index("cluster", inplace=True, drop=True, append=False)
    # df_clustered_routes.drop(columns="Unnamed: 0", inplace=True)
    # print(tabulate(df_clustered_routes.round(2), headers="keys"))
    # print(tabulate(df_clustered_routes.describe().round(2), headers="keys"))

    

    pass

    # reorder_routes()
    # reorder_comparison()
    # clustered_route_info()

    
    

def clustered_route_info():
    with open("data/inputs/cleaned/distance_info_cleaned.json", "r") as f:
        distances = json.load(f)


    df = pd.read_csv("data/outputs/HTM_ClusteredData_reordered.csv")
    clusters = list(df["cluster"].unique())
    clusters = clusters[1:]

    route_travel_times = {"cluster": [], "cleaning_time": [], "driving_time": [], "total_time": [], "feas": []}

    for cluster in clusters:

        df_c = df[df["cluster"] == cluster]
        df_c_sorted = df_c.sort_values(by="cluster_order")
        locations = list(df_c_sorted["ID_MAXIMO"])

        service_time = df_c["Service_time"].sum()
        driving_time = total_travel_time(distances, locations) / 60
        total_time = service_time + driving_time

        print(f"Cluster: {cluster}")
        print(f"Locations: {locations}")
        print(f"Cleaning time: {service_time}")
        print(f"Driving time: {driving_time:.2f}")

        route_travel_times["cluster"].append(cluster)
        route_travel_times["cleaning_time"].append(service_time)
        route_travel_times["driving_time"].append(driving_time)
        route_travel_times["total_time"].append(total_time)
        if total_time <= 7 * 60:
            route_travel_times["feas"].append(1)
        else:
            route_travel_times["feas"].append(0)


    out = pd.DataFrame.from_dict(route_travel_times)
    out.to_csv("data/outputs/clustered_tsp_times.csv")



def reorder_comparison():
    with open("data/inputs/cleaned/distance_info_cleaned.json", "r") as f:
        distances = json.load(f)

    df = pd.read_csv("data/outputs/HTM_CollapsedData_reordered.csv")
    routes = list(df["Route"].unique())
    routes = routes[1:]

    route_travel_times = {"route_ID": [], "cleaning_time": [], "original_time": [],"original_feas": [], "reordered_time": [], "reordered_feas": [], "improvement": [], "total_time_original": [], "total_time_reordered": []}

    for route in routes:

        df_r = df[df["Route"] == route]
        locations = list(df_r["ID_MAXIMO"])
        df_r_sorted = df_r.sort_values(by="TSP_Order")
        locations_sorted = list(df_r_sorted["ID_MAXIMO"])

        service_time = df_r["Service_time"].sum()
        old_travel_time = total_travel_time(distances, locations) / 60
        new_travel_time = total_travel_time(distances, locations_sorted) / 60
        total_time_original = old_travel_time + service_time
        total_time_reordered = new_travel_time + service_time

        print(f"Route: {route}")
        print(f"Locations: {locations}")
        print(f"Service time: {service_time}")
        print(f"Old driving time: {old_travel_time:.2f}")
        print(f"New driving time: {new_travel_time:.2f}")

        route_travel_times["route_ID"].append(route)
        route_travel_times["original_time"].append(old_travel_time)
        route_travel_times["reordered_time"].append(new_travel_time)
        route_travel_times["cleaning_time"].append(service_time)
        route_travel_times["improvement"].append(old_travel_time - new_travel_time)
        route_travel_times["total_time_original"].append(total_time_original)
        route_travel_times["total_time_reordered"].append(total_time_reordered)

        
        if total_time_original <= 7 * 60:
            route_travel_times["original_feas"].append(1)
        else:
            route_travel_times["original_feas"].append(0)
        
        if total_time_reordered <= 7 * 60:
            route_travel_times["reordered_feas"].append(1)
        else:
            route_travel_times["reordered_feas"].append(0)


    out = pd.DataFrame.from_dict(route_travel_times)
    out.to_csv("data/outputs/reordered_route_times.csv")


def order_clusters():
    with open("data/inputs/cleaned/distance_info_cleaned.json", "r") as f:
        distances = json.load(f)


    df = pd.read_csv("data/inputs/cleaned/HTM_ClusteredData.csv")
    clusters = list(df["cluster"].unique())
    clusters = clusters[1:]
    new_order = {"Depot": 0}


    for cluster in clusters:
        locations = list(df[df["cluster"] == cluster]["ID_MAXIMO"])
        order = solve_tsp_mtz(locations, distances, warmstart=True, heur=True, time_limit=300)
        print(order)
        print(len(order))
        for i in range(1, len(locations)):
            new_order[locations[i]] = order[i]


    df["cluster_order"] = df["ID_MAXIMO"].map(new_order)
    df.to_csv("data/outputs/HTM_ClusteredData_reordered.csv")


def reorder_routes():
    with open("data/inputs/cleaned/distance_info_cleaned.json", "r") as f:
        distances = json.load(f)


    df = pd.read_csv("data/inputs/cleaned/HTM_CollapsedData.csv")
    routes = list(df["Route"].unique())
    routes = routes[1:]
    new_order = {"Depot": 0}


    for route in routes:
        locations = list(df[df["Route"] == route]["ID_MAXIMO"])
        order = solve_tsp_mtz(locations, distances, warmstart=True, heur=False, time_limit=300)
        for i in range(1, len(locations)):
            new_order[locations[i]] = order[i]


    df["TSP_Order"] = df["ID_MAXIMO"].map(new_order)
    df.to_csv("data/outputs/HTM_CollapsedData_reordered.csv")

    

def total_travel_time(distances: dict, locations: list) -> float:
    total_time = distances["Depot"][locations[0]]["time"]
    for i in range(1, len(locations)):
        total_time += distances[locations[i-1]][locations[i]]["time"]
    total_time += distances[locations[-1]]["Depot"]["time"]
    return total_time

    


if __name__ == "__main__":
    main()