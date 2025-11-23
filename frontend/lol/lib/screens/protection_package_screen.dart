import 'package:flutter/material.dart';

import '../services/recommendation_service.dart';
import '../ui/app_colors.dart';

class _Feedback {
  const _Feedback({required this.text, required this.isPositive});

  final String text;
  final bool isPositive;
}

class ProtectionPackageScreen extends StatefulWidget {
  const ProtectionPackageScreen({super.key});

  @override
  State<ProtectionPackageScreen> createState() => _ProtectionPackageScreenState();
}

class _ProtectionPackageScreenState extends State<ProtectionPackageScreen> {
  int _selectedIndex = 0;
  final Map<int, bool> _expanded = {0: true};
  List<_Feedback> _feedbackAll = const [];
  List<_Feedback> _feedbackSmart = const [];
  List<_Feedback> _feedbackBasic = const [];
  List<_Feedback> _feedbackNone = const [];
  @override
  void initState() {
    super.initState();
    _loadProtectionFeedback();
  }

  Future<void> _loadProtectionFeedback() async {
    try {
      final payload = await RecommendationService.fetchRecommendation();
      final protection = payload.protectionFeedback;
      setState(() {
        _feedbackAll = _convertFeedback(protection.all);
        _feedbackSmart = _convertFeedback(protection.smart);
        _feedbackBasic = _convertFeedback(protection.basic);
        _feedbackNone = _convertFeedback(protection.none);
      });
    } catch (_) {
      // silently ignore, keep fallback lists empty
    }
  }

  List<_Feedback> _convertFeedback(List<TypedFeedback> entries) {
    if (entries.isEmpty) return const [];
    return entries
        .map((entry) => _Feedback(text: entry.text, isPositive: entry.isPositive))
        .toList();
  }

  void _toggleExpand(int index) {
    setState(() {
      _expanded[index] = !(_expanded[index] ?? false);
    });
  }

  void _select(int index) {
    setState(() => _selectedIndex = index);
  }

  void _handleContinue() {
    if (_selectedIndex != 0) {
      // Show confirmation dialog for non-All Inclusive Protection
      showDialog(
        context: context,
        builder: (context) => _ConfirmationDialog(
          feedbacks: _feedbackAll,
          onBuy: () {
            Navigator.of(context).pop(); // Close dialog
            Navigator.of(context).pushNamed('/addons');
          },
          onNoThanks: () {
            Navigator.of(context).pop(); // Close dialog
            Navigator.of(context).pushNamed('/addons');
          },
        ),
      );
    } else {
      // All Inclusive Protection - proceed directly
      Navigator.of(context).pushNamed('/addons');
    }
  }

  double _getTotalPrice() {
    switch (_selectedIndex) {
      case 0:
        return 37.83;
      case 1:
        return 19.85;
      case 2:
        return 8.32;
      case 3:
        return 0.0;
      default:
        return 0.0;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBackground,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 8),
                      IconButton(
                        onPressed: () => Navigator.of(context).maybePop(),
                        icon: const Icon(Icons.arrow_back_ios_new),
                        color: AppColors.darkBody,
                        tooltip: 'Back',
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'WHICH PROTECTION PACKAGE DO YOU NEED?',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.darkBody,
                              fontSize: 24,
                              height: 1.2,
                            ),
                      ),
                      const SizedBox(height: 32),
                      _ProtectionCard(
                        index: 0,
                        title: 'All Inclusive Protection',
                        deductibleText: 'No deductible',
                        deductibleColor: Colors.green,
                        rating: 3.5,
                        price: '37,83 € / day',
                        originalPrice: '54,05 €/day',
                        discount: '- 30% online discount',
                        feedbacks: _feedbackAll,
                        feedback: null,
                        features: const [
                          'Loss damage waiver for collision damages, scratches, bumps and theft',
                          'Tyre and Windscreen Protection',
                          'Interior Protection',
                          'Mobility service',
                          'Personal accident insurance',
                        ],
                        isSelected: _selectedIndex == 0,
                        isExpanded: _expanded[0] ?? false,
                        onSelect: () => _select(0),
                        onToggleExpand: () => _toggleExpand(0),
                      ),
                      const SizedBox(height: 16),
                      _ProtectionCard(
                        index: 1,
                        title: 'Smart Protection',
                        deductibleText: 'No deductible',
                        deductibleColor: Colors.green,
                        rating: 2.5,
                        price: '19,85 € / day',
                        originalPrice: '30,08€/day',
                        discount: '- 34% online discount',
                        feedbacks: _feedbackSmart,
                        feedback: null,
                        features: const [
                          'Loss damage waiver for collision damages, scratches, bumps and theft',
                          'Tyre and Windscreen Protection',
                        ],
                        excludedFeatures: const [
                          'Interior Protection',
                          'Mobility service',
                          'Personal accident insurance',
                        ],
                        isSelected: _selectedIndex == 1,
                        isExpanded: _expanded[1] ?? false,
                        onSelect: () => _select(1),
                        onToggleExpand: () => _toggleExpand(1),
                      ),
                      const SizedBox(height: 16),
                      _ProtectionCard(
                        index: 2,
                        title: 'Basic Protection',
                        deductibleText: 'Deductible: up to €1,100.00',
                        deductibleColor: Colors.red,
                        rating: 1.5,
                        price: '8,32 € / day',
                        originalPrice: null,
                        discount: null,
                        feedbacks: _feedbackBasic,
                        feedback: null,
                        features: const [
                          'Loss damage waiver for collision damages, scratches, bumps and theft',
                        ],
                        excludedFeatures: const [
                          'Tyre and Windscreen Protection',
                          'Interior Protection',
                          'Mobility service',
                          'Personal accident insurance',
                        ],
                        isSelected: _selectedIndex == 2,
                        isExpanded: _expanded[2] ?? false,
                        onSelect: () => _select(2),
                        onToggleExpand: () => _toggleExpand(2),
                      ),
                      const SizedBox(height: 16),
                      _ProtectionCard(
                        index: 3,
                        title: 'No extra protection',
                        deductibleText: 'Deductible: up to full vehicle value',
                        deductibleColor: Colors.red,
                        rating: 0.5,
                        price: null,
                        originalPrice: null,
                        discount: null,
                        includedText: 'Included',
                        feedbacks: _feedbackNone,
                        feedback: null,
                        features: const [],
                        excludedFeatures: const [
                          'Loss damage waiver for collision damages, scratches, bumps and theft',
                          'Tyre and Windscreen Protection',
                          'Interior Protection',
                          'Mobility service',
                          'Personal accident insurance',
                        ],
                        isSelected: _selectedIndex == 3,
                        isExpanded: _expanded[3] ?? false,
                        onSelect: () => _select(3),
                        onToggleExpand: () => _toggleExpand(3),
                      ),
                      const SizedBox(height: 24),
                    ],
                  ),
                ),
              ),
            ),
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: AppColors.darkSurface,
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.3),
                    blurRadius: 10,
                    offset: const Offset(0, -2),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Total',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.w700,
                                  color: AppColors.darkBody,
                                ),
                          ),
                          const SizedBox(height: 4),
                          TextButton(
                            onPressed: () {},
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: Size.zero,
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            child: Text(
                              'Price details',
                              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                    color: AppColors.primary,
                                    fontWeight: FontWeight.w500,
                                  ),
                            ),
                          ),
                        ],
                      ),
                      Text(
                        '${_getTotalPrice().toStringAsFixed(2)} €',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w800,
                              color: AppColors.darkBody,
                              fontSize: 24,
                            ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: _handleContinue,
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 18),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        textStyle: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w700,
                              color: Colors.white,
                            ),
                      ),
                      child: const Text('Continue'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ProtectionCard extends StatelessWidget {
  const _ProtectionCard({
    required this.index,
    required this.title,
    required this.deductibleText,
    required this.deductibleColor,
    required this.rating,
    this.price,
    this.originalPrice,
    this.discount,
    this.includedText,
    this.feedbacks = const [],
    this.feedback,
    required this.features,
    this.excludedFeatures = const [],
    required this.isSelected,
    required this.isExpanded,
    required this.onSelect,
    required this.onToggleExpand,
  });

  final int index;
  final String title;
  final String deductibleText;
  final Color deductibleColor;
  final double rating;
  final String? price;
  final String? originalPrice;
  final String? discount;
  final String? includedText;
  final List<_Feedback> feedbacks;
  final String? feedback;
  final List<String> features;
  final List<String> excludedFeatures;
  final bool isSelected;
  final bool isExpanded;
  final VoidCallback onSelect;
  final VoidCallback onToggleExpand;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onSelect,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppColors.darkSurface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected ? AppColors.primary : AppColors.darkSurfaceBorder,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                GestureDetector(
                  onTap: onSelect,
                  child: Container(
                    width: 24,
                    height: 24,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: isSelected ? AppColors.primary : AppColors.darkSubtleText,
                        width: 2,
                      ),
                      color: AppColors.darkSurface,
                    ),
                    child: isSelected
                        ? Center(
                            child: Container(
                              width: 12,
                              height: 12,
                              decoration: const BoxDecoration(
                                shape: BoxShape.circle,
                                color: AppColors.primary,
                              ),
                            ),
                          )
                        : null,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w700,
                              color: AppColors.darkBody,
                            ),
                      ),
                      if (feedbacks.isNotEmpty) ...[
                        const SizedBox(height: 4),
                        ...feedbacks.map(
                          (feedback) => Padding(
                            padding: const EdgeInsets.only(bottom: 2),
                            child: Text(
                              feedback.text,
                              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                    color: feedback.isPositive ? Colors.green : Colors.red,
                                    fontWeight: FontWeight.w500,
                                  ),
                            ),
                          ),
                        ),
                      ],
                      const SizedBox(height: 4),
                      Text(
                        deductibleText,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: deductibleColor,
                              fontWeight: FontWeight.w500,
                            ),
                      ),
                    ],
                  ),
                ),
                GestureDetector(
                  onTap: onToggleExpand,
                  child: Icon(
                    isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                    color: AppColors.darkSubtleText,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _StarRating(rating: rating),
                const Spacer(),
                if (price != null) ...[
                  if (originalPrice != null) ...[
                    Text(
                      originalPrice!,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppColors.darkSubtleText,
                            decoration: TextDecoration.lineThrough,
                          ),
                    ),
                    const SizedBox(width: 8),
                  ],
                  Text(
                    price!,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.darkBody,
                        ),
                  ),
                ] else if (includedText != null) ...[
                  Text(
                    includedText!,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.darkBody,
                        ),
                  ),
                ],
              ],
            ),
            if (discount != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  discount!,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                      ),
                ),
              ),
            ],
            if (isExpanded && (features.isNotEmpty || excludedFeatures.isNotEmpty)) ...[
              const SizedBox(height: 16),
              if (features.isNotEmpty)
                ...features.map(
                  (feature) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.check, color: Colors.greenAccent, size: 18),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            feature,
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: AppColors.darkBody,
                                ),
                          ),
                        ),
                        const SizedBox(width: 8),
                      ],
                    ),
                  ),
                ),
              if (excludedFeatures.isNotEmpty)
                ...excludedFeatures.map(
                  (feature) => Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.close, color: Colors.grey, size: 18),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            feature,
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: AppColors.darkSubtleText,
                                ),
                          ),
                        ),
                        const SizedBox(width: 8),
                      ],
                    ),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StarRating extends StatelessWidget {
  const _StarRating({required this.rating});

  final double rating;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        if (rating >= index + 1) {
          return const Icon(Icons.star, color: AppColors.primary, size: 18);
        } else if (rating > index) {
          return const Icon(Icons.star_half, color: AppColors.primary, size: 18);
        } else {
          return Icon(Icons.star_border, color: AppColors.darkSubtleText, size: 18);
        }
      }),
    );
  }
}

class _ConfirmationDialog extends StatelessWidget {
  const _ConfirmationDialog({
    required this.onBuy,
    required this.onNoThanks,
    required this.feedbacks,
  });

  final VoidCallback onBuy;
  final VoidCallback onNoThanks;
  final List<_Feedback> feedbacks;

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: AppColors.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(24),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close),
                  color: AppColors.darkBody,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    'Are you sure',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: AppColors.darkBody,
                        ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.darkBackground,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: AppColors.primary.withOpacity(0.3),
                  width: 1,
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'All Inclusive Protection',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: AppColors.darkBody,
                        ),
                  ),
                  const SizedBox(height: 12),
                  if (feedbacks.isEmpty)
                    Text(
                      'No live feedback available yet.',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppColors.darkSubtleText,
                          ),
                    )
                  else
                    ...feedbacks.map(
                      (feedback) => Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(
                              feedback.isPositive ? Icons.thumb_up : Icons.thumb_down,
                              size: 16,
                              color: feedback.isPositive ? Colors.green : Colors.redAccent,
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                feedback.text,
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
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: onNoThanks,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.darkBody,
                      side: BorderSide(color: AppColors.darkSurfaceBorder),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      textStyle: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    child: const Text('No thanks'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: onBuy,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      textStyle: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                          ),
                    ),
                    child: const Text('Buy'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

