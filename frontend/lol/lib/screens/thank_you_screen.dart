import 'package:flutter/material.dart';

import '../ui/app_colors.dart';

class ThankYouScreen extends StatefulWidget {
  const ThankYouScreen({super.key});

  @override
  State<ThankYouScreen> createState() => _ThankYouScreenState();
}

class _ThankYouScreenState extends State<ThankYouScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _checkAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );

    _checkAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeInOut,
      ),
    );

    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBackground,
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              AnimatedBuilder(
                animation: _checkAnimation,
                builder: (context, child) {
                  return CustomPaint(
                    size: const Size(120, 120),
                    painter: _CheckmarkPainter(_checkAnimation.value),
                  );
                },
              ),
              const SizedBox(height: 32),
              Text(
                'Thank you',
                style: Theme.of(context).textTheme.displayMedium?.copyWith(
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w700,
                      fontSize: 32,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CheckmarkPainter extends CustomPainter {
  _CheckmarkPainter(this.progress);

  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2 - 10;

    // Draw green circle
    final circlePaint = Paint()
      ..color = Colors.green
      ..style = PaintingStyle.fill;
    canvas.drawCircle(center, radius, circlePaint);

    // Draw checkmark path
    final checkPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    // Checkmark path: two lines forming a check
    final path = Path();
    final startX = center.dx - radius * 0.4;
    final startY = center.dy;
    final midX = center.dx - radius * 0.1;
    final midY = center.dy + radius * 0.3;
    final endX = center.dx + radius * 0.4;
    final endY = center.dy - radius * 0.2;

    // First line: from start to mid
    path.moveTo(startX, startY);
    path.lineTo(midX, midY);

    // Second line: from mid to end
    path.lineTo(endX, endY);

    // Animate the path drawing
    final pathMetrics = path.computeMetrics().first;
    final pathLength = pathMetrics.length;
    final animatedLength = pathLength * progress;

    final animatedPath = pathMetrics.extractPath(0, animatedLength, startWithMoveTo: true);

    canvas.drawPath(animatedPath, checkPaint);
  }

  @override
  bool shouldRepaint(_CheckmarkPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}

