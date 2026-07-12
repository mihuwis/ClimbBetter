import 'package:climbbetter_mobile/app/climbbetter_app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows recent trainings and repeat action', (tester) async {
    await tester.pumpWidget(const ClimbBetterMobileApp());

    expect(find.text('Ostatnie treningi'), findsOneWidget);
    expect(find.text('Bronx Boulder'), findsOneWidget);
    expect(find.text('Board'), findsWidgets);
    expect(find.text('Nowy'), findsOneWidget);
    expect(find.text('Ty'), findsOneWidget);
    expect(find.byIcon(Icons.settings_outlined), findsOneWidget);

    await tester.tap(find.byKey(const Key('session-menu-bronx-boulder')));
    await tester.pumpAndSettle();

    expect(find.text('Ponów trening'), findsOneWidget);
  });

  testWidgets('opens training action modal and log done session flow', (
    tester,
  ) async {
    await tester.pumpWidget(const ClimbBetterMobileApp());

    await tester.tap(find.text('Nowy'));
    await tester.pumpAndSettle();

    expect(find.text('Co chcesz zrobic?'), findsOneWidget);
    expect(find.text('Start new session'), findsOneWidget);
    expect(find.text('Repeat logged session'), findsOneWidget);
    expect(find.text('Log done session'), findsOneWidget);
    expect(find.text('Plan session'), findsOneWidget);

    await tester.tap(find.text('Log done session'));
    await tester.pumpAndSettle();

    expect(find.text('Log done session'), findsOneWidget);
    expect(find.text('Parametry sesji'), findsOneWidget);
    expect(find.text('Dodaj bald / droge'), findsOneWidget);
    expect(find.text('Dodaj wspin'), findsOneWidget);
  });

  testWidgets('navigates to profile tab', (tester) async {
    await tester.pumpWidget(const ClimbBetterMobileApp());

    await tester.tap(find.text('Ty'));
    await tester.pumpAndSettle();

    expect(find.text('Kalendarz'), findsOneWidget);
    await tester.drag(find.byType(ListView), const Offset(0, -500));
    await tester.pumpAndSettle();

    expect(find.text('Obciążenie'), findsOneWidget);
  });
}
