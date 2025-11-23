import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../ui/app_colors.dart';

class PremiumUpgradeScreen extends StatefulWidget {
  const PremiumUpgradeScreen({super.key, this.bookingId = 'ENTER_BOOKING_ID'});

  final String bookingId;

  @override
  State<PremiumUpgradeScreen> createState() => _PremiumUpgradeScreenState();
}

class _PremiumUpgradeScreenState extends State<PremiumUpgradeScreen> {
  late Future<List<_Vehicle>> _vehiclesFuture;

  @override
  void initState() {
    super.initState();
    _vehiclesFuture = _fetchVehicles();
  }

  Future<List<_Vehicle>> _fetchVehicles() async {
    final uri = Uri.parse(
      'https://hackatum25.sixt.io/api/booking/${widget.bookingId}/vehicles',
    );
    try {
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        return _Vehicle.fromApiResponse(jsonDecode(response.body));
      }
    } catch (_) {
      // fall through to fallback below
    }
    // fallback to locally defined mock data
    return _vehicles;
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
            if (snapshot.hasError || !snapshot.hasData || snapshot.data!.isEmpty) {
              return _ErrorState(
                onRetry: () => setState(() => _vehiclesFuture = _fetchVehicles()),
              );
            }
            final vehicles = snapshot.data!;
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
                  return _VehicleCard(vehicle: vehicle);
                },
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
                child: _HeroCard(onTap: () {}),
              ),
            ),
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
                  vehicle: vehicles.last,
                  showPrice: false,
                  showFeatures: false,
                  onTap: () => Navigator.of(context).pushNamed('/protection'),
                ),
              ),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 32)),
          ],
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
                  Text(
                    vehicle.name,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                        ),
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
            if (showFeatures || (showPrice && vehicle.pricePerDay.trim().isNotEmpty))
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

class _Vehicle {
  const _Vehicle({
    required this.name,
    required this.subtitle,
    required this.imageUrl,
    required this.features,
    required this.mileageLabel,
    required this.seatsLabel,
    required this.bagsLabel,
    required this.pricePerDay,
  });

  final String name;
  final String subtitle;
  final String imageUrl;
  final List<String> features;
  final String mileageLabel;
  final String seatsLabel;
  final String bagsLabel;
  final String pricePerDay;

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

      return _Vehicle(
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

const _vehicles = [
  _Vehicle(
    name: 'GMC Acadia',
    subtitle: '2.5 Turbo Elevation FWD',
    imageUrl:
        'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1100&q=80',
    mileageLabel: '~19k miles',
    seatsLabel: '8',
    bagsLabel: '4',
    features: ['Keyless ignition', 'Built-in navigation'],
    pricePerDay: '12,58',
  ),
  _Vehicle(
    name: 'BMW Series 4',
    subtitle: '430i xDrive Convertible',
    imageUrl:
        'https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=1100&q=80',
    mileageLabel: '~2k miles',
    seatsLabel: '4',
    bagsLabel: '3',
    features: ['Heated seats', 'Apple CarPlay'],
    pricePerDay: '25,99',
  ),
  _Vehicle(
    name: 'Audi Q7',
    subtitle: '55 TFSI quattro',
    imageUrl:
        'https://images.unsplash.com/photo-1511919884226-fd3cad34687c?auto=format&fit=crop&w=1100&q=80',
    mileageLabel: '~8k miles',
    seatsLabel: '7',
    bagsLabel: '4',
    features: ['Panoramic roof', 'Matrix LED headlights'],
    pricePerDay: '33,40 EUR /day',
  ),
  _Vehicle(
    name: 'Peugeot 408',
    subtitle: 'Hybrid sedan',
    imageUrl:
        'https://vehicle-pictures-prod.orange.sixt.com/143210/1e1e1e/18_1.png',
    mileageLabel: '~10k miles',
    seatsLabel: '5',
    bagsLabel: '0',
    features: ['Keyless ignition', 'Bluetooth connectivity'],
    pricePerDay: '',
  ),
];

const _fallbackImage =
    'https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?auto=format&fit=crop&w=1100&q=80';

