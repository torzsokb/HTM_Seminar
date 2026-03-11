from pathlib import Path
import pandas as pd

# Paths
solution_file = Path("src/results/results_Balanced_TSP_0.003_0.001.csv")
scenario_file = Path("data/inputs/stops_with_cleaning_times.csv")
output_dir = Path("src/core/scenario_instances/golden_scenario_csvs")
output_dir.mkdir(parents=True, exist_ok=True)

# Load files
solution = pd.read_csv(solution_file)
scenarios = pd.read_csv(scenario_file)

# Keep only one row per ID_MAXIMO (duplicates have identical values)
scenarios = scenarios.groupby("ID_MAXIMO", as_index=False).first()

# Columns to enforce as integers
int_cols = ["Order", "ID", "Night_shift"]

# -----------------------
# Prepare scenario maps
# -----------------------
scenario_maps = {"summer": scenarios.set_index("ID_MAXIMO")["Summer_cleaning_time"]}
for i in range(1, 11):
    scenario_maps[f"autumn_{i}"] = scenarios.set_index("ID_MAXIMO")[f"Autumn_cleaning_{i}"]

# -----------------------
# Generate all scenario CSVs
# -----------------------
for name, cleaning_map in scenario_maps.items():
    df = solution.copy()
    df["Service_time"] = df["ID_MAXIMO"].map(cleaning_map).fillna(df["Service_time"])

    # Ensure integer columns are written as ints, filling NaN with 0
    for col in int_cols:
        if col in df.columns:
            df[col] = df[col].fillna(0).astype(int)

    # Save
    output_file = output_dir / f"Golden_solution_{name}.csv"
    df.to_csv(output_file, index=False)

    print(f"Saved scenario: {output_file}")