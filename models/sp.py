import numpy as np
import numba as nb
from numba import njit
import networkx as nx
import gurobipy as gp
from gurobipy import GRB
from models.route import Route

BIG_M = 9999999

class SubProblem:

    def __init__(
            self,
            stops: list[str],
            distances: np.ndarray,
            cleaning_times: list[int],
            k: int,
            max_duration: int,
            min_duration: int,
            max_n_stops: int,
            min_n_stops: int):
        
        self.stops = stops
        self.distances = distances
        self.cleaning_times = cleaning_times
        self.k = k

        self.max_duration = max_duration
        self.max_duration = min_duration
        self.max_n_stops = max_n_stops
        self.min_n_stops = min_n_stops

        self.duals = np.zeros(len(stops), dtype=np.float64)



    def update_duals(self, new_duals: np.ndarray) -> None:
        raise NotImplementedError

    def get_new_route(self) -> Route:
        raise NotImplementedError
    
    def setup(self) -> None:
        raise NotImplementedError
    

    
class GurobiSP(SubProblem):
    
    def __init__(self, stops, distances, cleaning_times, k, max_duration, min_duration, max_n_stops, min_n_stops):
        
        super().__init__(stops, distances, cleaning_times, k, max_duration, min_duration, max_n_stops, min_n_stops)
        
        self.model = gp.Model()
        self.x = {}
        self.t = {}
        self.constraints = []

    
    def setup(self) -> None:
        self.model.ModelSense = GRB.MINIMIZE
        # self.model.params.OutputFlag = 0
        self.model.params.MIPFocus = 1
        self.model.params.TimeLimit = 60
        
        self.add_arc_vars()
        self.add_time_vars()
        self.add_flow_constraints()
        self.add_time_constraints()

        
        


    def add_arc_vars(self) -> None:
        for i, stop_from in enumerate(self.stops):
            for j, stop_to in enumerate(self.stops):
                if i == j:
                    continue
                self.x[i,j] = self.model.addVar(
                    vtype=GRB.BINARY, 
                    obj=self.distances[i,j], 
                    name=f"x[{i},{j}]")
                
    def add_time_vars(self) -> None:

        for i, stop in enumerate(self.stops):
            
            if stop == "Depot":
                self.t[i] = self.model.addVar(
                    vtype=GRB.CONTINUOUS,
                    lb=0, 
                    ub=0,
                    name=f"t[{i}]")
            else:
                self.t[i] = self.model.addVar(
                    vtype=GRB.CONTINUOUS,
                    lb=0, 
                    ub=(7 * 60 * 60 - self.distances[i, 0] - self.cleaning_times[i]),
                    name=f"t[{i}]")

    def add_flow_constraints(self) -> None:
        
        leave_depot_lhs = gp.LinExpr()
        arrive_depot_lhs = gp.LinExpr()

        for i, stop in enumerate(self.stops):
            if stop == "Depot":
                continue

            leave_depot_lhs += self.x[0,i]
            arrive_depot_lhs += self.x[i,0]

            stop_inflow = gp.LinExpr()
            stop_outflow = gp.LinExpr()

            for j, stop_other in enumerate(self.stops):
                if i == j:
                    continue
                stop_inflow += self.x[j,i]
                stop_outflow += self.x[i,j]

            self.constraints.append(self.model.addConstr(stop_inflow <= 1, name=f"inflow_limit_{stop}"))
            self.constraints.append(self.model.addConstr(stop_outflow <= 1, name=f"outflow_limit_{stop}"))
            self.constraints.append(self.model.addConstr(stop_inflow - stop_outflow == 0, name=f"flow_conserv_{stop}"))

        self.constraints.append(self.model.addConstr(leave_depot_lhs == 1, name="leave_depot"))
        self.constraints.append(self.model.addConstr(arrive_depot_lhs == 1, name="arrive_depot"))

        self.model.update()

    def add_time_constraints(self) -> None:
        for i, stop in enumerate(self.stops):
            for j, subsequent_stop in enumerate(self.stops):
                if j == 0 or j == i:
                    continue
                expr = gp.LinExpr()
                expr += self.t[j] - self.t[i]
                expr += 7 * 60 * 60 * (1 - self.x[i,j])
                self.constraints.append(self.model.addConstr(
                    expr >= self.distances[i,j] + self.cleaning_times[i],
                    name=f"precedence_{i}_{j}"
                ))
        self.model.update()
        
    def update_duals(self, new_duals: np.ndarray) -> None:
        self.duals = new_duals
        for i, stop in enumerate(self.stops):
            for j, stop_ohter in enumerate(self.stops):
                if i == j:
                    pass
                elif i == 0:
                    self.x[i,j].obj = self.distances[i,j] + new_duals[i]
                else:
                    self.x[i,j].obj = self.distances[i,j] - new_duals[i]
        self.model.update()

    def get_new_route(self) -> Route:
        self.model.optimize()

        travel_time = 0
        route = ["Depot"]
        cleaning_time = 0
        current = 0

        for j in range(len(self.stops)):
            # print(f"j: {j}, current: {current} aka: {self.stops[current]}")

            for i, next_stop in enumerate(self.stops):
                if i == current:
                    continue

                if self.x[current, i].X >= 0.5:
                    # print(f"we selected {i} aka {self.stops[i]}")

                    route.append(next_stop)
                    cleaning_time += self.cleaning_times[i]
                    travel_time += self.distances[current, i]
                    current = i
                    break

            if self.stops[current] == "Depot":
                break

        result = Route(stops=route, travel_time=travel_time, cleaning_time=cleaning_time)
        print(f"sp solution {result}")          
        return result


class HeuristicSP(SubProblem):
    def __init__(self, stops, distances, cleaning_times, k, max_duration, min_duration, max_n_stops, min_n_stops):
        super().__init__(stops, distances, cleaning_times, k, max_duration, min_duration, max_n_stops, min_n_stops)

    def update_duals(self, new_duals) -> None:
        self.duals = new_duals

    def get_new_route(self):
        pass

@njit
def rcespp(distances: np.ndarray, cleaning_times: np.ndarray, duals: np.ndarray, max_duration: int, min_duration: int):
    route = [0]
    time = 0
    cost = duals[0]
    
    return 1



    
