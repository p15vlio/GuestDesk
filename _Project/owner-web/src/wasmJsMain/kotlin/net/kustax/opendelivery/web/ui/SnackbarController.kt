package net.kustax.opendelivery.web.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object SnackbarController {
    val hostState = SnackbarHostState()
    private val scope = MainScope()

    fun show(message: String) {
        scope.launch {
            hostState.showSnackbar(message)
        }
    }
}

@Composable
fun SnackbarDisplay() {
    SnackbarHost(
        hostState = SnackbarController.hostState,
        modifier = Modifier.padding(16.dp)
    )
}
