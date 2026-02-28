import pandas as pd
import matplotlib.pyplot as plt

# Load data
sa = pd.read_csv("src/results/SA_stats.csv")
bal = pd.read_csv("src/results/Balanced_stats.csv")

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
plt.tight_layout()
plt.savefig("distribution_LNSSAvsBalanced.png", dpi=300)
plt.show()