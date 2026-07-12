import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/features/profile/widgets/calendar_weekday.dart';
import 'package:climbbetter_mobile/shared/widgets/field_panel.dart';

class CalendarPreview extends StatelessWidget {
  const CalendarPreview({super.key});

  static const activeDays = {2, 5, 9, 12, 15, 19, 22, 26};

  @override
  Widget build(BuildContext context) {
    return FieldPanel(
      child: Column(
        children: [
          Row(
            children: const [
              Expanded(child: CalendarWeekday(label: 'P')),
              Expanded(child: CalendarWeekday(label: 'W')),
              Expanded(child: CalendarWeekday(label: 'Ś')),
              Expanded(child: CalendarWeekday(label: 'C')),
              Expanded(child: CalendarWeekday(label: 'P')),
              Expanded(child: CalendarWeekday(label: 'S')),
              Expanded(child: CalendarWeekday(label: 'N')),
            ],
          ),
          const SizedBox(height: 8),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: 35,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 7,
              mainAxisSpacing: 6,
              crossAxisSpacing: 6,
            ),
            itemBuilder: (context, index) {
              final day = index - 1;
              if (day < 1 || day > 30) {
                return const SizedBox.shrink();
              }

              final isActive = activeDays.contains(day);
              return Container(
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: isActive
                      ? const Color(0xFF1F7A58)
                      : const Color(0xFFF7F4EF),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '$day',
                  style: TextStyle(
                    color: isActive ? Colors.white : const Color(0xFF403B36),
                    fontWeight: isActive ? FontWeight.w800 : FontWeight.w500,
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
