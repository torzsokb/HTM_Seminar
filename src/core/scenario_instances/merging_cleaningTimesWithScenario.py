from pathlib import Path
import pandas as pd

# -----------------------
# Solution files
# -----------------------
solutions = {
    "balanced": Path("data/results/Balanced.csv"),
    "htm": Path("data/results/HTM.csv"),
    "tsp": Path("data/results/TSP.csv"),
    "vnd": Path("data/results/VND.csv"),
    "vnd_min": Path("data/results/VNDmin.csv"),
}

# Scenario input files
summer_file = Path("data/inputs/cleaning_time_summer_scenarios.csv")
autumn_file = Path("data/inputs/cleaning_time_autumn_scenarios.csv")

# Base output folder
base_output_dir = Path("src/core/scenario_instances")

# Columns to enforce as integers
int_cols = ["Order", "ID", "Night_shift"]

# -----------------------
# Load scenario tables
# -----------------------
summer_df = pd.read_csv(summer_file)
autumn_df = pd.read_csv(autumn_file)

summer_df = summer_df.groupby("ID_MAXIMO", as_index=False).first()
autumn_df = autumn_df.groupby("ID_MAXIMO", as_index=False).first()

# -----------------------
# Build scenario maps
# -----------------------
scenario_maps = {}

# Summer scenarios
for col in summer_df.columns:
    if col.startswith("Summer_cleaning_time_fixed"):
        scenario_maps[col] = summer_df.set_index("ID_MAXIMO")[col]

# Autumn scenarios
for col in autumn_df.columns:
    if col.startswith("Autumn_cleaning"):
        scenario_maps[col] = autumn_df.set_index("ID_MAXIMO")[col]

# -----------------------
# Generate scenarios
# -----------------------
for solution_name, solution_path in solutions.items():

    solution = pd.read_csv(solution_path)

    # Create solution-specific folder
    solution_dir = base_output_dir / solution_name.upper()
    solution_dir.mkdir(parents=True, exist_ok=True)

    for scenario_name, cleaning_map in scenario_maps.items():

        df = solution.copy()

        df["Service_time"] = (
            df["ID_MAXIMO"]
            .map(cleaning_map)
            .fillna(df["Service_time"])
        )

        # enforce integer columns
        for col in int_cols:
            if col in df.columns:
                df[col] = df[col].fillna(0).astype(int)

        output_file = solution_dir / f"{scenario_name}.csv"

        df.to_csv(output_file, index=False)

    print(f"{solution_name}: {len(scenario_maps)} scenarios saved to {solution_dir}")