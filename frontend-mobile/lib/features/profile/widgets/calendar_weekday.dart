import 'package:flutter/material.dart';

class CalendarWeekday extends StatelessWidget {
  const CalendarWeekday({super.key, required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelMedium?.copyWith(
          color: const Color(0xFF6B625A),
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
