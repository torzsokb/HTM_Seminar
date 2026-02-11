def get_route_durations(
    routes,
    travel_times,
    id_col,
    depot_id,
    service_time_col,
):
    durations = {}

    for r_name, r in routes.items():
        ids = r[id_col].tolist()
        full = [depot_id] + ids + [depot_id]

        travel = sum(
            travel_times[full[k], full[k+1]]
            for k in range(len(full) - 1)
        )
        service = r[service_time_col].sum()

        durations[r_name] = travel + service

    return durations
