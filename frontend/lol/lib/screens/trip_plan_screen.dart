import 'package:flutter/material.dart';

import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class TripPlanScreen extends StatefulWidget {
  const TripPlanScreen({super.key});

  @override
  State<TripPlanScreen> createState() => _TripPlanScreenState();
}

class _TripPlanScreenState extends State<TripPlanScreen> {
  final TextEditingController _startController = TextEditingController();
  final TextEditingController _endController = TextEditingController();
  final List<TextEditingController> _stopControllers = [];

  @override
  void dispose() {
    _startController.dispose();
    _endController.dispose();
    for (final controller in _stopControllers) {
      controller.dispose();
    }
    super.dispose();
  }

  void _addStopField() {
    setState(() {
      _stopControllers.add(TextEditingController());
    });
  }

  void _removeStopAt(int index) {
    setState(() {
      final controller = _stopControllers.removeAt(index);
      controller.dispose();
    });
  }

  void _goNext() {
    final start = _startController.text.trim();
    if (start.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter your starting city.')),
      );
      return;
    }

    final hasEmptyStop = _stopControllers.any((controller) => controller.text.trim().isEmpty);
    if (hasEmptyStop) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please fill in or remove empty city fields.')),
      );
      return;
    }

    final end = _endController.text.trim().isEmpty ? start : _endController.text.trim();
    final stops = _stopControllers.map((c) => c.text.trim()).where((c) => c.isNotEmpty).toList();
    debugPrint('Trip start: $start, end: $end, stops: $stops');
    Navigator.of(context).pushNamed('/passengers');
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
                    'Trip Plan',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: AppColors.darkBody,
                          fontWeight: FontWeight.w600,
                        ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                'Step 1 of 6',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppColors.darkSubtleText,
                      fontWeight: FontWeight.w600,
                    ),
              ),
              const SizedBox(height: 12),
              const StepIndicator(
                progress: 1 / 6,
                fillColor: AppColors.primary,
                trackColor: AppColors.darkProgressTrack,
                height: 6,
              ),
              const SizedBox(height: 32),
              Text(
                'Which cities are you visiting?',
                style: Theme.of(context).textTheme.displaySmall?.copyWith(
                      fontSize: 32,
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w800,
                      height: 1.2,
                    ),
              ),
              const SizedBox(height: 28),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _TripField(
                        label: 'Pickup city',
                        hint: 'e.g. Berlin',
                        controller: _startController,
                      ),
                      const SizedBox(height: 16),
                      OutlinedButton.icon(
                        onPressed: _addStopField,
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: AppColors.primary, width: 2),
                          foregroundColor: AppColors.primary,
                          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 20),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                          textStyle: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
                        ),
                        icon: const Icon(Icons.add),
                        label: const Text('Add city'),
                      ),
                      if (_stopControllers.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        ReorderableListView.builder(
                          physics: const NeverScrollableScrollPhysics(),
                          shrinkWrap: true,
                          buildDefaultDragHandles: false,
                          proxyDecorator: (child, _, __) => Material(color: Colors.transparent, child: child),
                          itemCount: _stopControllers.length,
                          onReorder: (oldIndex, newIndex) {
                            setState(() {
                              if (newIndex > oldIndex) newIndex -= 1;
                              final controller = _stopControllers.removeAt(oldIndex);
                              _stopControllers.insert(newIndex, controller);
                            });
                          },
                          itemBuilder: (context, index) {
                            return Padding(
                              key: ValueKey('stop_$index'),
                              padding: const EdgeInsets.only(bottom: 12),
                              child: _TripField(
                                label: '',
                                hint: 'Enter city',
                                controller: _stopControllers[index],
                                prefix: ReorderableDragStartListener(
                                  index: index,
                                  child: const Icon(Icons.drag_indicator, color: AppColors.darkSubtleText),
                                ),
                                suffix: IconButton(
                                  onPressed: () => _removeStopAt(index),
                                  icon: const Icon(Icons.close, size: 18),
                                  color: AppColors.darkSubtleText,
                                  splashRadius: 18,
                                  visualDensity: VisualDensity.compact,
                                ),
                              ),
                            );
                          },
                        ),
                      ],
                      const SizedBox(height: 20),
                      _TripField(
                        label: 'Drop-off city (optional)',
                        hint: 'Leave empty if returning',
                        controller: _endController,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _goNext,
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                    textStyle: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
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

class _TripField extends StatelessWidget {
  const _TripField({
    required this.label,
    required this.hint,
    required this.controller,
    this.prefix,
    this.suffix,
  });

  final String label;
  final String hint;
  final TextEditingController controller;
  final Widget? prefix;
  final Widget? suffix;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (label.isNotEmpty) ...[
          Text(
            label,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: AppColors.darkBody,
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 8),
        ],
        TextField(
          controller: controller,
          style: const TextStyle(color: AppColors.darkBody),
          decoration: InputDecoration(
            hintText: hint,
            filled: true,
            hintStyle: const TextStyle(color: AppColors.darkSubtleText),
            fillColor: AppColors.darkSurface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(28),
              borderSide: const BorderSide(color: AppColors.darkSurfaceBorder, width: 2),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(28),
              borderSide: const BorderSide(color: AppColors.darkSurfaceBorder, width: 2),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(28),
              borderSide: const BorderSide(color: AppColors.primary, width: 2),
            ),
            prefixIcon: prefix != null
                ? Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: prefix,
                  )
                : null,
            prefixIconConstraints: const BoxConstraints(minWidth: 44, minHeight: 44),
            suffixIcon: suffix != null
                ? Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: suffix,
                  )
                : null,
            suffixIconConstraints: const BoxConstraints(minWidth: 44, minHeight: 44),
          ),
        ),
      ],
    );
  }
}

