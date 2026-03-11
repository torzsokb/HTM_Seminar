import pandas as pd

# Base file (the structure you showed)
base = pd.read_csv("src/core/data_all_feas_typeHalte.txt", sep="\t")

# File containing the computed scenario cleaning times
scenarios = pd.read_csv("data/inputs/stops_with_cleaning_times.csv")

# Column to overwrite
target_col = "Cleaning_time_abri_TypeHalte"

# ---- SUMMER SCENARIO ----
summer = base.copy()
summer[target_col] = scenarios["Summer_cleaning_time"]

summer.to_csv("src/core/scenario_instances/txt_files/summer.txt", sep="\t", index=False)

# ---- AUTUMN SCENARIOS ----
for i in range(1, 11):
    autumn = base.copy()
    autumn[target_col] = scenarios[f"Autumn_cleaning_{i}"]

    autumn.to_csv(f"src/core/scenario_instances/txt_files/autumn_{i}.txt", sep="\t", index=False)




    