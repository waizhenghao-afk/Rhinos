package com.rhinos.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhinos.app.AppContainer
import com.rhinos.app.data.model.Note
import com.rhinos.app.data.model.NoteCategory
import com.rhinos.app.ui.components.EmptyState
import com.rhinos.app.ui.components.Format
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Private investment notebook: strategies, trade ideas, lessons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(container: AppContainer) {
    val notes by container.noteRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }
    var search by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<NoteCategory?>(null) }

    val visible = remember(notes, search, categoryFilter) {
        notes.filter { note ->
            (categoryFilter == null || note.category == categoryFilter) &&
                (search.isBlank() || note.title.contains(search, ignoreCase = true) ||
                    note.content.contains(search, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Notes & Strategies", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search notes") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Button(onClick = { editing = null; showForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Note")
        }
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = categoryFilter == null,
                onClick = { categoryFilter = null },
                label = { Text("All") }
            )
            NoteCategory.entries.forEach { category ->
                FilterChip(
                    selected = categoryFilter == category,
                    onClick = { categoryFilter = category },
                    label = { Text(category.label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (notes.isEmpty()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                EmptyState(
                    title = "No notes yet",
                    message = "Save your investment strategies and lessons here."
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { editing = note; showForm = true },
                        onDelete = { deleting = note }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(onDismissRequest = { showForm = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NoteForm(container, editing = editing, onDismiss = { showForm = false })
            }
        }
    }

    deleting?.let { note ->
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete note?") },
            text = { Text("\u201C${note.title}\u201D will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = note
                    deleting = null
                    scope.launch { container.noteRepository.delete(toDelete) }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    note.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
                Text(
                    "Updated ${Format.shortDate(LocalDate.ofInstant(Instant.ofEpochMilli(note.updatedAt), ZoneId.systemDefault()))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit note") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete note") }
        }
    }
}

/** Add/edit note with title, content and category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteForm(
    container: AppContainer,
    editing: Note?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var content by remember { mutableStateOf(editing?.content ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: NoteCategory.STRATEGY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            if (editing == null) "Add Note" else "Edit Note",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NoteCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    errorMessage = null
                    if (title.isBlank()) {
                        errorMessage = "Title is required."
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val note = Note(
                        id = editing?.id ?: 0,
                        title = title.trim(),
                        content = content.trim(),
                        category = category,
                        createdAt = editing?.createdAt ?: now,
                        updatedAt = now
                    )
                    scope.launch {
                        try {
                            if (editing == null) container.noteRepository.add(note)
                            else container.noteRepository.update(note)
                            onDismiss()
                        } catch (e: IllegalArgumentException) {
                            errorMessage = e.message
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (editing == null) "Save" else "Update") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
