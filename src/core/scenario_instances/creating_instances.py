import pandas as pd

# Base file (the structure you showed)
base = pd.read_csv("src/core/data_all_feas_typeHalte.txt", sep="\t")

# File containing the computed scenario cleaning times
scenarios_summer = pd.read_csv("data/inputs/cleaning_time_summer_scenarios.csv")

scenarios_autumn = pd.read_csv("data/inputs/cleaning_time_autumn_scenarios.csv")

# Column to overwrite
target_col = "Cleaning_time_abri_TypeHalte"

# ---- SUMMER SCENARIO ----
for i in (3, 5, 10):
    summer = base.copy()
    summer[target_col] = scenarios_summer[f"Summer_cleaning_time_fixed_{i}"]

    summer.to_csv(f"src/core/scenario_instances/txt_files/summer/summer_{i}.txt", sep="\t", index=False)



for i in range(1, 101):
    for prob in (0.25, 0.5, 1):
        for penalty in (3, 5, 10):
            autumn = base.copy()
            autumn[target_col] = scenarios_autumn[f"Autumn_cleaning_{i}_prob_{prob}_pen_{penalty}"]

            autumn.to_csv(f"src/core/scenario_instances/txt_files/autumn/prob_{prob}/penalty_{penalty}/autumn_{i}.txt", sep="\t", index=False)




# # ---- AUTUMN SCENARIOS ----
# for i in range(1, 11):
#     autumn = base.copy()
#     autumn[target_col] = scenarios[f"Autumn_cleaning_{i}"]

#     autumn.to_csv(f"src/core/scenario_instances/txt_files/autumn_{i}.txt", sep="\t", index=False)




    