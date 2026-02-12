from models.tsp_solver import solve_tsp_mtz
from models.route import Route
from models.rmp import RestrictedMasterProblem
from models.sp import SubProblem, GurobiSP
from models.cvrp_solver import CVRPSolver

def main():
    a = solve_tsp_mtz([0,1,2,3], distances={}, warmstart=False, heur=False)

if __name__ == "__main__":
    main()