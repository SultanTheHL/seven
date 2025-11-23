from pydantic import BaseModel


# Class to store all calculated metrics

class Metrics(BaseModel):
    def __init__(self, risk_score: float, highway_percent: float, residential_road_percent: float,
                 max_slope: float, total_ascent: float, total_descent: float, average_slope: float,
                 total_distance: float):
        self.risk_score = risk_score
        self.highway_percent = highway_percent
        self.residential_road_percent = residential_road_percent
        self.max_slope = max_slope
        self.total_ascent = total_ascent
        self.total_descent = total_descent
        self.average_slope = average_slope
        self.total_distance = total_distance