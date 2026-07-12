import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/features/home/widgets/training_session_card.dart';
import 'package:climbbetter_mobile/features/home/widgets/training_summary_strip.dart';
import 'package:climbbetter_mobile/shared/data/mock_training_sessions.dart';
import 'package:climbbetter_mobile/shared/widgets/section_header.dart';

class HomeTab extends StatelessWidget {
  const HomeTab({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        const SectionHeader(title: 'Ostatnie treningi'),
        const SizedBox(height: 12),
        const TrainingSummaryStrip(),
        const SizedBox(height: 16),
        for (final session in recentSessions) ...[
          TrainingSessionCard(session: session),
          const SizedBox(height: 12),
        ],
      ],
    );
  }
}
