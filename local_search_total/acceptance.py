import math
import random

def greedy_accept_fn(improvement, current_obj, new_obj):
    return improvement > 0

def simulated_annealing_accept_fn(temperature):
    def fn(improvement, current_obj, new_obj):
        if improvement > 0:
            return True
        return random.random() < math.exp(improvement / temperature)
    return fn
