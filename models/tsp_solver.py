import gurobipy as gp
from gurobipy import GRB
from itertools import permutations, product
import numpy as np
import pandas as pd
import logging
import json

def nearest_neighbor_heuristic(arc_lengths, locations, starting_node):

    #assert starting_node in locations, "invalid starting node"

    tour = [starting_node]
    tour_tuples = []
    tour_length = 0

    while len(tour) < len(locations): 
        
        i = tour[-1]
        
        min_distance = min([arc_lengths[i, j] for j in locations if j not in tour and j != i])
        nearest = [j for j in locations if j not in tour and arc_lengths[i,j] == min_distance]
        j = nearest[0]

        tour.append(j)
        tour_tuples.append((i,j))
        tour_length += min_distance

    tour_tuples.append((tour[-1], tour[0]))
    tour_length += arc_lengths[tour_tuples[-1]]

    return tour_length, tour, tour_tuples

def solve_tsp_mtz(stops: list, distances: dict, warmstart: bool, heur: bool, time_limit: int, day_shift: bool=True):
    
    stops.insert(0, "Depot")
    n = len(stops)
    locations = list(range(n))
    speed_setting = "time_day" if day_shift else "time_night"
    arc_costs = {(i, j): distances[stops[i]][stops[j]][speed_setting] for i,j in permutations(locations, 2)}

    
    with gp.Env() as env, gp.Model(env=env) as m:

        
        m.Params.TimeLimit = time_limit
        m.Params.LogToConsole = 0

        # Create decision variables
        first = locations[0]
        z = m.addVars(arc_costs.keys(), obj=arc_costs, vtype=GRB.BINARY, name="z")
        u = m.addVars(locations[1:], vtype=GRB.INTEGER, lb=1, ub=n-1, name="u")

        # Add constraint to make sure each node is visited exactly once
        m.addConstrs(z.sum(i, '*') == 1 for i in locations)
        m.addConstrs(z.sum('*', i) == 1 for i in locations)

        # Add route order constraints
        m.addConstrs(u[i] - u[j] + (n - 1) * z[i,j] + (n - 3) * z[j,i] <= n - 2 for i,j in z.keys() if i > 0 and j > 0)
        m.addConstrs(u[i] >= 1 + (n - 3) * z[i,first] + z.sum('*', i) - z[first,i] for i in u.keys())
        m.addConstrs(u[i] <= n - 1 - (n - 3) * z[first,i] + z.sum(i, '*') - z[i,first] for i in u.keys())

        # Implementing warm start of original route
        if warmstart:
            if heur:
                obj, tour, tour_tuples = nearest_neighbor_heuristic(arc_lengths=arc_costs, locations=locations, starting_node=0)
                while tour.index(0) != 0:
                    tour.insert(0, tour.pop())

                for i,j in tour_tuples:
                    z[i,j].Start = 1
                for i in range(1, len(tour)):
                    u[tour[i]].Start = i
            else:
                for i in range(1, n):
                    z[i-1, i].Start = 1
                    u[i].Start = i
                z[n-1, first].Start = 1
            m.update()

        m.optimize()

        vals = m.getAttr('x', z)
        selected = gp.tuplelist((i, j) for i, j in vals.keys() if vals[i, j] > 0.5)

        order_vals = m.getAttr('x', u)
        placements = [order_vals[i] for i in order_vals.keys()]
        placements.insert(0, 0)

        placement_arr = np.asarray(placements, dtype=int)
        # print(placement_arr)
        order = np.argsort(placement_arr)
        # print(order)
        stops_arr = np.asarray(stops, dtype=str)

        print(f"mtz time: {m.ObjVal / 60 :.2f}")
        s = "old\tnew\n"
        for i in range(n):
            s += f"{stops_arr[i]}\t{stops_arr[order][i]}\n"
        # print(s)
        print(placement_arr)
        return placement_arr


def find_cycles(arcs):

    succesors = {}
    cycles = []

    for i, j in arcs:
        succesors[i] = j

    unvisited = set(succesors)

    while unvisited:
        
        cycle = []
        current = list(unvisited)[0]
        
        while current in unvisited:
            unvisited.remove(current)
            cycle.append(current)
            current = succesors[current]

        cycles.append(cycle)

    return cycles
    

class TSPCallback:

    def __init__(self, nodes, x):
        self.nodes = nodes
        self.x = x


    def __call__(self, model, where):

        if where == GRB.Callback.MIPSOL:
            try:
                self.eliminate_cycles(model)
            except:
                logging.exception("Exception occurred in MIPSOL callback")
                model.terminate()
        

    def eliminate_cycles(self, model):

        values = model.cbGetSolution(self.x)
        arcs = [(i, j) for (i, j), v in values.items() if v >= 0.5]
        cycles = find_cycles(arcs)

        for cycle in cycles:

            if len(cycle) < 2:
                continue

            if len(cycle) > len(self.nodes) / 2:
                continue

            outside = [i for i in self.nodes if not i in cycle]

            flow_from_cycle = gp.LinExpr()
            flow_to_cycle = gp.LinExpr()

            for i, j in product(cycle, outside):
                flow_from_cycle += self.x[i,j]
                flow_to_cycle += self.x[j,i]

            model.cbLazy(flow_from_cycle >= 1)
            model.cbLazy(flow_to_cycle >= 1)


        
def solve_tsp_lazy_constr(stops: list, distances: dict, route_name: str=None, day_shift: bool=None):

    if not route_name is None:
        print(f"solving TSP for route {route_name}")

    if day_shift is None:
        day_shift = route_name[4] == "N"

    speed_setting = "time_day" if day_shift else "time_night"

    old_time = total_travel_time(distances, stops, day_shift)
    



    stops.insert(0, "Depot")
    n = len(stops)
    nodes = list(range(n))
    arc_costs = {(i, j): distances[stops[i]][stops[j]][speed_setting] for i,j in permutations(nodes, 2)}
    
    with gp.Env() as env, gp.Model(env=env) as m:

        m.params.LogToConsole = 0
        m.ModelSense = GRB.MINIMIZE
        m.params.LazyConstraints = 1

        x = m.addVars(arc_costs.keys(), obj=arc_costs, vtype=GRB.BINARY, name="x")
    
        for node in nodes:

            outflow = gp.LinExpr()
            inflow = gp.LinExpr()

            for neighbour in nodes:
                if node == neighbour:
                    continue

                outflow += x[node, neighbour]
                inflow += x[neighbour, node]

            m.addConstr(outflow == 1, name=f"outflow_{node}")
            m.addConstr(inflow == 1, name=f"inflow_{node}")

        cb = TSPCallback(nodes, x)
        m.optimize(cb)

        print(f"old time: {old_time:.2f} new time: {m.ObjVal / 60:.2f} improvement: {(old_time - m.ObjVal / 60):.2f}")
        values = m.getAttr("x", x)
        selected_arcs = [(i, j) for (i, j), v in values.items() if v >= 0.5]
        tour = find_cycles(selected_arcs)[0]
        
        while tour.index(0) != 0:
            tour.insert(0, tour.pop())

        placement_arr = np.zeros(n, dtype=int)
        for i in range(n):
            placement_arr[tour[i]] = i
        # print(placement_arr)
        return placement_arr



        

def total_travel_time(distances: dict, stops: list, day_shift: bool) -> float:

    metric = "time_day" if day_shift else "time_night"

    total_time = distances["Depot"][stops[0]][metric]
    total_time += distances[stops[-1]]["Depot"][metric]
    
    for i in range(1, len(stops)):
        total_time += distances[stops[i-1]][stops[i]][metric]

    return total_time / 60


def main():
    stops_path = "data/inputs/HTM_Data_abriTypeStop.csv"
    dist_path = "data/inputs/cleaned/distance_info_cleanedv2.json"

    with open(dist_path, "r") as f:
        distances = json.load(f)
    
    df = pd.read_csv(stops_path)
    shifts = df["Route"].dropna().astype(str).unique().tolist()

    shifts = shifts[:5]
    for shift in shifts:

        print(shift)

        df_r = df[df["Route"] == shift]
        day_shift = shift[4] == "D"

        solve_tsp_lazy_constr(
            stops=list(df_r["ID_MAXIMO"]),
            distances=distances,
            route_name=shift,
            day_shift=day_shift
        )

        solve_tsp_mtz(
            stops=list(df_r["ID_MAXIMO"]),
            distances=distances,
            warmstart=True,
            heur=False,
            time_limit=15,
            day_shift=day_shift
        )

if __name__ == "__main__":
    main()