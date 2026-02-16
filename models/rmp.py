import numpy as np
import gurobipy as gp
from gurobipy import GRB
from models.route import Route

BIG_M = 999999

class RestrictedMasterProblem:
    
    def __init__(
            self,
            stops: list[str],
            distances: np.ndarray,
            cleaning_times: list[int],
            initital_routes: list[Route],
            k: int):
        
        self.stops = stops
        self.distances = distances
        self.cleaning_times = cleaning_times
        self.k = k
        self.big_m = 0

        self.model = gp.Model()
        self.x = {}
        self.dummy_vars = {}
        self.routes = initital_routes
        self.constraints = []

    def setup(self):
        
        self.model.ModelSense = GRB.MINIMIZE
        self.model.params.OutputFlag = 1

        
        self.dummy_vars[0] = self.model.addVar(
            vtype=GRB.CONTINUOUS, 
            lb=0, ub=1,obj=0,name="dummy_0"
            )
        self.constraints.append(25 * self.dummy_vars[0] <= self.k, name="number_of_shifts")
        
        
        for i, stop in enumerate(self.stops):
            if stop == "Depot":
                continue

            self.dummy_vars[i] = self.model.addVar(
                vtype=GRB.CONTINUOUS, 
                lb=0, ub=1,
                obj=self.big_m,
                name=f"dummy_{stop}"
                )
            
            self.constraints.append(self.model.addConstr(
                self.dummy_vars[i] >= 1,
                name=f"cover_stop_{stop}"
                ))


        self.model.update()


    def set_big_m(self) -> None:
        self.big_m = BIG_M


    def get_duals(self) -> np.ndarray:
        # for c in self.constraints:
        #     print(f"constraint: {c.ConstrName}, dual: {c.Pi}")
        duals = [c.Pi for c in self.constraints]
        return np.array(duals)
    
    def solve(self, TimeLimit: int=None, OutputFlag: int=None) -> None:
        
        if not TimeLimit is None:
            self.model.params.TimeLimit = TimeLimit
        
        if not OutputFlag is None:
            self.model.params.OutputFlag = OutputFlag

        self.model.optimize()
        print(f"obj: {self.model.ObjVal:.3f}")

    def add_columns(self, routes: list[Route]) -> None:
        for route in routes:
            self.add_column(route)

    def add_column(self, route: Route) -> None:
        
        coeffs = np.zeros(len(self.stops), dtype=np.uint8)
        for i in range(len(self.stops)):
            stop = self.stops[i]
            if route.covers(stop):
                coeffs[i] = 1
        new_col = gp.Column(coeffs=coeffs, constrs=self.constraints)

        self.x[len(self.routes)] = self.model.addVar(lb=0, vtype=GRB.CONTINUOUS, obj=route.travel_time, column=new_col, name=f"x_{len(self.routes)}")
        self.routes.append(route)
        self.model.update()
        




        
        
        
