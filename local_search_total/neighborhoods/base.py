from abc import ABC, abstractmethod

class Neighborhood(ABC):

    @abstractmethod
    def generate_moves(self, routes, route_compatible_fn):
        pass

    @abstractmethod
    def evaluate_move(
        self,
        move,
        routes,
        route_durations,
        travel_times,
        max_shift_duration,
    ):
        pass

    @abstractmethod
    def apply_move(self, move, stops, route_col, order_col):
        pass
