import pandas as pd
import numpy as np
import json
import gurobipy as gp
from gurobipy import GRB

def optimise_cleaning_times(
        stops_path: str,
        dist_path: str,
        day_penalty: float,
        night_penalty: float,
        max_shift_duration: int,
        min_shift_duration: int,
        min_setup_time: int,
        min_tram_time: int,
        min_tram_abri_time: int,
        min_bus_time: int,
        min_bus_abri_time: int,
        max_overtime: int,
        overtime_penalty: float
        ) -> dict:
    
    df = pd.read_csv(stops_path)
    shifts = df["Route"].dropna().astype(str).unique().tolist()
    print(len(shifts))
    # print(df.columns)
    print(df["n_in_run"].value_counts())

    shift_info = {}
    


    with open(dist_path, "r") as f:
        distances = json.load(f)

    

    
    
    model = gp.Model()
    model.ModelSense = GRB.MINIMIZE

    setup_time = model.addVar(
        lb=min_setup_time,
        ub=20,
        obj=0,
        vtype=GRB.INTEGER,
        name="setup_time"
    )

    tram_abri_time = model.addVar(
        lb=min_tram_abri_time,
        ub=20,
        obj=0,
        vtype=GRB.INTEGER,
        name="tram_abri_time"
    )

    tram_time = model.addVar(
        lb=min_tram_time,
        ub=20,
        obj=0,
        vtype=GRB.INTEGER,
        name="tram_time"
    )

    bus_abri_time = model.addVar(
        lb=min_bus_abri_time,
        ub=20,
        obj=0,
        vtype=GRB.INTEGER,
        name="bus_abri_time"
    )

    bus_time = model.addVar(
        lb=min_bus_time,
        ub=20,
        obj=0,
        vtype=GRB.INTEGER,
        name="bus_time"
    )

    time_vars_by_stop_type = {
        "Bushalte": {
            0: bus_time,
            1: bus_abri_time
        },
        "Tramhalte": {
            0: tram_time,
            1: tram_abri_time
        }
    }

    slack_time_vars = {}
    overtime_vars = {}
    constraints = {}

    overtime_sum_expr = gp.LinExpr()

    for shift in shifts:

        print(shift)
        df_r = df[df["Route"] == shift]

        day_time = shift[4] == "D"
        travel_time = total_travel_time(
            distances=distances,
            stops=list(df_r["ID_MAXIMO"]),
            day_time=day_time
        )

        # travel_time_penalty = day_penalty if shift[4] == "D" else night_penalty
        # travel_time = total_travel_time(
        #     distances=distances,
        #     locations=list(df_r["ID_MAXIMO"]),
        #     penalty=travel_time_penalty
        # )

        # speed = 26 if shift[4] == "D" else 33
        # travel_time = total_travel_time(
        #     distances=distances,
        #     locations=list(df_r["ID_MAXIMO"]),
        #     speed=speed
        # )

        shift_info[shift] = {
            "travel_time": travel_time,
            "n_stops": len(df_r),
            "n_stops_by_type": {
                "Bushalte": {
                    0: 0,
                    1: 0
                },
                "Tramhalte": {
                    0: 0,
                    1: 0
                }
            }
        }
        

        slack_time_vars[shift] = model.addVar(
            lb=0,
            ub=(max_shift_duration - min_shift_duration),
            obj=1,
            vtype=GRB.CONTINUOUS,
            name=f"slack_{shift}"
        )

        overtime_vars[shift] = model.addVar(
            lb=0,
            ub=15,
            obj=overtime_penalty,
            vtype=GRB.CONTINUOUS,
            name=f"overtime_{shift}"
        )
    
        overtime_sum_expr += overtime_vars[shift]
        

        lhs = gp.LinExpr()
        lhs += slack_time_vars[shift]
        lhs -= overtime_vars[shift]

        for i, row in df_r.iterrows():

        
            n_reps = row["n_in_run"]
            abri = row["abri"]
            type = row["Type_halte"]

            lhs += n_reps * time_vars_by_stop_type[type][abri]
            lhs += setup_time

            shift_info[shift]["n_stops_by_type"][type][abri] += n_reps

        constraints[shift] = model.addConstr(
            lhs == (max_shift_duration - travel_time),
            name=f"constraint_{shift}"
        )

        model.update()

    constraints["max_overtime"] = model.addConstr(
        overtime_sum_expr <= max_overtime,
        name="max_overtime_constr"
    )

    model.update()

    model.optimize()

    if model.Status == GRB.INFEASIBLE:
        print("model is infeasible")
        return {}

    optimised_times = {
        "setup_time": setup_time.X,
        "tram_time": tram_time.X,
        "tram_abri_time": tram_abri_time.X,
        "bus_time": bus_time.X,
        "bus_abri_time": bus_abri_time.X
    }

    for shift in shifts:
        travel_time = shift_info[shift]["travel_time"]
        cleaning_time = shift_info[shift]["n_stops"] * optimised_times["setup_time"]

        cleaning_time += shift_info[shift]["n_stops_by_type"]["Tramhalte"][0] * optimised_times["tram_time"]
        cleaning_time += shift_info[shift]["n_stops_by_type"]["Tramhalte"][1] * optimised_times["tram_abri_time"]
        
        cleaning_time += shift_info[shift]["n_stops_by_type"]["Bushalte"][0] * optimised_times["bus_time"]
        cleaning_time += shift_info[shift]["n_stops_by_type"]["Bushalte"][1] * optimised_times["bus_abri_time"]
        
        print(f"shift {shift} total time: {(travel_time + cleaning_time)/60:.2f} cleaning: {cleaning_time} travel: {travel_time:.2f}")
    
    
    model.dispose()
    return optimised_times

def total_travel_time(distances: dict, stops: list, day_time: bool) -> float:

    metric = "time_day" if day_time else "time_night"

    total_time = distances["Depot"][stops[0]][metric]
    total_time += distances[stops[-1]]["Depot"][metric]
    
    for i in range(1, len(stops)):
        total_time += distances[stops[i-1]][stops[i]][metric]

    print(f"total time: {total_time / 60:.2f}")

    return total_time / 60



# def total_travel_time(distances: dict, locations: list, penalty: float=None, speed: float=None) -> float:
    
#     if penalty is not None:
#         return total_travel_time_by_penalty(distances=distances, locations=locations, penalty=penalty)
    
#     if speed is not None:
#         return total_travel_time_by_speed(distances=distances, locations=locations, speed=speed)

    

# def total_travel_time_by_penalty(distances: dict, locations: list, penalty: float) -> float:

#     total_time = distances["Depot"][locations[0]]["time"]
#     total_time += distances[locations[-1]]["Depot"]["time"]
    
#     for i in range(1, len(locations)):
#         total_time += distances[locations[i-1]][locations[i]]["time"]

#     return penalty * total_time / 60

# def total_travel_time_by_speed(distances: dict, locations: list, speed: float) -> float:

#     speed_mps = speed / 3.6

#     total_time = distances["Depot"][locations[0]]["dist"] / speed_mps
#     total_time += distances[locations[-1]]["Depot"]["dist"] / speed_mps
    
#     for i in range(1, len(locations)):
#         total_time += distances[locations[i-1]][locations[i]]["dist"] / speed_mps

#     return total_time / 60
    



def main():

    stops_path = "data/inputs/HTM_Data_abriTypeStop.csv"
    dist_path = "data/inputs/cleaned/distance_info_cleanedv2.json"
    day_penalty = 1.606
    night_penalty = 1.18
    max_shift_duration = 7 * 60
    min_shift_duration = 0 * 60
    min_setup_time = 0
    min_tram_time = 0
    min_tram_abri_time = 0
    min_bus_time = 0
    min_bus_abri_time = 0
    max_overtime = 7500
    overtime_penalty = 1.0

    output = optimise_cleaning_times(
        stops_path=stops_path, dist_path=dist_path, day_penalty=day_penalty, night_penalty=night_penalty, max_shift_duration=max_shift_duration, min_shift_duration=min_shift_duration, 
        min_setup_time=min_setup_time, min_tram_time=min_tram_time, min_tram_abri_time=min_tram_abri_time, min_bus_time=min_bus_time, min_bus_abri_time=min_bus_abri_time,
        max_overtime=max_overtime, overtime_penalty=overtime_penalty)
    
    print(output)




if __name__ == "__main__":
    main()
