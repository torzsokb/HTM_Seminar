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

plt.figure(figsize=(12, 4))
plt.plot(iterations, temps_smooth, color="#fe44ba")
plt.xlabel("Iteration")
plt.ylabel("Temperature")
plt.title("SA oscillating temperature")

plt.ylim(bottom=0.0)
plt.xlim(left=0, right=len(temps))

# Save
# plt.tight_layout()
# plt.savefig("Figures/Figure_SAtemperatures.png", dpi=300)

plt.show()

##### DISTRIBUTION TIMES BALANCED VS NON-BALANCED #####
# Load data
sa = pd.read_csv("src/results/SA_stats_feasible.csv")
bal = pd.read_csv("src/results/Balanced_stats_feasible.csv")

# Create figure with 2 plots
fig, axes = plt.subplots(1, 2, figsize=(12, 5))

# ---- Plot 1: Total shift length ----
axes[0].hist(sa["totalLength"], bins=20, alpha=0.5, label="LNS + SA", density = True, color = "#fe72cb")
axes[0].hist(bal["totalLength"], bins=20, alpha=0.5, label="Balanced", density = True, color = "#d98fea")
axes[0].set_title("Shift length distribution")
axes[0].set_xlabel("Hours")
axes[0].set_ylabel("Density")
axes[0].legend()

# ---- Plot 2: Cleaning time ----
axes[1].hist(sa["totalCleaning"], bins=20, alpha=0.5, label="LNS + SA", density = True, color = "#fe72cb")
axes[1].hist(bal["totalCleaning"], bins=20, alpha=0.5, label="Balanced", density = True, color = "#d98fea")
axes[1].set_title("Cleaning time distribution")
axes[1].set_xlabel("Hours")
axes[1].set_ylabel("Density")
axes[1].legend()

# Layout + save
# plt.tight_layout()
# plt.savefig("Figures/Figure_distribution_LNSSAvsBalanced_feasible.png", dpi=300)

plt.show()