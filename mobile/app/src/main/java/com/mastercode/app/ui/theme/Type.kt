package com.mastercode.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mastercode.app.R

// Inter para el texto y JetBrains Mono para el código, empaquetadas en res/font (licencia OFL, mobile/licencias).
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val Tipografia = Typography(
    headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    // Estilo Consoly: los botones y las etiquetas van en mayúsculas (se pasan con .uppercase()) y con letra espaciada.
    labelLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.0.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp),
)

// El código de los ejercicios, de los marcadores y de las cifras grandes.
val EstiloDeCodigo = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)
