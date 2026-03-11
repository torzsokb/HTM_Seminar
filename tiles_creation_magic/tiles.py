import json

keys_to_remove = {"hourly", "monthly", "weekly", "yearly", "tiledData", "tiles"}

def remove_keys(obj):
    if isinstance(obj, dict):
        return {
            k: remove_keys(v)
            for k, v in obj.items()
            if k not in keys_to_remove
        }
    elif isinstance(obj, list):
        return [remove_keys(item) for item in obj]
    else:
        return obj

def add_times_slower(obj):
    if isinstance(obj, dict):
        if "v" in obj and "fv" in obj:
            try:
                if obj["fv"] != 0:
                    ratio = obj["v"] / obj["fv"]
                else:
                    ratio = 1  # avoid division by zero

                obj["times_slower"] = round(min(ratio, 1), 8)

            except TypeError:
                pass

        for value in obj.values():
            add_times_slower(value)

    elif isinstance(obj, list):
        for item in obj:
            add_times_slower(item)


# Load GeoJSON
with open("data/inputs/cleaned/polygons_The_Hague.geojson", "r", encoding="utf-8") as f:
    data = json.load(f)

# Remove unwanted keys
cleaned = remove_keys(data)

# Add ratio
add_times_slower(cleaned)

# Save result
with open("cleaned.geojson", "w", encoding="utf-8") as f:
    json.dump(cleaned, f, indent=2)

print("Done.")

import json

DAY_HOURS = set(range(7, 15))          # 7-14
NIGHT_HOURS = {23, 0, 1, 2, 3, 4, 5, 6}

def extract_hour(hour_string):
    # "MONDAY-13" → 13
    return int(hour_string.split("-")[1])

def compute_day_night_ratios(feature):
    averages = feature["properties"]["timedData"]["average"]

    day_values = []
    night_values = []

    for entry in averages:
        hour = extract_hour(entry["time"])
        ratio = entry.get("times_slower")

        if ratio is None:
            continue

        if hour in DAY_HOURS:
            day_values.append(ratio)
        elif hour in NIGHT_HOURS:
            night_values.append(ratio)

    day_ratio = round(sum(day_values) / len(day_values), 8) if day_values else None
    night_ratio = round(sum(night_values) / len(night_values), 8) if night_values else None

    return day_ratio, night_ratio


# Load your processed geojson (with times_slower already added)
with open("cleaned.geojson", "r", encoding="utf-8") as f:
    data = json.load(f)

new_features = []

for feature in data["features"]:
    day_ratio, night_ratio = compute_day_night_ratios(feature)

    new_feature = {
        "type": "Feature",
        "geometry": feature["geometry"],
        "properties": {
            "name": feature["properties"].get("name"),
            "day_ratio": day_ratio,
            "night_ratio": night_ratio
        }
    }

    new_features.append(new_feature)

new_geojson = {
    "type": "FeatureCollection",
    "features": new_features
}

with open("day_night_ratios.geojson", "w", encoding="utf-8") as f:
    json.dump(new_geojson, f, indent=2)

print("Day/Night ratio file created.")