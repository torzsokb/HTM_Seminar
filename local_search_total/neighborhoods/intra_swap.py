import pandas as pd
from .base import Neighborhood

class IntraSwapNeighborhood(Neighborhood):

    def generate_moves(self, routes, route_compatible_fn):
        for r_name, r in routes.items():
            n = len(r)
            for i in range(n - 1):
                for j in range(i + 1, n):
                    yield (r_name, i, j)

    def evaluate_move(self, move, routes, route_durations, travel_times, max_shift_duration):
        r_name, i, j = move
        r = routes[r_name]
        ids = r["ID"].tolist()
        n = len(ids)

        node_i = ids[i]
        node_j = ids[j]

        prev_i = 0 if i == 0 else ids[i - 1]
        next_i = 0 if i == n - 1 else ids[i + 1]

        prev_j = 0 if j == 0 else ids[j - 1]
        next_j = 0 if j == n - 1 else ids[j + 1]

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
        if delta <= 1e-6:
            return 0, False

        new_duration = route_durations[r_name] - delta
        if new_duration > max_shift_duration:
            return 0, False

        return delta, True

    def apply_move(self, move, stops, route_col, order_col):
        r_name, i, j = move
        r = stops[stops[route_col] == r_name].sort_values(order_col).copy()

        row_i = r.iloc[[i]].copy()
        row_j = r.iloc[[j]].copy()

        r.iloc[i] = row_j.iloc[0]
        r.iloc[j] = row_i.iloc[0]

        r[order_col] = range(1, len(r) + 1)

        others = stops[stops[route_col] != r_name]
        return pd.concat([others, r], ignore_index=True)
