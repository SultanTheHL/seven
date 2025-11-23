import 'package:flutter/material.dart';

import '../state/recommendation_form_state.dart';
import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class PriorityScreen extends StatefulWidget {
  const PriorityScreen({super.key});

  @override
  State<PriorityScreen> createState() => _PriorityScreenState();
}

class _PriorityScreenState extends State<PriorityScreen> {
  final Set<int> _selected = <int>{};

  static const List<_PriorityOption> _options = [
    _PriorityOption(
      label: 'Comfort',
      icon: Icons.event_seat,
      spanTwoColumns: false,
    ),
    _PriorityOption(
      label: 'Space',
      icon: Icons.luggage,
      spanTwoColumns: false,
    ),
    _PriorityOption(
      label: 'Driving fun',
      icon: Icons.speed,
      spanTwoColumns: false,
    ),
    _PriorityOption(
      label: 'Safety',
      icon: Icons.shield,
      spanTwoColumns: false,
    ),
    _PriorityOption(
      label: 'Price',
      icon: Icons.sell,
      spanTwoColumns: true,
    ),
  ];

  void _toggle(int index) {
    setState(() {
      if (_selected.contains(index)) {
        _selected.remove(index);
      } else {
        _selected.add(index);
      }
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
                    'Your Priorities',
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                'Step 4 of 6',
                style: textTheme.bodyMedium?.copyWith(
                  color: AppColors.darkSubtleText,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 12),
              const StepIndicator(
                progress: 4 / 6,
                fillColor: AppColors.primary,
                trackColor: AppColors.darkProgressTrack,
                height: 6,
              ),
              const SizedBox(height: 36),
              Text(
                'What matters most to\nyou today?',
                style: textTheme.displaySmall?.copyWith(
                  fontSize: 32,
                  color: AppColors.darkBody,
                  fontWeight: FontWeight.w700,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 28),
              Expanded(
                child: LayoutBuilder(
                  builder: (context, constraints) {
                    final cardSpacing = 18.0;
                    final halfWidth = (constraints.maxWidth - cardSpacing) / 2;

                    return SingleChildScrollView(
                      child: Wrap(
                        spacing: cardSpacing,
                        runSpacing: cardSpacing,
                        children: [
                          for (int i = 0; i < _options.length; i++)
                            SizedBox(
                              width: _options[i].spanTwoColumns
                                  ? constraints.maxWidth
                                  : halfWidth,
                              child: _PriorityCard(
                                option: _options[i],
                                selected: _selected.contains(i),
                                onTap: () => _toggle(i),
                              ),
                            ),
                        ],
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final selectedLabels = _selected
                        .map((index) => _options[index].label)
                        .toList();
                    RecommendationFormState.instance.updatePriorities(selectedLabels);
                    Navigator.of(context).pushNamed('/preference');
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

class _PriorityCard extends StatelessWidget {
  const _PriorityCard({
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final _PriorityOption option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final backgroundColor = selected
        ? AppColors.primary.withOpacity(0.16)
        : AppColors.darkSurface;
    final borderColor = selected ? AppColors.primary : AppColors.darkSurfaceBorder;
    final iconColor = selected ? AppColors.primary : AppColors.darkBody;
    final textColor = selected ? AppColors.darkBody : AppColors.darkBody;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(26),
        splashColor: Colors.transparent,
        hoverColor: AppColors.iconHover,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(26),
            border: Border.all(color: borderColor, width: 2),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(option.icon, size: 32, color: iconColor),
              const SizedBox(height: 48),
              Text(
                option.label,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: textColor,
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PriorityOption {
  const _PriorityOption({
    required this.label,
    required this.icon,
    required this.spanTwoColumns,
  });

  final String label;
  final IconData icon;
  final bool spanTwoColumns;
}

