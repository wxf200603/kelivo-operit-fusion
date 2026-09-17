import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData light() {
    return ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF6750A4),
        brightness: Brightness.light,
      ),
      useMaterial3: true,
      appBarTheme: const AppBarTheme(
        centerTitle: true,
      ),
    );
  }

  static ThemeData dark() {
    return ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF6750A4),
        brightness: Brightness.dark,
      ),
      useMaterial3: true,
      appBarTheme: const AppBarTheme(
        centerTitle: true,
      ),
    );
  }
}
