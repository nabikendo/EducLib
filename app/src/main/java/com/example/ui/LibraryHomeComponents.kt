package com.example.ui

import androidx.compose.animation.animateColor
import androidx.compose.runtime.getValue
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
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
    val motion = androidx.compose.animation.core.updateTransition(
        targetState = hasSearch,
        label = "library_header_state"
    )
    val background by motion.animateColor(label = "library_header_color") { searching ->
        if (searching) com.example.ui.ipad.IpadPalette.BlueSoft
        else com.example.ui.ipad.IpadPalette.Cream
    }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val wide = maxWidth >= 700.dp
        Surface(
            color = background,
            shape = RoundedCornerShape(if (wide) 32.dp else 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (wide) 28.dp else 20.dp,
                    vertical = if (wide) 26.dp else 18.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasSearch) "Resultados da busca" else "Sua biblioteca.",
                        style = if (wide) MaterialTheme.typography.headlineLarge
                                else MaterialTheme.typography.headlineMedium,
                        color = com.example.ui.ipad.IpadPalette.Ink
                    )
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = com.example.ui.ipad.IpadPalette.SecondaryInk,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (onAddBook != null) {
                        FilledTonalButton(
                            onClick = onAddBook,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = com.example.ui.ipad.IpadPalette.Ink,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .testTag("add_book_button_header")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar livro")
                        }
                    }
                }
                Surface(
                    color = com.example.ui.ipad.IpadPalette.Yellow,
                    shape = RoundedCornerShape(if (wide) 28.dp else 22.dp)
                ) {
                    Box(
                        modifier = Modifier.size(if (wide) 112.dp else 62.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = com.example.ui.ipad.IpadPalette.Ink,
                            modifier = Modifier.size(if (wide) 48.dp else 29.dp)
                        )
                    }
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
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = LibraryForest
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Limpar busca")
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
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Escanear")
        }
    }
}
