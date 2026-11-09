package fr.zesit.admin.traffic

import kotlinx.serialization.Serializable

// Types miroirs de l'API JSON traffic-inspector (/ti-api — zesit-vps/traffic-inspector).
// Convention du repo (cf. fr.zesit.admin.kpi.KpiModels) : noms de propriétés =
// clés JSON exactes (pas de naming strategy), pour éviter toute divergence
// de @SerialName. Champs potentiellement nuls côté serveur : types nullable.
// En-tête absent -> valeur par défaut : l'API peut évoluer sans casser le client.

/** `GET /ti-api/health` (public). */
@Serializable
data class TrafficHealth(
    val ok: Boolean,
    val geoip_local: Boolean = false,
    val secrets_captured: Boolean = false,
    val caddy_ingest: Boolean = false,
    /** False = TI_API_TOKEN vide côté serveur : toutes les autres routes renvoient 404. */
    val api_enabled: Boolean = false,
)

@Serializable
data class TrafficTopIp(val ip: String, val c: Int)

@Serializable
data class TrafficTopPath(val path: String, val c: Int)

@Serializable
data class TrafficTopUa(val ua: String, val c: Int)

@Serializable
data class TrafficTopHost(val host: String, val c: Int)

@Serializable
data class TrafficTopMethod(val method: String, val c: Int)

/** Point de la timeline : `h` = "YYYY-MM-DDTHH", `c` = requêtes. */
@Serializable
data class TrafficTimelinePoint(val h: String, val c: Int)

@Serializable
data class TrafficCountry(val cc: String? = null, val name: String? = null, val c: Int)

@Serializable
data class TrafficAsn(val asn: Long? = null, val org: String? = null, val c: Int)

@Serializable
data class TrafficStats(
    val total: Int,
    val unique_ips: Int,
    val suspicious: Int,
    val top_ips: List<TrafficTopIp> = emptyList(),
    val top_paths: List<TrafficTopPath> = emptyList(),
    val top_uas: List<TrafficTopUa> = emptyList(),
    val top_hosts: List<TrafficTopHost> = emptyList(),
    val methods: List<TrafficTopMethod> = emptyList(),
    val timeline: List<TrafficTimelinePoint> = emptyList(),
)

/** `GET /ti-api/stats`. */
@Serializable
data class TrafficStatsEnvelope(
    val generated_at: String,
    val hours: Double,
    val stats: TrafficStats,
    val countries: List<TrafficCountry> = emptyList(),
    val asns: List<TrafficAsn> = emptyList(),
)

/** Ligne du journal : requête résumée + IP enrichie (jointure serveur). */
@Serializable
data class TrafficRequest(
    val id: Long,
    val ts: String,
    val method: String,
    val path: String,
    val query: String? = null,
    val ip: String? = null,
    val ua_browser: String? = null,
    val ua_os: String? = null,
    val user_agent: String? = null,
    val suspicious: Int = 0,
    val reason: String? = null,
    val host: String? = null,
    val referer: String? = null,
    val tls_version: String? = null,
    val tls_cipher: String? = null,
    /** `honeypot` (traffic.*) ou `caddy` (access logs des sites réels). */
    val source: String = "honeypot",
    /** Null pour les lignes honeypot. */
    val status: Int? = null,
    val country: String? = null,
    val country_code: String? = null,
    val city: String? = null,
    val asn: Long? = null,
    val as_org: String? = null,
)

/** Enveloppe des listes paginées (total = nombre NON paginé de résultats). */
@Serializable
data class TrafficRequestList(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hours: Double = 0.0,
    val requests: List<TrafficRequest> = emptyList(),
)

/** `GET /ti-api/alerts`. */
@Serializable
data class TrafficAlerts(
    val total: Int,
    val limit: Int,
    val generated_at: String,
    val requests: List<TrafficRequest> = emptyList(),
)

/** `GET /ti-api/requests/<id>` : requête brute complète (headers parsés, body éventuel). */
@Serializable
data class TrafficRequestDetail(
    val id: Long? = null,
    val ts: String,
    val method: String,
    val path: String,
    val query: String? = null,
    val proto: String? = null,
    val remote_addr: String? = null,
    val xff: String? = null,
    val ip: String? = null,
    val host: String? = null,
    val user_agent: String? = null,
    val referer: String? = null,
    val content_type: String? = null,
    val content_length: Long? = null,
    /** Headers stockés (sensibles masqués côté serveur : Authorization, Cookie, ...). */
    val headers: Map<String, String> = emptyMap(),
    /** Body stocké (tronqué, secrets masqués) — null pour les lignes venant des access logs. */
    val body: String? = null,
    val body_truncated: Int = 0,
    val tls_version: String? = null,
    val tls_cipher: String? = null,
    val tls_sni: String? = null,
    val tls_client_issuer: String? = null,
    val ua_browser: String? = null,
    val ua_os: String? = null,
    val ua_device: String? = null,
    val suspicious: Int = 0,
    val suspicious_reason: String? = null,
    val duration_ms: Int? = null,
    val source: String = "honeypot",
    val status: Int? = null,
    val resp_size: Int? = null,
)

/** Enrichissement IP mis en cache par le serveur (GeoLite2/RDAP/AbuseIPDB). */
@Serializable
data class TrafficIntel(
    val ip: String? = null,
    @Suppress("ktlint:standard:property-naming") val `private`: Boolean? = null,
    val ptr: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val region: String? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val postal: String? = null,
    val timezone: String? = null,
    val asn: Long? = null,
    val as_org: String? = null,
    val isp: String? = null,
    val org: String? = null,
    val rdap_name: String? = null,
    val rdap_handle: String? = null,
    val rdap_country: String? = null,
    val rdap_start: String? = null,
    val rdap_end: String? = null,
    val abuse_emails: List<String> = emptyList(),
    val abuse_confidence: Int? = null,
    val abuse_reports: Int? = null,
    val abuse_country: String? = null,
    val abuse_isp: String? = null,
    val abuse_usage: String? = null,
    val abuse_last_reported: String? = null,
    val updated_at: String? = null,
)

/** `GET /ti-api/ip/<ip>`. */
@Serializable
data class TrafficIpSheet(
    val ip: String,
    val intel: TrafficIntel = TrafficIntel(),
    val total: Int,
    val requests: List<TrafficRequest> = emptyList(),
)
