import pandas as pd
import folium
import webbrowser
from pathlib import Path
from itertools import cycle
import requests

OSRM_BASE_URL = "http://localhost:5001"


class RouteMapComparer:
    """
    Compare two route-result CSVs and create two maps:
      1) Routes whose stop-sequence exists in A but not in B
      2) Routes whose stop-sequence exists in B but not in A

    Based directly on your plotting logic (folium polylines + circle markers).
    """

    def __init__(self, depot_name="Depot", osrm_base_url=OSRM_BASE_URL):
        self.depot_name = depot_name
        self.osrm_base_url = osrm_base_url

    # ---------- Public API ----------

    def compare_and_plot(
        self,
        csv_a: str,
        csv_b: str,
        output_prefix: str = "compare_routes",
        route_col: str = "Route",
        order_col: str = "Order",
        id_col: str = "ID_MAXIMO",
        lat_col: str = "latitude",
        lon_col: str = "longitude",
        zoom_start: int = 12,
        split_day_and_night: bool = False,
        osrm: bool = False,
        show_only: str = "All",  # "All" | "Day" | "Night"
        folium_map_background: str = "Black_White",  # "Black_White" | other -> OSM
        ignore_depot_in_signature: bool = True,
        open_in_browser: bool = True,
    ):
        """
        Returns:
            (path_only_in_a, path_only_in_b)
        """
        only_in_a_routes, only_in_b_routes, _, _ = self.compute_route_differences(
            csv_a=csv_a,
            csv_b=csv_b,
            route_col=route_col,
            order_col=order_col,
            id_col=id_col,
            depot_name=self.depot_name,
            ignore_depot=ignore_depot_in_signature,
        )

        out_a = Path(f"{output_prefix}_only_in_A.html").resolve()
        out_b = Path(f"{output_prefix}_only_in_B.html").resolve()

        title_a = f"Only in A: {Path(csv_a).name} (not in {Path(csv_b).name})"
        title_b = f"Only in B: {Path(csv_b).name} (not in {Path(csv_a).name})"

        self.plot_routes_from_csv(
            csv_path=csv_a,
            output_html=str(out_a),
            map_title=title_a,
            route_col=route_col,
            order_col=order_col,
            id_col=id_col,
            lat_col=lat_col,
            lon_col=lon_col,
            zoom_start=zoom_start,
            split_day_and_night=split_day_and_night,
            osrm=osrm,
            show_only=show_only,
            folium_map_background=folium_map_background,
            routes_to_plot=set(only_in_a_routes),
            open_in_browser=open_in_browser,
        )

        self.plot_routes_from_csv(
            csv_path=csv_b,
            output_html=str(out_b),
            map_title=title_b,
            route_col=route_col,
            order_col=order_col,
            id_col=id_col,
            lat_col=lat_col,
            lon_col=lon_col,
            zoom_start=zoom_start,
            split_day_and_night=split_day_and_night,
            osrm=osrm,
            show_only=show_only,
            folium_map_background=folium_map_background,
            routes_to_plot=set(only_in_b_routes),
            open_in_browser=open_in_browser,
        )

        return out_a, out_b

    # ---------- Core logic (diffing) ----------

    def compute_route_differences(
        self,
        csv_a: str,
        csv_b: str,
        route_col: str = "Route",
        order_col: str = "Order",
        id_col: str = "ID_MAXIMO",
        depot_name: str = "Depot",
        ignore_depot: bool = True,
    ):
        """
        Compare by stop-sequence signature (structure), not by route IDs.

        Returns:
          only_in_a_routes: list[route_id]
          only_in_b_routes: list[route_id]
          a_to_sig: dict[route_id -> signature]
          b_to_sig: dict[route_id -> signature]
        """
        df_a = pd.read_csv(csv_a)
        df_b = pd.read_csv(csv_b)

        a_to_sig = {
            rid: self._route_signature(rdf, order_col, id_col, depot_name, ignore_depot)
            for rid, rdf in df_a.groupby(route_col)
        }
        b_to_sig = {
            rid: self._route_signature(rdf, order_col, id_col, depot_name, ignore_depot)
            for rid, rdf in df_b.groupby(route_col)
        }

        sigs_a = set(a_to_sig.values())
        sigs_b = set(b_to_sig.values())

        only_in_a_sigs = sigs_a - sigs_b
        only_in_b_sigs = sigs_b - sigs_a

        only_in_a_routes = [rid for rid, sig in a_to_sig.items() if sig in only_in_a_sigs]
        only_in_b_routes = [rid for rid, sig in b_to_sig.items() if sig in only_in_b_sigs]

        return only_in_a_routes, only_in_b_routes, a_to_sig, b_to_sig

    def _route_signature(
        self,
        route_df: pd.DataFrame,
        order_col: str,
        id_col: str,
        depot_name: str,
        ignore_depot: bool,
    ):
        rdf = route_df.sort_values(order_col)
        ids = rdf[id_col].tolist()
        if ignore_depot:
            ids = [x for x in ids if x != depot_name]
        return tuple(ids)

    # ---------- Plotting (based on your function) ----------

    def plot_routes_from_csv(
        self,
        csv_path: str,
        output_html: str = "routes_map.html",
        map_title: str = "Routes map",
        route_col: str = "Route",
        order_col: str = "Order",
        id_col: str = "ID_MAXIMO",
        lat_col: str = "latitude",
        lon_col: str = "longitude",
        zoom_start: int = 12,
        split_day_and_night: bool = False,
        osrm: bool = False,
        show_only: str = "All",
        folium_map_background: str = "Black_White",
        routes_to_plot=None,  # set/list of route IDs
        open_in_browser: bool = True,
    ):
        df = pd.read_csv(csv_path)

        required_cols = {route_col, order_col, lat_col, lon_col, id_col}
        missing = required_cols - set(df.columns)
        if missing:
            raise ValueError(f"Missing required columns: {missing}")

        df = df.sort_values([route_col, order_col])

        depot_row = df[df[id_col] == self.depot_name]
        if depot_row.empty:
            raise ValueError(f"Depot not found: {id_col} == '{self.depot_name}'")

        depot = depot_row.iloc[0]
        depot_stop = depot_row.iloc[0]  # used for auto-start/end in plotting

        tiles = "CartoDB Positron" if folium_map_background == "Black_White" else "OpenStreetMap"

        m = folium.Map(
            location=[depot[lat_col], depot[lon_col]],
            zoom_start=zoom_start,
            tiles=tiles,
        )

        # Title/header overlay (simple HTML box)
        header_html = f"""
        <div style="
            position: fixed;
            top: 10px; left: 10px;
            z-index: 9999;
            background: rgba(255,255,255,0.9);
            padding: 10px 12px;
            border: 1px solid #ccc;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.15);
            font-family: Arial, sans-serif;
            max-width: 40%;
        ">
            <div style="font-size: 16px; font-weight: 700; margin-bottom: 4px;">
                {map_title}
            </div>
            <div style="font-size: 12px; color: #444;">
                File: {Path(csv_path).name} &nbsp;|&nbsp;
                Showing: {("ALL" if routes_to_plot is None else f"{len(routes_to_plot)} routes")}
            </div>
        </div>
        """
        m.get_root().html.add_child(folium.Element(header_html))

        # Color setup (your original approach)
        if split_day_and_night:
            day_colors = cycle(["#43F602", "#468C42", "#79BC8CFF", "#207129", "#6FD974"])
            night_colors = cycle(["#000099", "#3030A1FF", "#4682B4", "#111660", "#06283D"])
        else:
            colors = cycle([
                "#990f26", "#b33e52", "#cc7a88", "#e6b8bf",
                "#99600f", "#b3823e", "#ccaa7a", "#e6d2b8",
                "#54990f", "#78b33e", "#a3cc7a", "#cfe6b8",
                "#0f8299", "#3e9fb3", "#7abecc", "#b8dee6",
                "#3d0f99", "#653eb3", "#967acc", "#c7b8e6",
                "#333333", "#666666", "#999999", "#882d71", "#ffed21"
            ])

        # Group and plot
        plotted_any = False

        for route_id, route_df in df.groupby(route_col):
            if routes_to_plot is not None and route_id not in routes_to_plot:
                continue

            route_df = route_df.sort_values(order_col)

            # Optional day/night filtering (requires Night_shift column)
            if show_only in ("Day", "Night"):
                if "Night_shift" not in route_df.columns:
                    raise ValueError("show_only='Day'/'Night' requires a 'Night_shift' column.")
                if show_only == "Day" and route_df["Night_shift"].iloc[0] != 0:
                    continue
                if show_only == "Night" and route_df["Night_shift"].iloc[0] != 1:
                    continue

            if split_day_and_night:
                if "Night_shift" not in route_df.columns:
                    raise ValueError("split_day_and_night=True requires a 'Night_shift' column.")
                is_night = route_df["Night_shift"].iloc[0] == 1
                color = next(night_colors) if is_night else next(day_colors)
            else:
                color = next(colors)

            # Auto start/end at depot for plotting (your original behavior)
            if route_df.iloc[0][id_col] != self.depot_name:
                route_df = pd.concat([depot_stop.to_frame().T, route_df], ignore_index=True)

            if route_df.iloc[-1][id_col] != self.depot_name:
                route_df = pd.concat([route_df, depot_stop.to_frame().T], ignore_index=True)

            coords = list(zip(route_df[lat_col], route_df[lon_col]))
            if osrm:
                coords = self.osrm_route(coords)

            folium.PolyLine(
                locations=coords,
                color=color,
                weight=4,
                opacity=0.8,
                tooltip=f"Route {route_id}"
            ).add_to(m)

            for _, row in route_df.iterrows():
                is_depot = row[id_col] == self.depot_name
                folium.CircleMarker(
                    location=[row[lat_col], row[lon_col]],
                    radius=5 if is_depot else 4,
                    color="red" if is_depot else color,
                    fill=True,
                    fill_opacity=0.9,
                    popup=(
                        f"<b>{row[id_col]}</b><br>"
                        f"Route: {row[route_col]}<br>"
                        f"Order: {row[order_col]}"
                    )
                ).add_to(m)

            plotted_any = True

        # Fit bounds nicely
        if plotted_any:
            m.fit_bounds([
                [df[lat_col].min(), df[lon_col].min()],
                [df[lat_col].max(), df[lon_col].max()]
            ])
        else:
            # Still fit around depot if nothing plotted
            m.fit_bounds([
                [depot[lat_col] - 0.01, depot[lon_col] - 0.01],
                [depot[lat_col] + 0.01, depot[lon_col] + 0.01],
            ])

        output_path = Path(output_html).resolve()
        m.save(output_path)

        if open_in_browser:
            webbrowser.open(output_path.as_uri())

        return output_path

    def osrm_route(self, points, profile="driving"):
        """
        points: list of (lat, lon)
        returns: list of [lat, lon] along OSRM geometry
        """
        coordinates = ";".join(f"{lon},{lat}" for lat, lon in points)
        url = (
            f"{self.osrm_base_url}/route/v1/{profile}/"
            f"{coordinates}?overview=full&geometries=geojson"
        )

        response = requests.get(url)
        response.raise_for_status()

        data = response.json()
        geometry = data["routes"][0]["geometry"]["coordinates"]
        return [[lat, lon] for lon, lat in geometry]


# ---------------- Example usage ----------------
if __name__ == "__main__":
    comparer = RouteMapComparer(depot_name="Depot")

    a = "src/results/results_LS_abri.csv"
    b = "src/results/results_BalancedLS_abri.csv"

    out_a, out_b = comparer.compare_and_plot(
        csv_a=a,
        csv_b=b,
        output_prefix="LS_vs_BalancedLS",
        split_day_and_night=False,
        osrm=False,
        show_only="All",
        folium_map_background="Black_White",
        open_in_browser=True
    )

    print("Generated:")
    print("A-only map:", out_a)
    print("B-only map:", out_b)