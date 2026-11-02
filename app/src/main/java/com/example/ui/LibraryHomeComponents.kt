package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LibraryForest
import com.example.ui.theme.LibraryForestDeep
import com.example.ui.theme.LibraryInk
import com.example.ui.theme.LibrarySage
import com.example.ui.theme.LibrarySageStrong

@Composable
fun LibraryWelcomeHeader(
    bookCount: Int,
    hasSearch: Boolean,
    onAddBook: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val countLabel = when {
        hasSearch && bookCount == 1 -> "1 resultado"
        hasSearch -> "$bookCount resultados"
        bookCount == 1 -> "1 livro cadastrado"
        else -> "$bookCount livros cadastrados"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LibrarySage,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Seu acervo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LibraryInk
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LibraryForestDeep,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (onAddBook != null) {
                    FilledTonalButton(
                        onClick = onAddBook,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = LibraryForestDeep
                        ),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag("add_book_button_header")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar livro")
                    }
                }
            }
            Surface(color = LibrarySageStrong, shape = CircleShape) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = LibraryForestDeep,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LibrarySearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Buscar livro") },
            placeholder = { Text("Título ou autor") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = LibraryForest
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("search_books_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LibraryForest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = LibraryForest,
                cursorColor = LibraryForest
            ),
            shape = RoundedCornerShape(18.dp)
        )
        FilledTonalButton(
            onClick = onScan,
            modifier = Modifier
                .height(56.dp)
                .testTag("scanner_station_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = LibrarySage,
                contentColor = LibraryForestDeep
            )
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Escanear")
        }
    }
}
