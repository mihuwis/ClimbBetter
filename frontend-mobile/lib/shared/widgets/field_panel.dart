import 'package:flutter/material.dart';

class FieldPanel extends StatelessWidget {
  const FieldPanel({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFE3DED6)),
      ),
      child: child,
    );
  }
}
