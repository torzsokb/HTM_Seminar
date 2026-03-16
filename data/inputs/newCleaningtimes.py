import pandas as pd
import numpy as np

df = pd.read_csv("data/inputs/HTM_Data_withCounts.csv")
# SUMMER CLEANING

mask = df["seasonality"] == 1

for i in (3, 5, 10):
    df[f"Summer_cleaning_time_fixed_{i}"] = df["Service_time"]
    df.loc[mask, f"Summer_cleaning_time_fixed_{i}"] += i*df.loc[mask, "n_in_run"]


# AUTUMN CLEANING SCENARIOS

for i in range(1, 101):
    for prob in (0.25, 0.5, 1):
        for penalty in (3, 5, 10):
            np.random.seed(int(i*101 + 4*prob + penalty))
            df[f"Autumn_cleaning_{i}_prob_{prob}_pen_{penalty}"] = df["Service_time"]
            mask = df["abri"] == 1

            # number of stops that get extra cleaning
            extra_cleanings = np.random.binomial(
                n=df.loc[mask, "n_in_run"],
                p=prob)
            df.loc[mask, f"Autumn_cleaning_{i}_prob_{prob}_pen_{penalty}"] += penalty * extra_cleanings

df.to_csv("data/inputs/cleaning_time_autumn_scenarios.csv", index=False)