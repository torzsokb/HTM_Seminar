import pandas as pd
import folium
import folium.plugins
import webbrowser
from pathlib import Path
from itertools import cycle
import requests
from pydantic import BaseModel
import numpy as np

OSRM_BASE_URL = "http://localhost:5001" 

def plot_routes_from_csv(csv_path, 
                        output_html="routes_map.html", 
                        route_number = "Route", 
                        order_number = "Order", 
                        depot_name="Depot", 
                        zoom_start=12, 
                        split_day_and_night = False, 
                        OSRM = True, 
                        show_only = "All",
                        folium_map_background = "Black_White"):
    df = pd.read_csv(csv_path)

    depot_stop = df.iloc[0]

    required_cols = {route_number, order_number, "latitude", "longitude"}
    missing = required_cols - set(df.columns)
    if missing:
        raise ValueError(f"Bro you don't have the correct header names, namely: {missing}")

    df = df.sort_values([route_number, order_number])

    depot_row = df[df["ID_MAXIMO"] == depot_name]
    if depot_row.empty:
        raise ValueError(f"Bro, get the depot name straight == '{depot_name}'")

    depot = depot_row.iloc[0]

    m = folium.Map(
        location=[depot.latitude, depot.longitude],
        zoom_start=zoom_start,
        tiles= "CartoDB Positron" if folium_map_background == "Black_White" else "OpenStreetMap"
    )
    # If we want more/other colours, change this
    if split_day_and_night == True:
        day_colors = cycle([
        "#43F602",
        "#468C42",  
        "#79BC8CFF", 
        "#207129",  
        "#6FD974"   
        ])
        night_colors = cycle([
            "#000099",  
            "#3030A1FF",
            "#4682B4",
            "#111660",  
            "#06283D" 
        ])
    
    else:
        colors = cycle([
            "#990f26", "#b33e52", "#cc7a88", "#e6b8bf", "#99600f", "#b3823e", "#ccaa7a", "#e6d2b8", "#54990f", "#78b33e",
            "#a3cc7a", "#cfe6b8", "#0f8299", "#3e9fb3", "#7abecc", "#b8dee6", "#3d0f99", "#653eb3", "#967acc", "#c7b8e6",
            "#333333", "#666666", "#999999", "#882d71", "#ffed21"
        ])
    
    for route_id, route_df in df.groupby("Route"):
        route_df = route_df.sort_values("Order")

        if show_only == "Day":
            if route_df["Night_shift"].iloc[0] != 0:
                continue
        elif show_only == "Night":
            if route_df["Night_shift"].iloc[0] != 1:
                continue

        # if route_df[route_number].iloc[0] not in ("REI-D304"):
            # continue

        if split_day_and_night == True:
            is_night = route_df["Night_shift"].iloc[0] == 1
            color = next(night_colors) if is_night else next(day_colors)
        else:
            color = next(colors)


        # Automatically starts and end at depot for plotting
        if route_df.iloc[0].Name != depot_name:
            route_df = pd.concat([depot_stop.to_frame().T, route_df], ignore_index=True)

        if route_df.iloc[-1].Name != depot_name:
            route_df = pd.concat([route_df, depot_stop.to_frame().T], ignore_index=True)
        
        coords = list(zip(route_df.latitude, route_df.longitude))
        
        if OSRM:
            coords = osrm_route(coords)

        folium.PolyLine(
            locations=coords,
            color=color,
            weight=4,
            opacity=0.8,
            tooltip=f"Route {route_id}"
        ).add_to(m)

        # folium.plugins.AntPath(locations=coords, reverse="True", dash_array=[20, 30]).add_to(m)

        for _, row in route_df.iterrows():
            is_depot = row["ID_MAXIMO"] == depot_name

            folium.CircleMarker(
                location=[row.latitude, row.longitude],
                radius=5 if is_depot else 4,
                color = "red" if is_depot else color,
                fill=True,
                fill_opacity=0.9,
                popup=(
                    f"<b>{row.Name}</b><br>"
                    f"Route: {row.Route}<br>"
                    f"Order: {row.Order}"
                )
            ).add_to(m)

    m.fit_bounds([
        [df.latitude.min(), df.longitude.min()],
        [df.latitude.max(), df.longitude.max()]
    ])

    output_path = Path(output_html).resolve()
    m.save(output_path)
    webbrowser.open(output_path.as_uri())

    return output_path

def osrm_route(points, host=OSRM_BASE_URL, profile="driving"):
    coordinates = ";".join(f"{lon},{lat}" for lat, lon in points)
    url = (
        f"{host}/route/v1/{profile}/"
        f"{coordinates}?overview=full&geometries=geojson"
        )

    response = requests.get(url)
    response.raise_for_status()

    data = response.json()
    geometry = data["routes"][0]["geometry"]["coordinates"]

    return [[lat, lon] for lon, lat in geometry]

def main():
    csv_path = "data/inputs/cleaned/HTM_CollapsedDatav2.csv"
    plot_routes_from_csv(csv_path, split_day_and_night=False, OSRM=True, show_only= "Night")

if __name__ == "__main__":
    main()