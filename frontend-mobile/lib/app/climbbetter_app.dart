import 'package:flutter/material.dart';

import 'package:climbbetter_mobile/app/app_theme.dart';
import 'package:climbbetter_mobile/app/climbbetter_shell.dart';

class ClimbBetterMobileApp extends StatelessWidget {
  const ClimbBetterMobileApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'ClimbBetter',
      theme: climbBetterTheme,
      home: const ClimbBetterShell(),
    );
  }
}
