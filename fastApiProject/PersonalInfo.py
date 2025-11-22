# PersonalInfo.py

from __future__ import annotations  # Это нужно для Forward Reference
from typing import List, Tuple
from pydantic import BaseModel
import uuid

# Импортируем Vehicle
from Vehicle import Vehicle  # Убедитесь, что Vehicle.py и PersonalInfo.py находятся в одной директории

class PersonalInfo(BaseModel):
    people_count: int
    language_big_count: int
    language_small_count: int
    road_coordinates: List[Tuple[float, float, float, float]]  # (longitude, latitude, elevation, speed)
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
