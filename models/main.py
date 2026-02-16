import pandas as pd
import numpy as np

from local_search.local_search import local_search

from local_search.neighborhoods.inter_shift import InterShiftNeighborhood
from local_search.neighborhoods.inter_swap import InterSwapNeighborhood
from local_search.neighborhoods.intra_shift import IntraShiftNeighborhood
from local_search.neighborhoods.intra_swap import IntraSwapNeighborhood
from local_search.neighborhoods.intra_2opt import Intra2OptNeighborhood
from local_search.neighborhoods.inter_2opt_star import Inter2OptStarNeighborhood

from local_search.acceptance import greedy_accept_fn

stops = pd.read_csv("data/inputs/cleaned/results_Greedy_abri.csv")
travel_times = pd.read_csv("data/inputs/cleaned/travel_times_collapsedv2.txt", header=None, sep="\\s")
travel_times = travel_times.to_numpy()

neighborhoods = [
    InterShiftNeighborhood(),
    InterSwapNeighborhood(),
    IntraShiftNeighborhood(),
    IntraSwapNeighborhood(),
    Intra2OptNeighborhood(),
    Inter2OptStarNeighborhood()
]

stops_improved = local_search(
    stops,
    travel_times=travel_time_matrix,
    neighborhoods=neighborhoods,
    max_shift_duration=7*60,  
    improvement_choice="Best",
    accept_fn=greedy_accept_fn
)