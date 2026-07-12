import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/shared/widgets/field_panel.dart';

class LoadChart extends StatelessWidget {
  const LoadChart({super.key});

  static const loads = [70, 120, 95, 140, 80, 155, 110];

  @override
  Widget build(BuildContext context) {
    final maxLoad = loads.reduce((a, b) => a > b ? a : b);

    return FieldPanel(
      child: SizedBox(
        height: 180,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            for (var index = 0; index < loads.length; index++) ...[
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Expanded(
                      child: Align(
                        alignment: Alignment.bottomCenter,
                        child: FractionallySizedBox(
                          heightFactor: loads[index] / maxLoad,
                          widthFactor: 0.72,
                          alignment: Alignment.bottomCenter,
                          child: Container(
                            decoration: BoxDecoration(
                              color: index == loads.length - 1
                                  ? const Color(0xFFD45D3F)
                                  : const Color(0xFF1F7A58),
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text('W${index + 1}'),
                  ],
                ),
              ),
              if (index != loads.length - 1) const SizedBox(width: 6),
            ],
          ],
        ),
      ),
    );
  }
}
