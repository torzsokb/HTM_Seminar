import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

##### TEMPERATURES FIGURE FOR THE SA #####
temps = np.loadtxt("src/results/results_SA_feasible_alltemps.txt")
iterations = np.arange(len(temps))

# Window for smooth curves, must be odd, increase for extra smoothness
window = 51  
window = min(window, len(temps) if len(temps) % 2 == 1 else len(temps) - 1)  
if window < 3:
    temps_smooth = temps
else:
    kernel = np.ones(window) / window
    temps_smooth = np.convolve(temps, kernel, mode="same")


plt.plot(iterations, temps_smooth, color="#fe44ba")
plt.xlabel("Iteration")
plt.ylabel("Temperature")

plt.ylim(bottom=0.0)
plt.xlim(left=0, right=len(temps))

# Save
# plt.tight_layout()
# plt.savefig("Figures/Figure_SAtemperatures.png", dpi=300)

plt.show()

##### TEMPERATURES vs OBJECTIVES  FIGURE FOR THE SA #####
obj = np.loadtxt("src/results/results_SA_feasible_allobj.txt")
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

axes[0].plot(iterations, temps_smooth, color="#fe44ba")
axes[0].set_xlabel("Iteration")
axes[0].set_ylabel("Temperature")

axes[0].set_ylim(bottom=0.0)
axes[0].set_xlim(left=0, right=len(temps))

axes[1].plot(iterationsObj, obj, color="#b744fe")
axes[1].set_xlabel("Iteration")
axes[1].set_ylabel("Objective function")

#axes[1].set_ylim(bottom=0.0)
axes[1].set_xlim(left=0, right=len(obj))

# Save
plt.tight_layout()
plt.savefig("Figures/Figure_SA_tempvsobj.png", dpi=300)

plt.show()

fig, ax1 = plt.subplots(figsize=(12, 5))

# Temperature on left y-axis
ax1.plot(iterations, temps_smooth, color="#fe44ba", label="Temperature")
ax1.set_xlabel("Iteration")
ax1.set_ylabel("Temperature")
ax1.tick_params(axis="y")
ax1.set_ylim(bottom=0.0)
ax1.set_xlim(0, len(temps)-1)

# Objective on right y-axis
ax2 = ax1.twinx()
ax2.plot(iterationsObj, obj, color="#b744fe", label="Objective")
ax2.set_ylabel("Objective")
ax2.tick_params(axis="y")
ax2.set_xlim(0, len(obj)-1)

# One combined legend
lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc="upper right")

plt.tight_layout()
plt.savefig("Figures/Figure_SA_tempvsobj_overlap.png", dpi=300)
plt.show()

##### DISTRIBUTION TIMES BALANCED VS NON-BALANCED #####
# Load data
sa = pd.read_csv("src/results/SA_stats_feasible.csv")
bal = pd.read_csv("src/results/Balanced_stats_feasible.csv")

# Create figure with 2 plots
fig, axes = plt.subplots(1, 2, figsize=(12, 5))

# ---- Plot 1: Total shift length ----
axes[0].hist(sa["totalLength"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", color = "#d98fea", density = True)
axes[0].set_title("Shift length distribution")
axes[0].set_xlabel("Hours")
axes[0].set_ylabel("Density")
axes[0].legend()

# ---- Plot 2: Cleaning time ----
axes[1].hist(sa["totalCleaning"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced",  color = "#d98fea", density = True)
axes[1].set_title("Cleaning time distribution")
axes[1].set_xlabel("Hours")
axes[1].set_ylabel("Density")
axes[1].legend()

# Layout + save
# plt.tight_layout()
# plt.savefig("Figures/Figure_distribution_LNSSAvsBalanced_draftversion.png", dpi=300)

#plt.show()

##### DISTRIBUTION TIMES BALANCED VS NON-BALANCED VS INITIAL #####
# Load data
init = pd.read_csv("src/results/init_stats_feasible.csv")

# Create figure with 2 plots
fig, axes = plt.subplots(1, 2, figsize=(12, 5))

# ---- Plot 1: Total shift length ----
axes[0].hist(sa["totalLength"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", color = "#d98fea", density = True)
axes[0].hist(init["totalLength"], bins=20, alpha=0.5, label="HTM", color = "#bdf1f6", density = True)
axes[0].set_title("Shift length distribution")
axes[0].set_xlabel("Hours")
axes[0].set_ylabel("Density")
axes[0].legend()

# ---- Plot 2: Cleaning time ----
axes[1].hist(sa["totalCleaning"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced",  color = "#d98fea", density = True)
axes[1].hist(init["totalCleaning"], bins=20, alpha=0.5, label="HTM", color = "#bdf1f6", density = True)
axes[1].set_title("Cleaning time distribution")
axes[1].set_xlabel("Hours")
axes[1].set_ylabel("Density")
axes[1].legend()

# Layout + save
# plt.tight_layout()
# plt.savefig("Figures/Figure_distribution_all_draftversion.png", dpi=300)

#plt.show()

######## Create figure with 4 plots ########
fig, axes = plt.subplots(2, 2, figsize=(10, 6))

# ---- Plot 1: Total shift length ----
axes[0,0].hist(sa["totalLength"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[0,0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", color = "#d98fea", density = True)
axes[0,0].set_title("Shift length distribution")
axes[0,0].set_ylabel("Density")
axes[0,0].legend()

# ---- Plot 2: Cleaning time ----
axes[0,1].hist(sa["totalCleaning"], bins=20, alpha=0.5, label="VND",  color = "#fe72cb", density = True)
axes[0,1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced",  color = "#d98fea", density = True)
axes[0,1].set_title("Cleaning time distribution")
axes[0,1].set_ylabel("Density")
axes[0,1].legend()

# ---- Plot 1: Total shift length ----
axes[1,0].hist(init["totalLength"], bins=20, alpha=0.5, label="HTM", color = "#7ee8f2", density = True)
axes[1,0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", color = "#d98fea", density = True)
axes[1,0].set_xlabel("Hours")
axes[1,0].set_ylabel("Density")
axes[1,0].legend()

# ---- Plot 2: Cleaning time ----
axes[1,1].hist(init["totalCleaning"], bins=20, alpha=0.5, label="HTM", color = "#7ee8f2", density = True)
axes[1,1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced",  color = "#d98fea", density = True)
axes[1,1].set_xlabel("Hours")
axes[1,1].set_ylabel("Density")
axes[1,1].legend()

# Layout + save
# plt.tight_layout()
# plt.savefig("Figures/Figure_distribution_all_draftversion_4x4.png", dpi=300)

#plt.show()