import pandas as pd
import numpy as np

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
):

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    improved = True
    num_iterations = 0

    while improved:
        num_iterations += 1
        if num_iterations > 2:
            break
        improved = False
        best_delta = 0
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

        for r1_name, r1 in routes.items():
            for r2_name, r2 in routes.items():

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

                        if improvement > 0:
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)

        if best_move is not None:
            print("Improvement:", best_delta)

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
):

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    improved = True
    num_iterations = 0
    while improved:
        num_iterations += 1
        if num_iterations > 2:
            break
        improved = False
        best_delta = 0
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

        for idx1 in range(len(route_names)):
            for idx2 in range(idx1 + 1, len(route_names)):

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
                    for j in range(len(r2_ids)):

                        node1 = r1_ids[i]
                        node2 = r2_ids[j]

                        service1 = r1.iloc[i][service_time_col]
                        service2 = r2.iloc[j][service_time_col]

                        prev1 = depot_id if i == 0 else r1_ids[i-1]
                        next1 = depot_id if i == len(r1_ids)-1 else r1_ids[i+1]

                        prev2 = depot_id if j == 0 else r2_ids[j-1]
                        next2 = depot_id if j == len(r2_ids)-1 else r2_ids[j+1]

                        old_r1 = (
                            travel_times[prev1, node1]
                            + travel_times[node1, next1]
                        )

                        new_r1 = (
                            travel_times[prev1, node2]
                            + travel_times[node2, next1]
                        )

                        delta_r1 = old_r1 - new_r1


                        old_r2 = (
                            travel_times[prev2, node2]
                            + travel_times[node2, next2]
                        )

                        new_r2 = (
                            travel_times[prev2, node1]
                            + travel_times[node1, next2]
                        )

                        delta_r2 = old_r2 - new_r2


                        new_dur_r1 = (
                            route_duration[r1_name]
                            - service1
                            + service2
                            - delta_r1 
                        )

                        new_dur_r2 = (
                            route_duration[r2_name]
                            - service2
                            + service1
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

                        if improvement > best_delta:
                            best_delta = improvement
                            best_move = (r1_name, r2_name, i, j)

        if best_move is not None:
            print("Swap improvement:", best_delta)

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

            r1[order_col] = range(1, len(r1)+1)
            r2[order_col] = range(1, len(r2)+1)

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
):

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    improved = True

    while improved:
        improved = False
        best_delta = 0
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

        for r_name, r in routes.items():

            ids = r[id_col].tolist()
            n = len(ids)

            for i in range(n - 1):
                for j in range(i + 1, n):
                    node_i = ids[i]
                    node_j = ids[j]

                    prev_i = depot_id if i == 0 else ids[i-1]
                    next_i = depot_id if i == n-1 else ids[i+1]

                    prev_j = depot_id if j == 0 else ids[j-1]
                    next_j = depot_id if j == n-1 else ids[j+1]

                    # If 2 subsequent nodes
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

                    if delta <= best_delta:
                        continue

                    # Duration feasibility
                    new_duration = route_duration[r_name] - delta

                    if new_duration > max_shift_duration:
                        continue

                    best_delta = delta
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
            r[order_col] = range(1, len(r)+1)

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
    depot_id=0,
    max_shift_duration=7 * 60,
):

    travel_times = travel_time_matrix / 60
    stops = stops.copy()

    improved = True

    while improved:
        improved = False
        best_delta = 1e-6
        best_move = None

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        for r_name, r in routes.items():

            ids = r[id_col].tolist()
            n = len(ids)

            full = [depot_id] + ids + [depot_id]
            route_duration = sum(
                travel_times[full[k], full[k+1]]
                for k in range(len(full)-1)
            ) + r[service_time_col].sum()

            for i in range(n):

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

                    if delta <= best_delta:
                        continue

                    new_duration = route_duration - delta

                    if new_duration > max_shift_duration:
                        continue

                    best_delta = delta
                    best_move = (r_name, i, j)

        if best_move is not None:
            print("Intra-relocate improvement:", best_delta)

            r_name, i, j = best_move
            r = routes[r_name].copy()

            row = r.iloc[i].copy()
            r = r.drop(r.index[i]).reset_index(drop=True)

            r_part1 = r.iloc[:j]
            r_part2 = r.iloc[j:]

            r = pd.concat([r_part1, row.to_frame().T, r_part2]).reset_index(drop=True)
            r[order_col] = range(1, len(r)+1)

            stops = stops[stops[route_col] != r_name]
            stops = pd.concat([stops, r], ignore_index=True)

            improved = True

    return stops



def main():
    stops = pd.read_csv("data/inputs/cleaned/results_Greedy_abri.csv")
    travel_times = pd.read_csv("data/inputs/cleaned/travel_times_collapsedv2.txt", header=None, sep="\\s")
    travel_times = travel_times.to_numpy()
    
    new_stops = intra_route_shift(stops=stops, travel_time_matrix=travel_times)

    new_stops = new_stops.sort_values(["Route", "Order"]).reset_index(drop=True)

    output_path = "data/outputs/results_LocalSearch_abri.csv"
    new_stops.to_csv(output_path, index=False)

    return

if __name__ == "__main__":
    main()