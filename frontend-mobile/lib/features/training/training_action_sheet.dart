import 'package:flutter/material.dart';

enum TrainingAction {
  startNewSession('Start new session'),
  repeatLoggedSession('Repeat logged session'),
  logDoneSession('Log done session'),
  planSession('Plan session');

  const TrainingAction(this.label);

  final String label;
}

class TrainingActionSheet extends StatelessWidget {
  const TrainingActionSheet({super.key});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Co chcesz zrobic?',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w900,
                letterSpacing: 0,
              ),
            ),
            const SizedBox(height: 12),
            _TrainingActionTile(
              icon: Icons.play_arrow,
              title: TrainingAction.startNewSession.label,
              subtitle: 'Zacznij trening teraz.',
              action: TrainingAction.startNewSession,
            ),
            _TrainingActionTile(
              icon: Icons.refresh,
              title: TrainingAction.repeatLoggedSession.label,
              subtitle: 'Ponow jeden z zapisanych treningow.',
              action: TrainingAction.repeatLoggedSession,
            ),
            _TrainingActionTile(
              icon: Icons.edit_note,
              title: TrainingAction.logDoneSession.label,
              subtitle: 'Wpisz trening, ktory juz zostal zrobiony.',
              action: TrainingAction.logDoneSession,
              isPrimary: true,
            ),
            _TrainingActionTile(
              icon: Icons.event_note,
              title: TrainingAction.planSession.label,
              subtitle: 'Zaplanuj przyszla sesje.',
              action: TrainingAction.planSession,
            ),
          ],
        ),
      ),
    );
  }
}

class _TrainingActionTile extends StatelessWidget {
  const _TrainingActionTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.action,
    this.isPrimary = false,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final TrainingAction action;
  final bool isPrimary;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: CircleAvatar(
        backgroundColor: isPrimary
            ? const Color(0xFF1F7A58)
            : const Color(0xFFF7F4EF),
        foregroundColor: isPrimary ? Colors.white : const Color(0xFF1F7A58),
        child: Icon(icon),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => Navigator.of(context).pop(action),
    );
  }
}
