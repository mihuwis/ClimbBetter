import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/shared/models/training_session.dart';

const recentSessions = [
  TrainingSession(
    id: 'bronx-boulder',
    title: 'Bronx Boulder',
    place: 'Bronx',
    dateLabel: '26 czerwca',
    duration: '1h 35m',
    points: 125,
    entries: 18,
    note: 'Mocny dzień na przewieszeniu, najlepsza seria na projekcie.',
    grades: ['6B', '6C', '7A'],
    accent: Color(0xFF1F7A58),
  ),
  TrainingSession(
    id: 'avatar-sport',
    title: 'Avatar Sport',
    place: 'Avatar',
    dateLabel: '23 czerwca',
    duration: '1h 10m',
    points: 90,
    entries: 9,
    note: 'Technika nóg i spokojne prowadzenia bez dokładania objętości.',
    grades: ['6C', '6B'],
    accent: Color(0xFFD45D3F),
  ),
  TrainingSession(
    id: 'garage-circuit',
    title: 'Garaż Obwody',
    place: 'Garaż',
    dateLabel: '21 czerwca',
    duration: '50m',
    points: 70,
    entries: 6,
    note: 'Krótki trening po pracy, dużo kontroli i brak prób do odciny.',
    grades: ['6A+', '6B'],
    accent: Color(0xFF3F6DA8),
  ),
];
