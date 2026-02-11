import pandas as pd
import numpy as np

from local_search_total.vnd import vnd

from local_search_total.neighborhoods.intra_swap import IntraSwapNeighborhood
from local_search_total.neighborhoods.intra_shift import IntraShiftNeighborhood
from local_search_total.neighborhoods.inter_swap import InterSwapNeighborhood
from local_search_total.neighborhoods.inter_shift import InterShiftNeighborhood
from local_search_total.neighborhoods.intra_2opt import Intra2OptNeighborhood
from local_search_total.neighborhoods.inter_2opt_star import Inter2OptStarNeighborhood


def main():
    stops = pd.read_csv("data/inputs/cleaned/results_Greedy_abri.csv")

    travel_times = pd.read_csv(
        "data/inputs/cleaned/travel_times_collapsedv2.txt",
        header=None,
        sep="\\s+",
        engine="python",   # removes warning
    ).to_numpy()

    travel_times = travel_times / 60

    neighborhoods = [
        IntraShiftNeighborhood(),
        IntraSwapNeighborhood(),
        Intra2OptNeighborhood(),
        InterShiftNeighborhood(),
        InterSwapNeighborhood(),
        Inter2OptStarNeighborhood(),
    ]

    improved = vnd(
        stops=stops,
        travel_times=travel_times,
        neighborhoods=neighborhoods,
        max_iterations=1,          
        improvement_choice="Best",
        time_limit=60
    )

    improved = (
        improved
        .sort_values(["Route", "Order"])
        .reset_index(drop=True)
    )

    output_path = "data/outputs/results_VND_abri.csv"
    improved.to_csv(output_path, index=False)


if __name__ == "__main__":
    main()
