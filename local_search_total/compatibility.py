def same_night_shift(r1, r2, night_col="Night_shift"):
    return r1[night_col].iloc[0] == r2[night_col].iloc[0]

def always_compatible(r1, r2):
    return True

