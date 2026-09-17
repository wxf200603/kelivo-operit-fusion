import 'package:flutter/material.dart';

/// 聊天消息模型
class ChatMessage {
  final String id;
  final String role; // 'user' | 'assistant' | 'system'
  final String content;
  final DateTime timestamp;
  final bool isStreaming;

  ChatMessage({
    required this.id,
    required this.role,
    required this.content,
    DateTime? timestamp,
    this.isStreaming = false,
  }) : timestamp = timestamp ?? DateTime.now();

  Map<String, dynamic> toJson() => {
        'id': id,
        'role': role,
        'content': content,
        'timestamp': timestamp.toIso8601String(),
        'isStreaming': isStreaming,
      };

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
        id: json['id'],
        role: json['role'],
        content: json['content'],
        timestamp: DateTime.parse(json['timestamp']),
        isStreaming: json['isStreaming'] ?? false,
      );
}

/// 聊天会话模型
class Conversation {
  final String id;
  String title;
  List<ChatMessage> messages;
  DateTime createdAt;
  DateTime updatedAt;

  Conversation({
    required this.id,
    required this.title,
    List<ChatMessage>? messages,
    DateTime? createdAt,
    DateTime? updatedAt,
  })  : messages = messages ?? [],
        createdAt = createdAt ?? DateTime.now(),
        updatedAt = updatedAt ?? DateTime.now();

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'messages': messages.map((m) => m.toJson()).toList(),
        'createdAt': createdAt.toIso8601String(),
        'updatedAt': updatedAt.toIso8601String(),
      };

  factory Conversation.fromJson(Map<String, dynamic> json) => Conversation(
        id: json['id'],
        title: json['title'],
        messages: (json['messages'] as List)
            .map((m) => ChatMessage.fromJson(m))
            .toList(),
        createdAt: DateTime.parse(json['createdAt']),
        updatedAt: DateTime.parse(json['updatedAt']),
      );
}

/// 聊天状态管理
class ChatProvider extends ChangeNotifier {
  final List<Conversation> _conversations = [];
  String? _currentConversationId;
  bool _isLoading = false;
  String? _error;

  List<Conversation> get conversations => _conversations;
  String? get currentConversationId => _currentConversationId;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Conversation? get currentConversation => _currentConversationId == null
      ? null
      : _conversations.where((c) => c.id == _currentConversationId).firstOrNull;

  /// 创建新会话
  String createConversation({String title = '新对话'}) {
    final id = DateTime.now().millisecondsSinceEpoch.toString();
    final conv = Conversation(id: id, title: title);
    _conversations.insert(0, conv);
    _currentConversationId = id;
    notifyListeners();
    return id;
  }

  /// 删除会话
  void deleteConversation(String id) {
    _conversations.removeWhere((c) => c.id == id);
    if (_currentConversationId == id) {
      _currentConversationId = _conversations.isNotEmpty ? _conversations.first.id : null;
    }
    notifyListeners();
  }

  /// 切换会话
  void selectConversation(String id) {
    _currentConversationId = id;
    notifyListeners();
  }

  /// 发送消息
  Future<void> sendMessage(String content, {String? model, String? apiKey}) async {
    if (_currentConversationId == null) {
      createConversation();
    }

    final conv = currentConversation!;
    final userMsg = ChatMessage(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      role: 'user',
      content: content,
    );
    conv.messages.add(userMsg);
    conv.updatedAt = DateTime.now();
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // TODO: 调用 API 服务
      await Future.delayed(const Duration(seconds: 1)); // 模拟网络请求
      
      final assistantMsg = ChatMessage(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        role: 'assistant',
        content: '这是模拟回复：收到消息 "$content"',
      );
      conv.messages.add(assistantMsg);
      conv.updatedAt = DateTime.now();
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 清空当前会话消息
  void clearCurrentMessages() {
    if (currentConversation != null) {
      currentConversation!.messages.clear();
      notifyListeners();
    }
  }
}

extension FirstWhereOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
