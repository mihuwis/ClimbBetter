import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/features/profile/widgets/calendar_preview.dart';
import 'package:climbbetter_mobile/features/profile/widgets/load_chart.dart';
import 'package:climbbetter_mobile/features/profile/widgets/profile_stats_panel.dart';
import 'package:climbbetter_mobile/shared/widgets/section_header.dart';

class ProfileTab extends StatelessWidget {
  const ProfileTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: const [
        SectionHeader(title: 'Ty'),
        SizedBox(height: 12),
        ProfileStatsPanel(),
        SizedBox(height: 16),
        SectionHeader(title: 'Kalendarz'),
        SizedBox(height: 12),
        CalendarPreview(),
        SizedBox(height: 16),
        SectionHeader(title: 'Obciążenie'),
        SizedBox(height: 12),
        LoadChart(),
      ],
    );
  }
}
