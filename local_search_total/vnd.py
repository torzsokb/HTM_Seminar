from local_search_total.local_search import local_search
from local_search_total.utils import total_cost

import time


def vnd(
    stops,
    travel_times,
    neighborhoods,
    route_col="Route",
    order_col="Order",
    max_iterations=1,
    improvement_choice="First",
    accept_fn=None,
    route_compatible_fn=lambda r1, r2: True,
    time_limit = None
):
    print("Running vnd with a time limit of", time_limit, "seconds")
    start_time = time.time()
    current_cost = total_cost(
        stops, travel_times, route_col, order_col
    )

    k = 0

    while k < len(neighborhoods):

        if time_limit is not None:
            elapsed_time = time.time() - start_time
            if elapsed_time >= time_limit:
                print("Time limit reached")
                break

        neighborhood = neighborhoods[k]

        new_stops = local_search(
            stops=stops,
            travel_times=travel_times,
            neighborhood=neighborhood,
            route_col=route_col,
            order_col=order_col,
            max_iterations=max_iterations,
            improvement_choice=improvement_choice,
            accept_fn=accept_fn,
            route_compatible_fn=route_compatible_fn,
        )

        new_cost = total_cost(
            new_stops, travel_times, route_col, order_col
        )

        if new_cost < current_cost - 1e-6:
            stops = new_stops
            improvement = current_cost - new_cost
            print(f"Improvement with {type(neighborhood).__name__} = {improvement:.2f}")
            current_cost = new_cost
            k = 0  
        else:
            k += 1

    return stops
