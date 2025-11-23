import 'dart:convert';

class RecommendationRequestDto {
  RecommendationRequestDto({
    required this.origin,
    required this.destination,
    required this.waypoints,
    required this.travelDate,
    required this.rentalDays,
    required this.preferences,
    this.bookingId,
  });

  final String origin;
  final String destination;
  final List<CoordinateDto> waypoints;
  final DateTime travelDate;
  final int rentalDays;
  final PreferencesDto preferences;
  final String? bookingId;

  Map<String, dynamic> toJson() => {
        'origin': origin,
        'destination': destination,
        'waypoints': waypoints.map((e) => e.toJson()).toList(),
        'travelDate': travelDate.toUtc().toIso8601String(),
        'rentalDays': rentalDays,
        'preferences': preferences.toJson(),
        if (bookingId != null) 'booking_id': bookingId,
      };

  @override
  String toString() => jsonEncode(toJson());
}

class PreferencesDto {
  PreferencesDto({
    required this.peopleCount,
    required this.luggageBigCount,
    required this.luggageSmallCount,
    required this.preference,
    required this.automatic,
    required this.drivingSkills,
    this.currentVehicle,
  });

  final int peopleCount;
  final int luggageBigCount;
  final int luggageSmallCount;
  final String preference;
  final int automatic;
  final String drivingSkills;
  final VehicleDto? currentVehicle;

  Map<String, dynamic> toJson() => {
        'people_count': peopleCount,
        'luggage_big_count': luggageBigCount,
        'luggage_small_count': luggageSmallCount,
        'preference': preference,
        'automatic': automatic,
        'driving_skills': drivingSkills,
        if (currentVehicle != null) 'current_vehicle': currentVehicle!.toJson(),
      };
}

class VehicleDto {
  VehicleDto({
    this.id,
    this.brand,
    this.model,
    this.acrissCode,
    this.groupType,
    this.transmissionType,
    this.fuelType,
    this.passengersCount,
    this.bagsCount,
    this.isNewCar,
    this.isRecommended,
    this.isMoreLuxury,
    this.isExcitingDiscount,
    this.vehicleCostValueEur,
    this.modelAnnex,
    this.images,
    this.tyreType,
    this.attributes,
    this.vehicleStatus,
    this.vehicleCost,
    this.upsellReasons,
  });

  final String? id;
  final String? brand;
  final String? model;
  final String? acrissCode;
  final String? groupType;
  final String? transmissionType;
  final String? fuelType;
  final int? passengersCount;
  final int? bagsCount;
  final bool? isNewCar;
  final bool? isRecommended;
  final bool? isMoreLuxury;
  final bool? isExcitingDiscount;
  final double? vehicleCostValueEur;
  final String? modelAnnex;
  final List<String>? images;
  final String? tyreType;
  final List<Map<String, dynamic>>? attributes;
  final String? vehicleStatus;
  final Map<String, dynamic>? vehicleCost;
  final List<Map<String, dynamic>>? upsellReasons;

  Map<String, dynamic> toJson() => {
        if (id != null) 'id': id,
        if (brand != null) 'brand': brand,
        if (model != null) 'model': model,
        if (acrissCode != null) 'acriss_code': acrissCode,
        if (groupType != null) 'group_type': groupType,
        if (transmissionType != null) 'transmission_type': transmissionType,
        if (fuelType != null) 'fuel_type': fuelType,
        if (passengersCount != null) 'passengers_count': passengersCount,
        if (bagsCount != null) 'bags_count': bagsCount,
        if (isNewCar != null) 'is_new_car': isNewCar,
        if (isRecommended != null) 'is_recommended': isRecommended,
        if (isMoreLuxury != null) 'is_more_luxury': isMoreLuxury,
        if (isExcitingDiscount != null) 'is_exciting_discount': isExcitingDiscount,
        if (vehicleCostValueEur != null)
          'vehicle_cost_value_eur': vehicleCostValueEur,
        if (modelAnnex != null) 'model_annex': modelAnnex,
        if (images != null) 'images': images,
        if (tyreType != null) 'tyre_type': tyreType,
        if (attributes != null) 'attributes': attributes,
        if (vehicleStatus != null) 'vehicle_status': vehicleStatus,
        if (vehicleCost != null) 'vehicle_cost': vehicleCost,
        if (upsellReasons != null) 'upsell_reasons': upsellReasons,
      };
}

class CoordinateDto {
  const CoordinateDto({
    required this.lat,
    required this.lng,
  });

  final double lat;
  final double lng;

  Map<String, dynamic> toJson() => {
        'lat': lat,
        'lng': lng,
      };
}

