import pandas as pd
import folium
import webbrowser
from pathlib import Path
from itertools import cycle

def plot_routes_from_csv(csv_path, output_html="routes_map.html", route_number = "Route", order_number = "Order", depot_name="Depot", zoom_start=12):
    df = pd.read_csv(csv_path)
    
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
        tiles="OpenStreetMap"
    )
    # If we want more/other colours, change this
    colors = cycle(
        ["red", "blue", "green", "purple", "orange", "darkred", "cadetblue"]
    )

    for route_id, route_df in df.groupby("Route"):
        route_df = route_df.sort_values("Order")
        color = next(colors)

        coords = list(zip(route_df.latitude, route_df.longitude))

        # Automatically starts and end at depot for plotting
        if route_df.iloc[0].Name != depot_name:
            coords.insert(0, (depot.latitude, depot.longitude))
        if route_df.iloc[-1].Name != depot_name:
            coords.append((depot.latitude, depot.longitude))

        folium.PolyLine(
            locations=coords,
            color=color,
            weight=4,
            opacity=0.8,
            tooltip=f"Route {route_id}"
        ).add_to(m)

        for _, row in route_df.iterrows():
            folium.CircleMarker(
                location=[row.latitude, row.longitude],
                radius=6 if row.Name == depot_name else 4,
                color=color,
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

def main():
    csv_path = "data/inputs/cleaned/HTM_CollapsedDatav2.csv"
    plot_routes_from_csv(csv_path)

if __name__ == "__main__":
    main()
