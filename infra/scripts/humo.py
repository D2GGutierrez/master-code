"""Prueba de humo de MASTER CODE: recorre de punta a punta lo que se va a mostrar al profesor.

Docente: se registra, se activa, inicia sesión, crea un curso y un problema, genera problemas con IA y aprueba uno.
Estudiante: se registra, se inscribe con el código, ve los problemas, ejecuta código y envía su solución tres veces
(incorrecta, parcial, correcta) viendo cómo cambia el estado del problema. Usa solo la biblioteca estándar de Python.

Antes: `docker compose up -d` (db, admin-api, user-api y ai-service). Sin motor de ejecución real (SANDBOX_PROVIDER=mock)
todo el flujo corre, pero los envíos no se evalúan de verdad: el aviso lo dice y se omite el envío «parcial».
Uso, desde la raíz del repositorio:
    python infra/scripts/humo.py
    python infra/scripts/humo.py --sin-ia          # si no hay modelo de IA configurado
Cada paso imprime OK o MAL; si uno falla, el resto que depende de él se omite y el código de salida es 1.
Deja datos en la base de desarrollo (cuentas con «humo» en el correo y un curso): sirven de ejemplo y no molestan.
Las contraseñas se generan en cada corrida y no se imprimen.
"""

import argparse
import json
import secrets
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
# Un caso oculto cuya entrada no debe aparecer jamás en nada que reciba el estudiante (HU39).
SECRETO = "SECRETO-OCULTO-731"


class Fallo(Exception):
    """Un paso salió distinto de lo esperado; el mensaje dice qué."""


class Respuesta:
    def __init__(self, estado: int, texto: str):
        self.estado = estado
        self.texto = texto
        try:
            self.json = json.loads(texto)
        except ValueError:
            self.json = None


def llamar(metodo: str, url: str, cuerpo: dict | None = None, token: str | None = None) -> Respuesta:
    cabeceras = {"Content-Type": "application/json"}
    if token:
        cabeceras["Authorization"] = f"Bearer {token}"
    datos = None if cuerpo is None else json.dumps(cuerpo).encode()
    peticion = urllib.request.Request(url, data=datos, headers=cabeceras, method=metodo)
    try:
        with urllib.request.urlopen(peticion, timeout=120) as respuesta:
            return Respuesta(respuesta.status, respuesta.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as error:
        return Respuesta(error.code, error.read().decode("utf-8", "replace"))
    except (urllib.error.URLError, TimeoutError) as error:
        raise Fallo(f"no se pudo conectar con {url}: {getattr(error, 'reason', error)}") from None


def exigir(condicion: bool, mensaje: str) -> None:
    if not condicion:
        raise Fallo(mensaje)


def estado_esperado(respuesta: Respuesta, esperado: int, que: str) -> None:
    if respuesta.estado != esperado:
        detalle = ""
        if isinstance(respuesta.json, dict) and respuesta.json.get("detail"):
            detalle = f": {respuesta.json['detail']}"
        raise Fallo(f"{que} respondió HTTP {respuesta.estado} (esperaba {esperado}){detalle}")


class Humo:
    def __init__(self, argumentos):
        self.admin = argumentos.admin_api.rstrip("/")
        self.usuario = argumentos.user_api.rstrip("/")
        self.ia = argumentos.ai_service.rstrip("/")
        self.django_shell = shlex.split(argumentos.django_shell)
        self.sin_ia = argumentos.sin_ia
        self.dominio = argumentos.dominio
        self.sello = f"{int(time.time())}-{secrets.token_hex(2)}"
        self.clave_docente = "Humo-" + secrets.token_urlsafe(10)
        self.clave_estudiante = "Humo-" + secrets.token_urlsafe(10)
        self.datos: dict = {}
        self.pasos = 0
        self.fallos: list[str] = []
        self.omitidos: list[str] = []

    # ---------- infraestructura de pasos ----------

    def paso(self, nombre: str, funcion, *, necesita: tuple[str, ...] = ()) -> bool:
        faltan = [clave for clave in necesita if clave not in self.datos]
        if faltan:
            print(f"OMIT  {nombre} (depende de un paso que falló)")
            self.omitidos.append(nombre)
            return False
        self.pasos += 1
        try:
            nota = funcion()
        except Fallo as error:
            print(f"MAL   {nombre}: {error}")
            self.fallos.append(nombre)
            return False
        print(f"OK    {nombre}" + (f" ({nota})" if nota else ""))
        return True

    def django(self, codigo: str) -> str:
        try:
            resultado = subprocess.run(
                [*self.django_shell, codigo], cwd=RAIZ, capture_output=True, text=True, timeout=120, check=False
            )
        except (OSError, subprocess.TimeoutExpired) as error:
            raise Fallo(f"no se pudo ejecutar el shell de Django ({error}); revisa --django-shell") from None
        lineas = [linea for linea in resultado.stdout.splitlines() if linea.strip()]
        if resultado.returncode != 0 or not lineas:
            raise Fallo(f"el shell de Django falló: {(resultado.stderr or resultado.stdout).strip()[-200:]}")
        return lineas[-1].strip()

    def correo(self, rol: str) -> str:
        return f"{rol}.humo.{self.sello}@{self.dominio}"

    def iniciar_sesion(self, rol: str, cliente: str) -> str:
        clave = self.clave_docente if rol == "docente" else self.clave_estudiante
        respuesta = llamar("POST", f"{self.admin}/api/auth/login/", {"email": self.correo(rol), "password": clave, "cliente": cliente})
        estado_esperado(respuesta, 200, f"El inicio de sesión del {rol}")
        exigir(isinstance(respuesta.json, dict) and respuesta.json.get("access"), "la respuesta no trae el access token")
        return respuesta.json["access"]

    # ---------- docente ----------

    def servicios(self):
        for nombre, url in (("admin-api", self.admin), ("user-api", self.usuario), ("ai-service", self.ia)):
            estado_esperado(llamar("GET", f"{url}/health"), 200, f"/health de {nombre}")

    def motor(self):
        # En un motor real este programa imprime «probe-42»; el simulado no ejecuta nada y devuelve la entrada (vacía).
        respuesta = llamar("POST", f"{self.ia}/execute", {"codigo": "print('probe-' + '42')", "lenguaje": "python", "entrada": ""})
        estado_esperado(respuesta, 200, "POST /execute de ai-service")
        resultado = respuesta.json or {}
        exigir(
            resultado.get("estado") != "error_servicio",
            "el motor de ejecución falló: revisa que tenga Python instalado (docs/piston.md) y los logs de ai-service",
        )
        self.datos["motor_real"] = str(resultado.get("salida", "")).strip() == "probe-42"
        return "real: ejecuta el código" if self.datos["motor_real"] else "simulado: los envíos no se evalúan de verdad"

    def registrar_docente(self):
        respuesta = llamar(
            "POST",
            f"{self.admin}/api/auth/registro/docente/",
            {"nombre": "Docente de humo", "email": self.correo("docente"), "password": self.clave_docente},
        )
        estado_esperado(respuesta, 201, "El registro del docente")
        self.datos["docente_registrado"] = True

    def docente_pendiente(self):
        respuesta = llamar(
            "POST",
            f"{self.admin}/api/auth/login/",
            {"email": self.correo("docente"), "password": self.clave_docente, "cliente": "admin-web"},
        )
        exigir(respuesta.estado == 403, f"un docente sin activar debe recibir 403 y recibió HTTP {respuesta.estado}")

    def activar_docente(self):
        correo = self.correo("docente")
        codigo = (
            "from django.contrib.auth.models import User; "
            f"print(User.objects.filter(username={correo!r}).update(is_active=True))"
        )
        exigir(self.django(codigo) == "1", "no se encontró la cuenta del docente para activarla")
        self.datos["docente_activo"] = True

    def sesion_docente(self):
        self.datos["token_docente"] = self.iniciar_sesion("docente", "admin-web")

    def crear_curso(self):
        respuesta = llamar(
            "POST", f"{self.admin}/api/cursos/", {"nombre": f"Curso de humo {self.sello}", "lenguaje": "python"}, self.datos["token_docente"]
        )
        estado_esperado(respuesta, 201, "Crear el curso")
        curso = respuesta.json or {}
        exigir(len(str(curso.get("codigo_invitacion", ""))) == 8, "el curso no trae un código de invitación de 8 caracteres")
        exigir(bool(curso.get("codigo_vence")), "el curso no trae la fecha en que vence el código")
        self.datos["curso"] = curso["id"]
        self.datos["codigo"] = curso["codigo_invitacion"]
        self.datos["nombre_curso"] = curso["nombre"]

    def crear_problema(self):
        # Eco: el programa correcto es `print(input())`. Sirve igual con el motor real y con el simulado.
        problema = {
            "titulo": "Eco",
            "enunciado": "Imprime exactamente lo que lees.",
            "lenguaje": "python",
            "dificultad": "facil",
            "plataforma": "web",
            "tipo_ejercicio": "escritura_codigo",
            "casos_prueba": [
                {"entrada": "hola", "salida_esperada": "hola", "es_publico": True},
                {"entrada": SECRETO, "salida_esperada": SECRETO, "es_publico": False},
            ],
        }
        respuesta = llamar("POST", f"{self.admin}/api/cursos/{self.datos['curso']}/problemas/", problema, self.datos["token_docente"])
        estado_esperado(respuesta, 201, "Crear el problema")
        creado = respuesta.json or {}
        exigir(creado.get("aprobado_por_docente") is True, "un problema manual completo debe nacer aprobado")
        self.datos["eco"] = creado["id"]

    def generar_con_ia(self):
        respuesta = llamar(
            "POST",
            f"{self.admin}/api/cursos/{self.datos['curso']}/problemas/generar/",
            {"tema": "bucles", "nivel": "facil", "cantidad": 2, "plataforma": "web"},
            self.datos["token_docente"],
        )
        estado_esperado(respuesta, 201, "Generar problemas con IA")
        generados = (respuesta.json or {}).get("problemas", [])
        exigir(len(generados) == 2, f"pidió 2 problemas y recibió {len(generados)}")
        exigir(
            all(p.get("generado_por_ia") and not p.get("aprobado_por_docente") for p in generados),
            "lo que genera la IA debe quedar pendiente de aprobación",
        )
        self.datos["generados"] = [p["id"] for p in generados]
        return "quedan pendientes de revisión"

    def pendientes(self):
        respuesta = llamar(
            "GET", f"{self.admin}/api/problemas/pendientes/?curso={self.datos['curso']}", token=self.datos["token_docente"]
        )
        estado_esperado(respuesta, 200, "La bandeja de pendientes")
        ids = {p["id"] for p in respuesta.json or []}
        exigir(set(self.datos["generados"]) <= ids, "los problemas generados no aparecen en pendientes")

    def aprobar_generado(self):
        elegido = self.datos["generados"][0]
        respuesta = llamar("POST", f"{self.admin}/api/problemas/{elegido}/aprobar/", token=self.datos["token_docente"])
        estado_esperado(respuesta, 200, "Aprobar el problema")
        exigir((respuesta.json or {}).get("aprobado_por_docente") is True, "el problema no quedó aprobado")
        self.datos["aprobado_ia"] = elegido
        self.datos["pendiente_ia"] = self.datos["generados"][1]

    # ---------- estudiante ----------

    def registrar_estudiante(self):
        respuesta = llamar(
            "POST",
            f"{self.admin}/api/auth/registro/estudiante/",
            {"nombre": "Estudiante de humo", "email": self.correo("estudiante"), "password": self.clave_estudiante},
        )
        estado_esperado(respuesta, 201, "El registro del estudiante")
        self.datos["estudiante_registrado"] = True

    def sesion_estudiante(self):
        self.datos["token"] = self.iniciar_sesion("estudiante", "user-web")
        respuesta = llamar("GET", f"{self.usuario}/api/yo", token=self.datos["token"])
        estado_esperado(respuesta, 200, "GET /api/yo de user-api (valida el token del estudiante)")

    def listado(self) -> list:
        respuesta = llamar("GET", f"{self.usuario}/api/cursos/{self.datos['curso']}/problemas?plataforma=web", token=self.datos["token"])
        estado_esperado(respuesta, 200, "El listado de problemas")
        return respuesta.json or []

    def estado_de_eco(self) -> str:
        for problema in self.listado():
            if problema["id"] == self.datos["eco"]:
                return problema["estado"]
        raise Fallo("el problema Eco no aparece en el listado del estudiante")

    def sin_inscripcion(self):
        respuesta = llamar("GET", f"{self.usuario}/api/cursos/{self.datos['curso']}/problemas", token=self.datos["token"])
        exigir(respuesta.estado == 403, f"sin inscripción el listado debe dar 403 y dio HTTP {respuesta.estado}")

    def inscribirse(self):
        respuesta = llamar("POST", f"{self.usuario}/api/inscripciones", {"codigo": self.datos["codigo"]}, self.datos["token"])
        estado_esperado(respuesta, 201, "La inscripción con el código")
        self.datos["inscrito"] = True
        repetida = llamar("POST", f"{self.usuario}/api/inscripciones", {"codigo": self.datos["codigo"]}, self.datos["token"])
        exigir(repetida.estado == 409, f"inscribirse dos veces debe dar 409 y dio HTTP {repetida.estado}")

    def mis_cursos(self):
        respuesta = llamar("GET", f"{self.usuario}/api/mis-cursos", token=self.datos["token"])
        estado_esperado(respuesta, 200, "Mis cursos")
        exigir(any(c["curso_id"] == self.datos["curso"] for c in respuesta.json or []), "el curso no aparece en «Mis cursos»")

    def ver_listado(self):
        problemas = {p["id"]: p for p in self.listado()}
        exigir(self.datos["eco"] in problemas, "el problema Eco no aparece en el listado")
        exigir(problemas[self.datos["eco"]]["estado"] == "sin_abrir", "Eco debe empezar sin abrir")
        if "aprobado_ia" in self.datos:
            exigir(self.datos["aprobado_ia"] in problemas, "el problema de IA aprobado no aparece en el listado")
            exigir(self.datos["pendiente_ia"] not in problemas, "un problema de IA sin aprobar aparece en el listado del estudiante")
        return f"{len(problemas)} problemas visibles"

    def detalle(self):
        respuesta = llamar("GET", f"{self.usuario}/api/problemas/{self.datos['eco']}", token=self.datos["token"])
        estado_esperado(respuesta, 200, "El detalle del problema")
        exigir(len((respuesta.json or {}).get("casos_prueba", [])) == 1, "el estudiante debe ver solo el caso público")
        exigir(SECRETO not in respuesta.texto, "el detalle del problema deja ver la entrada de un caso oculto")

    def no_abre_pendiente(self):
        respuesta = llamar("GET", f"{self.usuario}/api/problemas/{self.datos['pendiente_ia']}", token=self.datos["token"])
        exigir(respuesta.estado == 404, f"un problema sin aprobar debe dar 404 y dio HTTP {respuesta.estado}")

    def ejecutar(self):
        respuesta = llamar(
            "POST", f"{self.usuario}/api/ejecuciones", {"codigo": "print(input())", "lenguaje": "python", "entrada": "hola"}, self.datos["token"]
        )
        estado_esperado(respuesta, 200, "Ejecutar")
        resultado = respuesta.json or {}
        exigir(resultado.get("estado") == "ok", f"la ejecución terminó en estado {resultado.get('estado')!r}")
        exigir(str(resultado.get("salida", "")).strip() == "hola", f"la salida fue {resultado.get('salida')!r}, esperaba 'hola'")
        exigir(self.estado_de_eco() == "sin_abrir", "«Ejecutar» no debe contar como intento")

    def enviar(self, codigo: str, resultado: str, puntaje: float, casos_superados: int) -> dict:
        respuesta = llamar(
            "POST", f"{self.usuario}/api/problemas/{self.datos['eco']}/envios", {"codigo": codigo}, self.datos["token"]
        )
        estado_esperado(respuesta, 201, "Enviar solución")
        envio = respuesta.json or {}
        exigir(envio.get("resultado") == resultado, f"el resultado fue {envio.get('resultado')!r}, esperaba {resultado!r}")
        exigir(float(envio.get("puntaje", -1)) == puntaje, f"el puntaje fue {envio.get('puntaje')}, esperaba {puntaje}")
        exigir(envio.get("casos_superados") == casos_superados and envio.get("casos_total") == 2, "los casos superados no coinciden")
        anterior = self.datos.get("intento", 0)
        exigir(envio.get("numero_intento") == anterior + 1, f"el intento fue {envio.get('numero_intento')}, esperaba {anterior + 1}")
        self.datos["intento"] = anterior + 1
        # El caso oculto solo dice si pasó; nada de su entrada o su salida llega al estudiante (HU39).
        exigir(SECRETO not in respuesta.texto, "la respuesta del envío deja ver el contenido de un caso oculto")
        oculto = [caso for caso in envio.get("casos", []) if not caso.get("publico")]
        exigir(len(oculto) == 1 and set(oculto[0]) == {"publico", "paso"}, "un caso oculto debe traer solo publico y paso")
        return envio

    def enviar_incorrecta(self):
        # El comentario provoca el error simulado; con el motor real el programa simplemente imprime algo equivocado.
        self.enviar('print("mal")  # MOCK_ERROR_EJECUCION', "incorrecto", 0.0, 0)
        exigir(self.estado_de_eco() == "intentado", "tras un envío incorrecto el problema debe quedar «intentado»")

    def enviar_parcial(self):
        # Eco bien para entradas cortas y mal para las largas: solo el caso público sale bien.
        self.enviar('x = input()\nprint(x if len(x) < 8 else "mal")', "parcial", 50.0, 1)
        exigir(self.estado_de_eco() == "intentado", "un envío parcial no resuelve el problema")

    def enviar_correcta(self):
        envio = self.enviar("print(input())", "correcto", 100.0, 2)
        exigir(envio.get("aviso") is None, f"un envío correcto no debe traer aviso y trae {envio.get('aviso')!r}")
        exigir(self.estado_de_eco() == "resuelto", "tras un envío correcto el problema debe quedar «resuelto»")
        return f"{self.datos['intento']} intentos en total"

    def avance(self):
        respuesta = llamar("GET", f"{self.usuario}/api/avance", token=self.datos["token"])
        estado_esperado(respuesta, 200, "El avance del estudiante")

    def inscritos(self):
        respuesta = llamar("GET", f"{self.admin}/api/cursos/{self.datos['curso']}/inscritos/", token=self.datos["token_docente"])
        estado_esperado(respuesta, 200, "La lista de inscritos del docente")
        exigir(any(i["email"] == self.correo("estudiante") for i in respuesta.json or []), "el estudiante no aparece entre los inscritos")

    # ---------- recorrido ----------

    def correr(self) -> int:
        print(f"MASTER CODE: prueba de humo ({self.sello})")
        if not self.paso("Los tres servicios responden", self.servicios):
            print("\nLevanta todo con `docker compose up -d` y espera a que arranque (docker compose ps).")
            return 1
        self.paso("Motor de ejecución", self.motor)
        if not self.dominio:
            self.dominio = self.django("from django.conf import settings; print(settings.DOMINIO_INSTITUCIONAL)")
        print(f"      correos con el dominio {self.dominio}")

        self.paso("Docente: se registra", self.registrar_docente)
        self.paso("Docente: no entra hasta que lo activen", self.docente_pendiente, necesita=("docente_registrado",))
        self.paso("Docente: lo activa el administrador", self.activar_docente, necesita=("docente_registrado",))
        self.paso("Docente: inicia sesión", self.sesion_docente, necesita=("docente_activo",))
        self.paso("Docente: crea un curso con su código de invitación", self.crear_curso, necesita=("token_docente",))
        self.paso("Docente: crea un problema con casos públicos y ocultos", self.crear_problema, necesita=("curso",))
        if self.sin_ia:
            print("OMIT  Docente: genera y aprueba problemas con IA (--sin-ia)")
        else:
            if self.paso("Docente: genera problemas con IA", self.generar_con_ia, necesita=("curso",)):
                self.paso("Docente: los ve en la bandeja de pendientes", self.pendientes, necesita=("generados",))
                self.paso("Docente: aprueba uno", self.aprobar_generado, necesita=("generados",))

        self.paso("Estudiante: se registra", self.registrar_estudiante)
        self.paso("Estudiante: inicia sesión y user-api acepta su token", self.sesion_estudiante, necesita=("estudiante_registrado",))
        self.paso("Estudiante: sin inscripción no ve los problemas", self.sin_inscripcion, necesita=("token", "curso"))
        self.paso("Estudiante: se inscribe con el código", self.inscribirse, necesita=("token", "codigo"))
        self.paso("Estudiante: ve el curso en «Mis cursos»", self.mis_cursos, necesita=("inscrito",))
        self.paso("Estudiante: ve solo los problemas aprobados", self.ver_listado, necesita=("inscrito", "eco"))
        self.paso("Estudiante: abre un problema y no ve los casos ocultos", self.detalle, necesita=("inscrito", "eco"))
        if "pendiente_ia" in self.datos:
            self.paso("Estudiante: no puede abrir un problema sin aprobar", self.no_abre_pendiente, necesita=("inscrito",))
        self.paso("Estudiante: ejecuta código sin gastar un intento", self.ejecutar, necesita=("inscrito", "eco"))
        if self.paso("Estudiante: envía una solución incorrecta", self.enviar_incorrecta, necesita=("inscrito", "eco")):
            if self.datos.get("motor_real"):
                self.paso("Estudiante: envía una solución parcial", self.enviar_parcial, necesita=("inscrito", "eco"))
            else:
                print("OMIT  Estudiante: envía una solución parcial (el motor simulado imprime la entrada y no puede fallar a medias)")
            self.paso("Estudiante: envía la solución correcta y resuelve el problema", self.enviar_correcta, necesita=("inscrito", "eco", "intento"))
        else:
            print("OMIT  Estudiante: envía la solución correcta (depende del envío incorrecto)")
            self.omitidos.append("Estudiante: envía la solución correcta")
        self.paso("Estudiante: ve su avance", self.avance, necesita=("inscrito",))
        self.paso("Docente: ve al estudiante entre los inscritos", self.inscritos, necesita=("token_docente", "inscrito"))

        print()
        if self.datos.get("motor_real") is False:
            print("Aviso: el motor de ejecución es simulado. Para probar la calificación de verdad usa Piston (docs/piston.md).")
        if self.datos.get("nombre_curso"):
            print(f"Quedaron en la base de desarrollo: el curso «{self.datos['nombre_curso']}» y dos cuentas con «humo» en el correo.")
        if self.fallos:
            print(f"\n{len(self.fallos)} de {self.pasos} pasos fallaron: {'; '.join(self.fallos)}.")
            return 1
        if self.omitidos:
            print(f"\nTodo lo que corrió salió bien, pero se omitieron {len(self.omitidos)} pasos.")
            return 1
        print(f"\nLos {self.pasos} pasos salieron bien: el sistema funciona de punta a punta.")
        return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Prueba de humo de punta a punta de MASTER CODE.")
    parser.add_argument("--admin-api", default="http://localhost:8000")
    parser.add_argument("--user-api", default="http://localhost:8081")
    parser.add_argument("--ai-service", default="http://localhost:8002")
    parser.add_argument("--sin-ia", action="store_true", help="omite generar y aprobar problemas con IA")
    parser.add_argument("--dominio", default="", help="dominio institucional; por defecto se lee de admin-api")
    parser.add_argument(
        "--django-shell",
        default="docker compose exec -T admin-api python manage.py shell -v 0 -c",
        help="comando que ejecuta el código Python que se le pasa al final, dentro de admin-api (activa al docente)",
    )
    humo = Humo(parser.parse_args())
    try:
        return humo.correr()
    except Fallo as error:
        print(f"MAL   {error}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
