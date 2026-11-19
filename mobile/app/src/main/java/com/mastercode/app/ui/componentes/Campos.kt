package com.mastercode.app.ui.componentes

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.mastercode.app.ui.theme.BordeSobreHero
import com.mastercode.app.ui.theme.HeroBrillo
import com.mastercode.app.ui.theme.MC
import com.mastercode.app.ui.theme.SobreHero
import com.mastercode.app.ui.theme.SobreHeroSuave

// Los colores de un campo de texto: fondo de tarjeta, borde fino y acento al enfocar. Sobre un bloque de código oscuro
// (sobreCodigo) el campo es de vidrio claro, como en un editor.
@Composable
fun coloresDeCampo(sobreCodigo: Boolean = false): TextFieldColors {
    val c = MC.colores
    return if (sobreCodigo) {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = SobreHero,
            unfocusedTextColor = SobreHero,
            disabledTextColor = SobreHeroSuave,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            focusedBorderColor = HeroBrillo,
            unfocusedBorderColor = BordeSobreHero,
            disabledBorderColor = BordeSobreHero,
            cursorColor = HeroBrillo,
            focusedPlaceholderColor = SobreHeroSuave,
            unfocusedPlaceholderColor = SobreHeroSuave,
        )
    } else {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.texto,
            unfocusedTextColor = c.texto,
            disabledTextColor = c.textoSuave,
            focusedContainerColor = c.fondoSuave,
            unfocusedContainerColor = c.fondoSuave,
            disabledContainerColor = c.fondoSuave,
            focusedBorderColor = c.acento,
            unfocusedBorderColor = c.bordeFuerte,
            disabledBorderColor = c.borde,
            errorBorderColor = c.error,
            focusedLabelColor = c.acento,
            unfocusedLabelColor = c.textoSuave,
            cursorColor = c.acento,
            focusedLeadingIconColor = c.acento,
            unfocusedLeadingIconColor = c.textoSuave,
            focusedTrailingIconColor = c.textoSuave,
            unfocusedTrailingIconColor = c.textoSuave,
        )
    }
}

// El campo de texto de toda la app: esquinas de 6, fondo suave y borde fino y la letra del tema (o la de código).
@Composable
fun CampoDeTextoMC(
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    placeholder: String? = null,
    habilitado: Boolean = true,
    unaLinea: Boolean = false,
    lineasMinimas: Int = 1,
    iconoInicial: (@Composable () -> Unit)? = null,
    iconoFinal: (@Composable () -> Unit)? = null,
    opcionesDeTeclado: KeyboardOptions = KeyboardOptions.Default,
    transformacionVisual: VisualTransformation = VisualTransformation.None,
    estiloDeTexto: TextStyle = MaterialTheme.typography.bodyLarge,
    sobreCodigo: Boolean = false,
) {
    // Con el tipo escrito, el compilador sabe que la lambda es @Composable (con un if directo en el parámetro no lo deduce).
    val textoDeEtiqueta: (@Composable () -> Unit)? = if (etiqueta != null) {
        { Text(etiqueta) }
    } else {
        null
    }
    val textoDePlaceholder: (@Composable () -> Unit)? = if (placeholder != null) {
        { Text(placeholder, style = estiloDeTexto) }
    } else {
        null
    }
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier,
        enabled = habilitado,
        textStyle = estiloDeTexto,
        label = textoDeEtiqueta,
        placeholder = textoDePlaceholder,
        leadingIcon = iconoInicial,
        trailingIcon = iconoFinal,
        singleLine = unaLinea,
        minLines = lineasMinimas,
        keyboardOptions = opcionesDeTeclado,
        visualTransformation = transformacionVisual,
        shape = MaterialTheme.shapes.medium,
        colors = coloresDeCampo(sobreCodigo),
    )
}
