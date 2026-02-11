from dataclasses import dataclass

@dataclass(frozen=True)
class IntraSwapMove:
    route: str
    i: int
    j: int

@dataclass(frozen=True)
class InterShiftMove:
    r1: str
    r2: str
    i: int
    j: int
