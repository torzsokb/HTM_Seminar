def total_cost(stops, travel_times, route_col="Route", order_col="Order", depot_id=0):
    total = 0

    for r_name, r in stops.groupby(route_col):

        r = r.sort_values(order_col)

        ids = r["ID"].tolist()

        # Travel cost
        full = [depot_id] + ids + [depot_id]

        travel = sum(
            travel_times[full[i], full[i + 1]]
            for i in range(len(full) - 1)
        )

        service = r["Service_time"].sum()

        total += travel + service

    return total
