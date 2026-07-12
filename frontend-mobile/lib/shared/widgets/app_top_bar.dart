import 'package:flutter/material.dart';

class AppTopBar extends StatelessWidget {
  const AppTopBar({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 12, 10),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 22,
            backgroundColor: Color(0xFF1F7A58),
            foregroundColor: Colors.white,
            child: Text('MW', style: TextStyle(fontWeight: FontWeight.w700)),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'ClimbBetter',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: const Color(0xFF6B625A),
                ),
              ),
              Text(
                title,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0,
                ),
              ),
            ],
          ),
          const Spacer(),
          IconButton(
            key: const Key('profile-settings-button'),
            icon: const Icon(Icons.settings_outlined),
            tooltip: 'Ustawienia profilu',
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Ustawienia profilu')),
              );
            },
          ),
        ],
      ),
    );
  }
}
