package fr.zesit.admin.security

import kotlinx.serialization.Serializable

// Types miroirs de l'API JSON security-scanner (/sc-api — zesit-vps/security-scanner).
// Convention du repo (cf. fr.zesit.admin.traffic.TrafficModels) : noms de
// propriétés = clés JSON exactes (pas de naming strategy), pour éviter toute
// divergence de @SerialName. Champs potentiellement nuls côté serveur :
// types nullable ; en-tête absent -> valeur par défaut : l'API peut évoluer
// sans casser le client.

/** `GET /sc-api/health` (public). `api_enabled` = SC_API_TOKEN configuré. */
@Serializable
data class SecurityHealth(
    val ok: Boolean,
    /** False = SC_API_TOKEN vide côté serveur : toutes les autres routes renvoient 404. */
    val api_enabled: Boolean = false,
    /** Un cycle de scan est en cours (ou demandé) sur le serveur. */
    val scanning: Boolean = false,
    val trivy_version: String? = null,
    val images_known: Int = 0,
    val findings: Int = 0,
    val last_run: SecurityRun? = null,
    /** Sévérités scannées côté serveur (ex. ["CRITICAL", "HIGH"]). */
    val severities: List<String> = emptyList(),
    /** CVE sans version corrigée ignorées côté serveur. */
    val ignore_unfixed: Boolean = false,
    val interval_hours: Double = 24.0,
)

/** Un cycle de scan (table `runs` côté serveur). */
@Serializable
data class SecurityRun(
    val id: Long,
    val started: String,
    val finished: String? = null,
    /** requested | running | done | error */
    val status: String = "done",
    /** startup | scheduled | manual */
    val trigger: String = "scheduled",
    val images_total: Int? = null,
    val images_ok: Int? = null,
    val images_error: Int? = null,
    val error: String? = null,
)

/** Conteneur partageant l'image de la fiche ([SecurityImage]). */
@Serializable
data class SecurityContainer(
    val name: String,
    val service: String? = null,
    val project: String? = null,
)

/** Vulnérabilité CWE d'une image (état courant ou delta « nouvelle »). */
@Serializable
data class SecurityFinding(
    val image_id: String? = null,
    val cve_key: String,
    val cve_id: String,
    val severity: String,
    @Suppress("ktlint:standard:property-naming") val `package`: String? = null,
    val installed: String? = null,
    /** Version qui corrige la vulnérabilité (vide = pas de fix connu). */
    val fixed: String? = null,
    val title: String? = null,
    val url: String? = null,
    val first_seen: String? = null,
)

/** Fiche image du résumé ([SecuritySummary.images]). */
@Serializable
data class SecurityImage(
    val image_id: String,
    val ref: String,
    val name: String,
    val containers: List<SecurityContainer> = emptyList(),
    /** False = l'image ne tourne plus au dernier scan (historique conservé). */
    val active: Boolean = true,
    /** Comptes par sévérité du dernier scan, ex. {"CRITICAL": 2, "HIGH": 7}. */
    val counts: Map<String, Int> = emptyMap(),
    val total: Int = 0,
    /** Sévérité la plus élevée présente, null si aucune vulnérabilité. */
    val severity_top: String? = null,
    /** Premier scan réussi de cette image (état initial, pas de delta). */
    val first_scan: Boolean = false,
    val last_scan: String? = null,
    /** done | error */
    val status: String? = null,
    val error: String? = null,
    /** Vulnérabilités APPARUES au dernier scan (deltas vs le précédent). */
    val new: List<SecurityFinding> = emptyList(),
    /** Clés CVE des vulnérabilités CORRIGÉES au dernier scan. */
    val fixed: List<String> = emptyList(),
)

/** `GET /sc-api/summary` : état courant de toutes les images suivies. */
@Serializable
data class SecuritySummary(
    val generated_at: String,
    val totals: Map<String, Int> = emptyMap(),
    val total: Int = 0,
    val images: List<SecurityImage> = emptyList(),
    val last_run: SecurityRun? = null,
)

/** En-tête image de la liste détaillée ([SecurityFindingsList]). */
@Serializable
data class SecurityImageBrief(
    val image_id: String,
    val ref: String,
    val name: String,
    val counts: Map<String, Int> = emptyMap(),
)

/** `GET /sc-api/findings?image=…` : vulnérabilités d'une image (état courant). */
@Serializable
data class SecurityFindingsList(
    val image: SecurityImageBrief,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val findings: List<SecurityFinding> = emptyList(),
)

/** `GET /sc-api/scans` : historique des cycles. */
@Serializable
data class SecurityScans(
    val runs: List<SecurityRun> = emptyList(),
)
