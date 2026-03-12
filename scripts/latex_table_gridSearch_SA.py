import pandas as pd

# Load data
df = pd.read_csv("src/results/grid_search_SA/grid_search_summary.csv")

# Round values for readability
df["finalObjective"] = df["finalObjective"].round(3)

osc_values = sorted(df["osc"].unique())
max_iters = sorted(df["maxIter"].unique())
T0_values = sorted(df["T0"].unique())

latex = []
latex.append("\\begin{table}[ht]")
latex.append("\\centering")
latex.append("\\begin{tabular}{ccc" + "c"*len(osc_values) + "}")
latex.append("\\toprule")
latex.append("maxIter & $T_0$ & Metric " +
             " & ".join([f"osc={o}" for o in osc_values]) +
             " \\\\")
latex.append("\\midrule")

for max_iter in max_iters:
    subset_iter = df[df["maxIter"] == max_iter]

    latex.append(f"\\multicolumn{{{3+len(osc_values)}}}{{c}}{{\\textbf{{maxIter = {max_iter}}}}} \\\\")
    latex.append("\\midrule")

    for T0 in T0_values:
        row = subset_iter[subset_iter["T0"] == T0]

        values = []
        for osc in osc_values:
            val = row[row["osc"] == osc]["finalObjective"].values
            values.append(f"{val[0]:.3f}" if len(val) else "-")

        latex.append(
            f"{max_iter} & {T0} & finalObj & " +
            " & ".join(values) +
            " \\\\"
        )

    latex.append("\\midrule")

latex.append("\\bottomrule")
latex.append("\\end{tabular}")
latex.append("\\caption{Grid search results for different $T_0$, oscillation parameters, and iteration limits.}")
latex.append("\\label{tab:gridsearch}")
latex.append("\\end{table}")

print("\n".join(latex))