import gurobipy as gp
from gurobipy import GRB
from itertools import permutations
import numpy as np

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

def solve_tsp_mtz(stops: list, distances: dict, warmstart: bool, heur: bool, time_limit: int):
    
    stops.insert(0, "Depot")
    n = len(stops)
    locations = list(range(n))
    arc_costs = {(i, j): distances[stops[i]][stops[j]]["time"] for i,j in permutations(locations, 2)}

    
    with gp.Env() as env, gp.Model(env=env) as m:

        
        m.Params.TimeLimit = time_limit
        m.Params.LogToConsole = 1

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
        print(placement_arr)
        order = np.argsort(placement_arr)
        print(order)
        stops_arr = np.asarray(stops, dtype=str)


        s = "old\tnew\n"
        for i in range(n):
            s += f"{stops_arr[i]}\t{stops_arr[order][i]}\n"
        print(s)
        return placement_arr

        

        

