package fr.zesit.admin.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.zesit.admin.ui.theme.fieldColors
import fr.zesit.admin.ui.theme.filledButtonColors

/**
 * Panneau d'authentification du serveur KPI, affiché par les pages KPI et
 * Réel / Prévisionnel (une seule et même session) quand l'utilisateur n'est
 * pas connecté : URL du serveur + identifiants, bouton « Se connecter ».
 * Une session existante (settings) pré-remplit les champs ; `connecting`
 * affiche le temps du login une progression à la place du bouton.
 */
@Composable
fun KpiAuthPanel(
    savedBaseUrl: String,
    savedUser: String,
    savedPassword: String,
    defaultBaseUrl: String,
    connecting: Boolean,
    error: String?,
    onConnect: (baseUrl: String, user: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var user by remember(savedUser) { mutableStateOf(savedUser) }
    var password by remember(savedPassword) { mutableStateOf(savedPassword) }

    StatePanel(modifier) {
        Text("Connexion au serveur KPI", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Renseignez l'URL du serveur et vos identifiants pour charger le " +
                "prévisionnel et les ventes réelles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL du serveur KPI") },
            placeholder = { Text(defaultBaseUrl) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Utilisateur") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        ConnectButton(connecting, enabled = baseUrl.isNotBlank() && user.isNotBlank() && password.isNotBlank()) {
            onConnect(baseUrl, user, password)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Panneau d'authentification ntfy, affiché par la page Notifications quand le
 * serveur n'est pas configuré : URL + topic + token (optionnel).
 */
@Composable
fun NtfyAuthPanel(
    savedBaseUrl: String,
    savedTopic: String,
    savedToken: String,
    defaultBaseUrl: String,
    error: String?,
    onConnect: (baseUrl: String, topic: String, token: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var topic by remember(savedTopic) { mutableStateOf(savedTopic) }
    var token by remember(savedToken) { mutableStateOf(savedToken) }

    StatePanel(modifier) {
        Text("Connexion au serveur ntfy", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Renseignez l'URL du serveur, le topic (device id) et le token " +
                "d'accès pour recevoir les notifications en temps réel.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL du serveur") },
            placeholder = { Text(defaultBaseUrl) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic (device id)") },
            placeholder = { Text("zesit-…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
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
            colors = fieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        ConnectButton(connecting = false, enabled = baseUrl.isNotBlank() && topic.isNotBlank()) {
            onConnect(baseUrl, topic, token)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Bouton « Se connecter » (icône login), remplacé par une progression pendant la connexion. */
@Composable
private fun ConnectButton(connecting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (connecting) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Connexion…", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Button(onClick = onClick, enabled = enabled, colors = filledButtonColors()) {
            Icon(
                Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Se connecter")
        }
    }
}

/**
 * Panneau de connexion traffic-inspector, affiché par la page Traffic quand le
 * serveur n'est pas configuré. L'API /ti-api est VPN-only (Caddy/WireGuard) et
 * s'authentifie par le token statique TI_API_TOKEN défini côté VPS.
 */
@Composable
fun TrafficAuthPanel(
    savedBaseUrl: String,
    savedToken: String,
    defaultBaseUrl: String,
    error: String?,
    onConnect: (baseUrl: String, token: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var token by remember(savedToken) { mutableStateOf(savedToken) }

    StatePanel(modifier) {
        Text("Connexion à traffic-inspector", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Renseignez l'URL du serveur (/ti-api, VPN-only via WireGuard) et le " +
                "token statique TI_API_TOKEN défini sur le VPS.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL du serveur") },
            placeholder = { Text(defaultBaseUrl) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token TI (Bearer …)") },
            placeholder = { Text("Token TI_API_TOKEN du VPS") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        ConnectButton(connecting = false, enabled = baseUrl.isNotBlank() && token.isNotBlank()) {
            onConnect(baseUrl, token)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}
