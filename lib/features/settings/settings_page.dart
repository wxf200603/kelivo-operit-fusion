import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/providers/settings_provider.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<AppSettingsProvider>();
    
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 主题设置
          Card(
            child: ListTile(
              title: const Text('主题'),
              subtitle: Text(_themeLabel(settings.themeMode)),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _showThemePicker(context, settings),
            ),
          ),
          const SizedBox(height: 8),
          
          // 模型选择
          Card(
            child: ListTile(
              title: const Text('当前模型'),
              subtitle: Text(settings.selectedModel),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _showModelPicker(context, settings),
            ),
          ),
          const SizedBox(height: 8),
          
          // API Key
          Card(
            child: ListTile(
              title: const Text('API Key'),
              subtitle: Text(settings.apiKey.isEmpty ? '未设置' : '已设置'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _showApiKeyDialog(context, settings),
            ),
          ),
          const SizedBox(height: 8),
          
          // API Base URL
          Card(
            child: ListTile(
              title: const Text('API Base URL'),
              subtitle: Text(settings.apiBaseUrl),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _showBaseUrlDialog(context, settings),
            ),
          ),
          const SizedBox(height: 24),
          
          // 关于
          Card(
            child: ListTile(
              title: const Text('关于'),
              subtitle: const Text('Kelivo Operit Fusion v1.0.0'),
              onTap: () {
                showAboutDialog(
                  context: context,
                  applicationName: 'Kelivo Operit Fusion',
                  applicationVersion: '1.0.0',
                  applicationLegalese: '融合自 Kelivo (AGPL-3.0) + Operit (LGPL-3.0)',
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  String _themeLabel(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return '浅色';
      case ThemeMode.dark:
        return '深色';
      case ThemeMode.system:
        return '跟随系统';
    }
  }

  Future<void> _showThemePicker(BuildContext context, AppSettingsProvider settings) async {
    final result = await showDialog<ThemeMode>(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('选择主题'),
        children: [
          SimpleDialogOption(
            onPressed: () => Navigator.pop(context, ThemeMode.light),
            child: const Text('浅色'),
          ),
          SimpleDialogOption(
            onPressed: () => Navigator.pop(context, ThemeMode.dark),
            child: const Text('深色'),
          ),
          SimpleDialogOption(
            onPressed: () => Navigator.pop(context, ThemeMode.system),
            child: const Text('跟随系统'),
          ),
        ],
      ),
    );
    if (result != null) {
      await settings.setThemeMode(result);
    }
  }

  Future<void> _showModelPicker(BuildContext context, AppSettingsProvider settings) async {
    final models = [
      'gpt-4o',
      'gpt-4o-mini',
      'gpt-4-turbo',
      'gpt-3.5-turbo',
      'claude-3-5-sonnet',
      'claude-3-opus',
      'gemini-1.5-pro',
      'gemini-1.5-flash',
    ];
    
    final result = await showDialog<String>(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('选择模型'),
        children: models.map((m) => SimpleDialogOption(
          onPressed: () => Navigator.pop(context, m),
          child: Text(m),
        )).toList(),
      ),
    );
    if (result != null) {
      await settings.setModel(result);
    }
  }

  Future<void> _showApiKeyDialog(BuildContext context, AppSettingsProvider settings) async {
    final controller = TextEditingController(text: settings.apiKey);
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('设置 API Key'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'sk-...',
            border: OutlineInputBorder(),
          ),
          obscureText: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (result != null) {
      await settings.setApiKey(result);
    }
  }

  Future<void> _showBaseUrlDialog(BuildContext context, AppSettingsProvider settings) async {
    final controller = TextEditingController(text: settings.apiBaseUrl);
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('设置 API Base URL'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'https://api.openai.com/v1',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('保存'),
          ),
        ],
      ),
    );
    if (result != null) {
      await settings.setApiBaseUrl(result);
    }
  }
}
