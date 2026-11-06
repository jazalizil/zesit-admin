package fr.zesit.admin.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.zesit.admin.data.AppSettings
import fr.zesit.admin.viewmodel.NotificationsViewModel

@Composable
fun SettingsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val current by viewModel.uiState.collectAsState()
    val saved by viewModel.settings.collectAsState()

    var baseUrl by remember(saved) { mutableStateOf(saved.baseUrl) }
    var topic by remember(saved) { mutableStateOf(saved.topic) }
    var token by remember(saved) { mutableStateOf(saved.token) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Serveur ntfy", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL du serveur") },
            placeholder = { Text("https://ntfy.zesit.fr") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic (device id)") },
            placeholder = { Text("zesit-…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token ntfy (Bearer tk_…)") },
            placeholder = { Text("tk_…") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.saveSettings(AppSettings(baseUrl, topic, token))
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enregistrer")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                testResult = "Test en cours…"
                viewModel.testConnection(AppSettings(baseUrl, topic, token)) { ok, err ->
                    testResult = if (ok) "✓ Connexion OK (auth acceptée)" else "✗ ${err ?: "échec"}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tester la connexion")
        }
        testResult?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Authentification : le serveur ntfy du VPS est en deny-all — le token Bearer " +
                "est envoyé sur toutes les requêtes (WebSocket, poll JSON, POST /v1/apns). " +
                "Sur iOS, le device token APNs est enregistré automatiquement (POST /v1/apns) " +
                "pour recevoir le push natif.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        current.apnsStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
