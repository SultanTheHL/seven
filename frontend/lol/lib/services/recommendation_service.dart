import 'dart:convert';

import 'package:http/http.dart' as http;

class RecommendationService {
  const RecommendationService._();

  static const _endpoint = 'https://troll-engaged-cougar.ngrok-free.app/recommendation';

  static Future<RecommendationPayload> fetchRecommendation() async {
    final uri = Uri.parse(_endpoint);
    final response = await http.get(uri);
    if (response.statusCode != 200) {
      throw Exception('Failed to load recommendation data');
    }
    final Map<String, dynamic> json = jsonDecode(response.body) as Map<String, dynamic>;
    return RecommendationPayload.fromJson(json);
  }
}

class RecommendationPayload {
  RecommendationPayload({
    required this.vehicleRecommendations,
    required this.protectionFeedback,
  });

  final List<VehicleRecommendation> vehicleRecommendations;
  final ProtectionFeedback protectionFeedback;

  factory RecommendationPayload.fromJson(Map<String, dynamic> json) {
    final vehiclesJson = (json['vehicles'] as List<dynamic>? ?? [])
        .cast<Map<String, dynamic>>();
    final vehicleRecommendations = vehiclesJson
        .map(VehicleRecommendation.fromJson)
        .toList();

    final protectionList = (json['feedbackProtection'] as List<dynamic>? ?? []);
    final protectionJson = protectionList.isNotEmpty
        ? (protectionList.first as Map<String, dynamic>)
        : <String, dynamic>{};

    return RecommendationPayload(
      vehicleRecommendations: vehicleRecommendations,
      protectionFeedback: ProtectionFeedback.fromJson(protectionJson),
    );
  }
}

class VehicleRecommendation {
  VehicleRecommendation({
    required this.vehicleId,
    required this.rank,
    required this.feedback,
  });

  final String vehicleId;
  final int rank;
  final List<String> feedback;

  factory VehicleRecommendation.fromJson(Map<String, dynamic> json) {
    final feedbackList = (json['feedback'] as List<dynamic>? ?? [])
        .cast<Map<String, dynamic>>()
        .map((entry) => entry['feedbackText']?.toString() ?? '')
        .where((text) => text.isNotEmpty)
        .toList();

    return VehicleRecommendation(
      vehicleId: json['vehicleId']?.toString() ?? '',
      rank: json['rank'] is int
          ? json['rank'] as int
          : int.tryParse('${json['rank']}') ?? 0,
      feedback: feedbackList,
    );
  }
}

class ProtectionFeedback {
  ProtectionFeedback({
    required this.all,
    required this.smart,
    required this.basic,
    required this.none,
  });

  final List<TypedFeedback> all;
  final List<TypedFeedback> smart;
  final List<TypedFeedback> basic;
  final List<TypedFeedback> none;

  factory ProtectionFeedback.fromJson(Map<String, dynamic> json) {
    return ProtectionFeedback(
      all: _parseFeedbackList(json['protectionAll']),
      smart: _parseFeedbackList(json['protectionSmart']),
      basic: _parseFeedbackList(json['protectionBasic']),
      none: _parseFeedbackList(json['protectionNone']),
    );
  }

  static List<TypedFeedback> _parseFeedbackList(dynamic data) {
    return (data as List<dynamic>? ?? [])
        .cast<Map<String, dynamic>>()
        .map(TypedFeedback.fromJson)
        .toList();
  }
}

class TypedFeedback {
  const TypedFeedback({
    required this.text,
    required this.isPositive,
  });

  final String text;
  final bool isPositive;

  factory TypedFeedback.fromJson(Map<String, dynamic> json) {
    final type = json['feedbackType']?.toString().toUpperCase();
    final isPositive = type == 'POSITIVE';
    return TypedFeedback(
      text: json['feedbackText']?.toString() ?? '',
      isPositive: isPositive,
    );
  }
}

