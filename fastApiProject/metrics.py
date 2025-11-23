from dataclasses import dataclass


@dataclass
class Metrics:
    risk_score: float
    highway_percent: float
    residential_road_percent: float
    max_slope: float
    total_ascent: float
    total_descent: float
    average_slope: float
    total_distance: float