import pandas as pd
import numpy as np


def compute_route_metrics(
    stops: pd.DataFrame,
    travel_time_matrix: np.ndarray,
    *,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    shift_col=None,
    depot_id=0,
    max_shift_duration=7 * 60,
):
    travel_times_min = travel_time_matrix / 60

    route_rows = []
    violated_routes = 0
    total_travel_time = 0.0
    total_service_time = 0.0

    for route, group in stops.groupby(route_col):
        group_sorted = group.sort_values(order_col)
        stop_ids = group_sorted[id_col].tolist()

        full_ids = [depot_id] + stop_ids + [depot_id]

        travel_time = sum(
            travel_times_min[full_ids[i], full_ids[i + 1]]
            for i in range(len(full_ids) - 1)
        )

        service_time = group_sorted[service_time_col].sum()
        total_time = travel_time + service_time

        total_travel_time += travel_time
        total_service_time += service_time

        if total_time > max_shift_duration:
            violated_routes += 1

        if len(stop_ids) > 0:
            max_avg_service = min(
                20,
                (max_shift_duration - travel_time) / len(stop_ids)
            )
        else:
            max_avg_service = 0
        
        night_shift = int(group_sorted["Night_shift"].iloc[0])
        shift_type = "Night" if night_shift == 1 else "Day"

        route_rows.append({
        "route": route,
        "shift": shift_type,
        "is_night": night_shift,
        "stops": len(stop_ids),
        "travel_time_min": travel_time,
        "service_time_min": service_time,
        "total_time_min": total_time,
        "max_avg_service_time": max_avg_service,
    })

    routes_df = pd.DataFrame(route_rows)
    day_df = routes_df[routes_df["is_night"] == 0]
    night_df = routes_df[routes_df["is_night"] == 1]

    day_stats = compute_shift_stats(day_df, max_shift_duration)
    night_stats = compute_shift_stats(night_df, max_shift_duration)

    return {
        "routes_df": routes_df,
        "objective_value_hours": routes_df["total_time_min"].mean() / 60,
        "shortest_shift_hours": routes_df["total_time_min"].min() / 60,
        "longest_shift_hours": routes_df["total_time_min"].max() / 60,
        "total_travel_time_hours": total_travel_time / 60,
        "total_service_time_hours": total_service_time / 60,
        "violated_routes": violated_routes,
        "day_stats": day_stats,
        "night_stats": night_stats,
    }

def print_route_metrics_summary(results):

    print_shift_stats("Day", results["day_stats"])
    print_shift_stats("Night", results["night_stats"])

    print("\n Overall:")
    print(f"Avg shift length:     {results['objective_value_hours']:.5f} h")
    print(f"Shortest shift:       {results['shortest_shift_hours']:.5f} h")
    print(f"Longest shift:        {results['longest_shift_hours']:.5f} h")
    print(f"Violated routes:      {results['violated_routes']} ")


def compute_shift_stats(df, max_shift_duration):
    return {
        "num_routes": len(df),
        "avg_shift_hours": df["total_time_min"].mean() / 60,
        "min_shift_hours": df["total_time_min"].min() / 60,
        "max_shift_hours": df["total_time_min"].max() / 60,
        "total_travel_hours": df["travel_time_min"].sum() / 60,
        "total_service_hours": df["service_time_min"].sum() / 60,
        "violated_routes": int((df["total_time_min"] > max_shift_duration).sum()),
    }

def print_shift_stats(name, stats):
    def h(x): return f"{x:.5f} h"

    print(f"\n {name} routes:")
    print(f"Number of routes:     {stats['num_routes']}")
    print(f"Avg shift length:     {h(stats['avg_shift_hours'])}")
    print(f"Shortest shift:       {h(stats['min_shift_hours'])}")
    print(f"Longest shift:        {h(stats['max_shift_hours'])}")
    print(f"Total travel time:    {h(stats['total_travel_hours'])}")
    print(f"Total service time:   {h(stats['total_service_hours'])}")
    print(f"Violated routes:      {stats['violated_routes']}")



def main():
    stops = pd.read_csv("data/inputs/cleaned/HTM_ClusteredData_reordered.csv")
    travel_times = pd.read_csv("data/inputs/cleaned/travel_times_collapsed.txt", header=None, sep="\\s")
    travel_times = travel_times.to_numpy()

    metrics_cluster = compute_route_metrics(stops, travel_times, route_col="cluster", order_col= "cluster_order")
    print_route_metrics_summary(metrics_cluster)

    # metrics_original = compute_route_metrics(stops, travel_times)
    # print_route_metrics_summary(metrics_original)

if __name__ == "__main__":
    main()