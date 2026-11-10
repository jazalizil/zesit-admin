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
 * Ligne d'information non éditable : l'URL du serveur utilisée par le panneau,
 * fournie par l'environnement (env.json / variables, cf. AppEnv) — plus aucun
 * champ URL n'est saisi dans les formulaires de connexion.
 */
@Composable
private fun EnvBaseUrlLine(label: String, baseUrl: String) {
    if (baseUrl.isBlank()) return
    Text(
        "$label (environnement) : $baseUrl",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Panneau d'authentification du serveur KPI, affiché par les pages KPI et
 * Réel / Prévisionnel (une seule et même session) quand l'utilisateur n'est
 * pas connecté : identifiants seulement — l'URL du serveur vient de
 * l'environnement ([EnvBaseUrlLine]). `connecting` affiche le temps du login
 * une progression à la place du bouton.
 */
@Composable
fun KpiAuthPanel(
    savedUser: String,
    savedPassword: String,
    baseUrl: String,
    connecting: Boolean,
    error: String?,
    onConnect: (user: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var user by remember(savedUser) { mutableStateOf(savedUser) }
    var password by remember(savedPassword) { mutableStateOf(savedPassword) }

    StatePanel(modifier) {
        Text("Connexion au serveur KPI", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Renseignez vos identifiants pour charger le prévisionnel et les " +
                "ventes réelles — l'URL du serveur vient de l'environnement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        EnvBaseUrlLine("Serveur KPI", baseUrl)
        Spacer(Modifier.height(16.dp))
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
        ConnectButton(connecting, enabled = user.isNotBlank() && password.isNotBlank()) {
            onConnect(user, password)
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
 * serveur n'est pas configuré : topic (device id) seulement — l'URL ET le
 * token viennent de l'environnement (env.json : *_BASE_URL / NTFY_TOKEN).
 */
@Composable
fun NtfyAuthPanel(
    savedTopic: String,
    baseUrl: String,
    tokenAvailable: Boolean,
    error: String?,
    onConnect: (topic: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var topic by remember(savedTopic) { mutableStateOf(savedTopic) }

    StatePanel(modifier) {
        Text("Connexion au serveur ntfy", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Renseignez le topic (device id) pour recevoir les notifications " +
                "en temps réel — l'URL du serveur et le token d'accès viennent " +
                "de l'environnement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        EnvBaseUrlLine("Serveur", baseUrl)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic (device id)") },
            placeholder = { Text("zesit-…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
        )
        if (!tokenAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Token ntfy introuvable dans l'environnement (NTFY_TOKEN) — " +
                    "définissez-le puis relancez l'app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Sans token dans l'environnement (serveur deny-all), connecter est inutile.
        ConnectButton(connecting = false, enabled = topic.isNotBlank() && tokenAvailable) {
            onConnect(topic)
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
 * s'authentifie par le token statique TI_API_TOKEN défini côté VPS. URL comme
 * token viennent de l'environnement : aucun champ de saisie.
 */
@Composable
fun TrafficAuthPanel(
    baseUrl: String,
    tokenAvailable: Boolean,
    error: String?,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatePanel(modifier) {
        Text("Connexion à traffic-inspector", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "L'URL du serveur /ti-api (VPN-only via WireGuard) et le token d'accès " +
                "viennent de l'environnement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        EnvBaseUrlLine("Serveur", baseUrl)
        if (!tokenAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Token TI introuvable dans l'environnement (TI_API_TOKEN) — " +
                    "définissez-le puis relancez l'app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Sans token TI dans l'environnement, connecter est inutile (401) :
        // le bouton attend que la variable TI_API_TOKEN soit fournie.
        ConnectButton(connecting = false, enabled = tokenAvailable) {
            onConnect()
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
 * Panneau de connexion security-scanner, affiché par la page Sécurité quand le
 * serveur n'est pas configuré. L'API /sc-api est VPN-only (Caddy/WireGuard) et
 * s'authentifie par le token statique SC_API_TOKEN défini côté VPS. Pattern de
 * [TrafficAuthPanel] : URL et token viennent de l'environnement.
 */
@Composable
fun SecurityAuthPanel(
    baseUrl: String,
    tokenAvailable: Boolean,
    error: String?,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatePanel(modifier) {
        Text("Connexion à security-scanner", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Suivi des vulnérabilités des images des conteneurs du VPS : " +
                "l'URL du serveur /sc-api (VPN-only via WireGuard) et le token " +
                "d'accès viennent de l'environnement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        EnvBaseUrlLine("Serveur", baseUrl)
        if (!tokenAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Token SC introuvable dans l'environnement (SC_API_TOKEN) — " +
                    "définissez-le puis relancez l'app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Sans token SC dans l'environnement, connecter est inutile (401) :
        // le bouton attend que la variable SC_API_TOKEN soit fournie.
        ConnectButton(connecting = false, enabled = tokenAvailable) {
            onConnect()
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
