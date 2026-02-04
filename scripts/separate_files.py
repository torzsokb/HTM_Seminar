import pandas as pd

sheet_name=["Dagroutes", "Nachtroutes", "Halteinfo"]

df = pd.read_excel(
    "data/inputs/raw/Data_POH_5WK_REINIGEN_ABRI_EN_HEKWERK.xlsx", 
    sheet_name=sheet_name)

for sheet in sheet_name:
    df_sheet = df[sheet]
    df_sheet.to_csv(f"data/inputs/raw/{sheet}.csv")