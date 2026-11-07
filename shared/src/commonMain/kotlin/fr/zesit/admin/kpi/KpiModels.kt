package fr.zesit.admin.kpi

import kotlinx.serialization.Serializable

// Types miroirs de l'API Flask (zesit-kpi/apps/api/model.py) — noms de propriétés
// = clés JSON exactes pour éviter toute divergence de @SerialName.

/** Hypothèses d'un format de bouteille (25 / 33 / 50 cl). */
@Serializable
data class KpiFmt(
    val cl: Int,
    val qty: Double,
    val prixCHR: Double,
    val prixGMS: Double,
    val copackVerre: Double,
    val copackPET: Double,
)

/** Hypothèses globales envoyées à `POST /api/model` (source de vérité : le serveur). */
@Serializable
data class KpiInputs(
    val formats: List<KpiFmt>,
    val casse: Double,
    val partGMS: Double,
    val tauxRFA: Double,
    val pate: Double,
    val fret: Double,
    val sucre: Double,
    val vanille: Double,
    val transport: Double,
    val marketing: Double,
    val compta: Double,
    val loyer: Double,
    val remu: Double,
    val laboParLot: Double,
    val laboLotsAn: Double,
    val oneShot: Double,
    val apport: Double,
)

/** Compte de résultat d'un mois du prévisionnel année 1. */
@Serializable
data class KpiMonth(
    val mois: String,
    val prod: Double,
    val volume: Double,
    val vendu: Double,
    val caGMS: Double,
    val caCHR: Double,
    val caTotal: Double,
    val coutsVar: Double,
    val marge: Double,
    val rfa: Double,
    val resultat: Double,
    val cumul: Double,
    val treso: Double,
)

/** Dérivés d'un format calculés par l'API (coûts, marges par canal). */
@Serializable
data class KpiFormatComputed(
    val cl: Int,
    val qty: Double,
    val prixCHR: Double,
    val prixGMS: Double,
    val copackVerre: Double,
    val copackPET: Double,
    val vol: Double,
    val ingBtl: Double,
    val cvVerre: Double,
    val cvPET: Double,
    val margeCHR: Double,
    val margeGMS: Double,
)

@Serializable
data class KpiTotals(
    val prod: Double,
    val vendu: Double,
    val caGMS: Double,
    val caCHR: Double,
    val caTotal: Double,
    val marge: Double,
    val rfa: Double,
    val resultat: Double,
)

@Serializable
data class KpiSensiPate(val pate: Double, val resultat: Double)

@Serializable
data class KpiSensiMix(val mix: Double, val resultat: Double)

@Serializable
data class KpiSensibilite(val pate: List<KpiSensiPate>, val mix: List<KpiSensiMix>)

/** Réponse complète de `POST /api/model` : mois, totaux, trésorerie, seuil, sensibilités. */
@Serializable
data class KpiResponse(
    val months: List<KpiMonth>,
    val totals: KpiTotals,
    val tresoMin: Double,
    val tresoFin: Double,
    val annee2: Double,
    val ingPerL: Double,
    val formats: List<KpiFormatComputed>,
    val volPlen: Double,
    val qtyTot: Double,
    val pateKgMois: Double,
    val pateKgAn1: Double,
    val pateKgAn2: Double,
    val pateCoutMois: Double,
    val laboAn: Double,
    val chargesMois: Double,
    val seuilL: Double? = null,
    val sensibilite: KpiSensibilite,
)

// --- Modèles enregistrés (persistance serveur, /api/models) ---

@Serializable
data class KpiModelKpi(
    val caAnnee1: Double,
    val resultatAnnee1: Double,
    val tresorerieFinAnnee1: Double,
)

@Serializable
data class KpiModelSummary(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val kpi: KpiModelKpi,
)

@Serializable
data class KpiModelDetail(
    val id: String,
    val user: String,
    val name: String,
    val description: String,
    val inputs: KpiInputs,
    val resultats: KpiResponse,
    val createdAt: String,
    val updatedAt: String,
)
