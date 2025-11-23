import 'dart:math';

import '../models/recommendation_request.dart';

class RecommendationFormState {
  RecommendationFormState._();

  static final RecommendationFormState instance = RecommendationFormState._();

  TripPlanData? _tripPlan;
  int _passengers = 1;
  bool _hasYoungChildren = false;
  int _luggageBig = 0;
  int _luggageSmall = 0;
  List<String> _priorities = const [];
  TransmissionPreference _transmissionPreference = TransmissionPreference.automatic;
  String _drivingConfidence = 'Comfortable in any condition';
  DateTime _travelDate = DateTime.now().toUtc();
  int _rentalDays = 1;
  String? bookingId = 'ENTER_BOOKING_ID';

  void updateTripPlan({
    required String start,
    required String end,
    required List<String> stops,
  }) {
    _tripPlan = TripPlanData(start: start, end: end, stops: stops);
  }

  void updatePassengers(int passengers, bool hasYoungChildren) {
    _passengers = passengers;
    _hasYoungChildren = hasYoungChildren;
  }

  void updateLuggage({
    required int large,
    required int small,
  }) {
    _luggageBig = large;
    _luggageSmall = small;
  }

  void updatePriorities(List<String> priorities) {
    _priorities = priorities;
  }

  void updateTransmission(int selectedIndex) {
    _transmissionPreference = TransmissionPreference.values
        .elementAt(selectedIndex.clamp(0, TransmissionPreference.values.length - 1));
  }

  void updateDrivingConfidence(String label) {
    _drivingConfidence = label;
  }

  RecommendationRequestDto buildRequest() {
    final trip = _tripPlan ??
        TripPlanData(
          start: 'Munich',
          end: 'Munich',
          stops: const [],
        );

    final origin = trip.start.trim().isEmpty ? 'Munich' : trip.start.trim();
    final destination = trip.end.trim().isEmpty ? trip.start.trim() : trip.end.trim();
    final waypoints = trip.stops
        .take(25)
        .toList()
        .asMap()
        .entries
        .map((entry) => _coordinateFromInput(entry.value, entry.key + 2))
        .toList();

    final List<String> priorityLabels = List<String>.from(_priorities);
    if (_hasYoungChildren && !priorityLabels.contains('Family friendly')) {
      priorityLabels.add('Family friendly');
    }
    final preferenceLabel =
        priorityLabels.isEmpty ? 'Comfort' : priorityLabels.join(', ');

    final currentVehicle = VehicleDto(
      id: 'bb728740-95ac-7a86-2025-087de2dd7a66',
      brand: 'FORD',
      model: 'MUSTANG',
      acrissCode: 'FTAR',
      groupType: 'CONVERTIBLE',
      transmissionType: 'Automatic',
      fuelType: '',
      passengersCount: 4,
      bagsCount: 0,
      isNewCar: false,
      isRecommended: true,
      isMoreLuxury: false,
      isExcitingDiscount: false,
      vehicleCostValueEur: 43100.0,
      modelAnnex: '2.3 ECOBOOST PREMIUM CONVERTIBLE RWD',
      images: const [
        'https://vehicle-pictures-prod.orange.sixt.com/5144354/ffffff/18_1.png',
      ],
      tyreType: 'ALL-YEAR_TYRES',
      attributes: const [
        {
          'key': 'P100_VEHICLE_ATTRIBUTE_MILEAGE',
          'title': 'Mileage',
          'value': '~10k miles',
          'attribute_type': 'CARD_ATTRIBUTE',
          'icon_url': 'https://www.sixt.com/shared/icons/trex/p100/speed1.png',
        },
        {
          'key': 'P100_VEHICLE_ATTRIBUTE_SEATS',
          'title': 'Seats',
          'value': '4',
          'attribute_type': 'CARD_ATTRIBUTE',
          'icon_url': 'https://www.sixt.com/shared/icons/trex/p100/seat.png',
        },
        {
          'key': 'P100_VEHICLE_UPSELL_ATTRIBUTE_KEYLESS_IGNITION',
          'title': 'Keyless ignition',
          'value': 'Keyless ignition',
          'attribute_type': 'UPSELL_ATTRIBUTE',
        },
        {
          'key': 'P100_VEHICLE_UPSELL_ATTRIBUTE_NAVIGATION',
          'title': 'Built-in navigation',
          'value': 'Built-in navigation',
          'attribute_type': 'UPSELL_ATTRIBUTE',
        },
      ],
      vehicleStatus: 'AVAILABLE',
      vehicleCost: const {
        'currency': 'USD',
        'value': 43100.0,
      },
      upsellReasons: const [
        {
          'title': 'Winter Ready Safety',
          'description': 'Conquer winter roads confidently with premium tires.',
        },
        {
          'title': 'Convertible Luxury',
          'description': 'Experience stylish comfort while navigating scenic routes.',
        },
        {
          'title': 'Ecoboost Power',
          'description': 'Embrace thrilling winter drives with robust horsepower.',
        },
      ],
    );

    final prefs = PreferencesDto(
      peopleCount: _passengers,
      luggageBigCount: _luggageBig,
      luggageSmallCount: _luggageSmall,
      preference: preferenceLabel,
      automatic: _transmissionPreference.index,
      drivingSkills: _drivingConfidence,
      currentVehicle: currentVehicle,
    );

    return RecommendationRequestDto(
      origin: origin,
      destination: destination,
      waypoints: waypoints,
      travelDate: _travelDate,
      rentalDays: _rentalDays,
      preferences: prefs,
      bookingId: bookingId,
    );
  }

  CoordinateDto _coordinateFromInput(String input, int salt) {
    final trimmed = input.trim();
    if (trimmed.isEmpty) {
      return _fallbackCoordinate(salt);
    }

    final numericPattern = RegExp(r'(-?\d+(?:\.\d+)?)');
    final matches = numericPattern.allMatches(trimmed).toList();
    if (matches.length >= 2) {
      final lat = double.tryParse(matches[0].group(0)!) ?? 0;
      final lng = double.tryParse(matches[1].group(0)!) ?? 0;
      return CoordinateDto(lat: lat.clamp(-90, 90).toDouble(), lng: lng.clamp(-180, 180).toDouble());
    } else if (matches.isNotEmpty) {
      final lat = double.tryParse(matches.first.group(0)!) ?? 0;
      return CoordinateDto(lat: lat.clamp(-90, 90).toDouble(), lng: _noiseForIndex(salt));
    }

    return _fallbackCoordinate(trimmed.hashCode + salt);
  }

  CoordinateDto _fallbackCoordinate(int seed) {
    final lat = _noiseForIndex(seed) / 2;
    final lng = _noiseForIndex(seed + 42);
    return CoordinateDto(lat: lat, lng: lng);
  }

  double _noiseForIndex(int seed) {
    final random = Random(seed);
    return random.nextDouble() * 360 - 180;
  }
}

class TripPlanData {
  TripPlanData({
    required this.start,
    required this.end,
    required this.stops,
  });

  final String start;
  final String end;
  final List<String> stops;
}

enum TransmissionPreference { manual, automatic, dontCare }

