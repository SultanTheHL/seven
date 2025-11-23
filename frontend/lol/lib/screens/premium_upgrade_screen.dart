import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../services/recommendation_service.dart';
import '../ui/app_colors.dart';

class PremiumUpgradeScreen extends StatefulWidget {
  const PremiumUpgradeScreen({super.key, this.bookingId = 'ENTER_BOOKING_ID'});

  final String bookingId;

  @override
  State<PremiumUpgradeScreen> createState() => _PremiumUpgradeScreenState();
}

class _PremiumUpgradeScreenState extends State<PremiumUpgradeScreen> {
  late Future<List<_Vehicle>> _vehiclesFuture;
  late Future<_Vehicle?> _currentVehicleFuture;

  @override
  void initState() {
    super.initState();
    _vehiclesFuture = _loadVehicles();
    _currentVehicleFuture = _loadCurrentVehicle();
  }

  Future<List<_Vehicle>> _loadVehicles() async {
    print('[PremiumUpgrade] Requesting vehicle list from SIXT API...');
    final vehiclesFuture = _fetchVehiclesFromSixt();
    RecommendationPayload? recommendation = RecommendationService.latestPayload;
    if (recommendation == null) {
      try {
        recommendation = await RecommendationService.fetchRecommendation();
      } catch (_) {
        // ignore recommendation failure, fall back to base data
      }
    }
    final vehicles = await vehiclesFuture;
    if (recommendation == null || recommendation.vehicleRecommendations.isEmpty) {
      return vehicles;
    }
    final recommendedVehicles = _applyRecommendations(vehicles, recommendation.vehicleRecommendations);
    // Only return vehicles that have a rank (are recommended by backend)
    return recommendedVehicles.where((v) => v.rank != null).toList();
  }

  Future<_Vehicle?> _loadCurrentVehicle() async {
    try {
      final uri = Uri.parse(
        'https://hackatum25.sixt.io/api/booking/${widget.bookingId}/vehicles',
      );
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        final json = jsonDecode(response.body) as Map<String, dynamic>;
        final vehicles = _Vehicle.fromApiResponse(json);
        // Find OPEL ASTRA (id: 138cc34b-7c44-4a0d-aa4f-346d0780a68d)
        return vehicles.firstWhere(
          (v) => v.id == '138cc34b-7c44-4a0d-aa4f-346d0780a68d',
          orElse: () => vehicles.isNotEmpty ? vehicles.first : throw Exception('No vehicles found'),
        );
      }
    } catch (error) {
      print('[PremiumUpgrade] Error fetching current vehicle: $error');
    }
    return null;
  }

  Future<List<_Vehicle>> _fetchVehiclesFromSixt() async {
    final uri = Uri.parse(
      'https://hackatum25.sixt.io/api/booking/${widget.bookingId}/vehicles',
    );
    try {
      final response = await http.get(uri);
      print('[PremiumUpgrade] SIXT response status: ${response.statusCode}');
      if (response.statusCode == 200) {
        print('[PremiumUpgrade] Successfully fetched ${response.body.length} bytes.');
        return _Vehicle.fromApiResponse(jsonDecode(response.body));
      }
      return [];
    } catch (error) {
      print('[PremiumUpgrade] Error fetching vehicles: $error');
      return [];
    }
  }

  List<_Vehicle> _applyRecommendations(
    List<_Vehicle> vehicles,
    List<VehicleRecommendation> recommendations,
  ) {
    final recById = {
      for (final rec in recommendations) rec.vehicleId: rec,
    };

    final originalIndex = {
      for (var i = 0; i < vehicles.length; i++) vehicles[i].id: i,
    };

    final updatedVehicles = vehicles.map((vehicle) {
      final rec = recById[vehicle.id];
      if (rec == null) return vehicle;
      return vehicle.copyWith(
        rank: rec.rank,
        recommendationFeedbacks: rec.feedback,
      );
    }).toList();

    // sort by rank (lower rank first)
    updatedVehicles.sort((a, b) {
      final rankA = a.rank ?? 1 << 20;
      final rankB = b.rank ?? 1 << 20;
      if (rankA == rankB) {
        final indexA = originalIndex[a.id] ?? 0;
        final indexB = originalIndex[b.id] ?? 0;
        return indexA.compareTo(indexB);
      }
      return rankA.compareTo(rankB);
    });

    return updatedVehicles;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.darkBackground,
      body: SafeArea(
        child: FutureBuilder<List<_Vehicle>>(
          future: _vehiclesFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return _ErrorState(
                onRetry: () => setState(() {
                  _vehiclesFuture = _loadVehicles();
                  _currentVehicleFuture = _loadCurrentVehicle();
                }),
              );
            }
            if (!snapshot.hasData) {
              return const Center(child: CircularProgressIndicator());
            }
            final vehicles = snapshot.data!;
            return FutureBuilder<_Vehicle?>(
              future: _currentVehicleFuture,
              builder: (context, currentVehicleSnapshot) {
                final currentVehicle = currentVehicleSnapshot.data;
                return CustomScrollView(
                  slivers: [
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                IconButton(
                                  onPressed: () => Navigator.of(context).maybePop(),
                                  icon: const Icon(Icons.arrow_back_ios_new),
                                  color: Colors.white,
                                  tooltip: 'Back',
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(
                                    'Select a premium upgrade now',
                                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                          fontWeight: FontWeight.w800,
                                          color: Colors.white,
                                          height: 1.1,
                                        ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 24),
                          ],
                        ),
                      ),
                    ),
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
                      sliver: SliverList.separated(
                        itemCount: vehicles.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 16),
                        itemBuilder: (context, index) {
                          final vehicle = vehicles[index];
                          return _VehicleCard(
                            vehicle: vehicle,
                            onTap: () => Navigator.of(context).pushNamed('/protection'),
                          );
                        },
                      ),
                    ),
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
                        child: _HeroCard(onTap: () {}),
                      ),
                    ),
                    if (currentVehicle != null) ...[
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(24, 0, 24, 12),
                          child: Text(
                            'OR CONTINUE WITH THE VEHICLE BELOW',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.w800,
                                  color: Colors.white.withOpacity(0.85),
                                  letterSpacing: 0.5,
                                ),
                          ),
                        ),
                      ),
                      SliverPadding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        sliver: SliverToBoxAdapter(
                          child: _VehicleCard(
                            vehicle: currentVehicle,
                            showPrice: false,
                            showFeatures: false,
                            onTap: () => Navigator.of(context).pushNamed('/protection'),
                          ),
                        ),
                      ),
                    ],
                    const SliverToBoxAdapter(child: SizedBox(height: 32)),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }
}

class _HeroCard extends StatelessWidget {
  const _HeroCard({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFF0B1220),
          borderRadius: BorderRadius.circular(24),
          image: const DecorationImage(
            image: NetworkImage(
              'https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=900&q=80',
            ),
            fit: BoxFit.cover,
            colorFilter: ColorFilter.mode(Colors.black54, BlendMode.darken),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 104),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.35),
                borderRadius: BorderRadius.circular(18),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: const [
                  Text(
                    'Show all vehicles',
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                  ),
                  SizedBox(width: 8),
                  Icon(Icons.arrow_forward_ios, color: Colors.white, size: 16),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AnimatedGradientText extends StatefulWidget {
  const _AnimatedGradientText({
    required this.text,
    this.style,
  });

  final String text;
  final TextStyle? style;

  @override
  State<_AnimatedGradientText> createState() => _AnimatedGradientTextState();
}

class _AnimatedGradientTextState extends State<_AnimatedGradientText>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 10),
      vsync: this,
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return ShaderMask(
          shaderCallback: (bounds) {
            final t = _controller.value;
            // Create a smooth continuous cycling gradient
            // Pattern: white -> purple -> white (repeating seamlessly)
            final speed = 0.5; // Скорость движения градиента
            final offset = (t * speed) % 1.0;
            
            // More saturated purple color
            const purple = Color(0xFF7B2CBF); // Более насыщенный фиолетовый
            
            // Create many color stops for smooth transitions
            final stops = <double>[];
            final colors = <Color>[];
            
            // Generate smooth gradient with many intermediate points
            final numPoints = 30; // Больше точек = более плавный переход
            for (int i = 0; i <= numPoints; i++) {
              final pos = i / numPoints;
              stops.add(pos);
              
              // Calculate position in the repeating pattern
              // Pattern repeats every 0.5 (white->purple->white)
              // Add offset to create sliding effect
              final patternPos = ((pos + offset) % 0.5) / 0.5;
              
              if (patternPos < 0.5) {
                // First half: white to purple (smooth transition)
                final progress = patternPos * 2.0;
                // Use smooth step function for very smooth transition
                final smoothProgress = progress * progress * (3.0 - 2.0 * progress); // Smoothstep
                colors.add(Color.lerp(Colors.white, purple, smoothProgress)!);
              } else {
                // Second half: purple to white (smooth transition)
                final progress = (patternPos - 0.5) * 2.0;
                // Use smooth step function for very smooth transition
                final smoothProgress = progress * progress * (3.0 - 2.0 * progress); // Smoothstep
                colors.add(Color.lerp(purple, Colors.white, smoothProgress)!);
              }
            }
            
            return LinearGradient(
              colors: colors,
              begin: Alignment.centerLeft,
              end: Alignment.centerRight,
              stops: stops,
            ).createShader(bounds);
          },
          child: Text(
            widget.text,
            style: widget.style,
          ),
        );
      },
    );
  }
}

class _VehicleCard extends StatelessWidget {
  const _VehicleCard({
    required this.vehicle,
    this.showPrice = true,
    this.showFeatures = true,
    this.onTap,
  });

  final _Vehicle vehicle;
  final bool showPrice;
  final bool showFeatures;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Container(
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF050B14), Color(0xFF1F2634)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.08),
              blurRadius: 18,
              offset: const Offset(0, 12),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          vehicle.name,
                          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w700,
                              ),
                        ),
                      ),
                      if (vehicle.rank != null)
                        _RankBadge(rank: vehicle.rank!),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    vehicle.subtitle,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Colors.white70,
                        ),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _InfoChip(icon: Icons.speed, label: vehicle.mileageLabel),
                      _InfoChip(icon: Icons.event_seat, label: vehicle.seatsLabel),
                      _InfoChip(icon: Icons.luggage, label: vehicle.bagsLabel),
                      TextButton(
                        onPressed: () {},
                        style: TextButton.styleFrom(
                          foregroundColor: Colors.white,
                          padding: EdgeInsets.zero,
                          textStyle: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        child: const Text('More'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            AspectRatio(
              aspectRatio: 16 / 9,
              child: Image.network(
                vehicle.imageUrl,
                fit: BoxFit.cover,
                loadingBuilder: (context, child, loadingProgress) {
                  if (loadingProgress == null) return child;
                  return const Center(
                    child: CircularProgressIndicator(strokeWidth: 2),
                  );
                },
                errorBuilder: (_, __, ___) => Container(
                  color: Colors.black12,
                  alignment: Alignment.center,
                  child: const Icon(Icons.directions_car, size: 56, color: Colors.white38),
                ),
              ),
            ),
            if (showFeatures ||
                (showPrice && vehicle.pricePerDay.trim().isNotEmpty) ||
                vehicle.recommendationFeedbacks.isNotEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (showFeatures)
                      ...vehicle.features.map(
                        (feature) => Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Row(
                            children: [
                              const Icon(Icons.check, color: Colors.greenAccent, size: 18),
                              const SizedBox(width: 8),
                              Text(
                                feature,
                                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                      color: Colors.white,
                                    ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    if (showFeatures && showPrice && vehicle.pricePerDay.trim().isNotEmpty)
                      const SizedBox(height: 8),
                    if (showPrice && vehicle.pricePerDay.trim().isNotEmpty)
                      Text(
                        vehicle.pricePerDay,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              color: Colors.white,
                              fontWeight: FontWeight.w800,
                            ),
                      ),
                    if (vehicle.recommendationFeedbacks.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      _AnimatedGradientText(
                        text: 'Our personalized feedback',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              color: Colors.white,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      const SizedBox(height: 8),
                      ...vehicle.recommendationFeedbacks.map(
                        (text) => Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Icon(Icons.messenger_outline,
                                  size: 16, color: Colors.white70),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  text,
                                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                        color: Colors.white70,
                                      ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
          ],
        ),
      ),
    ),
  );
}

}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.12),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: Colors.white),
          const SizedBox(width: 6),
          Text(
            label,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white),
          ),
        ],
      ),
    );
  }
}

class _RankBadge extends StatelessWidget {
  const _RankBadge({required this.rank});

  final int rank;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.primary.withOpacity(0.15),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: AppColors.primary),
      ),
      child: Text(
        '#$rank',
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: AppColors.primary,
              fontWeight: FontWeight.w700,
            ),
      ),
    );
  }
}

class _Vehicle {
  const _Vehicle({
    required this.id,
    required this.name,
    required this.subtitle,
    required this.imageUrl,
    required this.features,
    required this.mileageLabel,
    required this.seatsLabel,
    required this.bagsLabel,
    required this.pricePerDay,
    this.rank,
    this.recommendationFeedbacks = const [],
  });

  final String id;
  final String name;
  final String subtitle;
  final String imageUrl;
  final List<String> features;
  final String mileageLabel;
  final String seatsLabel;
  final String bagsLabel;
  final String pricePerDay;
  final int? rank;
  final List<String> recommendationFeedbacks;

  static List<_Vehicle> fromApiResponse(Map<String, dynamic> json) {
    final deals = json['deals'] as List<dynamic>? ?? [];
    return deals.map((deal) {
      final vehicle = deal['vehicle'] as Map<String, dynamic>? ?? {};
      final pricing = deal['pricing'] as Map<String, dynamic>? ?? {};
      final displayPrice = pricing['displayPrice'] as Map<String, dynamic>? ?? {};
      final currency = (displayPrice['currency'] ?? 'USD').toString();
      final amount = displayPrice['amount'];
      final formattedPrice = amount == null
          ? '0'
          : double.tryParse('$amount')?.toStringAsFixed(2) ?? '$amount';
      final prefix = displayPrice['prefix']?.toString() ?? '+ ';
      final suffix = displayPrice['suffix']?.toString() ?? '/day';

      final attributes = (vehicle['attributes'] as List<dynamic>? ?? [])
          .cast<Map<String, dynamic>>();

      String attributeValue(String titleFallback, String key) {
        final match = attributes.firstWhere(
          (attr) => attr['key'] == key || attr['title'] == titleFallback,
          orElse: () => const {},
        );
        return match['value']?.toString() ?? titleFallback;
      }

      final upsellFeatures = attributes
          .where((attr) => attr['attributeType'] == 'UPSELL_ATTRIBUTE')
          .map((attr) => attr['title']?.toString() ?? '')
          .where((title) => title.isNotEmpty)
          .toList();

      if (upsellFeatures.isEmpty) {
        final reasons = (vehicle['upsellReasons'] as List<dynamic>? ?? [])
            .cast<Map<String, dynamic>>();
        upsellFeatures.addAll(
          reasons.map((r) => r['title']?.toString() ?? '').where((r) => r.isNotEmpty),
        );
      }

      final images = (vehicle['images'] as List<dynamic>? ?? [])
          .map((e) => e.toString())
          .toList();

      final id = vehicle['id']?.toString() ??
          deal['vehicleId']?.toString() ??
          vehicle['vin']?.toString() ??
          (vehicle['model']?.toString() ?? '');

      return _Vehicle(
        id: id,
        name: '${vehicle['brand'] ?? ''} ${vehicle['model'] ?? ''}'.trim(),
        subtitle: vehicle['modelAnnex']?.toString() ?? '',
        imageUrl: images.isNotEmpty ? images.first : _fallbackImage,
        mileageLabel: attributeValue('~10k miles', 'P100_VEHICLE_ATTRIBUTE_MILEAGE'),
        seatsLabel: vehicle['passengersCount']?.toString() ??
            attributeValue('4', 'P100_VEHICLE_ATTRIBUTE_SEATS'),
        bagsLabel: vehicle['bagsCount']?.toString() ??
            attributeValue('3', 'P100_VEHICLE_ATTRIBUTE_BOOT_CAPACITY'),
        features: upsellFeatures.isNotEmpty ? upsellFeatures : ['Premium interior'],
        pricePerDay: '${prefix.trim()} $formattedPrice ${currency.toUpperCase()} $suffix',
      );
    }).toList();
  }

  _Vehicle copyWith({
    int? rank,
    List<String>? recommendationFeedbacks,
  }) {
    return _Vehicle(
      id: id,
      name: name,
      subtitle: subtitle,
      imageUrl: imageUrl,
      features: features,
      mileageLabel: mileageLabel,
      seatsLabel: seatsLabel,
      bagsLabel: bagsLabel,
      pricePerDay: pricePerDay,
      rank: rank ?? this.rank,
      recommendationFeedbacks:
          recommendationFeedbacks ?? this.recommendationFeedbacks,
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.wifi_off, size: 48, color: Colors.white54),
          const SizedBox(height: 12),
          Text(
            'Unable to load vehicles',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Colors.white70),
          ),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: onRetry,
            style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }
}

const _fallbackImage =
    'https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=1100&q=80';

