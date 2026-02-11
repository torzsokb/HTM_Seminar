import pandas as pd
from .base import Neighborhood

class Intra2OptNeighborhood(Neighborhood):

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

        a = 0 if i == 0 else ids[i - 1]
        b = ids[i]
        c = ids[j]
        d = 0 if j == n - 1 else ids[j + 1]

        delta_ext = (
            travel_times[a, b]
            + travel_times[c, d]
            - travel_times[a, c]
            - travel_times[b, d]
        )

        orig_internal = sum(travel_times[ids[k], ids[k + 1]] for k in range(i, j))
        rev_internal = sum(travel_times[ids[k + 1], ids[k]] for k in range(i, j))

        delta_internal = orig_internal - rev_internal
        delta = delta_ext + delta_internal

        if delta <= 1e-6:
            return 0, False

        new_duration = route_durations[r_name] - delta
        if new_duration > max_shift_duration:
            return 0, False

        return delta, True

    def apply_move(self, move, stops, route_col, order_col):
        r_name, i, j = move
        r = stops[stops[route_col] == r_name].sort_values(order_col).copy()

        r.iloc[i:j + 1] = r.iloc[i:j + 1].iloc[::-1].values
        r[order_col] = range(1, len(r) + 1)

        others = stops[stops[route_col] != r_name]
        return pd.concat([others, r], ignore_index=True)
