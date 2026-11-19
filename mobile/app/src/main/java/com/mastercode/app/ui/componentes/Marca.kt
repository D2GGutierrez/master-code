package com.mastercode.app.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mastercode.app.R

// La marca de MASTER CODE: el escudo con los corchetes del código, en su versión clara para las cabeceras oscuras. Es decorativa.
@Composable
fun LogoMC(modifier: Modifier = Modifier, tamano: Dp = 56.dp) {
    Image(painter = painterResource(R.drawable.ilu_logo_claro), contentDescription = null, modifier = modifier.size(tamano))
}
