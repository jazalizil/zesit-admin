package fr.zesit.admin.kpi

import kotlinx.serialization.Serializable

// Types miroirs de l'API Medusa (zesit-kpi/apps/api/medusa.py) — clés JSON exactes.

/** État d'une synchro quotidienne Medusa → SQLite. */
@Serializable
data class MedusaSyncStatus(
    val run_date: String,
    val window_start: String,
    val window_end: String,
    val synced_at: String,
    val status: String,
    val orders_count: Int,
    val error: String? = null,
)

@Serializable
data class MedusaStatus(
    val syncs: List<MedusaSyncStatus>,
    val last: MedusaSyncStatus? = null,
    val totalLines: Long,
    val totalQty: Long,
)

/** Variante produit Medusa mappée (ou non) à un format du prévisionnel. */
@Serializable
data class MedusaVariant(
    val variant_id: String,
    val product_title: String? = null,
    val variant_title: String? = null,
    val sku: String? = null,
    val format_cl: Int? = null,
)

/** Sales channel Medusa : son nom fait office de canal de vente dans la comparaison. */
@Serializable
data class MedusaChannel(
    val sales_channel_id: String,
    val name: String? = null,
)

@Serializable
data class MedusaMapping(
    val variants: List<MedusaVariant>,
    val channels: List<MedusaChannel>,
)

@Serializable
data class MedusaSyncRun(
    val run_date: String,
    val status: String,
    val orders_count: Int,
    val lines: Int,
)

@Serializable
data class MedusaSyncFailure(val run_date: String, val error: String)

@Serializable
data class MedusaSyncResult(
    val results: List<MedusaSyncRun>,
    val errors: List<MedusaSyncFailure>,
)

@Serializable
data class MedusaGlobal(
    val prevu: Double,
    val reel: Double,
    val ecart: Double,
    val ecartPct: Double? = null,
)

@Serializable
data class MedusaByFormatChannel(
    val format: Int,
    val canal: String,
    val prevu: Double,
    val reel: Double,
    val ecart: Double,
    val ecartPct: Double? = null,
)

@Serializable
data class MedusaByFormat(
    val format: Int,
    val prevu: Double,
    val reel: Double,
    val ecart: Double,
    val ecartPct: Double? = null,
)

@Serializable
data class MedusaByChannel(
    val canal: String,
    val prevu: Double,
    val reel: Double,
    val ecart: Double,
    val ecartPct: Double? = null,
)

@Serializable
data class MedusaDay(
    val day: String,
    val prevu: Double,
    val reel: Double,
    val cumulPrevu: Double,
    val cumulReel: Double,
)

@Serializable
data class MedusaUnmappedSale(
    val variant_id: String,
    val product_title: String? = null,
    val variant_title: String? = null,
    val sku: String? = null,
    val qty: Double,
)

/** Réponse de `POST /api/medusa/comparison` : réel (ventes Medusa synchronisées) vs prévisionnel. */
@Serializable
data class MedusaComparison(
    val from: String,
    val to: String,
    val global: MedusaGlobal,
    val byFormatChannel: List<MedusaByFormatChannel>,
    val byFormat: List<MedusaByFormat>,
    val byChannel: List<MedusaByChannel>,
    val byDay: List<MedusaDay>,
    val unmapped: List<MedusaUnmappedSale>,
    val sync: List<MedusaSyncStatus>,
)
