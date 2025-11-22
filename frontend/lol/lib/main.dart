import 'package:flutter/material.dart';

import 'screens/confidence_screen.dart';
import 'screens/luggage_screen.dart';
import 'screens/passenger_details_screen.dart';
import 'screens/preference_screen.dart';
import 'screens/priority_screen.dart';
import 'screens/trip_plan_screen.dart';
import 'ui/app_colors.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final baseTextTheme = ThemeData.light().textTheme;

    return MaterialApp(
      title: 'Travel Flow',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: false,
        scaffoldBackgroundColor: AppColors.darkBackground,
        textTheme: baseTextTheme.copyWith(
          bodyMedium: baseTextTheme.bodyMedium?.copyWith(
            color: AppColors.lightBody,
            fontSize: 16,
          ),
        ),
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppColors.primary,
          brightness: Brightness.dark,
        ),
      ),
      initialRoute: '/',
      routes: {
        '/': (_) => const TripPlanScreen(),
        '/confidence': (_) => const ConfidenceScreen(),
        '/priority': (_) => const PriorityScreen(),
        '/luggage': (_) => const LuggageScreen(),
        '/passengers': (_) => const PassengerDetailsScreen(),
        '/preference': (_) => const PreferenceScreen(),
      },
    );
  }
}
