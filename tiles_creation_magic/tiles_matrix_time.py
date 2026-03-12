import geopandas as gpd
import pandas as pd
import numpy as np
from shapely.geometry import LineString

# -------------------------
# Load regions
# -------------------------
regions = gpd.read_file("day_night_ratios.geojson").to_crs(epsg=28992)

# CRITICAL: Ensure regions are disjoint. 
# If polygons overlap, sjoin creates duplicate rows, breaking iloc alignment.
# We dissolve to ensure every location belongs to exactly one polygon record.
# If your regions are already disjoint, this step is safe but redundant.
regions = regions.dissolve(by=regions.index, aggfunc='first').reset_index(drop=True)

region_sindex = regions.sindex

# -------------------------
# Load stops
# -------------------------
stops_df = pd.read_csv("src/core/data_all_feas_typeHalte.txt", sep="\t")

stops_gdf = gpd.GeoDataFrame(
    stops_df,
    geometry=gpd.points_from_xy(stops_df.longitude, stops_df.latitude),
    crs="EPSG:4326"
).to_crs(epsg=28992)

# CRITICAL: Reset index to ensure clean 0..n-1 alignment
stops_gdf = stops_gdf.reset_index(drop=True)
n = len(stops_gdf)

day_matrix = np.zeros((n, n))
night_matrix = np.zeros((n, n))

# -------------------------
# Pre-assign each stop to region
# -------------------------
stops_with_region = gpd.sjoin(
    stops_gdf,
    regions[["geometry", "day_ratio", "night_ratio"]],
    how="left",
    predicate="within"
)

# CRITICAL: Reset index on joined data to match stops_gdf
# If sjoin created duplicates (overlaps), we must handle them. 
# We keep the first match to ensure 1-to-1 alignment with stops_gdf.
stops_with_region = stops_with_region[~stops_with_region.index.duplicated(keep='first')]
stops_with_region = stops_with_region.reset_index(drop=True)

# DEBUG: Check alignment
print(f"Stops count: {n}")
print(f"Joined count: {len(stops_with_region)}")
if n != len(stops_with_region):
    raise ValueError("Stop and Region join alignment failed. Check for overlapping regions.")

# Check for NaNs introduced by the join (stops outside any region)
na_sum = stops_with_region[["day_ratio", "night_ratio"]].isna().sum()
print(f"NaNs in joined ratios: {na_sum}")

# -------------------------
# Compute only upper triangle
# -------------------------

for i in range(n):
    p1 = stops_gdf.geometry.iloc[i]
    r1 = stops_with_region.iloc[i]

    for j in range(i + 1, n):
        print("i", i, "j", j)
        p2 = stops_gdf.geometry.iloc[j]
        r2 = stops_with_region.iloc[j]

        # Check if both are NA first
        r1_night_na = pd.isna(r1.night_ratio)
        r2_night_na = pd.isna(r2.night_ratio)
        r1_day_na = pd.isna(r1.day_ratio)
        r2_day_na = pd.isna(r2.day_ratio)

        # Same region shortcut (Handles NaNs correctly)
        # Only use shortcut if both are valid AND equal, OR both are NA (though NA shortcut assigns NA)
        if not r1_day_na and not r2_day_na and not r1_night_na and not r2_night_na:
             if (r1.day_ratio == r2.day_ratio and r1.night_ratio == r2.night_ratio):
                day_val = r1.day_ratio
                night_val = r1.night_ratio
             else:
                # Calculate intersection
                line = LineString([p1, p2])
                total_length = line.length
                
                if total_length == 0:
                    day_val = 0
                    night_val = 0
                else:
                    weighted_day = 0
                    weighted_night = 0
                    covered_length = 0
                    
                    possible_matches_index = list(region_sindex.intersection(line.bounds))
                    possible_regions = regions.iloc[possible_matches_index]
                    
                    for _, region in possible_regions.iterrows():
                        intersection = line.intersection(region.geometry)
                        if not intersection.is_empty:
                            length = intersection.length
                            covered_length += length

                            r_day = region.day_ratio if not pd.isna(region.day_ratio) else 0
                            r_night = region.night_ratio if not pd.isna(region.night_ratio) else 0
                            
                            weighted_day += length * r_day 
                            weighted_night += length * r_night
                            
                    if covered_length > 0:
                        day_val = round(weighted_day / covered_length, 8)
                        night_val = round(weighted_night / covered_length, 8)
                    else:
                        # Fallback if line doesn't intersect any region
                        day_val = 0  # or use r1.day_ratio, or np.nan
                        night_val = 0
        else:
            # If any ratio is NA (stop outside region), we must calculate via geometry
            # We cannot assume the ratio of the stop point applies to the line
            line = LineString([p1, p2])
            total_length = line.length
            
            if total_length == 0:
                day_val = 0
                night_val = 0
            else:
                weighted_day = 0
                weighted_night = 0
                
                possible_matches_index = list(region_sindex.intersection(line.bounds))
                possible_regions = regions.iloc[possible_matches_index]
                
                for _, region in possible_regions.iterrows():
                    intersection = line.intersection(region.geometry)
                    if not intersection.is_empty:
                        length = intersection.length
                        weight = length / total_length
                        # Ensure region values are not NaN before multiplying
                        r_day = region.day_ratio if not pd.isna(region.day_ratio) else 0
                        r_night = region.night_ratio if not pd.isna(region.night_ratio) else 0
                        
                        weighted_day += weight * r_day
                        weighted_night += weight * r_night
                        
                day_val = round(weighted_day, 8)
                night_val = round(weighted_night, 8)

        day_matrix[i, j] = day_val
        day_matrix[j, i] = day_val
        night_matrix[i, j] = night_val
        night_matrix[j, i] = night_val

print("Matrices computed.")

stop_ids = stops_df["ID"].tolist()
day_df = pd.DataFrame(day_matrix, index=stop_ids, columns=stop_ids)
night_df = pd.DataFrame(night_matrix, index=stop_ids, columns=stop_ids)

day_df.to_csv("day_matrix2.txt", sep="\t", float_format="%.8f")
night_df.to_csv("night_matrix2.txt", sep="\t", float_format="%.8f")

print("Day and Night matrices saved as TXT files.")