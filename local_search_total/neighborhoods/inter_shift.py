import pandas as pd
from .base import Neighborhood

class InterShiftNeighborhood(Neighborhood):

    def generate_moves(self, routes, route_compatible_fn):
        for r1_name, r1 in routes.items():
            for r2_name, r2 in routes.items():
                if r1_name == r2_name:
                    continue
                if not route_compatible_fn(r1, r2):
                    continue
                for i in range(len(r1)):
                    for j in range(len(r2) + 1):
                        yield (r1_name, r2_name, i, j)

    def evaluate_move(self, move, routes, route_durations, travel_times, max_shift_duration):
        r1_name, r2_name, i, j = move
        r1 = routes[r1_name]
        r2 = routes[r2_name]

        ids1 = r1["ID"].tolist()
        ids2 = r2["ID"].tolist()

        node = ids1[i]
        service = r1.iloc[i]["Service_time"]

        prev1 = 0 if i == 0 else ids1[i - 1]
        next1 = 0 if i == len(ids1) - 1 else ids1[i + 1]

        delta_remove = (
            -travel_times[prev1, node]
            - travel_times[node, next1]
            + travel_times[prev1, next1]
        )

        prev2 = 0 if j == 0 else ids2[j - 1]
        next2 = 0 if j == len(ids2) else ids2[j]

        delta_insert = (
            -travel_times[prev2, next2]
            + travel_times[prev2, node]
            + travel_times[node, next2]
        )

        new_dur_r1 = route_durations[r1_name] + delta_remove - service
        new_dur_r2 = route_durations[r2_name] + delta_insert + service

        if new_dur_r1 > max_shift_duration or new_dur_r2 > max_shift_duration:
            return 0, False

        improvement = (route_durations[r1_name] + route_durations[r2_name]) - (new_dur_r1 + new_dur_r2)
        return improvement, improvement > 1e-6

    def apply_move(self, move, stops, route_col, order_col):
        r1_name, r2_name, i, j = move

        r1 = stops[stops[route_col] == r1_name].sort_values(order_col).copy()
        r2 = stops[stops[route_col] == r2_name].sort_values(order_col).copy()

        row = r1.iloc[[i]].copy()
        r1 = r1.drop(r1.index[i])

        row[route_col] = r2_name
        r2 = pd.concat([r2.iloc[:j], row, r2.iloc[j:]], ignore_index=True)

        r1[order_col] = range(1, len(r1) + 1)
        r2[order_col] = range(1, len(r2) + 1)

        others = stops[~stops[route_col].isin([r1_name, r2_name])]
        return pd.concat([others, r1, r2], ignore_index=True)
