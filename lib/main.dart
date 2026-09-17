import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/providers/chat_provider.dart';
import 'core/providers/settings_provider.dart';
import 'features/home/home_page.dart';
import 'features/settings/settings_page.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  final settings = AppSettingsProvider();
  await settings.initialize();
  
  final chat = ChatProvider();
  
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: settings),
        ChangeNotifierProvider.value(value: chat),
      ],
      child: const KelivoOperitFusionApp(),
    ),
  );
}

class KelivoOperitFusionApp extends StatelessWidget {
  const KelivoOperitFusionApp({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<AppSettingsProvider>();
    
    return MaterialApp(
      title: 'Kelivo Operit Fusion',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: settings.themeMode,
      home: const HomePage(),
      routes: {
        '/settings': (context) => const SettingsPage(),
      },
    );
  }
}
