import 'package:flutter/material.dart';

import '../state/recommendation_form_state.dart';
import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class PreferenceScreen extends StatefulWidget {
  const PreferenceScreen({super.key});

  @override
  State<PreferenceScreen> createState() => _PreferenceScreenState();
}

class _PreferenceScreenState extends State<PreferenceScreen> {
  int _selectedIndex = 1;

  void _select(int index) {
    setState(() => _selectedIndex = index);
  }

  void _goNext(BuildContext context) {
    RecommendationFormState.instance.updateTransmission(_selectedIndex);
    Navigator.of(context).pushNamed('/confidence');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBackground,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: SizedBox(
                  height: constraints.maxHeight,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 24,
                      vertical: 20,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            IconButton(
                              onPressed: () => Navigator.of(context).maybePop(),
                              padding: EdgeInsets.zero,
                              splashRadius: 24,
                              tooltip: 'Back',
                              icon: const Icon(Icons.arrow_back_ios_new),
                              color: AppColors.darkBody,
                              hoverColor: AppColors.iconHover,
                              highlightColor: AppColors.iconHover,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                'Driving Mode',
                                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                      color: AppColors.darkBody,
                                      fontWeight: FontWeight.w600,
                                    ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),
                        Text(
                          'Step 5 of 6',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: AppColors.darkSubtleText,
                                fontWeight: FontWeight.w500,
                              ),
                        ),
                        const SizedBox(height: 12),
                        const StepIndicator(
                          progress: 5 / 6,
                          fillColor: AppColors.primary,
                          trackColor: AppColors.darkProgressTrack,
                          height: 6,
                        ),
                        const SizedBox(height: 24),
                        Text(
                          'What type of transmission\ndo you prefer?',
                          style: Theme.of(context).textTheme.displaySmall?.copyWith(
                                fontSize: 32,
                                fontWeight: FontWeight.w700,
                                height: 1.2,
                                color: AppColors.darkBody,
                              ),
                        ),
                        const Spacer(),
                        Column(
                          children: [
                            OptionButton(
                              label: 'Manual',
                              selected: _selectedIndex == 0,
                              onPressed: () => _select(0),
                            ),
                            const SizedBox(height: 12),
                            OptionButton(
                              label: 'Automatic',
                              selected: _selectedIndex == 1,
                              onPressed: () => _select(1),
                            ),
                            const SizedBox(height: 12),
                            OptionButton(
                              label: "I don't mind",
                              selected: _selectedIndex == 2,
                              onPressed: () => _select(2),
                            ),
                            const SizedBox(height: 24),
                            SizedBox(
                              width: double.infinity,
                              child: FilledButton(
                                onPressed: () => _goNext(context),
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
                                child: const Center(child: Text('Continue')),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class OptionButton extends StatelessWidget {
  const OptionButton({
    super.key,
    required this.label,
    required this.onPressed,
    required this.selected,
  });

  final String label;
  final VoidCallback onPressed;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final background = selected ? AppColors.primary.withOpacity(0.12) : AppColors.darkSurface;
    final border = selected ? AppColors.primary : AppColors.darkSurfaceBorder;
    final textColor = AppColors.darkBody;

    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ButtonStyle(
          elevation: const MaterialStatePropertyAll(0),
          padding: const MaterialStatePropertyAll(
            EdgeInsets.symmetric(
              vertical: 18,
              horizontal: 20,
            ),
          ),
          backgroundColor: MaterialStatePropertyAll(background),
          foregroundColor: MaterialStatePropertyAll(textColor),
          overlayColor: const MaterialStatePropertyAll(Colors.transparent),
          shape: MaterialStatePropertyAll(
            RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(32),
              side: BorderSide(color: border, width: 2),
            ),
          ),
          textStyle: MaterialStatePropertyAll(
            Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.1,
                ),
          ),
        ),
        child: Align(
          alignment: Alignment.centerLeft,
          child: Text(label),
        ),
      ),
    );
  }
}

