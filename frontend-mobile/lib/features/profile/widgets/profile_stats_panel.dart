import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/shared/widgets/field_panel.dart';
import 'package:climbbetter_mobile/shared/widgets/summary_tile.dart';

class ProfileStatsPanel extends StatelessWidget {
  const ProfileStatsPanel({super.key});

  @override
  Widget build(BuildContext context) {
    return FieldPanel(
      child: Row(
        children: const [
          Expanded(
            child: SummaryTile(value: '47', label: 'treningów'),
          ),
          SizedBox(width: 10),
          Expanded(
            child: SummaryTile(value: '2/3', label: 'tydzień'),
          ),
          SizedBox(width: 10),
          Expanded(
            child: SummaryTile(value: '125', label: 'max pkt'),
          ),
        ],
      ),
    );
  }
}
