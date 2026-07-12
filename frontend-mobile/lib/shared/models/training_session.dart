import 'package:flutter/material.dart';

class TrainingSession {
  const TrainingSession({
    required this.id,
    required this.title,
    required this.place,
    required this.dateLabel,
    required this.duration,
    required this.points,
    required this.entries,
    required this.note,
    required this.grades,
    required this.accent,
  });

  final String id;
  final String title;
  final String place;
  final String dateLabel;
  final String duration;
  final int points;
  final int entries;
  final String note;
  final List<String> grades;
  final Color accent;
}
