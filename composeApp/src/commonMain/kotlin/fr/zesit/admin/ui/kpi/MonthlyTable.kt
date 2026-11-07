package fr.zesit.admin.ui.kpi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.zesit.admin.kpi.KpiResponse

/** Compte de résultat mensuel (web : onglet « Détail mensuel », table 11 colonnes). */
@Composable
fun MonthlyTable(model: KpiResponse, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            TableRow(
                cells = listOf(
                    "Mois", "Production", "Volume (L)", "Vendues", "CA GMS", "CA CHR",
                    "CA total", "Coûts var.", "RFA", "Résultat", "Trésorerie",
                ),
                widths = COLUMN_WIDTHS,
                header = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            model.months.forEach { m ->
                TableRow(
                    cells = listOf(
                        m.mois,
                        fmtInt(m.prod), fmtInt(m.volume), fmtInt(m.vendu),
                        fmtInt(m.caGMS), fmtInt(m.caCHR), fmtInt(m.caTotal),
                        fmtInt(m.coutsVar), fmtInt(m.rfa), fmtInt(m.resultat), fmtInt(m.treso),
                    ),
                    widths = COLUMN_WIDTHS,
                    resultat = m.resultat,
                    treso = m.treso,
                    boldCa = true,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TableRow(
                cells = listOf(
                    "Année 1",
                    fmtInt(model.totals.prod),
                    fmtInt(model.volPlen * 9.75),
                    fmtInt(model.totals.vendu),
                    fmtInt(model.totals.caGMS),
                    fmtInt(model.totals.caCHR),
                    fmtInt(model.totals.caTotal),
                    "—",
                    fmtInt(model.totals.rfa),
                    fmtInt(model.totals.resultat),
                    fmtInt(model.tresoFin),
                ),
                widths = COLUMN_WIDTHS,
                resultat = model.totals.resultat,
                treso = model.tresoFin,
                total = true,
            )
        }
    }
}

private val COLUMN_WIDTHS = listOf(56, 92, 84, 76, 84, 84, 84, 84, 76, 84, 92).map { it.dp }

@Composable
private fun TableRow(
    cells: List<String>,
    widths: List<androidx.compose.ui.unit.Dp>,
    header: Boolean = false,
    total: Boolean = false,
    resultat: Double? = null,
    treso: Double? = null,
    boldCa: Boolean = false,
) {
    val base = if (header) {
        MaterialTheme.typography.labelSmall
    } else if (total) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    Row(modifier = Modifier.padding(horizontal = 12.dp)) {
        cells.forEachIndexed { i, cell ->
            val isResultatCol = i == 9
            val isTresoCol = i == 10
            val color = when {
                isResultatCol && resultat != null ->
                    if (resultat >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                isTresoCol && treso != null && treso < 0 -> Color(0xFFDC2626)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                cell,
                style = base,
                color = color,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                modifier = Modifier.width(widths[i]),
            )
        }
    }
}
