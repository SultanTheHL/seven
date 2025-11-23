import math
import uuid
from typing import List

import requests
from Vehicle import Vehicle
from PersonalInfo import PersonalInfo, RoadCoordinate
from metrics import Metrics

VEHICLE_API_URL = "https://hackatum25.sixt.io/api/booking/{booking_id}/vehicles"

class Logic:

    def _extract_coord_values(self, coord):
        """Return (lat, lon, elevation, speed) regardless of the source structure."""
        if isinstance(coord, RoadCoordinate):
            return coord.lat, coord.lon, coord.elevation, coord.speed

        if isinstance(coord, dict):
            return coord["lat"], coord["lon"], coord["elevation"], coord["speed"]

        if isinstance(coord, (list, tuple)) and len(coord) == 4:
            lat, lon, elevation, speed = coord
            return lat, lon, elevation, speed

        raise ValueError("Unsupported road coordinate format")

    def _fetch_available_vehicles(self, booking_id: str) -> List[Vehicle]:
        if not booking_id:
            raise ValueError("booking_id is required to fetch vehicles")

        url = VEHICLE_API_URL.format(booking_id=booking_id)
        try:
            response = requests.get(url, timeout=10)
            response.raise_for_status()
        except requests.RequestException as exc:
            raise RuntimeError(f"Unable to fetch vehicles for booking {booking_id}") from exc

        payload = response.json()
        deals = payload.get("deals", [])
        vehicles: List[Vehicle] = []

        for deal in deals:
            vehicle_payload = deal.get("vehicle")
            if not vehicle_payload:
                continue

            vehicle_id = vehicle_payload.get("id")
            if not vehicle_id:
                continue

            try:
                vehicle = Vehicle(
                    id=uuid.UUID(vehicle_id),
                    brand=vehicle_payload.get("brand", ""),
                    model=vehicle_payload.get("model", ""),
                    acriss_code=vehicle_payload.get("acrissCode", ""),
                    group_type=vehicle_payload.get("groupType", ""),
                    transmission_type=vehicle_payload.get("transmissionType", ""),
                    fuel_type=vehicle_payload.get("fuelType", ""),
                    passengers_count=vehicle_payload.get("passengersCount", 0),
                    bags_count=vehicle_payload.get("bagsCount", 0),
                    is_new_car=vehicle_payload.get("isNewCar", False),
                    is_recommended=vehicle_payload.get("isRecommended", False),
                    is_more_luxury=vehicle_payload.get("isMoreLuxury", False),
                    is_exciting_discount=vehicle_payload.get("isExcitingDiscount", False),
                    vehicle_cost_value_eur=float(
                        vehicle_payload.get("vehicleCost", {}).get("value", 0)
                    ),
                )
                vehicles.append(vehicle)
            except (ValueError, TypeError):
                # Skip malformed vehicle entries
                continue

        if not vehicles:
            raise ValueError(f"No vehicles returned for booking {booking_id}")

        return vehicles

    # Haversine function to calculate distance (in meters)
    def haversine(self, lat1, lon1, lat2, lon2):
        R = 6371.0  # Radius of the Earth in kilometers
        lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = math.sin(dlat / 2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2)**2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        distance = R * c
        return distance * 1000  # in meters


    # Function to compute the max slope, total ascent, total descent, and average slope
    def calculate_slope_metrics(self, elevation_data):
        total_ascent = 0
        total_descent = 0
        total_distance = 0
        max_slope = 0
        total_slope = 0
        num_points = len(elevation_data)

        for i in range(1, num_points):
            # Now unpack 4 elements (longitude, latitude, elevation, speed)
            lat1, lon1, e1, _ = self._extract_coord_values(elevation_data[i - 1])  # Speed is ignored here
            lat2, lon2, e2, _ = self._extract_coord_values(elevation_data[i])      # Speed is ignored here

            distance = self.haversine(lat1, lon1, lat2, lon2)
            slope = (e2 - e1) / distance

            max_slope = max(max_slope, abs(slope))

            if slope > 0:
                total_ascent += e2 - e1
            elif slope < 0:
                total_descent += e1 - e2

            total_distance += distance
            total_slope += abs(slope)

        average_slope = (total_ascent + total_descent) / total_distance if total_distance != 0 else 0

        return {
            "max_slope": max_slope,
            "total_ascent": total_ascent,
            "total_descent": total_descent,
            "average_slope": average_slope,
            "total_distance": total_distance
        }


    # Function to calculate weather risk based on conditions
    def calculate_weather_risk(self, snow_volume_1h, rain_volume_1h, visibility_m, temperature_c, wind_speed_mps):
        risk = 0
        snow_factor = min(snow_volume_1h, 50)
        rain_factor = min(rain_volume_1h, 50)
        visibility_factor = max(0, 100 - visibility_m / 10)
        temperature_factor = max(0, 1 if temperature_c < 0 else 0)
        wind_factor = min(wind_speed_mps, 30)

        risk += (snow_factor * 0.2) + (rain_factor * 0.15) + (visibility_factor * 0.25) + (temperature_factor * 10) + (wind_factor * 0.3)
        return risk


    # Function to calculate trip and road risk based on speed and road conditions
    def calculate_trip_road_risk(self, road_coordinates, trip_length_km, trip_length_hours):
        risk = 0
        trip_length_factor = min(trip_length_km / 100, 10)
        trip_time_factor = trip_length_hours
        road_speed_factor = sum([self._extract_coord_values(coord)[3] < 50 for coord in road_coordinates])
        road_type_factor = road_speed_factor * 2
        risk += (trip_length_factor * 5) + (trip_time_factor * 0.5) + (road_type_factor)
        return risk


    # Function to calculate vehicle and driver risk based on skills and vehicle type
    def calculate_vehicle_driver_risk(self, automatic, driving_skills):
        risk = 0
        vehicle_factor = 10 if automatic == 0 else 0
        skill_factor = 0 if driving_skills == "comfortable" else 5
        risk += vehicle_factor + skill_factor
        return risk


    # Function to calculate the overall risk score based on all factors
    def calculate_risk(self, personal_info: PersonalInfo):
        risk_score = 0

        risk_score += self.calculate_weather_risk(personal_info.snow_volume_1h, personal_info.rain_volume_1h,
                                              personal_info.visibility_m, personal_info.temperature_c,
                                              personal_info.wind_speed_mps)

        risk_score += self.calculate_trip_road_risk(personal_info.road_coordinates, personal_info.trip_length_km,
                                                personal_info.trip_length_hours)

        risk_score += self.calculate_vehicle_driver_risk(personal_info.automatic, personal_info.driving_skills)

        risk_score = min(risk_score, 100)
        return risk_score


    # Function to calculate the percentage of highway and residential roads
    def calculate_highway_and_residential_percent(self, road_coordinates):
        highway_count = 0
        residential_count = 0
        total_count = len(road_coordinates)

        highway_threshold = 80
        residential_threshold = 60

        for coord in road_coordinates:
            speed = self._extract_coord_values(coord)[3]  # Extract the speed value
            if speed >= highway_threshold:
                highway_count += 1
            elif speed <= residential_threshold:
                residential_count += 1

        highway_percent = (highway_count / total_count) * 100 if total_count > 0 else 0
        residential_road_percent = (residential_count / total_count) * 100 if total_count > 0 else 0

        return highway_percent, residential_road_percent


    def calculate_vehicle_score(self, vehicle: Vehicle, personal_info: PersonalInfo, metrics: Metrics):
        score = 0

        # 1. Vehicle Suitability Score
        if vehicle.is_recommended:
            score += 20  # If the vehicle is recommended, add to the score.

        # 2. Vehicle Luxury Score (If the vehicle is more luxurious)
        if vehicle.is_more_luxury:
            score += 15

        # 3. Vehicle Exciting Discount (Discount gives positive impact)
        if vehicle.is_exciting_discount:
            score += 10

        # 4. Vehicle Cost Factor
        cost_factor = 100 - min(vehicle.vehicle_cost_value_eur / 1000, 100)  # Normalized price
        score += cost_factor

        # 5. Vehicle Compatibility with Trip - Based on passengers count and luggage
        if vehicle.passengers_count >= personal_info.people_count and vehicle.bags_count >= (personal_info.language_big_count + personal_info.language_small_count):
            score += 10  # Vehicle suits the trip well if it has enough capacity

        # 6. Road and Trip Compatibility Score (Based on road type percentage, slope severity)
        score += metrics.highway_percent * 0.2  # Positive impact for highway roads
        score -= metrics.residential_road_percent * 0.1  # Negative impact for residential roads
        score -= metrics.max_slope * 0.5  # Negative impact for steep slopes

        # 7. Risk Factor Adjustment
        score -= metrics.risk_score * 0.2  # Decrease score based on risk

        # 8. Vehicle Type Preference Score (Based on user preference for comfort, price, etc.)
        if personal_info.preference == "comfort" and vehicle.transmission_type == "Automatic":
            score += 10  # Automatic transmission is better for comfort
        elif personal_info.preference == "price" and vehicle.vehicle_cost_value_eur < 30000:
            score += 15  # If the vehicle is cheaper, it's more suitable for price-conscious users

        # 9. Parking Difficulty Adjustment
        score -= personal_info.parking_difficulty * 0.5  # Higher parking difficulty lowers the score

        return score


    def generate_recommendation(self, personal_info: PersonalInfo, booking_id: str, vehicles: List[Vehicle] = None):
        """Generate ML recommendation response for given PersonalInfo and booking."""
        if vehicles is None:
            vehicles = self._fetch_available_vehicles(booking_id)

        # Calculate metrics
        risk_score = self.calculate_risk(personal_info)
        highway_percent, residential_percent = self.calculate_highway_and_residential_percent(personal_info.road_coordinates)
        slope_metrics = self.calculate_slope_metrics(personal_info.road_coordinates)
        
        # Calculate scores for each vehicle and rank them
        vehicle_scores = []
        for vehicle in vehicles:
            metrics = Metrics(
                risk_score=risk_score,
                highway_percent=highway_percent,
                residential_road_percent=residential_percent,
                max_slope=slope_metrics["max_slope"],
                total_ascent=slope_metrics["total_ascent"],
                total_descent=slope_metrics["total_descent"],
                average_slope=slope_metrics["average_slope"],
                total_distance=slope_metrics["total_distance"]
            )
            score = self.calculate_vehicle_score(vehicle, personal_info, metrics)
            vehicle_scores.append((vehicle, score))
        
        # Sort by score (higher is better) and assign ranks
        vehicle_scores.sort(key=lambda x: x[1], reverse=True)
        ranked_vehicles = [
            {"id": str(vehicle.id), "rank": rank + 1}
            for rank, (vehicle, _) in enumerate(vehicle_scores)
        ]
        ranked_vehicles = ranked_vehicles[:3]

        return {
            "highway_percent": highway_percent,
            "max_slope": slope_metrics["max_slope"],
            "total_ascent": slope_metrics["total_ascent"],
            "total_descent": slope_metrics["total_descent"],
            "average_slope": slope_metrics["average_slope"],
            "risk_score": risk_score,
            "vehicles": ranked_vehicles
        }
