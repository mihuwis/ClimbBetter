import 'dart:math';

import 'package:flutter/material.dart';

import 'package:climbbetter_mobile/features/training/models/ascent_style_preset.dart';
import 'package:climbbetter_mobile/features/training/models/climb_profile_preset.dart';
import 'package:climbbetter_mobile/shared/widgets/field_panel.dart';
import 'package:climbbetter_mobile/shared/widgets/section_header.dart';

const _places = ['Bronx', 'Avatar', 'Garaz', 'Skaly'];
const _startTimes = ['07:00', '08:00', '12:00', '17:00', '18:00', '19:00'];
const _durationPresets = [1.0, 1.5, 2.0, 3.0];
const _gradePresets = ['5', '6A', '6A+', '6B', '6B+', '6C', '7A', '7A+'];
const _knownClimbs = [
  'czerwone placki / Bronx',
  'niebieski kant / Bronx',
  'zielony dach / Avatar',
  'czarny oblaczek / Garaz',
];

class LogDoneSessionSheet extends StatefulWidget {
  const LogDoneSessionSheet({super.key});

  @override
  State<LogDoneSessionSheet> createState() => _LogDoneSessionSheetState();
}

class _LogDoneSessionSheetState extends State<LogDoneSessionSheet> {
  String _place = _places.first;
  String _day = 'Dzisiaj';
  String _startTime = '18:00';
  double _durationHours = 1.5;

  String _climbName = '';
  String _grade = '6B';
  bool _completed = true;
  ClimbProfilePreset _profile = climbProfilePresets.first;
  AscentStylePreset _style = defaultDoneStyle;
  int _edl = climbProfilePresets.first.defaultEdl;
  int _movesDone = climbProfilePresets.first.defaultEdl;

  late final TextEditingController _climbNameController;
  final List<_LoggedClimbDraft> _entries = [];

  @override
  void initState() {
    super.initState();
    _climbNameController = TextEditingController();
  }

  @override
  void dispose() {
    _climbNameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.92,
      minChildSize: 0.55,
      maxChildSize: 0.96,
      builder: (context, scrollController) {
        return ListView(
          controller: scrollController,
          padding: EdgeInsets.fromLTRB(
            20,
            0,
            20,
            MediaQuery.viewInsetsOf(context).bottom + 24,
          ),
          children: [
            const SectionHeader(title: 'Log done session'),
            const SizedBox(height: 12),
            _buildSessionPanel(),
            const SizedBox(height: 16),
            _buildClimbPanel(),
            const SizedBox(height: 16),
            _buildEntriesPreview(),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _entries.isEmpty ? null : _saveSession,
              icon: const Icon(Icons.check),
              label: const Text('Zapisz sesje'),
            ),
          ],
        );
      },
    );
  }

  Widget _buildSessionPanel() {
    return FieldPanel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Parametry sesji',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w900,
              letterSpacing: 0,
            ),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _place,
            decoration: const InputDecoration(
              labelText: 'Miejsce',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final place in _places)
                DropdownMenuItem(value: place, child: Text(place)),
            ],
            onChanged: (value) {
              if (value != null) {
                setState(() => _place = value);
              }
            },
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            children: [
              ChoiceChip(
                label: const Text('Dzisiaj'),
                selected: _day == 'Dzisiaj',
                onSelected: (_) => setState(() => _day = 'Dzisiaj'),
              ),
              ChoiceChip(
                label: const Text('Wczoraj'),
                selected: _day == 'Wczoraj',
                onSelected: (_) => setState(() => _day = 'Wczoraj'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _startTime,
            decoration: const InputDecoration(
              labelText: 'Start',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final time in _startTimes)
                DropdownMenuItem(value: time, child: Text(time)),
            ],
            onChanged: (value) {
              if (value != null) {
                setState(() => _startTime = value);
              }
            },
          ),
          const SizedBox(height: 12),
          Text(
            'Czas trwania',
            style: Theme.of(
              context,
            ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              for (final duration in _durationPresets)
                ChoiceChip(
                  label: Text(_formatDuration(duration)),
                  selected: _durationHours == duration,
                  onSelected: (_) => setState(() => _durationHours = duration),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildClimbPanel() {
    final suggestions = _knownClimbs
        .where(
          (climb) => climb.toLowerCase().contains(_climbName.toLowerCase()),
        )
        .take(3)
        .toList();

    return FieldPanel(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Dodaj bald / droge',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w900,
              letterSpacing: 0,
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _climbNameController,
            decoration: const InputDecoration(
              labelText: 'Nazwa / lokacja',
              hintText: 'np. czerwone placki / Bronx',
              border: OutlineInputBorder(),
            ),
            onChanged: (value) => setState(() => _climbName = value),
          ),
          if (_climbName.isNotEmpty && suggestions.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final suggestion in suggestions)
                  ActionChip(
                    label: Text(suggestion),
                    onPressed: () {
                      setState(() => _climbName = suggestion);
                      _climbNameController.text = suggestion;
                    },
                  ),
              ],
            ),
          ],
          const SizedBox(height: 12),
          DropdownButtonFormField<ClimbProfilePreset>(
            initialValue: _profile,
            decoration: const InputDecoration(
              labelText: 'Profil wspinu',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final profile in climbProfilePresets)
                DropdownMenuItem(
                  value: profile,
                  child: Text('${profile.name} (${profile.defaultEdl})'),
                ),
            ],
            onChanged: (value) {
              if (value != null) {
                _setProfile(value);
              }
            },
          ),
          const SizedBox(height: 8),
          Text(_profile.description),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _grade,
            decoration: const InputDecoration(
              labelText: 'Wycena',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final grade in _gradePresets)
                DropdownMenuItem(value: grade, child: Text(grade)),
            ],
            onChanged: (value) {
              if (value != null) {
                setState(() => _grade = value);
              }
            },
          ),
          const SizedBox(height: 8),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Zrobiony'),
            value: _completed,
            onChanged: _setCompleted,
          ),
          DropdownButtonFormField<AscentStylePreset>(
            initialValue: _style,
            decoration: const InputDecoration(
              labelText: 'Styl',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final style in ascentStylePresets)
                DropdownMenuItem(
                  value: style,
                  child: Text('${style.name} x${style.multiplier}'),
                ),
            ],
            onChanged: (value) {
              if (value != null) {
                setState(() => _style = value);
              }
            },
          ),
          const SizedBox(height: 8),
          Text(_style.description),
          const SizedBox(height: 12),
          _NumberStepper(
            label: 'EDL / ruchy drogi',
            value: _edl,
            onChanged: _setEdl,
          ),
          const SizedBox(height: 8),
          _completed
              ? Text('Ruchy wykonane: $_edl / $_edl')
              : _NumberStepper(
                  label: 'Ruchy wykonane',
                  value: _movesDone,
                  maxValue: _edl,
                  onChanged: _setMovesDone,
                ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _addEntry,
            icon: const Icon(Icons.add),
            label: const Text('Dodaj wspin'),
          ),
        ],
      ),
    );
  }

  Widget _buildEntriesPreview() {
    if (_entries.isEmpty) {
      return const Text('Brak wpisow w sesji.');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Wpisy w sesji',
          style: Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 8),
        for (final entry in _entries)
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(entry.name),
            subtitle: Text(
              '${entry.profile} · ${entry.grade} · ${entry.style} · ${entry.movesDone}/${entry.edl}',
            ),
          ),
      ],
    );
  }

  void _setProfile(ClimbProfilePreset profile) {
    setState(() {
      _profile = profile;
      _edl = profile.defaultEdl;
      _movesDone = _completed ? _edl : min(_movesDone, _edl);
    });
  }

  void _setCompleted(bool value) {
    setState(() {
      _completed = value;
      _style = value ? defaultDoneStyle : defaultAttemptStyle;
      _movesDone = value ? _edl : min(_movesDone, _edl);
    });
  }

  void _setEdl(int value) {
    setState(() {
      _edl = max(0, value);
      _movesDone = _completed ? _edl : min(_movesDone, _edl);
    });
  }

  void _setMovesDone(int value) {
    setState(() => _movesDone = max(0, min(value, _edl)));
  }

  void _addEntry() {
    final name = _climbName.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Wpisz nazwe baldu/drogi.')));
      return;
    }

    setState(() {
      _entries.add(
        _LoggedClimbDraft(
          name: name,
          profile: _profile.name,
          grade: _grade,
          style: _style.name,
          edl: _edl,
          movesDone: _completed ? _edl : _movesDone,
        ),
      );
      _climbName = '';
      _climbNameController.clear();
      _profile = climbProfilePresets.first;
      _grade = '6B';
      _completed = true;
      _style = defaultDoneStyle;
      _edl = climbProfilePresets.first.defaultEdl;
      _movesDone = _edl;
    });
  }

  void _saveSession() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Zapis roboczy: $_place, $_day, $_startTime, ${_formatDuration(_durationHours)}',
        ),
      ),
    );
    Navigator.of(context).pop();
  }

  String _formatDuration(double hours) {
    return hours == hours.roundToDouble() ? '${hours.toInt()}h' : '${hours}h';
  }
}

class _NumberStepper extends StatelessWidget {
  const _NumberStepper({
    required this.label,
    required this.value,
    required this.onChanged,
    this.maxValue,
  });

  final String label;
  final int value;
  final int? maxValue;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: Text(label)),
        IconButton(
          onPressed: () => onChanged(value - 1),
          icon: const Icon(Icons.remove_circle_outline),
        ),
        SizedBox(
          width: 42,
          child: Center(
            child: Text(
              '$value',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
        ),
        IconButton(
          onPressed: maxValue != null && value >= maxValue!
              ? null
              : () => onChanged(value + 1),
          icon: const Icon(Icons.add_circle_outline),
        ),
      ],
    );
  }
}

class _LoggedClimbDraft {
  const _LoggedClimbDraft({
    required this.name,
    required this.profile,
    required this.grade,
    required this.style,
    required this.edl,
    required this.movesDone,
  });

  final String name;
  final String profile;
  final String grade;
  final String style;
  final int edl;
  final int movesDone;
}
