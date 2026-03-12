import pandas as pd
import numpy as np

df = pd.read_csv("data/inputs/HTM_Data_withCounts.csv")
# SUMMER CLEANING

df["Summer_cleaning_time"] = df["Service_time"]

mask = df["seasonality"] == 1
df.loc[mask, "Summer_cleaning_time"] += 5 * df.loc[mask, "n_in_run"]

# AUTUMN CLEANING SCENARIOS

for i in range(1, 11):
    np.random.seed(i)

    df[f"Autumn_cleaning_{i}"] = df["Service_time"]

    mask = df["abri"] == 1

    # number of stops that get extra cleaning
    extra_cleanings = np.random.binomial(
        n=df.loc[mask, "n_in_run"],
        p=0.25
    )

    df.loc[mask, f"Autumn_cleaning_{i}"] += 3 * extra_cleanings

df.to_csv("data/inputs/stops_with_cleaning_times.csv", index=False)