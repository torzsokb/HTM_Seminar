from models.tsp_solver import solve_tsp_mtz
import json
import pandas as pd

def main():

    with open("data/inputs/cleaned/distance_info_cleaned.json", "r") as f:
        distances = json.load(f)

    

    df = pd.read_csv("data/inputs/cleaned/HTM_ClusteredData.csv")
    
    routes = list(df["cluster"].unique())
    routes = routes[1:]
    print(routes)
    new_order = {"Depot": 0}

    for route in routes:
        locations = list(df[df["cluster"] == route]["ID_MAXIMO"])
        order = solve_tsp_mtz(stops=locations, distances=distances, warmstart=True, time_limit=300, heur=True)
        for i, loc in enumerate(locations):
            new_order[loc] = order[i-1]

    df["Cluster_Order"] = df["ID_MAXIMO"].map(new_order)

    df.to_csv("data/inputs/cleaned/HTM_ClusteredData_reordered.csv")


if __name__ == "__main__":
    main()