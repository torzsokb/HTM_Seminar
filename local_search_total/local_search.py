def local_search(
    stops,
    travel_times,
    neighborhood,
    route_col="Route",
    order_col="Order",
    id_col="ID",
    service_time_col="Service_time",
    depot_id=0,
    max_shift_duration=7*60,
    max_iterations=None,
    improvement_choice="Best",
    accept_fn=None,
    route_compatible_fn=None,
):

    if accept_fn is None:
        from .acceptance import greedy_accept_fn
        accept_fn = greedy_accept_fn

    if route_compatible_fn is None:
        from .compatibility import same_night_shift
        route_compatible_fn = same_night_shift

    from .cost import get_route_durations

    EPS = 1e-6
    iteration = 0
    improved = True

    while improved and (max_iterations is None or iteration < max_iterations):
        iteration += 1
        improved = False

        routes = {
            r: g.sort_values(order_col).copy()
            for r, g in stops.groupby(route_col)
        }

        route_durations = get_route_durations(
            routes, travel_times, id_col, depot_id, service_time_col
        )

        best_move = None
        best_delta = EPS

        for move in neighborhood.generate_moves(routes, route_compatible_fn):

            delta, feasible = neighborhood.evaluate_move(
                move,
                routes,
                route_durations,
                travel_times,
                max_shift_duration,
            )

            if not feasible or delta <= best_delta:
                continue

            current_obj = sum(route_durations.values())
            new_obj = current_obj - delta

            if not accept_fn(delta, current_obj, new_obj):
                continue

            if improvement_choice == "First":
                return neighborhood.apply_move(move, stops, route_col, order_col)

            best_move = move
            best_delta = delta

        if best_move is not None:
            stops = neighborhood.apply_move(best_move, stops, route_col, order_col)
            improved = True

    return stops
