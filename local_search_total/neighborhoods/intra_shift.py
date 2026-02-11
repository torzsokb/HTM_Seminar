import pandas as pd
from .base import Neighborhood

class IntraShiftNeighborhood(Neighborhood):

    def generate_moves(self, routes, route_compatible_fn):
        for r_name, r in routes.items():
            n = len(r)
            for i in range(n):
                for j in range(n + 1):
                    if j == i or j == i + 1:
                        continue  
                    yield (r_name, i, j)


    def evaluate_move(self, move, routes, route_durations, travel_times, max_shift_duration):
        r_name, i, j = move
        r = routes[r_name]
        ids = r["ID"].tolist()
        n = len(ids)

        node = ids[i]
        depot = 0  # ideally use self.depot_id

        prev_i = depot if i == 0 else ids[i - 1]
        next_i = depot if i == n - 1 else ids[i + 1]

        # cost of removal
        delta_remove = (
            travel_times[prev_i, node]
            + travel_times[node, next_i]
            - travel_times[prev_i, next_i]
        )

        # Adjust insertion index after removal
        if j > i:
            j_adj = j - 1
        else:
            j_adj = j

        ids_removed = ids[:i] + ids[i + 1:]

        prev_j = depot if j_adj == 0 else ids_removed[j_adj - 1]
        next_j = depot if j_adj == len(ids_removed) else ids_removed[j_adj]

        # cost of insertion
        delta_insert = (
            travel_times[prev_j, next_j]
            - travel_times[prev_j, node]
            - travel_times[node, next_j]
        )

        delta = delta_remove + delta_insert

        if delta <= 1e-6:
            return 0, False

        new_duration = route_durations[r_name] - delta
        if new_duration > max_shift_duration:
            return 0, False

        return delta, True


    def apply_move(self, move, stops, route_col, order_col):
        r_name, i, j = move
        r = stops[stops[route_col] == r_name].sort_values(order_col).copy()

        row = r.iloc[[i]].copy()
        r = r.drop(r.index[i]).reset_index(drop=True)

        r = pd.concat([r.iloc[:j], row, r.iloc[j:]], ignore_index=True)
        r[order_col] = range(1, len(r) + 1)

        others = stops[stops[route_col] != r_name]
        return pd.concat([others, r], ignore_index=True)
