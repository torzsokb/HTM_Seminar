import numpy as np
import pandas as pd
import gurobipy as gp
from gurobipy import GRB
import json
from models.rmp import RestrictedMasterProblem
from models.sp import SubProblem, GurobiSP
from models.route import Route
from models.utils import get_shift_data

class CVRPSolver:
    def __init__(
            self, 
            stops: list[str],
            cleaning_times: list[int],
            distances: np.ndarray,
            k: int,
            routes: list[Route],
            max_duration: int,
            min_duration: int=None,
            max_n_stops: int=None,
            min_n_stops: int=None):
        
        self.stops = stops
        self.cleaning_times = cleaning_times
        self.distances = distances
        self.k = k
        self.n = len(stops)
        self.routes = routes

        self.rmp = RestrictedMasterProblem(
            distances=distances,
            cleaning_times=cleaning_times,
            stops=stops,
            initital_routes=self.routes,
            k=k)
        
        self.sp = GurobiSP(
            distances=distances,
            stops=stops,
            cleaning_times=cleaning_times,
            k=k,
            max_duration=7 * 60 * 60,
            min_duration=min_duration,
            max_n_stops=max_n_stops,
            min_n_stops=min_n_stops)
        
        self.max_duration = max_duration
        self.min_duration = min_duration
        self.max_n_stops = max_n_stops
        self.min_n_stops = min_n_stops

    def setup(self) -> None:
        self.rmp.setup()
        self.sp.setup()

    def solve(self) -> None:
        for i in range(6):
            print(f"iteration: {i}")
            print(f"RMP RMP RMP RMP")
            self.rmp.solve()
            self.sp.update_duals(self.rmp.get_duals())
            print("SPSPSPSPSPSPSPSPSPSPS")
            self.rmp.add_column(self.sp.get_new_route())


def main():

    instance_data = pd.read_csv("data/inputs/cleaned/HTM_CollapsedDatav2.csv")
    with open("data/inputs/cleaned/distance_info_cleanedv2.json", "r") as f:
        distances = json.load(f)

    service_time_model = "Service_time"
    k = 25
    max_duration = 7 * 60 * 60
    # min_duration = 0
    # max_n_stops = 30
    # min_n_stops = 0

    day_stops, day_service_times, day_dist_matrix, day_routes = get_shift_data(
        distances=distances, 
        instance_data=instance_data, 
        night=False, 
        service_time_model=service_time_model,
        add_end_depot=False
        )
    
    # night_stops, night_service_times, night_dist_matrix, night_routes = get_shift_data(
    #     distances=distances, 
    #     instance_data=instance_data, 
    #     night=True, 
    #     service_time_model=service_time_model,
    #     add_end_depot=True
    # )

    day_solver = CVRPSolver(
        stops=day_stops, 
        cleaning_times=day_service_times, 
        distances=day_dist_matrix, 
        k=k,
        routes=day_routes,
        max_duration=max_duration,
        # min_duration=min_duration,
        # max_n_stops=max_n_stops,
        # min_n_stops=min_n_stops
        )
    
    # for route in day_routes:
    #     print(route)
    day_solver.setup()
    day_solver.solve()





if __name__ == "__main__":
    main()