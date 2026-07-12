import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/shared/widgets/summary_tile.dart';

class TrainingSummaryStrip extends StatelessWidget {
  const TrainingSummaryStrip({super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: const [
        Expanded(
          child: SummaryTile(value: '3', label: 'sesje'),
        ),
        SizedBox(width: 10),
        Expanded(
          child: SummaryTile(value: '385', label: 'pkt'),
        ),
        SizedBox(width: 10),
        Expanded(
          child: SummaryTile(value: '4 dni', label: 'streak'),
        ),
      ],
    );
  }
}
