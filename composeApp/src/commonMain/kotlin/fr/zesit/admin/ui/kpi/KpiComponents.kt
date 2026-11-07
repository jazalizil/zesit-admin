package fr.zesit.admin.ui.kpi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiFormatComputed
import fr.zesit.admin.viewmodel.KpiFormatKey
import fr.zesit.admin.viewmodel.KpiInputKey
import kotlin.math.roundToInt

// Composants du dashboard — portage des KpiCard / SliderRow / NumRow / FormatCard
// du web (zesit-kpi/apps/web/src/pages/DashboardPage.tsx).
// Chaque composant est un « expect » : la variante desktop vit dans desktopMain
// (un fichier par composant) ; l'agencement commun de référence est Default*.

enum class KpiTone { NEUTRAL, GOOD, BAD }

/** Carte d'indicateur (web : composant KpiCard) — agencement commun : [DefaultKpiCard]. */
@Composable
expect fun KpiCard(
    label: String,
    value: String,
    sub: String?,
    tone: KpiTone,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultKpiCard(
    label: String,
    value: String,
    sub: String?,
    tone: KpiTone,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = when (tone) {
                    KpiTone.GOOD -> MaterialTheme.colorScheme.secondary
                    KpiTone.BAD -> MaterialTheme.colorScheme.error
                    KpiTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (!sub.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Slider avec libellé + valeur formatée (web : composant SliderRow) — commun : [DefaultSliderRow]. */
@Composable
expect fun SliderRow(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    step: Double,
    display: String,
    onValueChange: (Double) -> Unit,
)

@Composable
internal fun DefaultSliderRow(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    step: Double,
    display: String,
    onValueChange: (Double) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val steps = (((max - min) / step).roundToInt() - 1).coerceAtLeast(0)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps,
        )
    }
}

/** Champ numérique borné (web : composant NumRow) — commun : [DefaultNumRow]. */
@Composable
expect fun NumRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
)

@Composable
internal fun DefaultNumRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        var text by remember(value) { mutableStateOf(fmtEditable(value)) }
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                raw.replace(',', '.').toDoubleOrNull()?.let { if (it != value) onValueChange(it) }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(120.dp),
        )
    }
}

private fun fmtEditable(v: Double): String = v.toString()

/** Carte d'édition d'un format (web : composant FormatCard) — commun : [DefaultFormatCardEditor]. */
@Composable
internal expect fun FormatCardEditor(
    input: KpiFmt,
    computed: KpiFormatComputed?,
    onSet: (KpiFormatKey, Double) -> Unit,
)

@Composable
internal fun DefaultFormatCardEditor(
    input: KpiFmt,
    computed: KpiFormatComputed?,
    onSet: (KpiFormatKey, Double) -> Unit,
) {
    val actif = input.qty > 0
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (actif) Modifier else Modifier.alpha(0.6f))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Format ${input.cl} cl", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { onSet(KpiFormatKey.QTY, if (actif) 0.0 else 1000.0) },
                    label = {
                        Text(
                            if (actif) "${fmtQty(input.qty)} btl/mois" else "Inactif (quantité 0)",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            SliderRow(
                label = "Quantité plein régime",
                value = input.qty,
                min = 0.0,
                max = 5000.0,
                step = 100.0,
                display = "${fmtQty(input.qty)} btl/mois · ${fmtQty(input.qty * input.cl / 100.0)} L",
            ) { onSet(KpiFormatKey.QTY, it) }
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    NumRow("Prix CHR verre (HT)", input.prixCHR) { onSet(KpiFormatKey.PRIX_CHR, it) }
                }
                NumRow("Prix achat GMS PET (HT)", input.prixGMS) { onSet(KpiFormatKey.PRIX_GMS, it) }
                NumRow("Copack verre (€/btl)", input.copackVerre) { onSet(KpiFormatKey.COPACK_VERRE, it) }
                NumRow("Copack PET (€/btl)", input.copackPET) { onSet(KpiFormatKey.COPACK_PET, it) }
            }
            computed?.let { c ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ingrédients : ${eur2(c.ingBtl)}/btl (${eur2(c.ingBtl / c.vol)}/L) · Coût variable : " +
                        "${eur2(c.cvVerre)} (verre) / ${eur2(c.cvPET)} (PET) · Marge : " +
                        "${eur2(c.margeCHR)} (CHR) / ${eur2(c.margeGMS)} (GMS)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small,
                        )
                        .padding(10.dp),
                )
            }
        }
    }
}

/** Panneau des hypothèses globales (web : « Hypothèses globales ») — commun : [DefaultHypothesesPanel]. */
@Composable
internal expect fun HypothesesPanel(
    inputs: fr.zesit.admin.kpi.KpiInputs,
    model: fr.zesit.admin.kpi.KpiResponse?,
    onSet: (KpiInputKey, Double) -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultHypothesesPanel(
    inputs: fr.zesit.admin.kpi.KpiInputs,
    model: fr.zesit.admin.kpi.KpiResponse?,
    onSet: (KpiInputKey, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hypothèses globales", style = MaterialTheme.typography.titleMedium)
            SliderRow("Pâte de citron (€/kg)", inputs.pate, 1.0, 2.0, 0.05, fmtPate(inputs.pate)) {
                onSet(KpiInputKey.PATE, it)
            }
            SliderRow(
                "Part GMS (PET) — tous formats",
                inputs.partGMS,
                0.0,
                1.0,
                0.05,
                "${(inputs.partGMS * 100).roundToInt()} % GMS / ${((1 - inputs.partGMS) * 100).roundToInt()} % CHR",
            ) { onSet(KpiInputKey.PART_GMS, it) }
            SliderRow("Casse et invendus", inputs.casse, 0.0, 0.08, 0.005, fmtPct(inputs.casse * 100)) {
                onSet(KpiInputKey.CASSE, it)
            }

            SectionTitle("Ingrédients")
            NumRow(KpiInputKey.SUCRE.label, inputs.sucre) { onSet(KpiInputKey.SUCRE, it) }
            NumRow(KpiInputKey.VANILLE.label, inputs.vanille) { onSet(KpiInputKey.VANILLE, it) }
            NumRow(KpiInputKey.FRET.label, inputs.fret) { onSet(KpiInputKey.FRET, it) }

            SectionTitle("Charges & structure (€/mois)")
            NumRow(KpiInputKey.TRANSPORT.label, inputs.transport) { onSet(KpiInputKey.TRANSPORT, it) }
            NumRow(KpiInputKey.MARKETING.label, inputs.marketing) { onSet(KpiInputKey.MARKETING, it) }
            NumRow(KpiInputKey.COMPTA.label, inputs.compta) { onSet(KpiInputKey.COMPTA, it) }
            NumRow(KpiInputKey.LOYER.label, inputs.loyer) { onSet(KpiInputKey.LOYER, it) }
            NumRow(KpiInputKey.REMU.label, inputs.remu) { onSet(KpiInputKey.REMU, it) }

            SectionTitle("Lancement")
            NumRow(KpiInputKey.LABO_PAR_LOT.label, inputs.laboParLot) { onSet(KpiInputKey.LABO_PAR_LOT, it) }
            NumRow(KpiInputKey.LABO_LOTS_AN.label, inputs.laboLotsAn) { onSet(KpiInputKey.LABO_LOTS_AN, it) }
            NumRow(KpiInputKey.ONE_SHOT.label, inputs.oneShot) { onSet(KpiInputKey.ONE_SHOT, it) }
            NumRow(KpiInputKey.APPORT.label, inputs.apport) { onSet(KpiInputKey.APPORT, it) }

            model?.let { m ->
                Text(
                    "Ingrédients : ${eur2(m.ingPerL)}/L · Analyses labo : ${eur(m.laboAn)}/an · " +
                        "Charges : ${eur(m.chargesMois)}/mois",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                        .padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}
