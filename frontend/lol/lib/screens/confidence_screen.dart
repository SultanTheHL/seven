import 'package:flutter/material.dart';

import '../services/recommendation_service.dart';
import '../state/recommendation_form_state.dart';
import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class ConfidenceScreen extends StatefulWidget {
  const ConfidenceScreen({super.key});

  @override
  State<ConfidenceScreen> createState() => _ConfidenceScreenState();
}

class _ConfidenceScreenState extends State<ConfidenceScreen> {
  int _selectedIndex = 0;
  bool _submitting = false;
  Future<void> _submit() async {
    if (_submitting) return;
    setState(() => _submitting = true);
    RecommendationFormState.instance
        .updateDrivingConfidence(_options[_selectedIndex].title);
    try {
      final dto = RecommendationFormState.instance.buildRequest();
      final bookingId = RecommendationFormState.instance.bookingId;
      final payload = await RecommendationService.submitRecommendation(
        dto,
        bookingId: bookingId,
      );
      RecommendationService.cachePayload(payload);
      if (!mounted) return;
      Navigator.of(context).pushNamed('/upgrades');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to fetch recommendations: $error')),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }


  static const List<_ConfidenceOption> _options = [
    _ConfidenceOption(
      title: 'Comfortable in any condition',
      description: 'For the experienced, all-weather driver.',
      icon: Icons.shield,
    ),
    _ConfidenceOption(
      title: 'I prefer extra safety features',
      description: 'Prefers modern driver-assistance systems.',
      icon: Icons.settings_input_component,
    ),
    _ConfidenceOption(
      title: 'I need condition-specific gear',
      description: 'For features like winter tires or AWD.',
      icon: Icons.ac_unit,
    ),
  ];

  void _onSelect(int index) {
    setState(() => _selectedIndex = index);
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
                    'Unfamiliar Conditions',
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                'Step 6 of 6',
                style: textTheme.bodyMedium?.copyWith(
                  color: AppColors.darkSubtleText,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 12),
              const StepIndicator(
                progress: 1,
                fillColor: AppColors.primary,
                trackColor: AppColors.darkProgressTrack,
                height: 6,
              ),
              const SizedBox(height: 32),
              Text(
                'How confident are you\nwith unfamiliar conditions?',
                style: textTheme.displaySmall?.copyWith(
                  fontSize: 32,
                  color: AppColors.darkBody,
                  fontWeight: FontWeight.w700,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 28),
              Expanded(
                child: ListView.separated(
                  itemCount: _options.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 18),
                  itemBuilder: (context, index) {
                    return _ConfidenceTile(
                      option: _options[index],
                      selected: _selectedIndex == index,
                      onTap: () => _onSelect(index),
                    );
                  },
                ),
              ),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _submitting ? null : _submit,
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
                  child: _submitting
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Text('Show me my cars'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConfidenceTile extends StatelessWidget {
  const _ConfidenceTile({
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final _ConfidenceOption option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final borderColor = selected ? AppColors.primary : AppColors.darkSurfaceBorder;
    final iconBackground = selected ? AppColors.primary.withOpacity(0.15) : AppColors.darkSurface;
    final iconColor = selected ? AppColors.primary : AppColors.darkBody;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(26),
        splashColor: Colors.transparent,
        hoverColor: AppColors.iconHover,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(26),
            border: Border.all(color: borderColor, width: 2),
            color: AppColors.darkSurface,
          ),
          child: Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  color: iconBackground,
                ),
                child: Icon(option.icon, size: 28, color: iconColor),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      option.title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: AppColors.darkBody,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      option.description,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppColors.darkSubtleText,
                          ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
            ],
          ),
        ),
      ),
    );
  }
}

class _ConfidenceOption {
  const _ConfidenceOption({
    required this.title,
    required this.description,
    required this.icon,
  });

  final String title;
  final String description;
  final IconData icon;
}

