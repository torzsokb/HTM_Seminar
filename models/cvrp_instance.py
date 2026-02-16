from dataclasses import dataclass
import numpy as np
import pandas as pd
from cvrp_solver import CVRPSolver
from utils import get_shift_data

class CVRPInstance:
    def __init__(
            self,
            instance_data: pd.DataFrame,
            distances: dict,
            split_nights_days: bool):
        
        self.instance_data = instance_data
        self.distances = distances
        self.split_nights_days = split_nights_days

