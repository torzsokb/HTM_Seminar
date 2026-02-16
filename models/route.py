import numpy as np
from numba.experimental import jitclass

@jitclass
class Route:

    def __init__(
            self,
            stops: list[str],
            travel_time: int,
            cleaning_time: int
            ):

        self.stops = stops
        self.unique_stops = set(stops)
        self.travel_time = travel_time
        self.cleaning_time = cleaning_time
        self.duration = travel_time + cleaning_time

    def covers(self, stop: str) -> bool:
        return stop in self.unique_stops

    def __str__(self):
        return f"Route\ncleaning time: {self.cleaning_time / 60:.2f}\ttravel time: {self.travel_time / 60:.2f}\ttotal time: {self.duration / 60:.2f}\nstops: {self.stops}"
    
    def __repr__(self):
        return self.__str__()