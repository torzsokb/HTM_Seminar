import pandas as pd
from .base import Neighborhood


class Inter2OptStarNeighborhood(Neighborhood):
    def __init__(self, depot_id=0, eps=1e-6):
        self.depot_id = depot_id
        self.EPS = eps

    def generate_moves(self, routes, route_compatible_fn):
        for r1_name, r1 in routes.items():
            for r2_name, r2 in routes.items():

                if r1_name >= r2_name:
                    continue

                if not route_compatible_fn(r1, r2):
                    continue

                n1 = len(r1)
                n2 = len(r2)

                for i in range(n1 - 1):
                    for j in range(n2 - 1):
                        yield (r1_name, r2_name, i, j)

    def evaluate_move(
    self,
    move,
    routes,
    route_durations,
    travel_times,
    max_shift_duration,
):
        r1_name, r2_name, i, j = move

        r1 = routes[r1_name]
        r2 = routes[r2_name]

        ids1 = r1["ID"].tolist()
        ids2 = r2["ID"].tolist()

        A = ids1[i]
        B = ids1[i + 1]
        C = ids2[j]
        D = ids2[j + 1]

        # Per-route arc deltas
        delta_r1 = travel_times[A, B] - travel_times[A, D]
        delta_r2 = travel_times[C, D] - travel_times[C, B]

        improvement = delta_r1 + delta_r2

        if improvement <= self.EPS:
            return 0, False

        # Service tails
        tail1_service = r1.iloc[i + 1:]["Service_time"].sum()
        tail2_service = r2.iloc[j + 1:]["Service_time"].sum()

        new_dur_r1 = (
            route_durations[r1_name]
            - tail1_service
            + tail2_service
            - delta_r1
        )

        new_dur_r2 = (
            route_durations[r2_name]
            - tail2_service
            + tail1_service
            - delta_r2
        )

        if new_dur_r1 > max_shift_duration:
            return 0, False

        if new_dur_r2 > max_shift_duration:
            return 0, False

        return improvement, True


    def apply_move(self, move, stops, route_col, order_col):
        r1_name, r2_name, i, j = move

        r1 = stops[stops[route_col] == r1_name].sort_values(order_col).copy()
        r2 = stops[stops[route_col] == r2_name].sort_values(order_col).copy()

        ids1 = r1["ID"].tolist()
        ids2 = r2["ID"].tolist()

        new_ids1 = ids1[:i + 1] + ids2[j + 1:]
        new_ids2 = ids2[:j + 1] + ids1[i + 1:]

        new_r1 = (
            stops[stops["ID"].isin(new_ids1)]
            .set_index("ID")
            .loc[new_ids1]
            .reset_index()
        )
        new_r1[route_col] = r1_name
        new_r1[order_col] = range(1, len(new_r1) + 1)

        new_r2 = (
            stops[stops["ID"].isin(new_ids2)]
            .set_index("ID")
            .loc[new_ids2]
            .reset_index()
        )
        new_r2[route_col] = r2_name
        new_r2[order_col] = range(1, len(new_r2) + 1)

        others = stops[
            ~stops["ID"].isin(new_ids1 + new_ids2)
        ]

        return pd.concat([others, new_r1, new_r2], ignore_index=True)
