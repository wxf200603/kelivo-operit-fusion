import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/providers/chat_provider.dart';
import '../../core/providers/settings_provider.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final TextEditingController _inputController = TextEditingController();

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Kelivo Operit Fusion'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => Navigator.pushNamed(context, '/settings'),
          ),
        ],
      ),
      drawer: _buildDrawer(context),
      body: Column(
        children: [
          Expanded(child: _buildChatArea(context)),
          _buildInputArea(context),
        ],
      ),
    );
  }

  Widget _buildDrawer(BuildContext context) {
    final chat = context.watch<ChatProvider>();
    return Drawer(
      child: SafeArea(
        child: Column(
          children: [
            ListTile(
              title: const Text('会话列表'),
              trailing: IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  chat.createConversation();
                  Navigator.pop(context);
                },
              ),
            ),
            const Divider(),
            Expanded(
              child: ListView.builder(
                itemCount: chat.conversations.length,
                itemBuilder: (context, index) {
                  final conv = chat.conversations[index];
                  final isSelected = conv.id == chat.currentConversationId;
                  return ListTile(
                    selected: isSelected,
                    title: Text(
                      conv.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: Text(
                      '${conv.messages.length} 条消息',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete, size: 18),
                      onPressed: () => chat.deleteConversation(conv.id),
                    ),
                    onTap: () {
                      chat.selectConversation(conv.id);
                      Navigator.pop(context);
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildChatArea(BuildContext context) {
    final chat = context.watch<ChatProvider>();
    final conv = chat.currentConversation;
    
    if (conv == null || conv.messages.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.chat_bubble_outline, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '选择或新建会话开始对话',
              style: TextStyle(color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: conv.messages.length,
      itemBuilder: (context, index) {
        final msg = conv.messages[index];
        final isUser = msg.role == 'user';
        return Align(
          alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
          child: Container(
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: isUser ? Theme.of(context).colorScheme.primary : Colors.grey[200],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              msg.content,
              style: TextStyle(color: isUser ? Colors.white : Colors.black87),
            ),
          ),
        );
      },
    );
  }

  Widget _buildInputArea(BuildContext context) {
    final chat = context.watch<ChatProvider>();
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(8),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _inputController,
                decoration: InputDecoration(
                  hintText: '输入消息...',
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                ),
                enabled: !chat.isLoading,
                onSubmitted: _sendMessage,
              ),
            ),
            const SizedBox(width: 8),
            IconButton(
              icon: chat.isLoading
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.send),
              onPressed: chat.isLoading ? null : () => _sendMessage(_inputController.text),
            ),
          ],
        ),
      ),
    );
  }

  void _sendMessage(String text) {
    if (text.trim().isEmpty) return;
    final settings = context.read<AppSettingsProvider>();
    context.read<ChatProvider>().sendMessage(
          text,
          model: settings.selectedModel,
          apiKey: settings.apiKey,
        );
    _inputController.clear();
  }
}
