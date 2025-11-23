import 'package:flutter/material.dart';

import '../state/recommendation_form_state.dart';
import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class PassengerDetailsScreen extends StatefulWidget {
  const PassengerDetailsScreen({super.key});

  @override
  State<PassengerDetailsScreen> createState() => _PassengerDetailsScreenState();
}

class _PassengerDetailsScreenState extends State<PassengerDetailsScreen> {
  static const int _minPassengers = 1;
  static const int _maxPassengers = 6;

  int _passengers = 1;
  bool _hasYoungChildren = false;

  void _updateCount(int delta) {
    setState(() {
      _passengers = (_passengers + delta).clamp(_minPassengers, _maxPassengers);
    });
  }

  void _toggleChildren(bool value) {
    setState(() {
      _hasYoungChildren = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBackground,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  IconButton(
                    onPressed: () => Navigator.of(context).maybePop(),
                    tooltip: 'Back',
                    icon: const Icon(Icons.arrow_back_ios_new),
                    color: AppColors.darkBody,
                    splashRadius: 24,
                    hoverColor: AppColors.iconHover,
                    highlightColor: AppColors.iconHover,
                  ),
                  const SizedBox(width: 12),
                  Text(
                    'Passenger Details',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: AppColors.darkBody,
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                'Step 2 of 6',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppColors.darkSubtleText,
                      fontWeight: FontWeight.w500,
                    ),
              ),
              const SizedBox(height: 12),
              const StepIndicator(
                progress: 2 / 6,
                fillColor: AppColors.primary,
                trackColor: AppColors.darkProgressTrack,
                height: 6,
              ),
              const SizedBox(height: 48),
              Text(
                'How many people\nare traveling with you?',
                style: Theme.of(context).textTheme.displaySmall?.copyWith(
                      fontSize: 32,
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w700,
                      height: 1.2,
                    ),
              ),
              const SizedBox(height: 48),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  _PassengerCountButton(
                    icon: Icons.remove,
                    enabled: _passengers > _minPassengers,
                    onPressed: () => _updateCount(-1),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 36),
                    child: Text(
                      '$_passengers',
                      style: Theme.of(context).textTheme.displayMedium?.copyWith(
                            color: AppColors.darkBody,
                            fontSize: 48,
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                  ),
                  _PassengerCountButton(
                    icon: Icons.add,
                    enabled: _passengers < _maxPassengers,
                    onPressed: () => _updateCount(1),
                  ),
                ],
              ),
              if (_passengers > 1) ...[
                const SizedBox(height: 32),
                _ChildrenQuestion(
                  value: _hasYoungChildren,
                  onChanged: _toggleChildren,
                ),
              ],
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    RecommendationFormState.instance.updatePassengers(
                      _passengers,
                      _hasYoungChildren,
                    );
                    Navigator.of(context).pushNamed('/luggage');
                  },
                  style: ButtonStyle(
                    backgroundColor: MaterialStateProperty.resolveWith(
                      (states) {
                        if (states.contains(MaterialState.pressed)) {
                          return AppColors.primaryPressed;
                        }
                        if (states.contains(MaterialState.hovered)) {
                          return AppColors.primaryHover;
                        }
                        return AppColors.primary;
                      },
                    ),
                    foregroundColor: const MaterialStatePropertyAll(Colors.white),
                    padding: const MaterialStatePropertyAll(
                      EdgeInsets.symmetric(vertical: 18),
                    ),
                    shape: MaterialStatePropertyAll(
                      RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(28),
                      ),
                    ),
                    textStyle: MaterialStatePropertyAll(
                      Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ),
                  child: const Text('Continue'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PassengerCountButton extends StatelessWidget {
  const _PassengerCountButton({
    required this.icon,
    required this.onPressed,
    required this.enabled,
  });

  final IconData icon;
  final VoidCallback onPressed;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    Color resolveColor(Set<MaterialState> states) {
      if (!enabled) {
        return AppColors.darkSurface.withOpacity(0.5);
      }
      if (states.contains(MaterialState.pressed)) {
        return AppColors.darkSurfacePressed;
      }
      if (states.contains(MaterialState.hovered)) {
        return AppColors.darkSurfaceHover;
      }
      return AppColors.darkSurface;
    }

    return SizedBox(
      width: 68,
      height: 68,
      child: ElevatedButton(
        onPressed: enabled ? onPressed : null,
        style: ButtonStyle(
          elevation: const MaterialStatePropertyAll(0),
          backgroundColor: MaterialStateProperty.resolveWith(resolveColor),
          foregroundColor: const MaterialStatePropertyAll(Colors.white),
          shape: MaterialStatePropertyAll(
            RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(34),
            ),
          ),
          overlayColor: const MaterialStatePropertyAll(Colors.transparent),
        ),
        child: Icon(icon, size: 28),
      ),
    );
  }
}

class _ChildrenQuestion extends StatelessWidget {
  const _ChildrenQuestion({
    required this.value,
    required this.onChanged,
  });

  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final borderColor = value ? AppColors.primary : AppColors.darkSurfaceBorder;
    final background = value ? AppColors.primary.withOpacity(0.12) : AppColors.darkSurface;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(24),
        onTap: () => onChanged(!value),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
          decoration: BoxDecoration(
            color: background,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(
              color: borderColor,
              width: 2,
            ),
          ),
          child: Text(
            'Children under 12 years old among the passengers',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: AppColors.darkBody,
                  fontWeight: FontWeight.w600,
                ),
          ),
        ),
      ),
    );
  }
}

