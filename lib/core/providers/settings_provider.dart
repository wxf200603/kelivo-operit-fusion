import 'package:flutter/material.dart';

/// 应用设置状态管理
class AppSettingsProvider extends ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  String _selectedModel = 'gpt-4o';
  String _apiKey = '';
  String _apiBaseUrl = 'https://api.openai.com/v1';

  ThemeMode get themeMode => _themeMode;
  String get selectedModel => _selectedModel;
  String get apiKey => _apiKey;
  String get apiBaseUrl => _apiBaseUrl;

  Future<void> initialize() async {
    // TODO: 从持久化存储加载
    notifyListeners();
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    _themeMode = mode;
    notifyListeners();
  }

  Future<void> setModel(String model) async {
    _selectedModel = model;
    notifyListeners();
  }

  Future<void> setApiKey(String key) async {
    _apiKey = key;
    notifyListeners();
  }

  Future<void> setApiBaseUrl(String url) async {
    _apiBaseUrl = url;
    notifyListeners();
  }
}
