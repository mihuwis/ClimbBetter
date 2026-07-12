import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/shared/models/training_session.dart';
import 'package:climbbetter_mobile/shared/widgets/metric_chip.dart';

class TrainingSessionCard extends StatelessWidget {
  const TrainingSessionCard({super.key, required this.session});

  final TrainingSession session;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: const BorderSide(color: Color(0xFFE3DED6)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 4,
                  height: 42,
                  decoration: BoxDecoration(
                    color: session.accent,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        session.title,
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0,
                            ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${session.dateLabel} · ${session.place}',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: const Color(0xFF6B625A),
                        ),
                      ),
                    ],
                  ),
                ),
                PopupMenuButton<SessionAction>(
                  key: Key('session-menu-${session.id}'),
                  icon: const Icon(Icons.more_horiz),
                  tooltip: 'Opcje treningu',
                  onSelected: (action) {
                    if (action == SessionAction.repeat) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Ponawiam: ${session.title}')),
                      );
                    }
                  },
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: SessionAction.repeat,
                      child: Row(
                        children: [
                          Icon(Icons.refresh, size: 20),
                          SizedBox(width: 10),
                          Text('Ponów trening'),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text(session.note, style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 14),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                MetricChip(icon: Icons.bolt, label: '${session.points} pkt'),
                MetricChip(icon: Icons.timer_outlined, label: session.duration),
                MetricChip(
                  icon: Icons.format_list_numbered,
                  label: '${session.entries} wpisów',
                ),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final grade in session.grades)
                  Chip(
                    label: Text(grade),
                    visualDensity: VisualDensity.compact,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
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

enum SessionAction { repeat }
