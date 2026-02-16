import pandas as pd
import numpy as np

def greedy_accept_fn(improvement, current_obj, new_obj):
    return improvement > 0

# General function to calculate the objective
def objective_total_duration(
    stops,
    travel_time_matrix,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    depot_id=0,
):
    travel_times = travel_time_matrix / 60  # keep same units as your neighbourhoods
    total = 0.0

    for r_name, r in stops.groupby(route_col):
        r = r.sort_values(order_col)
        ids = r[id_col].tolist()
        full = [depot_id] + ids + [depot_id]

        travel = sum(travel_times[full[k], full[k+1]] for k in range(len(full) - 1))
        service = r[service_time_col].sum()

        total += travel + service

    return float(total)

def VND(stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7 * 60,
    # Max iterations and improvement choice can differ per neighbourhood
    # Which iteration / improvement choice is for which neighbourhood is determined by the order of neighbourhoods (N_order)
    max_iterations_per_N = [1,1,1,1,1,1],
    improvement_choice_per_N = ["Best","Best","Best","Best","Best","Best"], # "Best" or "First"
    max_iterations = None,
    accept_fn = None):
    
    N_order = [intra_route_swap, intra_route_shift, intra_route_2opt,
           inter_route_swap, inter_shift, inter_route_2opt_star]

    EPS = 1e-6
    iteration = 0

    while max_iterations is None or iteration < max_iterations:
        improved_any = False
        current_obj = objective_total_duration(stops, travel_time_matrix)

        for i, N in enumerate(N_order):
            candidate = N(
                stops, travel_time_matrix,
                max_iterations=max_iterations_per_N[i],
                improvement_choice=improvement_choice_per_N[i],
                accept_fn=accept_fn)
            new_obj = objective_total_duration(candidate, travel_time_matrix)

            if new_obj < current_obj - EPS:
                stops = candidate
                improved_any = True
                break  # restart from first neighbourhood

        iteration += 1
        if not improved_any:
            break

    return stops


def inter_shift(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7 * 60,
    max_iterations = None,
    improvement_choice = "Best", # "Best" or "First"
    accept_fn = None): # Acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        route_travel = {}
        route_service = {}
        route_duration = {}

        for r_name, r in routes.items():
            ids = r[id_col].tolist()
            full = [depot_id] + ids + [depot_id]

            travel = sum(
                travel_times[full[k], full[k+1]]
                for k in range(len(full)-1)
            )

            service = r[service_time_col].sum()

            route_travel[r_name] = travel
            route_service[r_name] = service
            route_duration[r_name] = travel + service
        
        stop_search = False

        for r1_name, r1 in routes.items():
            if stop_search:
                break
            for r2_name, r2 in routes.items():
                if stop_search: 
                    break

                if r1_name == r2_name:
                    continue

                if r1[night_col].iloc[0] != r2[night_col].iloc[0]:
                    continue

                r1_ids = r1[id_col].tolist()
                r2_ids = r2[id_col].tolist()

                for i in range(len(r1_ids)):
                    node = r1_ids[i]
                    node_service = r1.iloc[i][service_time_col]

                    prev1 = depot_id if i == 0 else r1_ids[i-1]
                    next1 = depot_id if i == len(r1_ids)-1 else r1_ids[i+1]

                    delta_remove = (
                        - travel_times[prev1, node]
                        - travel_times[node, next1]
                        + travel_times[prev1, next1]
                    )

                    for j in range(len(r2_ids)+1):

                        prev2 = depot_id if j == 0 else r2_ids[j-1]
                        next2 = depot_id if j == len(r2_ids) else r2_ids[j]

                        delta_insert = (
                            - travel_times[prev2, next2]
                            + travel_times[prev2, node]
                            + travel_times[node, next2]
                        )

                        new_dur_r1 = (
                            route_duration[r1_name]
                            + delta_remove
                            - node_service
                        )

                        new_dur_r2 = (
                            route_duration[r2_name]
                            + delta_insert
                            + node_service
                        )

                        if new_dur_r1 > max_shift_duration:
                            continue
                        if new_dur_r2 > max_shift_duration:
                            continue

                        current_obj = (
                            route_duration[r1_name]
                            + route_duration[r2_name]
                        )

                        new_obj = new_dur_r1 + new_dur_r2

                        improvement = current_obj - new_obj
                        
                        if not accept_fn(improvement, current_obj, new_obj): 
                            continue

                        # First improvement
                        if improvement_choice == "First": 
                            best_delta = improvement 
                            best_move = (r1_name, r2_name, i, j) 
                            stop_search = True 
                            break 
                        
                        # Best-improvement 
                        if improvement > best_delta: 
                            best_delta = improvement 
                            best_move = (r1_name, r2_name, i, j)

        if best_move is not None:
            print("Inter-shift improvement:", best_delta)

            r1_name, r2_name, i, j = best_move

            r1 = routes[r1_name].copy()
            r2 = routes[r2_name].copy()

            node_row = r1.iloc[i].copy()

            r1 = r1.drop(node_row.name)

            node_row[route_col] = r2_name
            r2 = pd.concat(
                [r2.iloc[:j], node_row.to_frame().T, r2.iloc[j:]]
            )

            r1 = r1.reset_index(drop=True)
            r2 = r2.reset_index(drop=True)

            r1[order_col] = range(1, len(r1)+1)
            r2[order_col] = range(1, len(r2)+1)

            stops = stops[~stops[route_col].isin([r1_name, r2_name])]
            stops = pd.concat([stops, r1, r2], ignore_index=True)

            improved = True

    return stops

def inter_route_swap(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7 * 60,
    max_iterations = None,
    improvement_choice="Best",      # "Best" or "First"
    accept_fn=None):                  # acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        route_duration = {}
        for r_name, r in routes.items():
            ids = r[id_col].tolist()
            full = [depot_id] + ids + [depot_id]

            travel = sum(
                travel_times[full[k], full[k+1]]
                for k in range(len(full)-1)
            )
            service = r[service_time_col].sum()
            route_duration[r_name] = travel + service

        route_names = list(routes.keys())

        stop_search = False

        for idx1 in range(len(route_names)):
            if stop_search:
                break

            for idx2 in range(idx1 + 1, len(route_names)):
                if stop_search:
                    break

                r1_name = route_names[idx1]
                r2_name = route_names[idx2]

                r1 = routes[r1_name]
                r2 = routes[r2_name]

                # keep day/night separated
                if r1[night_col].iloc[0] != r2[night_col].iloc[0]:
                    continue

                r1_ids = r1[id_col].tolist()
                r2_ids = r2[id_col].tolist()

                for i in range(len(r1_ids)):
                    if stop_search:
                        break

                    for j in range(len(r2_ids)):

                        node1 = r1_ids[i]
                        node2 = r2_ids[j]

                        service1 = r1.iloc[i][service_time_col]
                        service2 = r2.iloc[j][service_time_col]

                        prev1 = depot_id if i == 0 else r1_ids[i-1]
                        next1 = depot_id if i == len(r1_ids)-1 else r1_ids[i+1]

                        prev2 = depot_id if j == 0 else r2_ids[j-1]
                        next2 = depot_id if j == len(r2_ids)-1 else r2_ids[j+1]

                        # Old arcs
                        old_r1 = (
                            travel_times[prev1, node1]
                            + travel_times[node1, next1]
                        )
                        old_r2 = (
                            travel_times[prev2, node2]
                            + travel_times[node2, next2]
                        )

                        # New arcs after swap
                        new_r1 = (
                            travel_times[prev1, node2]
                            + travel_times[node2, next1]
                        )
                        new_r2 = (
                            travel_times[prev2, node1]
                            + travel_times[node1, next2]
                        )

                        delta_r1 = old_r1 - new_r1
                        delta_r2 = old_r2 - new_r2

                        new_dur_r1 = (
                            route_duration[r1_name]
                            - service1 + service2
                            - delta_r1
                        )
                        new_dur_r2 = (
                            route_duration[r2_name]
                            - service2 + service1
                            - delta_r2
                        )

                        # Feasibility
                        if new_dur_r1 > max_shift_duration:
                            continue
                        if new_dur_r2 > max_shift_duration:
                            continue

                        current_obj = (
                            route_duration[r1_name]
                            + route_duration[r2_name]
                        )
                        new_obj = new_dur_r1 + new_dur_r2
                        improvement = current_obj - new_obj

                        # Acceptance rule
                        if not accept_fn(improvement, current_obj, new_obj):
                            continue

                        # FIRST IMPROVEMENT
                        if improvement_choice == "First":
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)
                            stop_search = True
                            break

                        # BEST IMPROVEMENT
                        if improvement > best_delta:
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)

        # APPLY BEST MOVE
        if best_move is not None:
            print("Inter-swap improvement:", best_delta)

            r1_name, r2_name, i, j = best_move

            r1 = routes[r1_name].copy()
            r2 = routes[r2_name].copy()

            row1 = r1.iloc[i].copy()
            row2 = r2.iloc[j].copy()

            row1[route_col] = r2_name
            row2[route_col] = r1_name

            r1.iloc[i] = row2
            r2.iloc[j] = row1

            r1 = r1.reset_index(drop=True)
            r2 = r2.reset_index(drop=True)

            r1[order_col] = range(1, len(r1) + 1)
            r2[order_col] = range(1, len(r2) + 1)

            stops = stops[~stops[route_col].isin([r1_name, r2_name])]
            stops = pd.concat([stops, r1, r2], ignore_index=True)

            improved = True

    return stops

def intra_route_swap(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7 * 60,
    max_iterations = None,
    improvement_choice="Best",      # "Best" or "First"
    accept_fn=None):                  # acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        route_duration = {}
        for r_name, r in routes.items():
            ids = r[id_col].tolist()
            full = [depot_id] + ids + [depot_id]

            travel = sum(
                travel_times[full[k], full[k+1]]
                for k in range(len(full)-1)
            )
            service = r[service_time_col].sum()
            route_duration[r_name] = travel + service

        stop_search = False

        for r_name, r in routes.items():
            if stop_search:
                break

            ids = r[id_col].tolist()
            n = len(ids)

            for i in range(n - 1):
                if stop_search:
                    break

                for j in range(i + 1, n):
                    node_i = ids[i]
                    node_j = ids[j]

                    prev_i = depot_id if i == 0 else ids[i-1]
                    next_i = depot_id if i == n-1 else ids[i+1]

                    prev_j = depot_id if j == 0 else ids[j-1]
                    next_j = depot_id if j == n-1 else ids[j+1]

                    if j == i + 1:
                        old = (
                            travel_times[prev_i, node_i]
                            + travel_times[node_i, node_j]
                            + travel_times[node_j, next_j]
                        )
                        new = (
                            travel_times[prev_i, node_j]
                            + travel_times[node_j, node_i]
                            + travel_times[node_i, next_j]
                        )
                    else:
                        old = (
                            travel_times[prev_i, node_i]
                            + travel_times[node_i, next_i]
                            + travel_times[prev_j, node_j]
                            + travel_times[node_j, next_j]
                        )
                        new = (
                            travel_times[prev_i, node_j]
                            + travel_times[node_j, next_i]
                            + travel_times[prev_j, node_i]
                            + travel_times[node_i, next_j]
                        )

                    delta = old - new
                    if delta <= EPS:
                        continue

                    current_obj = route_duration[r_name]
                    new_duration = current_obj - delta

                    if new_duration > max_shift_duration:
                        continue

                    improvement = current_obj - new_duration  

                    if not accept_fn(improvement, current_obj, new_duration):
                        continue

                    if improvement_choice == "First":
                        best_delta = improvement
                        best_move = (r_name, i, j)
                        stop_search = True
                        break

                    if improvement > best_delta:
                        best_delta = improvement
                        best_move = (r_name, i, j)

        if best_move is not None:
            print("Intra-swap improvement:", best_delta)

            r_name, i, j = best_move
            r = routes[r_name].copy()

            row_i = r.iloc[i].copy()
            row_j = r.iloc[j].copy()

            r.iloc[i] = row_j
            r.iloc[j] = row_i

            r = r.reset_index(drop=True)
            r[order_col] = range(1, len(r) + 1)

            stops = stops[stops[route_col] != r_name]
            stops = pd.concat([stops, r], ignore_index=True)

            improved = True

    return stops


def intra_route_shift(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7 * 60,
    max_iterations = None,
    improvement_choice="Best",      # "Best" or "First"
    accept_fn=None):                  # acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        stop_search = False

        for r_name, r in routes.items():
            if stop_search:
                break

            ids = r[id_col].tolist()
            n = len(ids)

            full = [depot_id] + ids + [depot_id]
            route_duration = (
                sum(travel_times[full[k], full[k+1]] for k in range(len(full)-1))
                + r[service_time_col].sum()
            )

            for i in range(n):
                if stop_search:
                    break

                node = ids[i]
                prev_i = depot_id if i == 0 else ids[i-1]
                next_i = depot_id if i == n-1 else ids[i+1]

                delta_remove = (
                    travel_times[prev_i, node]
                    + travel_times[node, next_i]
                    - travel_times[prev_i, next_i]
                )

                ids_removed = ids[:i] + ids[i+1:]

                for j in range(len(ids_removed) + 1):
                    if j == i:
                        continue

                    prev_j = depot_id if j == 0 else ids_removed[j-1]
                    next_j = depot_id if j == len(ids_removed) else ids_removed[j]

                    delta_insert = (
                        travel_times[prev_j, next_j]
                        - travel_times[prev_j, node]
                        - travel_times[node, next_j]
                    )

                    delta = delta_remove + delta_insert
                    if delta <= EPS:
                        continue

                    current_obj = route_duration
                    new_duration = route_duration - delta
                    if new_duration > max_shift_duration:
                        continue

                    improvement = current_obj - new_duration  

                    if not accept_fn(improvement, current_obj, new_duration):
                        continue

                    if improvement_choice == "First":
                        best_delta = improvement
                        best_move = (r_name, i, j)
                        stop_search = True
                        break

                    if improvement > best_delta:
                        best_delta = improvement
                        best_move = (r_name, i, j)

        if best_move is not None:
            print("Intra-route improvement:", best_delta)

            r_name, i, j = best_move
            r = routes[r_name].copy()

            row = r.iloc[i].copy()
            r = r.drop(r.index[i]).reset_index(drop=True)

            r_part1 = r.iloc[:j]
            r_part2 = r.iloc[j:]

            r = pd.concat([r_part1, row.to_frame().T, r_part2]).reset_index(drop=True)
            r[order_col] = range(1, len(r) + 1)

            stops = stops[stops[route_col] != r_name]
            stops = pd.concat([stops, r], ignore_index=True)

            improved = True

    return stops


def intra_route_2opt(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    depot_id=0,
    max_shift_duration=7 * 60,
    max_iterations = None,
    improvement_choice="Best",      # "Best" or "First"
    accept_fn=None):                  # acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        stop_search = False

        for r_name, r in routes.items():
            if stop_search:
                break

            ids = r[id_col].tolist()
            n = len(ids)
            if n < 3:
                continue

            full = [depot_id] + ids + [depot_id]
            travel_sum = sum(travel_times[full[k], full[k+1]] for k in range(len(full)-1))
            service_sum = r[service_time_col].sum()
            route_duration = travel_sum + service_sum

            for i in range(n - 1):
                if stop_search:
                    break

                for j in range(i + 1, n):
                    a = depot_id if i == 0 else ids[i-1]
                    b = ids[i]
                    c = ids[j]
                    d = depot_id if j == n-1 else ids[j+1]

                    delta_ext = (
                        travel_times[a, b] +
                        travel_times[c, d] -
                        travel_times[a, c] -
                        travel_times[b, d]
                    )

                    orig_internal = sum(travel_times[ids[k], ids[k+1]] for k in range(i, j))
                    rev_internal  = sum(travel_times[ids[k+1], ids[k]] for k in range(i, j))
                    delta_internal = orig_internal - rev_internal

                    delta = delta_ext + delta_internal
                    if delta <= EPS:
                        continue

                    current_obj = route_duration
                    new_duration = route_duration - delta
                    if new_duration > max_shift_duration:
                        continue

                    improvement = current_obj - new_duration

                    if not accept_fn(improvement, current_obj, new_duration):
                        continue

                    if improvement_choice == "First":
                        best_delta = improvement
                        best_move = (r_name, i, j)
                        stop_search = True
                        break

                    if improvement > best_delta:
                        best_delta = improvement
                        best_move = (r_name, i, j)

        if best_move is not None:
            print("2-opt improvement:", best_delta)

            r_name, i, j = best_move
            r = routes[r_name].copy()

            r.iloc[i:j+1] = r.iloc[i:j+1].iloc[::-1].values
            r = r.reset_index(drop=True)
            r[order_col] = range(1, len(r) + 1)

            stops = stops[stops[route_col] != r_name]
            stops = pd.concat([stops, r], ignore_index=True)

            improved = True

    return stops

def inter_route_2opt_star(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    night_col="Night_shift",
    depot_id=0,
    max_shift_duration=7*60,
    max_iterations = None,
    improvement_choice="Best",      # "Best" or "First"
    accept_fn=None):                  # acceptance function

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    if accept_fn is None: 
        accept_fn = greedy_accept_fn

    improved = True
    iteration = 0
    EPS = 1e-6
    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False
        best_delta = EPS
        best_move = None

        routes = {r: g.sort_values(order_col).copy() for r, g in stops.groupby(route_col)}
        route_names = list(routes.keys())

        def route_duration(ids, df):
            full = [depot_id] + ids + [depot_id]
            travel = sum(travel_times[full[k], full[k+1]] for k in range(len(full)-1))
            service = df[service_time_col].sum()
            return travel + service

        stop_search = False

        for idx1 in range(len(route_names)):
            if stop_search:
                break

            for idx2 in range(idx1+1, len(route_names)):
                if stop_search:
                    break

                r1_name, r2_name = route_names[idx1], route_names[idx2]
                r1, r2 = routes[r1_name], routes[r2_name]

                if r1[night_col].iloc[0] != r2[night_col].iloc[0]:
                    continue

                ids1, ids2 = r1[id_col].tolist(), r2[id_col].tolist()
                n1, n2 = len(ids1), len(ids2)
                if n1 < 2 or n2 < 2:
                    continue

                dur1_orig = route_duration(ids1, r1)
                dur2_orig = route_duration(ids2, r2)
                current_obj = dur1_orig + dur2_orig

                for i in range(n1 - 1):
                    if stop_search:
                        break

                    A, B = ids1[i], ids1[i+1]

                    for j in range(n2 - 1):
                        C, D = ids2[j], ids2[j+1]

                        delta_ext = (
                            travel_times[A, B] +
                            travel_times[C, D] -
                            travel_times[A, D] -
                            travel_times[C, B]
                        )

                        new_ids1 = ids1[:i+1] + ids2[j+1:]
                        new_ids2 = ids2[:j+1] + ids1[i+1:]

                        def tail_cost(seq):
                            return sum(travel_times[seq[k], seq[k+1]] for k in range(len(seq)-1))

                        orig_tail1 = tail_cost(ids1[i+1:]) if i+1 < n1 else 0.0
                        orig_tail2 = tail_cost(ids2[j+1:]) if j+1 < n2 else 0.0
                        new_tail1  = tail_cost(new_ids1[i+1:]) if i+1 < len(new_ids1) else 0.0
                        new_tail2  = tail_cost(new_ids2[j+1:]) if j+1 < len(new_ids2) else 0.0

                        delta_internal = (orig_tail1 + orig_tail2) - (new_tail1 + new_tail2)
                        delta = delta_ext + delta_internal
                        if delta <= EPS:
                            continue

                        dur1_new = route_duration(new_ids1, stops.loc[stops[id_col].isin(new_ids1)])
                        dur2_new = route_duration(new_ids2, stops.loc[stops[id_col].isin(new_ids2)])

                        if dur1_new > max_shift_duration or dur2_new > max_shift_duration:
                            continue

                        new_obj = dur1_new + dur2_new
                        improvement = current_obj - new_obj  # == delta

                        if not accept_fn(improvement, current_obj, new_obj):
                            continue

                        if improvement_choice == "First":
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)
                            stop_search = True
                            break

                        if improvement > best_delta:
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)

        if best_move is not None:
            r1_name, r2_name, i, j = best_move
            r1, r2 = routes[r1_name].copy(), routes[r2_name].copy()
            ids1, ids2 = r1[id_col].tolist(), r2[id_col].tolist()

            new_ids1 = ids1[:i+1] + ids2[j+1:]
            new_ids2 = ids2[:j+1] + ids1[i+1:]

            for pos, nid in enumerate(new_ids1):
                stops.loc[stops[id_col] == nid, route_col] = r1_name
                stops.loc[stops[id_col] == nid, order_col] = pos + 1

            for pos, nid in enumerate(new_ids2):
                stops.loc[stops[id_col] == nid, route_col] = r2_name
                stops.loc[stops[id_col] == nid, order_col] = pos + 1

            print("2-opt* improvement:", best_delta)
            improved = True

    return stops



def main():
    stops = pd.read_csv("data/inputs/cleaned/results_Greedy_abri.csv")
    travel_times = pd.read_csv("data/inputs/cleaned/travel_times_collapsedv2.txt", header=None, sep="\\s")

    travel_times = travel_times.to_numpy()
    
    new_stops = VND(stops=stops, travel_time_matrix=travel_times)#, improvement_choice = "First")

    new_stops = new_stops.sort_values(["Route", "Order"]).reset_index(drop=True)

    output_path = "data/outputs/results_LocalSearch_abri.csv"
    new_stops.to_csv(output_path, index=False)

    return

if __name__ == "__main__":
    main()