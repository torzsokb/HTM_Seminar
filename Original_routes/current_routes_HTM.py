import pandas as pd
import numpy as np

stops = pd.read_csv("data/inputs/cleaned/HTM_CollapsedDatav2.csv")
travel_times = pd.read_csv("data/inputs/cleaned/travel_times_collapsed.txt", header=None, sep="\\s")
travel_times = travel_times.to_numpy()
total_cleaningTime = 0
max_shift_duration = 7*60

route_times = []
num_violated_routes = 0
for route, group in stops.groupby("Route"):
    group_sorted = group.sort_values("Order")
    indices = group_sorted["ID"].tolist()
    full_indices = [0] + indices + [0]

    original_travel_time = 0
    for i in range(len(full_indices)-1):
        from_id = full_indices[i]
        to_id = full_indices[i+1]
        original_travel_time += travel_times[from_id, to_id]/60
    
    total_cleaningTime = group_sorted["Service_time"].sum()
    
    original_total = original_travel_time + total_cleaningTime

    max_avg_cleaning = min(20, (max_shift_duration- original_travel_time)/len(indices))

    night_shift = group_sorted["Night_shift"].iloc[0]  # 0=day, 1=night
    shift_type = "Night" if night_shift == 1 else "Day"

    if original_total > max_shift_duration:
        num_violated_routes += 1
    
    route_times.append({
        "Route": route,
        "Shift": shift_type,
        "Stops": len(indices),
        "Total_Driving_Time": original_travel_time,
        "Service_Time_Min": total_cleaningTime,
        "Total_time": original_total,
        "Max_Avg_Cleaning_Time": max_avg_cleaning,
    })

route_times_df = pd.DataFrame(route_times)
print(route_times_df)
print("\nNumber of violated routes:", num_violated_routes)

objective_value = route_times_df.loc[:, 'Total_time'].sum()/50.0/60.0
print("Objective value", objective_value)

shortest_shift = route_times_df.loc[:, 'Total_time'].min()/60.0
print("Shortest shift", shortest_shift)

longest_shift = route_times_df.loc[:, 'Total_time'].max()/60.0
print("Longest shift", longest_shift)