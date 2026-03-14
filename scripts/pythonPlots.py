import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

## Quickly making a new file with the balanced solution for new cleaning time calculations 

# Read the original HTM file
htm = pd.read_csv("data/inputs/HTM_Data_abriTypeStop.csv")

# Read the new route file
balanced = pd.read_csv("src/results/results_Balanced_final_0.002_0.002.csv")

# Keep only the columns needed from the balanced file
balanced_small = balanced[["ID", "Route", "Order"]].copy()

# Rename the new Route and Order columns so they do not clash during merge
balanced_small = balanced_small.rename(columns={
    "Route": "Route_new",
    "Order": "Order_new"
})

# Merge on ID
merged = htm.merge(balanced_small, on="ID", how="left")

# Replace Route and Order by the new ones where available
merged["Route"] = merged["Route_new"].combine_first(merged["Route"])
merged["Order"] = merged["Order_new"].combine_first(merged["Order"])

# Drop temporary columns
merged = merged.drop(columns=["Route_new", "Order_new"])

# Save the new file
merged.to_csv("data/inputs/HTM_data_balancedVersion_0.002.csv", index=False)

print("New file saved as HTM_data_balancedVersion_0.002.csv")

##### TEMPERATURES FIGURE FOR THE SA ##### 
temps_example = np.loadtxt("src/results/results_SA_example_alltemps.txt")
iterations = np.arange(len(temps_example))

# Window for smooth curves, must be odd, increase for extra smoothness
window = 51  
window = min(window, len(temps_example) if len(temps_example) % 2 == 1 else len(temps_example) - 1)  
if window < 3:
    temps_example_smooth = temps_example
else:
    kernel = np.ones(window) / window
    temps_example_smooth = np.convolve(temps_example, kernel, mode="same")


plt.plot(iterations, temps_example_smooth, color="#D81B60")
plt.xlabel("Iteration")
plt.ylabel("Temperature")

plt.ylim(bottom=0.0)
plt.xlim(left=0, right=len(temps_example))

# Save
plt.tight_layout()
plt.savefig("Figures/Figure_SAtemperatures_example.png", dpi=300)

plt.show()

##### TEMPERATURES vs OBJECTIVES  FIGURE FOR THE SA #####
temps = np.loadtxt("src/results/results_SA_feasible_alltemps.txt")
obj = np.loadtxt("src/results/results_SA_feasible_allobj.txt")
iterations_temps = np.arange(len(temps))

# Window for smooth curves, must be odd, increase for extra smoothness
window = 51  
window = min(window, len(temps) if len(temps) % 2 == 1 else len(temps) - 1)  
if window < 3:
    temps_smooth = temps
else:
    kernel = np.ones(window) / window
    temps_smooth = np.convolve(temps, kernel, mode="same")

iterationsObj = np.arange(len(obj))

# Window for smooth curves, must be odd, increase for extra smoothness
windowObj = 51  
windowObj = min(windowObj, len(obj) if len(obj) % 2 == 1 else len(obj) - 1)  
if windowObj < 3:
    obj_smooth = obj
else:
    kernel = np.ones(windowObj) / windowObj
    obj_smooth = np.convolve(obj, kernel, mode="same")

fig, axes = plt.subplots(1, 2, figsize=(12, 5))

axes[0].plot(iterations_temps, temps_smooth, color="#D81B60")
axes[0].set_xlabel("Iteration")
axes[0].set_ylabel("Temperature")

axes[0].set_ylim(bottom=0.0)
axes[0].set_xlim(left=0, right=len(temps))

axes[1].plot(iterationsObj, obj, color="#1E88E5")
axes[1].set_xlabel("Iteration")
axes[1].set_ylabel("Objective function")

#axes[1].set_ylim(bottom=0.0)
axes[1].set_xlim(left=0, right=len(obj))

# Save
plt.tight_layout()
plt.savefig("Figures/Figure_SA_tempvsobj_final.png", dpi=300)

plt.show()

fig, ax1 = plt.subplots(figsize=(8, 5))

# Temperature on left y-axis
ax1.plot(iterations_temps, temps_smooth, color="#D81B60", label="Temperature")
ax1.set_xlabel("Iteration")
ax1.set_ylabel("Temperature")
ax1.tick_params(axis="y")
ax1.set_ylim(bottom=0.0)
ax1.set_xlim(0, len(temps)-1)

# Objective on right y-axis
ax2 = ax1.twinx()
ax2.plot(iterationsObj, obj, color="#1E88E5", label="Objective")
ax2.set_ylabel("Objective")
ax2.tick_params(axis="y")
ax2.set_xlim(0, len(obj)-1)

# One combined legend
lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc="upper right")

plt.tight_layout()
plt.savefig("Figures/Figure_SA_tempvsobj_overlap_final.png", dpi=300)
plt.show()

##### DISTRIBUTION TIMES BALANCED VS NON-BALANCED #####

# Load data
sa = pd.read_csv("src/results/SA_stats_best.csv")
bal = pd.read_csv("src/results/Balanced_stats_TSP.csv")

# Create figure with 2 plots
fig, axes = plt.subplots(1, 2, figsize=(12, 5))

# ---- Plot 1: Total shift length ----
axes[0].hist(sa["totalLength"], bins=20, alpha=0.5, label="VND",  color = "#D81B60", density = True)
axes[0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", color = "#1E88E5", density = True)
axes[0].set_title("Shift length distribution")
axes[0].set_xlabel("Hours")
axes[0].set_ylabel("Density")
axes[0].legend()

# ---- Plot 2: Cleaning time ----
axes[1].hist(sa["totalCleaning"], bins=20, alpha=0.5, label="VND",  color = "#D81B60", density = True)
axes[1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced",  color = "#1E88E5", density = True)
axes[1].set_title("Cleaning time distribution")
axes[1].set_xlabel("Hours")
axes[1].set_ylabel("Density")
axes[1].legend()

# Layout + save
plt.tight_layout()
plt.savefig("Figures/Figure_distribution_LNSSAvsBalanced_finalversion.png", dpi=300)

plt.show()

##### DISTRIBUTION TIMES BALANCED VS NON-BALANCED VS INITIAL #####
# Load data
sa = pd.read_csv("src/results/SA_stats_best.csv")
bal = pd.read_csv("src/results/Balanced_stats_TSP.csv")
init = pd.read_csv("src/results/init_stats_feasible.csv")

######## Define fixed bins ########
# For totalLength
all_lengths = pd.concat([sa["totalLength"], bal["totalLength"], init["totalLength"]])
length_bins = np.arange(all_lengths.min(), all_lengths.max() + 0.25, 0.25)  
# 0.25 = bin width in hours, change as needed

# For totalCleaning
all_cleaning = pd.concat([sa["totalCleaning"], bal["totalCleaning"], init["totalCleaning"]])
cleaning_bins = np.arange(all_cleaning.min(), all_cleaning.max() + 0.25, 0.25)

######## Create figure with 4 plots ########
fig, axes = plt.subplots(2, 2, figsize=(10, 6))

# ---- Plot 1: Total shift length ----
axes[0,0].hist(sa["totalLength"], bins=length_bins, alpha=0.5, label="VND",  color = "#D81B60", density = False)
axes[0,0].hist(bal["totalLength"], bins=length_bins, alpha=0.5, label="Balanced", color = "#1E88E5", density = False)
axes[0,0].set_title("Shift length distribution")
axes[0,0].set_ylabel("Number of shifts")
axes[0,0].legend()

# ---- Plot 2: Cleaning time ----
axes[0,1].hist(sa["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="VND",  color = "#D81B60", density = False)
axes[0,1].hist(bal["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="Balanced",  color = "#1E88E5", density = False)
axes[0,1].set_title("Cleaning time distribution")
axes[0,1].set_ylabel("Number of shifts")
axes[0,1].legend()

# ---- Plot 1: Total shift length ----
axes[1,0].hist(init["totalLength"], bins=length_bins, alpha=0.5, label="HTM", color = "#FFC107", density = False)
axes[1,0].hist(bal["totalLength"], bins=length_bins, alpha=0.5, label="Balanced", color = "#1E88E5", density = False)
axes[1,0].set_xlabel("Hours")
axes[1,0].set_ylabel("Number of shifts")
axes[1,0].legend()

# ---- Plot 2: Cleaning time ----
axes[1,1].hist(init["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="HTM", color = "#FFC107", density = False)
axes[1,1].hist(bal["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="Balanced",  color = "#1E88E5", density = False)
axes[1,1].set_xlabel("Hours")
axes[1,1].set_ylabel("Number of shifts")
axes[1,1].legend()

# Layout + save
plt.tight_layout()
plt.savefig("Figures/Figure_distribution_all_finalversion_BINS.png", dpi=300)

plt.show()

##### DISTRIBUTION TIMES VND_min VS VND VS INITIAL #####
# Load data
init = pd.read_csv("src/results/init_stats_feasible.csv")
sa = pd.read_csv("src/results/SA_stats_best.csv")
mi = pd.read_csv("src/results/MinShifts_stats.csv")

######## Define fixed bins ########
# For totalLength
all_lengths = pd.concat([sa["totalLength"], mi["totalLength"], init["totalLength"]])
length_bins = np.arange(all_lengths.min(), all_lengths.max() + 0.25, 0.25)  
# 0.25 = bin width in hours, change as needed

# For totalCleaning
all_cleaning = pd.concat([sa["totalCleaning"], mi["totalCleaning"], init["totalCleaning"]])
cleaning_bins = np.arange(all_cleaning.min(), all_cleaning.max() + 0.25, 0.25)

######## Create figure with 4 plots ########
fig, axes = plt.subplots(2, 2, figsize=(10, 6))

# ---- Plot 1: Total shift length ----
axes[0,0].hist(sa["totalLength"], bins=length_bins, alpha=0.5, label="VND",  color = "#D81B60", density = True)
axes[0,0].hist(mi["totalLength"], bins=length_bins, alpha=0.5, label="VND_min", color = "#009E73", density = True)
axes[0,0].set_title("Shift length distribution")
axes[0,0].set_ylabel("Density")
axes[0,0].legend()

# ---- Plot 2: Cleaning time ----
axes[0,1].hist(sa["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="VND",  color = "#D81B60", density = True)
axes[0,1].hist(mi["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="VND_min",  color = "#009E73", density = True)
axes[0,1].set_title("Cleaning time distribution")
axes[0,1].set_ylabel("Density")
axes[0,1].legend()

# ---- Plot 1: Total shift length ----
axes[1,0].hist(init["totalLength"], bins=length_bins, alpha=0.5, label="HTM", color = "#FFC107", density = True)
axes[1,0].hist(mi["totalLength"], bins=length_bins, alpha=0.5, label="VND_min", color = "#009E73", density = True)
axes[1,0].set_xlabel("Hours")
axes[1,0].set_ylabel("Density")
axes[1,0].legend()

# ---- Plot 2: Cleaning time ----
axes[1,1].hist(init["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="HTM", color = "#FFC107", density = True)
axes[1,1].hist(mi["totalCleaning"], bins=cleaning_bins, alpha=0.5, label="VND_min",  color = "#009E73", density = True)
axes[1,1].set_xlabel("Hours")
axes[1,1].set_ylabel("Density")
axes[1,1].legend()

# Layout + save
plt.tight_layout()
plt.savefig("Figures/Figure_distribution_minShifts_finalversion_4x4.png", dpi=300)

plt.show()

