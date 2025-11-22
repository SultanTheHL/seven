import 'package:flutter/material.dart';

import '../ui/app_colors.dart';
import '../widgets/step_indicator.dart';

class RouteScreen extends StatefulWidget {
  const RouteScreen({super.key});

  @override
  State<RouteScreen> createState() => _RouteScreenState();
}

class _RouteScreenState extends State<RouteScreen> {
  final _fromController = TextEditingController();
  final _toController = TextEditingController();
  final List<String> _stops = [];

  @override
  void dispose() {
    _fromController.dispose();
    _toController.dispose();
    super.dispose();
  }

  Future<void> _promptAddStop() async {
    final controller = TextEditingController();
    final stopName = await showDialog<String>(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.darkSurface,
          title: const Text('Add stop / city'),
          content: TextField(
            controller: controller,
            autofocus: true,
            decoration: const InputDecoration(
              hintText: 'e.g. Lyon, France',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () {
                final text = controller.text.trim();
                if (text.isNotEmpty) {
                  Navigator.of(context).pop(text);
                }
              },
              child: const Text('Add'),
            ),
          ],
        );
      },
    );

    if (stopName != null && stopName.trim().isNotEmpty) {
      setState(() {
        _stops.add(stopName.trim());
      });
    }
  }

  void _removeStop(int index) {
    setState(() {
      _stops.removeAt(index);
    });
  }

  void _goNext() {
    if (_fromController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter your starting city')),
      );
      return;
    }
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
                    'Trip Route',
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
                      fontWeight: FontWeight.w500,
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
                'Where are you driving?',
                style: Theme.of(context).textTheme.displaySmall?.copyWith(
                      fontSize: 32,
                      color: AppColors.darkBody,
                      fontWeight: FontWeight.w700,
                      height: 1.2,
                    ),
              ),
              const SizedBox(height: 24),
              _RouteField(
                label: 'Start from',
                hint: 'e.g. Paris, France',
                controller: _fromController,
              ),
              const SizedBox(height: 16),
              _RouteField(
                label: 'Go to (optional)',
                hint: 'Leave empty if returning to the start city',
                controller: _toController,
              ),
              const SizedBox(height: 24),
              OutlinedButton.icon(
                onPressed: _promptAddStop,
                icon: const Icon(Icons.add),
                label: const Text('Add intermediate stops / cities'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.primary,
                  side: const BorderSide(color: AppColors.primary),
                  padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(28),
                  ),
                ),
              ),
              if (_stops.isNotEmpty) ...[
                const SizedBox(height: 16),
                Expanded(
                  child: SingleChildScrollView(
                    child: Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        for (int i = 0; i < _stops.length; i++)
                          Chip(
                            label: Text(_stops[i]),
                            deleteIcon: const Icon(Icons.close, size: 18),
                            onDeleted: () => _removeStop(i),
                            backgroundColor: AppColors.darkSurface,
                            labelStyle: const TextStyle(color: AppColors.darkBody),
                          ),
                      ],
                    ),
                  ),
                ),
              ] else
                const Spacer(),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _goNext,
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

class _RouteField extends StatelessWidget {
  const _RouteField({
    required this.label,
    required this.hint,
    required this.controller,
  });

  final String label;
  final String hint;
  final TextEditingController controller;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppColors.darkBody,
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          style: const TextStyle(color: AppColors.darkBody),
          decoration: InputDecoration(
            hintText: hint,
            filled: true,
            fillColor: AppColors.darkSurface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(20),
              borderSide: const BorderSide(color: AppColors.darkSurfaceBorder, width: 2),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(20),
              borderSide: const BorderSide(color: AppColors.darkSurfaceBorder, width: 2),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(20),
              borderSide: const BorderSide(color: AppColors.primary, width: 2),
            ),
          ),
        ),
      ],
    );
  }
}

