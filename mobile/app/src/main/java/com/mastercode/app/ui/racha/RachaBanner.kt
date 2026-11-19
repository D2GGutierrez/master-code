package com.mastercode.app.ui.racha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercode.app.R
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.CristalSobreHero
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave

// HU49-T3 (CA-49.3): la racha actual en el hero de la pantalla principal, en una tarjeta de vidrio con una llama. Mientras
// carga o si no se pudo saber, no ocupa lugar (el espacio de arriba va dentro de la tarjeta: sin racha, no queda un hueco).
@Composable
fun RachaBanner(viewModel: RachaViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val racha = estado as? EstadoDeRacha.ConRacha ?: return

    val forma = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(CristalSobreHero, forma)
            .border(1.dp, BordeSobreHero, forma)
            .padding(14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(CristalSobreHero, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Con racha, la llama encendida; sin racha, la misma llama apagada (en gris y más tenue).
            Image(
                painter = painterResource(R.drawable.ilu_racha),
                contentDescription = null,
                modifier = Modifier.size(34.dp).alpha(if (racha.rachaActual > 0) 1f else 0.5f),
                colorFilter = if (racha.rachaActual > 0) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (racha.rachaActual > 0) {
                val titulo = if (racha.rachaActual == 1) R.string.racha_dias_uno else R.string.racha_dias_otros
                Text(stringResource(titulo, racha.rachaActual), style = MaterialTheme.typography.titleMedium, color = SobreHero)
            }
            val mensaje = when (racha.mensaje) {
                MensajeDeRacha.Empezar -> R.string.racha_empezar
                MensajeDeRacha.PracticarHoy -> R.string.racha_practicar_hoy
                MensajeDeRacha.YaPractico -> R.string.racha_ya_practicaste
            }
            Text(stringResource(mensaje), style = MaterialTheme.typography.bodySmall, color = SobreHeroSuave)
        }
    }
}
