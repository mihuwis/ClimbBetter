import 'package:flutter/material.dart';
import 'package:climbbetter_mobile/features/home/home_tab.dart';
import 'package:climbbetter_mobile/features/profile/profile_tab.dart';
import 'package:climbbetter_mobile/features/training/log_done_session_sheet.dart';
import 'package:climbbetter_mobile/features/training/training_action_sheet.dart';
import 'package:climbbetter_mobile/shared/widgets/app_top_bar.dart';

class ClimbBetterShell extends StatefulWidget {
  const ClimbBetterShell({super.key});

  @override
  State<ClimbBetterShell> createState() => _ClimbBetterShellState();
}

// To jest główna ramka aplikacji, która zawiera pasek nawigacyjny 
// i zarządza wyświetlaniem odpowiednich zakładek w zależności od wybranego indeksu.
class _ClimbBetterShellState extends State<ClimbBetterShell> {
  int _selectedIndex = 0;

// daje tu normalne ekrany, 1 to jest ekran "modalny" z akcjami treningowymi.

  String get _currentTitle {
    if (_selectedIndex == 2) {
      return 'Ty';
    }
    return 'Board';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            AppTopBar(title: _currentTitle),
            Expanded(
              child: _selectedIndex == 2 ? const ProfileTab() : const HomeTab(),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) {
          if (index == 1) {
            _openTrainingActions();
            return;
          }

          setState(() => _selectedIndex = index);
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard),
            label: 'Board',
          ),
          NavigationDestination(
            icon: Icon(Icons.add_circle_outline),
            selectedIcon: Icon(Icons.add_circle),
            label: 'Nowy',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Ty',
          ),
        ],
      ),
    );
  }

  Future<void> _openTrainingActions() async {
    final action = await showModalBottomSheet<TrainingAction>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => const TrainingActionSheet(),
    );

    if (!mounted || action == null) {
      return;
    }

    switch (action) {
      case TrainingAction.logDoneSession:
        _openLogDoneSession();
      case TrainingAction.startNewSession:
      case TrainingAction.repeatLoggedSession:
      case TrainingAction.planSession:
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('${action.label}: zaslepka')));
    }
  }

  Future<void> _openLogDoneSession() {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      useSafeArea: true,
      builder: (context) => const LogDoneSessionSheet(),
    );
  }
}
