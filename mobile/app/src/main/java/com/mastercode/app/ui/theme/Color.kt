package com.mastercode.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// El lenguaje visual de GitHub, igual que el diseno.css de la web (docs/diseno-movil.md): lienzo gris claro u oscuro, tarjetas con
// borde fino, texto casi negro, el azul de los enlaces como acento y un botón principal verde. En los dos temas hay una barra y un
// «hero» oscuros con un brillo violeta que anclan cada pantalla.
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
    lienzo = Color(0xFF0D1117),
    fondo = Color(0xFF151B23),
    fondoSuave = Color(0xFF1C232D),
    texto = Color(0xFFF0F6FC),
    textoSuave = Color(0xFF9198A1),
    borde = Color(0xFF3D444D),
    bordeFuerte = Color(0xFF656C76),
    acento = Color(0xFF4493F8),
    acentoSuave = Color(0x264493F8),
    sobreAcento = Color(0xFF0D1117),
    exito = Color(0xFF3FB950),
    error = Color(0xFFF85149),
    resalte = Color(0x33BB8009),
    primario = Color(0xFF238636),
    sobrePrimario = Color(0xFFFFFFFF),
)

// La barra y el hero son oscuros en los dos temas (como en la web), así que sus colores no cambian.
val HeroA = Color(0xFF010409)
val HeroB = Color(0xFF1A1245)
val HeroC = Color(0xFF5B3BB5)
val HeroBrillo = Color(0xFF8957E5)
val Barra = Color(0xFF25292E)
val BarraOscura = Color(0xFF010409)
val SobreHero = Color(0xFFF0F6FC)
val SobreHeroSuave = Color(0xFFB1BAC4)
val CristalSobreHero = Color(0x1AFFFFFF)
val BordeSobreHero = Color(0x26FFFFFF)
val ExitoSobreHero = Color(0xFF3FB950)
val ErrorSobreHero = Color(0xFFFFA198)
val FuegoDeRacha = Color(0xFFFFB020)

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
