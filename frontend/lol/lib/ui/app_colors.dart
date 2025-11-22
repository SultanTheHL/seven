import 'package:flutter/material.dart';

/// Centralized color tokens shared across the flow screens.
class AppColors {
  const AppColors._();

  // Shared brand colors.
  static const primary = Color(0xFFFF5A00);
  static const primaryHover = Color(0xFFE84F00);
  static const primaryPressed = Color(0xFFCC4300);

  // Light screen palette (e.g. preference screen).
  static const lightBackground = Color(0xFFFFFBF6);
  static const lightBody = Color(0xFF101010);
  static const lightSubtleText = Color(0xFF6F6F6F);
  static const lightCard = Color(0xFFEFF0F4);
  static const lightCardHover = Color(0xFFF6F7FA);
  static const lightCardPressed = Color(0xFFE0E3EA);
  static const lightProgressTrack = Color(0xFFE4E5EB);
  static const iconHover = Color(0x1A000000);

  // Dark screen palette (e.g. passenger details screen).
  static const darkBackground = Color(0xFF120C08);
  static const darkSurface = Color(0xFF141B2C);
  static const darkSurfaceHover = Color(0xFF1B2438);
  static const darkSurfacePressed = Color(0xFF0F1422);
  static const darkSurfaceBorder = Color(0xFF2C2420);
  static const darkBody = Colors.white;
  static const darkSubtleText = Color(0xFFA8A8A8);
  static const darkProgressTrack = Color(0xFF2D2B32);
}

