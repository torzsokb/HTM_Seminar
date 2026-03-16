import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

import numpy as np

def main():
    # plot_day_night_avg()
    plot_penalty_distr()

def plot_penalty_distr():

    col = "Congestion level [%]"
    df = pd.read_csv("data/inputs/raw/2026-03-03T16_54_54.466Z.csv")
    df["Speed penalty"] = df["Speed [kmh]"] / df["Free flow speed [kmh]"]
    df["Speed penalty"] = np.where(df["Speed penalty"] >= 1, 1, df["Speed penalty"])

    print(df[col].max())
    fig = px.histogram(df, x=col)
    fig.show()



def plot_day_night_avg():

    col = "Speed penalty"

    df = pd.read_csv("data/inputs/raw/2026-03-03T16_54_54.466Z.csv")
    df["Speed penalty"] = df["Speed [kmh]"] / df["Free flow speed [kmh]"]
    df["Speed penalty"] = np.where(df["Speed penalty"] >= 1, 1, df["Speed penalty"])

    df["Time"] = pd.to_datetime(df["Time"])
    df["Hour"] = df["Time"].dt.strftime("%H:%M")
    # df["hr"] = df["Time"].dt.hour

    wanted_order = [
        "23:00",
        "00:00", "01:00", "02:00", "03:00", "04:00", "05:00",
        "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
        "12:00", "13:00", "14:00", "15:00"
    ]

    df = df[df["Hour"].isin(wanted_order)].copy()

    night_avg = df[(df["Time"].dt.hour >= 23) | (df["Time"].dt.hour < 7)][col].mean()
    day_avg = df[(df["Time"].dt.hour >= 7) & (df["Time"].dt.hour < 15)][col].mean()

    df["Hour"] = pd.Categorical(df["Hour"], categories=wanted_order, ordered=True)
    df = df.sort_values(["Region label", "Hour"])

    avg_values = [
        night_avg,  # 23
        night_avg, night_avg, night_avg, night_avg, night_avg, night_avg, night_avg,  # 00–06
        day_avg,  # 07
        day_avg, day_avg, day_avg, day_avg, day_avg, day_avg, day_avg, day_avg  # 08–15
    ]


    hourly_avg = (
        df.groupby("Hour", observed=False)[col]
        .mean()
        .reset_index()
    )


    # Create line plot
    fig = px.line(
        df,
        x="Hour",
        y=col,
        color="Region label",
        title="Average/Free-flow Speed Ratio by Region (23:00 to 15:00)",
        labels={
            "Hour": "Time of day",
            col: col,
            "Region label": "Region"
        }
    )

    fig.update_traces(
        line=dict(color="lightgray", width=1.5),
        opacity=0.4
    )
    fig.add_trace(go.Scatter(
        x=hourly_avg["Hour"],
        y=hourly_avg[col],
        mode="lines",
        name="Hourly average",
        line=dict(color="red", width=4)
    ))

    fig.add_trace(go.Scatter(
        x=wanted_order,
        y=avg_values,
        mode="lines",
        name="Night/Day average",
        line=dict(color="black", width=4),
        line_shape="hv"
    ))

    # fig.add_hline(
    #     y=night_avg,
    #     line_dash="dash",
    #     line_color="blue",
    #     annotation_text=f"Night Shift Average: {night_avg:.2f}",
    #     annotation_position="top left"
    # )
    # fig.add_hline(
    #     y=day_avg,
    #     line_dash="dot",
    #     line_color="yellow",
    #     annotation_text=f"Day Shift Average: {day_avg:.2f}",
    #     annotation_position="bottom left"
    # )

    fig.add_vline(
        x="07:00",
        line_dash="dash",
        line_color="black",
        line_width=2
    )


    fig.update_layout(hovermode="x unified")
    fig.show()


        

if __name__ == "__main__":
    main()