package com.phantom.ai.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.ai.ui.viewmodel.ChatViewModel

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatPanelContent(
    onClose: () -> Unit,
    viewModel: ChatViewModel,
    streamerUrl: String
) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val listState = rememberLazyListState()

    // Connect to streamer when panel opens / URL changes
    LaunchedEffect(streamerUrl) {
        if (streamerUrl.isNotBlank()) {
            viewModel.connect(streamerUrl)
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xF0101020))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(com.phantom.ai.R.drawable.ic_orb),
                    contentDescription = "O.R.B.",
                    modifier = Modifier.size(24.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column {
                    Text(
                        "O.R.B.",
                        color = Color(0xFFE0D4FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    // Connection status dot
                    val (statusColor, statusText) = when (connectionStatus) {
                        "connected"    -> Color(0xFF4CAF50) to "● live"
                        "connecting"   -> Color(0xFFFF9800) to "● connecting"
                        "reconnecting" -> Color(0xFFFF9800) to "● reconnecting"
                        else           -> Color(0xFF666680) to "● offline"
                    }
                    Text(statusText, color = statusColor, fontSize = 10.sp)
                }
                IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color(0xFF8B7FB8), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8B7FB8))
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    // TODO: Implement file/doc/image delivery
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252540))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File", tint = Color(0xFFE0D4FF), modifier = Modifier.size(24.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF252540))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text("Command O.R.B...", color = Color(0xFF5C5C7A), fontSize = 14.sp)
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(color = Color(0xFFE0D4FF), fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFFBB86FC)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendPrompt(streamerUrl, inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6C3CE0))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) Color(0xFF6C3CE0) else Color(0xFF1E1E36)
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = Color(0xFFE0D4FF), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
