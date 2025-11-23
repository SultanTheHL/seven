import 'package:flutter/material.dart';

import '../state/recommendation_form_state.dart';
import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class LuggageScreen extends StatefulWidget {
  const LuggageScreen({super.key});

  @override
  State<LuggageScreen> createState() => _LuggageScreenState();
}

class _LuggageScreenState extends State<LuggageScreen> {
  static const int _maxItems = 6;
  int _largeSuitcases = 2;
  int _smallBackpacks = 1;

  void _updateLarge(int delta) {
    setState(() {
      _largeSuitcases = (_largeSuitcases + delta).clamp(0, _maxItems);
    });
  }

  void _updateSmall(int delta) {
    setState(() {
      _smallBackpacks = (_smallBackpacks + delta).clamp(0, _maxItems);
    });
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

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
                    'Your Luggage',
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                'Step 3 of 6',
                style: textTheme.bodyMedium?.copyWith(
                  color: AppColors.darkSubtleText,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 12),
              const StepIndicator(
                progress: 3 / 6,
                fillColor: AppColors.primary,
                trackColor: AppColors.darkProgressTrack,
                height: 6,
              ),
              const SizedBox(height: 40),
              Text(
                'Do you have a lot of\nluggage or equipment\nwith you?',
                style: textTheme.displaySmall?.copyWith(
                  fontSize: 32,
                  color: AppColors.darkBody,
                  fontWeight: FontWeight.w700,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 32),
              LuggageCard(
                title: 'Large (suitcase)',
                subtitle: '$_largeSuitcases ${_largeSuitcases == 1 ? 'suitcase' : 'suitcases'}',
                count: _largeSuitcases,
                icon: Icons.luggage_rounded,
                onIncrement: () => _updateLarge(1),
                onDecrement: () => _updateLarge(-1),
                canIncrement: _largeSuitcases < _maxItems,
                canDecrement: _largeSuitcases > 0,
              ),
              const SizedBox(height: 20),
              LuggageCard(
                title: 'Small (backpack)',
                subtitle: '$_smallBackpacks ${_smallBackpacks == 1 ? 'backpack' : 'backpacks'}',
                count: _smallBackpacks,
                icon: Icons.backpack_rounded,
                onIncrement: () => _updateSmall(1),
                onDecrement: () => _updateSmall(-1),
                canIncrement: _smallBackpacks < _maxItems,
                canDecrement: _smallBackpacks > 0,
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    RecommendationFormState.instance.updateLuggage(
                      large: _largeSuitcases,
                      small: _smallBackpacks,
                    );
                    Navigator.of(context).pushNamed('/priority');
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
                      textTheme.titleMedium?.copyWith(
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

class LuggageCard extends StatelessWidget {
  const LuggageCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.count,
    required this.icon,
    required this.onIncrement,
    required this.onDecrement,
    required this.canIncrement,
    required this.canDecrement,
  });

  final String title;
  final String subtitle;
  final int count;
  final IconData icon;
  final VoidCallback onIncrement;
  final VoidCallback onDecrement;
  final bool canIncrement;
  final bool canDecrement;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.darkSurface,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: textTheme.titleMedium?.copyWith(
                        color: AppColors.darkBody,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: textTheme.bodyMedium?.copyWith(
                        color: AppColors.darkSubtleText,
                      ),
                    ),
                  ],
                ),
              ),
              Row(
                children: [
                  _CircleIconButton(
                    icon: Icons.remove,
                    onPressed: canDecrement ? onDecrement : null,
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Text(
                      '$count',
                      style: textTheme.titleLarge?.copyWith(
                        color: AppColors.darkBody,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  _CircleIconButton(
                    icon: Icons.add,
                    onPressed: canIncrement ? onIncrement : null,
                  ),
                ],
              ),
            ],
          ),
          if (count > 0) ...[
            const SizedBox(height: 16),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: List.generate(
                count,
                (_) => Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: AppColors.primary,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(
                    icon,
                    color: Colors.white,
                    size: 28,
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _CircleIconButton extends StatelessWidget {
  const _CircleIconButton({
    required this.icon,
    required this.onPressed,
  });

  final IconData icon;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 44,
      height: 44,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ButtonStyle(
          elevation: const MaterialStatePropertyAll(0),
          backgroundColor: MaterialStateProperty.resolveWith(
            (states) {
              if (onPressed == null) {
                return AppColors.darkSurface.withOpacity(0.4);
              }
              if (states.contains(MaterialState.pressed)) {
                return AppColors.darkSurfacePressed;
              }
              if (states.contains(MaterialState.hovered)) {
                return AppColors.darkSurfaceHover;
              }
              return AppColors.darkSurface;
            },
          ),
          foregroundColor: const MaterialStatePropertyAll(Colors.white),
          shape: MaterialStatePropertyAll(
            RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(22),
            ),
          ),
          overlayColor: const MaterialStatePropertyAll(Colors.transparent),
        ),
        child: Icon(icon, size: 20),
      ),
    );
  }
}

