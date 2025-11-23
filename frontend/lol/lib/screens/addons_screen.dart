import 'package:flutter/material.dart';

import '../ui/app_colors.dart';

class AddonsScreen extends StatefulWidget {
  const AddonsScreen({super.key});

  @override
  State<AddonsScreen> createState() => _AddonsScreenState();
}

class _AddonsScreenState extends State<AddonsScreen> {
  int _selectedIndex = 0;

  static final List<_AddonOption> _options = [
    // Platinum Plan first
    _AddonOption(
      title: 'Platinum Plan',
      subtitle: 'Ultimate coverage bundle',
      features: const [
        'Everything in Golden',
        'International coverage',
        'Zero deductible',
        'Spare vehicle guarantee',
        'Premium GPS & connectivity',
        'Winter & off-road gear',
        'Luxury lounge access',
        '24/7 trip concierge',
        'Accident forgiveness',
        'Complimentary detailing',
      ],
      background: const Color(0x33F8F7F3),
    ),
    // Golden Plan second
    _AddonOption(
      title: 'Golden Plan',
      subtitle: 'Best for most drivers',
      features: const [
        'Priority customer support',
        'Roadside assistance',
        'Full damage waiver',
      ],
      background: const Color(0x33FFC857),
    ),
    // No plan third
    _AddonOption(
      title: 'No plan',
      subtitle: 'Stick with the basics',
      features: const [
        'Standard manufacturer warranty only',
      ],
    ),
  ];

  void _select(int index) {
    setState(() => _selectedIndex = index);
  }

  void _finish(BuildContext context) {
    Navigator.of(context).pushNamed('/thank-you');
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
                    'Add-ons',
                    style: textTheme.titleMedium?.copyWith(
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 32),
              Text(
                'Choose add-ons to\ncomplete your plan',
                style: textTheme.displaySmall?.copyWith(
                  fontSize: 32,
                  fontWeight: FontWeight.w700,
                  height: 1.2,
                  color: AppColors.darkBody,
                ),
              ),
              const SizedBox(height: 24),
              Expanded(
                child: ListView.separated(
                  itemCount: _options.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 16),
                  itemBuilder: (context, index) {
                    return _AddonCard(
                      option: _options[index],
                      selected: _selectedIndex == index,
                      onTap: () => _select(index),
                    );
                  },
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => _finish(context),
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
                  child: const Text('Complete purchase'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AddonCard extends StatelessWidget {
  const _AddonCard({
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final _AddonOption option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final baseBackground = option.background ?? AppColors.darkSurface;
    final borderColor = selected ? AppColors.primary : AppColors.darkSurfaceBorder;
    final background = selected
        ? baseBackground.withOpacity((baseBackground.opacity + 0.1).clamp(0, 1))
        : baseBackground;
    final titleColor = AppColors.darkBody;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(28),
        splashColor: Colors.transparent,
        hoverColor: AppColors.iconHover,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: borderColor, width: 2),
            color: background,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                option.title,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: titleColor,
                      fontWeight: FontWeight.w700,
                    ),
              ),
              const SizedBox(height: 4),
              Text(
                option.subtitle,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppColors.darkSubtleText,
                    ),
              ),
              const SizedBox(height: 12),
              ...option.features.map(
                (feature) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        '• ',
                        style: TextStyle(color: AppColors.darkBody),
                      ),
                      Expanded(
                        child: Text(
                          feature,
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: AppColors.darkBody,
                              ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AddonOption {
  const _AddonOption({
    required this.title,
    required this.subtitle,
    required this.features,
    this.background,
  });

  final String title;
  final String subtitle;
  final List<String> features;
  final Color? background;
}

