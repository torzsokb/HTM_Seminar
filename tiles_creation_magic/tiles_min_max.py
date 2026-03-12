import pandas as pd
import numpy as np

# -------------------------
# Load matrices
# -------------------------
day_df = pd.read_csv("day_matrix.txt", sep="\t", index_col=0)
night_df = pd.read_csv("night_matrix.txt", sep="\t", index_col=0)

# Convert to NumPy arrays
day_matrix = day_df.to_numpy()
night_matrix = night_df.to_numpy()

# Mask the diagonal to ignore zeros
day_mask = ~np.eye(day_matrix.shape[0], dtype=bool)  # True for all off-diagonal
night_mask = ~np.eye(night_matrix.shape[0], dtype=bool)

# Apply mask to matrices
day_vals = day_matrix[day_mask]
night_vals = night_matrix[night_mask]

# Compute max, min, average
day_max, day_min, day_avg = day_vals.max(), day_vals.min(), day_vals.mean()
night_max, night_min, night_avg = night_vals.max(), night_vals.min(), night_vals.mean()

print(night_df.isna().sum().sum())

print(f"Day ratio → Max: {day_max:.8f}, Min: {day_min:.8f}, Avg: {day_avg:.8f}")
print(f"Night ratio → Max: {night_max:.8f}, Min: {night_min:.8f}, Avg: {night_avg:.8f}")

import pandas as pd
import numpy as np

# Load night matrix
night_df = pd.read_csv("night_matrix.txt", sep="\t", index_col=0)

# Convert to NumPy array
night_matrix = night_df.to_numpy()
stop_ids = night_df.index.tolist()

# Mask diagonal
mask = ~np.eye(night_matrix.shape[0], dtype=bool)
night_vals = night_matrix[mask]

# Find indices of NaNs
nan_positions = np.argwhere(np.isnan(night_matrix))

print(f"Total NaNs: {len(nan_positions)}\n")

# Print stop IDs for the NaNs
for row_idx, col_idx in nan_positions:
    row_id = stop_ids[row_idx]
    col_id = stop_ids[col_idx]
    print(f"NaN at row {row_idx} (ID: {row_id}) - col {col_idx} (ID: {col_id})")

import geopandas as gpd

# Load the GeoJSON
regions = gpd.read_file("day_night_ratios.geojson")

# Check how many features are missing day_ratio or night_ratio
missing_day = regions["day_ratio"].isna().sum()
missing_night = regions["night_ratio"].isna().sum()

print(f"Missing day_ratio count: {missing_day}")
print(f"Missing night_ratio count: {missing_night}")