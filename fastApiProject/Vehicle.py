# Vehicle.py

import uuid
from pydantic import BaseModel

class Vehicle(BaseModel):
    id: uuid.UUID
    brand: str
    model: str
    acriss_code: str
    group_type: str
    transmission_type: str
    fuel_type: str
    passengers_count: int
    bags_count: int
    is_new_car: bool
    is_recommended: bool
    is_more_luxury: bool
    is_exciting_discount: bool
    vehicle_cost_value_eur: float
