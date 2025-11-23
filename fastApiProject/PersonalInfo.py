# PersonalInfo.py

from __future__ import annotations  # Это нужно для Forward Reference
from typing import List, Tuple, Union, Any
from pydantic import BaseModel, field_validator
import uuid

# Импортируем Vehicle
from Vehicle import Vehicle  # Убедитесь, что Vehicle.py и PersonalInfo.py находятся в одной директории


class RoadCoordinate(BaseModel):
    lon: float
    lat: float
    elevation: float
    speed: float


class PersonalInfo(BaseModel):
    people_count: int
    language_big_count: int
    language_small_count: int
    road_coordinates: List[RoadCoordinate]  # Accepts dicts (auto-converted) or tuples (via validator)
    trip_length_km: float
    trip_length_hours: int
    condition_id: int  # Crucial: 800=Clear, 6xx=Snow, 2xx=Thunderstorm
    temperature_c: float  # float: e.g. -1.5 (freezing point matters)
    wind_speed_mps: float  # float: e.g. 12.5 m/s (approx 45km/h)
    rain_volume_1h: float  # float: mm/hour. 0.0 if dry.
    snow_volume_1h: float  # float: mm/hour. Critical for risk.
    visibility_m: int  # int: meters. < 150m is high risk.
    current_vehicle: Vehicle  # Здесь будет ссылка на Vehicle
    preference: str  # comfort, price, space, driving_fun, safety
    automatic: int  # 0 - manual, 1 - automatic, 2 - i don't mind
    driving_skills: str  # comfortable, extra_safety, condition-specific
    parking_difficulty: int  # 0-10

    @field_validator('road_coordinates', mode='before')
    @classmethod
    def validate_road_coordinates(cls, v: Any) -> List[Any]:
        """Convert tuples to dicts for RoadCoordinate parsing. Dicts pass through unchanged."""
        if isinstance(v, list):
            result = []
            for coord in v:
                # If it's already a dict, keep it as is (Pydantic will parse it as RoadCoordinate)
                if isinstance(coord, dict):
                    result.append(coord)
                # If it's a tuple/list, convert to dict
                # Format: (lat, lon, elevation, speed) based on example data
                elif isinstance(coord, (list, tuple)) and len(coord) == 4:
                    result.append({
                        "lat": coord[0],
                        "lon": coord[1],
                        "elevation": coord[2],
                        "speed": coord[3]
                    })
                else:
                    # Already a RoadCoordinate or other format, pass through
                    result.append(coord)
            return result
        return v

    def get_risk_multiplier(self) -> float:
        # Группа 6xx: Снег (Критически)
        if 600 <= self.condition_id < 700:
            return 1.6  # 60% более высокий риск

        # Группа 2xx: Гроза или 5xx: Сильный дождь
        if (200 <= self.condition_id < 300) or (self.condition_id >= 502 and self.condition_id < 600):
            return 1.4  # 40% более высокий риск

        # Группа 7xx: Туман/Мгла
        if 700 <= self.condition_id < 800:
            return 1.3

        return 1.0  # Стандартные условия

# Обновляем ссылки на типы
PersonalInfo.__annotations__['current_vehicle'] = Vehicle
