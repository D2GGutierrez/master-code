package com.mastercode.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// El lenguaje visual de la app móvil, inspirado en Consoly (docs/diseno-movil.md): fondo azul marino casi negro, tarjetas con borde
// fino, azul de acento y un botón principal azul grande. La app es oscura siempre; ColoresClaros se conserva solo por compatibilidad.
@Immutable
class ColoresMC(
    val esOscuro: Boolean,
    val lienzo: Color,
    val fondo: Color,
    val fondoSuave: Color,
    val texto: Color,
    val textoSuave: Color,
    val borde: Color,
    val bordeFuerte: Color,
    val acento: Color,
    val acentoSuave: Color,
    val sobreAcento: Color,
    val exito: Color,
    val error: Color,
    val resalte: Color,
    // El botón de la acción principal es verde, como «Create account» de GitHub.
    val primario: Color,
    val sobrePrimario: Color,
)

internal val ColoresClaros = ColoresMC(
    esOscuro = false,
    lienzo = Color(0xFFF6F8FA),
    fondo = Color(0xFFFFFFFF),
    fondoSuave = Color(0xFFF6F8FA),
    texto = Color(0xFF1F2328),
    textoSuave = Color(0xFF59636E),
    borde = Color(0xFFD1D9E0),
    bordeFuerte = Color(0xFFAFB8C1),
    acento = Color(0xFF0969DA),
    acentoSuave = Color(0xFFDDF4FF),
    sobreAcento = Color(0xFFFFFFFF),
    exito = Color(0xFF116329),
    error = Color(0xFFD1242F),
    resalte = Color(0xFFFFF8C5),
    primario = Color(0xFF1F883D),
    sobrePrimario = Color(0xFFFFFFFF),
)

internal val ColoresOscuros = ColoresMC(
    esOscuro = true,
    lienzo = Color(0xFF0B0F17),
    fondo = Color(0xFF151B27),
    fondoSuave = Color(0xFF1B2333),
    texto = Color(0xFFF2F5FA),
    textoSuave = Color(0xFF8B95A7),
    borde = Color(0xFF263044),
    bordeFuerte = Color(0xFF34405A),
    acento = Color(0xFF2B90E8),
    acentoSuave = Color(0x262B90E8),
    sobreAcento = Color(0xFFFFFFFF),
    exito = Color(0xFF4CC15F),
    error = Color(0xFFF85149),
    resalte = Color(0x33F5A524),
    primario = Color(0xFF0078D4),
    sobrePrimario = Color(0xFFFFFFFF),
)

// Los colores que la app comparte en todas las pantallas (antes eran los del «hero» oscuro; ahora siguen el estilo de Consoly:
// azul marino casi negro, tarjetas con borde fino y azul de acento). Se conservan los nombres para no tocar cada pantalla.
val HeroA = Color(0xFF0B0F17)
val HeroB = Color(0xFF151B27)
val HeroC = Color(0xFF1B2333)
val HeroBrillo = Color(0xFF2B90E8)
val Barra = Color(0xFF0F1521)
val BarraOscura = Color(0xFF0B0F17)
val SobreHero = Color(0xFFF2F5FA)
val SobreHeroSuave = Color(0xFF8B95A7)
val CristalSobreHero = Color(0xFF151B27)
val BordeSobreHero = Color(0xFF263044)
val ExitoSobreHero = Color(0xFF4CC15F)
val ErrorSobreHero = Color(0xFFFF7B72)
val FuegoDeRacha = Color(0xFFFFB020)

// Los detalles de Consoly: el oro del desglose de puntos, el anillo naranja del botón de Batallas y el banner violeta.
val Dorado = Color(0xFFF5A524)
val DoradoFondoA = Color(0xFF3B2609)
val DoradoFondoB = Color(0xFF1C1307)
val NaranjaDeBatalla = Color(0xFFFF8A3D)
val FondoDeBatalla = Color(0xFF2B1810)
val BannerAzul = Color(0xFF123A6B)
val BannerVioleta = Color(0xFF5B2FC4)

// El código con colores, como un editor oscuro.
val CodigoNormal = Color(0xFFE6EDF7)
val CodigoPalabra = Color(0xFFC792EA)
val CodigoTexto = Color(0xFFC3E88D)
val CodigoNumero = Color(0xFFF78C6C)
val CodigoComentario = Color(0xFF637085)
val CodigoLlamada = Color(0xFF82AAFF)
val CodigoSigno = Color(0xFF89DDFF)
val CodigoNumeroDeLinea = Color(0xFF5B667A)

// Medallas del ranking.
val Oro = Color(0xFFF5B301)
val Plata = Color(0xFFB4B8C5)
val Bronce = Color(0xFFC77B3B)

// Un lenguaje, una paleta: se ve igual en la portada del curso, en las fichas del Dojo y en los chips.
@Immutable
class PaletaDeLenguaje(val p1: Color, val p2: Color, val p3: Color, val monograma: String)

private val PaletaPython = PaletaDeLenguaje(Color(0xFF2563EB), Color(0xFF0E7490), Color(0xFF67E8F9), "Py")
private val PaletaJava = PaletaDeLenguaje(Color(0xFFF97316), Color(0xFFBE123C), Color(0xFFFDBA74), "Jv")
private val PaletaJavaScript = PaletaDeLenguaje(Color(0xFFEAB308), Color(0xFF0F766E), Color(0xFFFDE047), "JS")
private val PaletaOtro = PaletaDeLenguaje(Color(0xFF7C3AED), Color(0xFF4338CA), Color(0xFFA5B4FC), "</>")

// El lenguaje llega como lo escribió el docente («Python», «java», «JavaScript»…): se compara sin mayúsculas.
fun paletaDe(lenguaje: String): PaletaDeLenguaje {
    val l = lenguaje.trim().lowercase()
    return when {
        l.startsWith("python") -> PaletaPython
        l == "javascript" || l == "js" -> PaletaJavaScript
        l.startsWith("java") -> PaletaJava
        else -> PaletaOtro
    }
}

// Las luces del fondo «aurora» (Efectos.kt): se dibujan muy transparentes sobre el lienzo.
val AuroraAzul = Color(0xFF1E6FD9)
val AuroraVioleta = Color(0xFF7B3FE4)
val AuroraCian = Color(0xFF00B3C7)

// Los tres puntos de la barra de una ventana de editor (cerrar, minimizar, ampliar). Solo decoran.
val VentanaRojo = Color(0xFFFF5F57)
val VentanaAmarillo = Color(0xFFFEBC2E)
val VentanaVerde = Color(0xFF28C840)
