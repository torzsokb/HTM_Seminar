import geopandas as gpd
import pandas as pd
import numpy as np
from shapely.geometry import LineString

# -------------------------
# Load regions
# -------------------------
regions = gpd.read_file("day_night_ratios.geojson").to_crs(epsg=28992)

# Ensure regions are disjoint (same as your original code)
regions = regions.dissolve(by=regions.index, aggfunc='first').reset_index(drop=True)

# Create region ids
regions["region_id"] = regions.index

region_sindex = regions.sindex
region_ids = regions["region_id"].tolist()

# -------------------------
# Load stops
# -------------------------
stops_df = pd.read_csv("src/core/data_all_feas_typeHalte.txt", sep="\t")

stops_gdf = gpd.GeoDataFrame(
    stops_df,
    geometry=gpd.points_from_xy(stops_df.longitude, stops_df.latitude),
    crs="EPSG:4326"
).to_crs(epsg=28992)

stops_gdf = stops_gdf.reset_index(drop=True)
n = len(stops_gdf)

print("Stops:", n)
print("Regions:", len(regions))

# -------------------------
# Storage
# -------------------------
rows = []

# -------------------------
# Compute weights
# -------------------------
for i in range(n):

    p1 = stops_gdf.geometry.iloc[i]
    id1 = stops_df["ID"].iloc[i]

    for j in range(i + 1, n):

        print("i", i, "j", j)

        p2 = stops_gdf.geometry.iloc[j]
        id2 = stops_df["ID"].iloc[j]

        line = LineString([p1, p2])
        total_length = line.length

        weights = {f"region_{rid}": 0.0 for rid in region_ids}

        if total_length > 0:

            weighted_lengths = {rid: 0.0 for rid in region_ids}
            covered_length = 0

            possible_matches_index = list(region_sindex.intersection(line.bounds))
            possible_regions = regions.iloc[possible_matches_index]

            for _, region in possible_regions.iterrows():

                intersection = line.intersection(region.geometry)

                if not intersection.is_empty:

                    length = intersection.length
                    covered_length += length

                    rid = region.region_id
                    weighted_lengths[rid] += length

            # Normalize exactly like your original code
            if covered_length > 0:

                for rid in region_ids:
                    weights[f"region_{rid}"] = round(
                        weighted_lengths[rid] / covered_length, 8
                    )

        row = {
            "from": id1,
            "to": id2,
            **weights
        }

        rows.append(row)

# -------------------------
# Save CSV
# -------------------------
weights_df = pd.DataFrame(rows)

weights_df.to_csv("region_weights_per_stop_pair.csv", index=False)

print("CSV saved: region_weights_per_stop_pair.csv")