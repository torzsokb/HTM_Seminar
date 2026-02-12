import numpy as np
import gurobipy as gp
from gurobipy import GRB
from models.route import Route

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

        self.model = gp.Model()
        self.x = {}
        self.routes = initital_routes
        self.constraints = []

    def setup(self):
        self.model.ModelSense = GRB.MINIMIZE
        self.model.params.OutputFlag = 1
        
        expr = gp.LinExpr()
        for i, route in enumerate(self.routes):
            print(route)
            self.x[i] = self.model.addVar(lb=0, ub=1, vtype=GRB.CONTINUOUS, obj=route.travel_time, name=f"x_{i}")
            expr += self.x[i]
        self.constraints.append(self.model.addConstr(expr <= self.k * 2, name=f"max_routes"))

        for i, stop in enumerate(self.stops):
            if stop == "Depot":
                continue
            lhs = gp.LinExpr()
            for j, route in enumerate(self.routes):
                if route.covers(stop):
                    lhs += self.x[j]
            self.constraints.append(self.model.addConstr(lhs >= 1, name=f"c_{stop}"))

        self.model.update()


    def get_duals(self) -> np.ndarray:
        # for c in self.constraints:
        #     print(f"constraint: {c.ConstrName}, dual: {c.Pi}")
        duals = [c.Pi for c in self.constraints]
        return np.array(duals)
    
    def solve(self, TimeLimit: int=None, OutputFlag: int=None):
        
        if not TimeLimit is None:
            self.model.params.TimeLimit = TimeLimit
        
        if not OutputFlag is None:
            self.model.params.OutputFlag = OutputFlag

        self.model.optimize()
        print(f"obj: {self.model.ObjVal:.3f}")

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
        




        
        
        
