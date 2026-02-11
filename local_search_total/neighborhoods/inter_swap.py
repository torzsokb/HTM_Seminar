import pandas as pd
from .base import Neighborhood

class InterSwapNeighborhood(Neighborhood):

    def generate_moves(self, routes, route_compatible_fn):
        for r1_name, r1 in routes.items():
            for r2_name, r2 in routes.items():
                if r1_name >= r2_name:
                    continue
                if not route_compatible_fn(r1, r2):
                    continue
                for i in range(len(r1)):
                    for j in range(len(r2)):
                        yield (r1_name, r2_name, i, j)

    def evaluate_move(self, move, routes, route_durations, travel_times, max_shift_duration):
        r1_name, r2_name, i, j = move
        r1 = routes[r1_name]
        r2 = routes[r2_name]

        ids1 = r1["ID"].tolist()
        ids2 = r2["ID"].tolist()

        node1 = ids1[i]
        node2 = ids2[j]

        service1 = r1.iloc[i]["Service_time"]
        service2 = r2.iloc[j]["Service_time"]

        prev1 = 0 if i == 0 else ids1[i - 1]
        next1 = 0 if i == len(ids1) - 1 else ids1[i + 1]

        prev2 = 0 if j == 0 else ids2[j - 1]
        next2 = 0 if j == len(ids2) - 1 else ids2[j + 1]

        old_r1 = travel_times[prev1, node1] + travel_times[node1, next1]
        old_r2 = travel_times[prev2, node2] + travel_times[node2, next2]

        new_r1 = travel_times[prev1, node2] + travel_times[node2, next1]
        new_r2 = travel_times[prev2, node1] + travel_times[node1, next2]

        delta_r1 = old_r1 - new_r1
        delta_r2 = old_r2 - new_r2

        new_dur_r1 = route_durations[r1_name] - service1 + service2 - delta_r1
        new_dur_r2 = route_durations[r2_name] - service2 + service1 - delta_r2

        if new_dur_r1 > max_shift_duration or new_dur_r2 > max_shift_duration:
            return 0, False

        improvement = (route_durations[r1_name] + route_durations[r2_name]) - (new_dur_r1 + new_dur_r2)
        return improvement, improvement > 1e-6

    def apply_move(self, move, stops, route_col, order_col):
        r1_name, r2_name, i, j = move

        r1 = stops[stops[route_col] == r1_name].sort_values(order_col).copy()
        r2 = stops[stops[route_col] == r2_name].sort_values(order_col).copy()

        row1 = r1.iloc[[i]].copy()
        row2 = r2.iloc[[j]].copy()

        row1[route_col] = r2_name
        row2[route_col] = r1_name

        r1.iloc[i] = row2.iloc[0]
        r2.iloc[j] = row1.iloc[0]

        r1[order_col] = range(1, len(r1) + 1)
        r2[order_col] = range(1, len(r2) + 1)

        others = stops[~stops[route_col].isin([r1_name, r2_name])]
        return pd.concat([others, r1, r2], ignore_index=True)
