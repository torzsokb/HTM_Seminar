import numpy as np

# -------------------------
# Load matrices
# -------------------------
distance_path = "data/inputs/cleaned/distances_collapsedv2.txt"
time_path = "data/inputs/cleaned/travel_times_collapsedv2.txt"
output_path = "data/inputs/cleaned/speed_old_collapsedv2.txt"

distances = np.loadtxt(distance_path)
times = np.loadtxt(time_path)

# -------------------------
# Safety checks
# -------------------------
if distances.shape != times.shape:
    raise ValueError("Distance and time matrices have different shapes.")

# Avoid division by zero
with np.errstate(divide='ignore', invalid='ignore'):
    speed = np.divide(distances, times)
    speed[times == 0] = 0  # or np.nan if preferred

# -------------------------
# Save result
# -------------------------
np.savetxt(output_path, speed, fmt="%.18e")

print("Speed matrix saved to:", output_path)

import numpy as np
import pandas as pd

# -------------------------
# Load matrices
# -------------------------
speed_old = np.loadtxt("data/inputs/cleaned/speed_old_collapsedv2.txt")

day_df = pd.read_csv("day_matrix.txt", sep="\t", index_col=0)
night_df = pd.read_csv("night_matrix.txt", sep="\t", index_col=0)

day_matrix = day_df.to_numpy()
night_matrix = night_df.to_numpy()

# -------------------------
# Safety check
# -------------------------
if not (speed_old.shape == day_matrix.shape == night_matrix.shape):
    raise ValueError("Matrix shapes do not match.")

# -------------------------
# Element-wise multiplication
# -------------------------
speed_day = speed_old * day_matrix
speed_night = speed_old * night_matrix

# -------------------------
# Save results
# -------------------------
np.savetxt(
    "data/inputs/cleaned/speed_day_collapsedv2.txt",
    speed_day,
    fmt="%.18e"
)

np.savetxt(
    "data/inputs/cleaned/speed_night_collapsedv2.txt",
    speed_night,
    fmt="%.18e"
)

print("Speed day and night matrices saved.")

import numpy as np

# -------------------------
# Load required matrices
# -------------------------
distances = np.loadtxt("data/inputs/cleaned/distances_collapsedv2.txt")
speed_day = np.loadtxt("data/inputs/cleaned/speed_day_collapsedv2.txt")
speed_night = np.loadtxt("data/inputs/cleaned/speed_night_collapsedv2.txt")

# -------------------------
# Safety check
# -------------------------
if not (distances.shape == speed_day.shape == speed_night.shape):
    raise ValueError("Matrix shapes do not match.")

# -------------------------
# Compute new travel times
# -------------------------
with np.errstate(divide='ignore', invalid='ignore'):
    travel_time_day = np.divide(distances, speed_day)
    travel_time_night = np.divide(distances, speed_night)

    # Handle zero speeds safely
    travel_time_day[speed_day == 0] = 0      # or np.nan
    travel_time_night[speed_night == 0] = 0  # or np.nan

# -------------------------
# Save results
# -------------------------
np.savetxt(
    "data/inputs/cleaned/travel_time_day_collapsedv2.txt",
    travel_time_day,
    fmt="%.18e"
)

np.savetxt(
    "data/inputs/cleaned/travel_time_night_collapsedv2.txt",
    travel_time_night,
    fmt="%.18e"
)

print("New day and night travel time matrices saved.")