import 'package:flutter/material.dart';

/// Rounded progress bar used on multiple screens.
class StepIndicator extends StatelessWidget {
  const StepIndicator({
    super.key,
    required this.progress,
    required this.trackColor,
    required this.fillColor,
    this.height = 8,
  });

  final double progress;
  final double height;
  final Color trackColor;
  final Color fillColor;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(height),
        child: Stack(
          fit: StackFit.expand,
          children: [
            DecoratedBox(
              decoration: BoxDecoration(color: trackColor),
            ),
            FractionallySizedBox(
              alignment: Alignment.centerLeft,
              widthFactor: progress.clamp(0, 1),
              child: DecoratedBox(
                decoration: BoxDecoration(color: fillColor),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

