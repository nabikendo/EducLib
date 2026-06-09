const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/example/ui/LibraryApp.kt', 'utf8');

// replace verticalScroll(rememberScrollState()) -> verticalScrollWithKeyboard(...)
file = file.replace(/\.verticalScroll\(rememberScrollState\(\)\)/g, '.verticalScrollWithKeyboard(rememberScrollState())');
file = file.replace(/\.horizontalScroll\(rememberScrollState\(\)\)/g, '.horizontalScrollWithKeyboard(rememberScrollState())');

// Add imports
if (!file.includes('import androidx.compose.ui.input.key.*')) {
  file = file.replace('import androidx.compose.ui.Modifier', `import androidx.compose.ui.Modifier\nimport androidx.compose.ui.input.key.*\nimport androidx.compose.ui.input.pointer.*\nimport androidx.compose.foundation.gestures.*`);
}

// Add extension function at the end
const extension = `
@Composable
fun Modifier.verticalScrollWithKeyboard(state: ScrollState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this
        .verticalScroll(state)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionDown -> { coroutineScope.launch { state.scrollBy(150f) }; true }
                    Key.DirectionUp -> { coroutineScope.launch { state.scrollBy(-150f) }; true }
                    Key.PageDown -> { coroutineScope.launch { state.scrollBy(500f) }; true }
                    Key.PageUp -> { coroutineScope.launch { state.scrollBy(-500f) }; true }
                    else -> false
                }
            } else {
                false
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (delta != 0f) {
                            coroutineScope.launch {
                                state.scrollBy(delta * 100f)
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun Modifier.horizontalScrollWithKeyboard(state: ScrollState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this
        .horizontalScroll(state)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionRight -> { coroutineScope.launch { state.scrollBy(150f) }; true }
                    Key.DirectionLeft -> { coroutineScope.launch { state.scrollBy(-150f) }; true }
                    else -> false
                }
            } else {
                false
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.firstOrNull()?.scrollDelta?.x ?: (event.changes.firstOrNull()?.scrollDelta?.y ?: 0f)
                        if (delta != 0f) {
                            coroutineScope.launch {
                                state.scrollBy(delta * 100f)
                            }
                        }
                    }
                }
            }
        }
}
`;

if (!file.includes('verticalScrollWithKeyboard')) {
  file += extension;
}

fs.writeFileSync('app/src/main/java/com/example/ui/LibraryApp.kt', file);
