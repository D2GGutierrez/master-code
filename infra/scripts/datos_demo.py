"""Datos de demostración de MASTER CODE: deja la plataforma con vida para mostrársela al profesor.

Crea (y si ya existen, reutiliza) estas cuentas y datos:
  - 1 docente activo, con 2 cursos (Python y Java).
  - Problemas generados con IA para cada curso: casi todos aprobados (los ven los estudiantes) y 2 que quedan
    pendientes en la bandeja de revisión para aprobarlos o descartarlos en vivo.
  - 6 estudiantes con inscripciones y envíos variados (resueltos, a medias, fallidos y sin intentar), para que
    «Mi avance», la lista de inscritos y el estado de cada problema tengan algo que mostrar.
  - Para la demo del celular (C1), tres de esos estudiantes con una sesión del Dojo ya jugada (ejercicios del celular
    acertados y fallados, que dejan puntos y un cinturón), el nivel de dificultad guardado y una racha de práctica.

Es seguro correrlo más de una vez: no duplica cuentas, cursos, inscripciones, envíos ni sesiones del Dojo. Si la IA falló a medias,
volver a correrlo completa lo que falte.

Antes: `docker compose up -d --build` (db, admin-api, user-api y ai-service) y, para que los envíos se califiquen
de verdad, el motor Piston (docs/piston.md). Con el motor simulado se omiten los envíos.
La contraseña de todas las cuentas de demostración la pones tú, con la variable DEMO_CLAVE; no se guarda ni se
imprime. Al menos 8 caracteres, no demasiado común ni parecida al nombre o al correo. En Git Bash, desde la raíz:
    read -s -p "Clave de demo: " DEMO_CLAVE; export DEMO_CLAVE; echo
    python infra/scripts/datos_demo.py

Los intentos del celular se hacen por la API real (sesión del Dojo, respuesta, terminar), así que pasan por la
regla de dificultad y suman puntos de verdad. La racha no tiene todavía una ruta que la actualice (HU49-T2), así que se
escribe directo en la base con `--db-shell` (por defecto, el contenedor `db` de docker compose).

Los envíos «correctos» de los estudiantes no son soluciones reales: son programas que devuelven la salida
esperada de cada caso de prueba, armados a partir de los casos que el docente puede ver. Sirven solo para tener
datos de ejemplo en el avance.
"""

import argparse
import getpass
import os
import shlex
import subprocess
import sys
import time
from pathlib import Path

from humo import Fallo, Respuesta, estado_esperado, exigir, llamar

RAIZ = Path(__file__).resolve().parents[2]

DOCENTE = {"nombre": "Daniela Rojas Vega", "usuario": "docente.demo"}

# tandas: (tema, nivel, cantidad). Su suma es cuántos problemas debe tener el curso; cada corrida genera solo los que faltan.
# pendientes: cuántos (los más recientes) se dejan sin aprobar, para mostrar la bandeja de revisión.
CURSOS = [
    {
        "nombre": "Introducción a la programación con Python",
        "lenguaje": "python",
        "tandas": [
            ("Variables y operaciones aritméticas", "facil", 3),
            ("Condicionales y bucles", "facil", 3),
            ("Listas y cadenas de texto", "medio", 2),
        ],
        "pendientes": 2,
    },
    {
        "nombre": "Estructuras de datos en Java",
        "lenguaje": "java",
        "tandas": [
            ("Arreglos y recorridos", "facil", 2),
            ("Pilas y colas con arreglos", "medio", 2),
        ],
        "pendientes": 0,
    },
]

# Cada intento es «correcto», «parcial» (solo acierta los casos públicos) o «incorrecto». La lista de un curso sigue
# el orden de sus problemas aprobados en los que se escribe código; lo que sobra se ignora y lo que falta queda sin
# abrir. [] = no lo abrió.
ESTUDIANTES = [
    {
        "nombre": "Ana Torres Quispe",
        "usuario": "ana.torres.demo",
        "envios": {
            0: [["correcto"], ["incorrecto", "correcto"], ["parcial", "correcto"], ["correcto"], ["correcto"], ["incorrecto"]],
            1: [["correcto"], ["correcto"], ["incorrecto", "parcial"], []],
        },
    },
    {
        "nombre": "Luis Ramírez Salas",
        "usuario": "luis.ramirez.demo",
        "envios": {0: [["correcto"], ["incorrecto", "parcial"], ["incorrecto"], ["correcto"]]},
    },
    {
        "nombre": "María Castillo Paredes",
        "usuario": "maria.castillo.demo",
        "envios": {
            0: [["parcial"], ["correcto"], ["incorrecto", "incorrecto", "correcto"]],
            1: [["correcto"], ["incorrecto"]],
        },
    },
    {
        "nombre": "Carlos Mendoza Rivas",
        "usuario": "carlos.mendoza.demo",
        "envios": {0: [["incorrecto"], ["incorrecto"], ["parcial"]]},
    },
    {
        "nombre": "Lucía Paredes Núñez",
        "usuario": "lucia.paredes.demo",
        "envios": {
            0: [["correcto"], ["correcto"]],
            1: [["correcto"], ["correcto"], ["correcto"], ["parcial"]],
        },
    },
    {
        "nombre": "Jorge Salas Huamán",
        "usuario": "jorge.salas.demo",
        # Se inscribió pero todavía no abrió nada: se ve como «sin abrir» en todo.
        "envios": {0: []},
    },
]

# Lo que cada estudiante hizo en el celular (C1). respuestas: una letra por ejercicio del Dojo, A = acierto y E = error
# (si el curso tiene menos ejercicios del celular, se detiene antes). racha: (días seguidos actuales, la máxima).
MOVIL = {
    "ana.torres.demo": {"lenguaje": "python", "respuestas": "AAEAAA", "racha": (5, 9)},
    "luis.ramirez.demo": {"lenguaje": "python", "respuestas": "EEA", "racha": (1, 2)},
    "lucia.paredes.demo": {"lenguaje": "python", "respuestas": "AAAEE", "racha": (3, 3)},
}

# Intentos fallidos: un programa que no resuelve nada.
INCORRECTO = {
    "python": 'print("todavia no se como resolverlo")\n',
    "java": 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("todavia no se como resolverlo");\n    }\n}\n',
}


def normalizar(texto: str) -> str:
    return texto.replace("\r\n", "\n").strip()


def literal(texto: str, lenguaje: str) -> str:
    """Cadena entre comillas para Java o Python. Todo lo que no es ASCII imprimible va escapado, para no depender de la codificación."""
    partes = []
    for caracter in texto:
        codigo = ord(caracter)
        if caracter == "\\":
            partes.append("\\\\")
        elif caracter == '"':
            partes.append('\\"')
        elif caracter == "\n":
            partes.append("\\n")
        elif caracter == "\r":
            partes.append("\\r")
        elif caracter == "\t":
            partes.append("\\t")
        elif 32 <= codigo < 127:
            partes.append(caracter)
        elif codigo > 0xFFFF and lenguaje == "python":
            partes.append(f"\\U{codigo:08x}")
        elif codigo > 0xFFFF:
            alto, bajo = divmod(codigo - 0x10000, 0x400)
            partes.append(f"\\u{0xD800 + alto:04x}\\u{0xDC00 + bajo:04x}")
        else:
            partes.append(f"\\u{codigo:04x}")
    return '"' + "".join(partes) + '"'


def programa_que_responde(lenguaje: str, casos: list[dict]) -> str:
    """Un programa que, para cada entrada conocida, imprime su salida esperada. Solo para datos de ejemplo."""
    pares = {normalizar(caso["entrada"]): normalizar(caso["salida_esperada"]) for caso in casos}
    if lenguaje == "java":
        puestas = "".join(f"        respuestas.put({literal(e, 'java')}, {literal(s, 'java')});\n" for e, s in pares.items())
        return (
            "import java.nio.charset.StandardCharsets;\nimport java.util.HashMap;\nimport java.util.Map;\n\n"
            "public class Main {\n    public static void main(String[] args) throws Exception {\n"
            "        Map<String, String> respuestas = new HashMap<>();\n"
            f"{puestas}"
            "        String entrada = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).replace(\"\\r\\n\", \"\\n\").strip();\n"
            '        System.out.write((respuestas.getOrDefault(entrada, "") + "\\n").getBytes(StandardCharsets.UTF_8));\n'
            "        System.out.flush();\n    }\n}\n"
        )
    entradas = ",\n".join(f"    {literal(e, 'python')}: {literal(s, 'python')}" for e, s in pares.items())
    return (
        "import sys\n\n"
        f"RESPUESTAS = {{\n{entradas},\n}}\n"
        'entrada = sys.stdin.buffer.read().decode("utf-8").replace("\\r\\n", "\\n").strip()\n'
        'sys.stdout.buffer.write((RESPUESTAS.get(entrada, "") + "\\n").encode("utf-8"))\n'
    )


class Demo:
    def __init__(self, argumentos, clave: str):
        self.admin = argumentos.admin_api.rstrip("/")
        self.usuario = argumentos.user_api.rstrip("/")
        self.ia = argumentos.ai_service.rstrip("/")
        self.django_shell = shlex.split(argumentos.django_shell)
        self.dominio = argumentos.dominio
        self.db_shell = shlex.split(argumentos.db_shell)
        self.clave = clave
        self.fallos: list[str] = []
        self.token_docente = ""
        self.cursos: list[dict] = []  # los de CURSOS, ya con id, código y problemas aprobados
        self.problemas_docente: dict[int, dict] = {}  # id -> detalle que ve el docente (con casos ocultos)
        self.motor_real = False

    # ---------- utilidades ----------

    def paso(self, nombre: str, funcion):
        try:
            nota = funcion()
        except Fallo as error:
            print(f"MAL   {nombre}: {error}")
            self.fallos.append(nombre)
            return None
        print(f"OK    {nombre}" + (f" ({nota})" if nota else ""))
        return True

    def django(self, codigo: str) -> str:
        try:
            resultado = subprocess.run(
                [*self.django_shell, codigo], cwd=RAIZ, capture_output=True, text=True, timeout=120
            )
        except (OSError, subprocess.TimeoutExpired) as error:
            raise Fallo(f"no se pudo ejecutar el shell de Django ({error}); revisa --django-shell") from None
        lineas = [linea for linea in resultado.stdout.splitlines() if linea.strip()]
        if resultado.returncode != 0 or not lineas:
            raise Fallo(f"el shell de Django falló: {(resultado.stderr or resultado.stdout).strip()[-200:]}")
        return lineas[-1].strip()

    def sql(self, consulta: str) -> str:
        """Corre SQL en la base (por stdin) y devuelve la última línea de la salida."""
        try:
            resultado = subprocess.run(
                self.db_shell, cwd=RAIZ, input=consulta, capture_output=True, text=True, timeout=60
            )
        except (OSError, subprocess.TimeoutExpired) as error:
            raise Fallo(f"no se pudo ejecutar psql ({error}); revisa --db-shell") from None
        if resultado.returncode != 0:
            raise Fallo(f"la base respondió con error: {(resultado.stderr or resultado.stdout).strip()[-200:]}")
        lineas = [linea for linea in resultado.stdout.splitlines() if linea.strip()]
        return lineas[-1].strip() if lineas else ""

    def correo(self, usuario: str) -> str:
        return f"{usuario}@{self.dominio}"

    def login(self, correo: str, cliente: str) -> Respuesta:
        return llamar("POST", f"{self.admin}/api/auth/login/", {"email": correo, "password": self.clave, "cliente": cliente})

    def asegurar_cuenta(self, rol: str, nombre: str, correo: str, cliente: str) -> str:
        """Devuelve el access token de la cuenta; la crea (y activa, si es docente) cuando hace falta."""
        respuesta = self.login(correo, cliente)
        if respuesta.estado == 401:
            registro = llamar(
                "POST", f"{self.admin}/api/auth/registro/{rol}/", {"nombre": nombre, "email": correo, "password": self.clave}
            )
            if registro.estado == 400 and "Ya existe" in registro.texto:
                raise Fallo(
                    f"{correo} ya existe con otra contraseña: usa la misma DEMO_CLAVE de la primera vez "
                    "(o borra esa cuenta desde el administrador de Django)"
                )
            estado_esperado(registro, 201, f"El registro de {correo}")
            respuesta = self.login(correo, cliente)
        if respuesta.estado == 403 and rol == "docente":
            codigo = (
                "from django.contrib.auth.models import User; "
                f"print(User.objects.filter(username={correo!r}).update(is_active=True))"
            )
            exigir(self.django(codigo) == "1", f"no se encontró la cuenta {correo} para activarla")
            respuesta = self.login(correo, cliente)
        estado_esperado(respuesta, 200, f"El inicio de sesión de {correo}")
        exigir(isinstance(respuesta.json, dict) and respuesta.json.get("access"), "la respuesta no trae el access token")
        return respuesta.json["access"]

    # ---------- verificación previa ----------

    def servicios(self):
        for nombre, url in (("admin-api", self.admin), ("user-api", self.usuario), ("ai-service", self.ia)):
            estado_esperado(llamar("GET", f"{url}/health"), 200, f"/health de {nombre}")

    def motor(self):
        respuesta = llamar("POST", f"{self.ia}/execute", {"codigo": "print('probe-' + '42')", "lenguaje": "python", "entrada": ""})
        estado_esperado(respuesta, 200, "POST /execute de ai-service")
        resultado = respuesta.json or {}
        exigir(resultado.get("estado") != "error_servicio", "el motor de ejecución falló: revisa los logs de ai-service y docs/piston.md")
        self.motor_real = str(resultado.get("salida", "")).strip() == "probe-42"
        return "real" if self.motor_real else "simulado: se omitirán los envíos"

    # ---------- docente ----------

    def sesion_docente(self):
        self.token_docente = self.asegurar_cuenta("docente", DOCENTE["nombre"], self.correo(DOCENTE["usuario"]), "admin-web")
        return self.correo(DOCENTE["usuario"])

    def listar_cursos(self) -> list[dict]:
        respuesta = llamar("GET", f"{self.admin}/api/cursos/", token=self.token_docente)
        estado_esperado(respuesta, 200, "La lista de cursos del docente")
        return respuesta.json or []

    def asegurar_curso(self, plan: dict) -> dict:
        existente = next((c for c in self.listar_cursos() if c["nombre"] == plan["nombre"]), None)
        if existente:
            return existente
        respuesta = llamar(
            "POST", f"{self.admin}/api/cursos/", {"nombre": plan["nombre"], "lenguaje": plan["lenguaje"]}, self.token_docente
        )
        estado_esperado(respuesta, 201, f"Crear el curso «{plan['nombre']}»")
        return {**respuesta.json, "cantidad_problemas": 0}

    def generar_tanda(self, curso_id: int, tema: str, nivel: str, cantidad: int) -> int:
        """Pide a la IA; reintenta si el servicio de IA está saturado o caído (Gemini responde 503 a ratos)."""
        for intento, espera in enumerate((8, 20, None), start=1):
            try:
                respuesta = llamar(
                    "POST",
                    f"{self.admin}/api/cursos/{curso_id}/problemas/generar/",
                    {"tema": tema, "nivel": nivel, "cantidad": cantidad},
                    self.token_docente,
                )
            except Fallo:
                respuesta = None
            if respuesta is not None and respuesta.estado == 201:
                return len((respuesta.json or {}).get("problemas", []))
            reintentable = respuesta is None or respuesta.estado in (502, 503, 504)
            if not reintentable or espera is None:
                detalle = ""
                if respuesta is not None:
                    if isinstance(respuesta.json, dict) and respuesta.json.get("detail"):
                        detalle = f": {respuesta.json['detail']}"
                    else:
                        detalle = f": {respuesta.texto[:150]}"
                estado = "sin respuesta" if respuesta is None else f"HTTP {respuesta.estado}"
                sugerencia = ". Vuelve a correr el script en unos minutos: completa lo que falte" if reintentable else ""
                raise Fallo(f"la IA no generó «{tema}» ({estado}{detalle}){sugerencia}")
            print(f"      la IA no respondió (intento {intento}); reintento en {espera} s…")
            time.sleep(espera)
        return 0

    def preparar_curso(self, plan: dict):
        curso = self.asegurar_curso(plan)
        objetivo = sum(cantidad for _, _, cantidad in plan["tandas"])
        existentes = curso.get("cantidad_problemas", 0)
        generados = 0
        # Cada tanda se salta si ya hay problemas de sobra para cubrirla; así una segunda corrida no repite nada.
        cubiertos = 0
        for tema, nivel, cantidad in plan["tandas"]:
            cubiertos += cantidad
            if existentes >= cubiertos:
                continue
            falta = min(cantidad, cubiertos - existentes)
            generados += self.generar_tanda(curso["id"], tema, nivel, falta)
            existentes += falta
        plan["curso"] = curso
        plan["generados"] = generados
        return f"{curso['codigo_invitacion']}; {generados} problemas nuevos de {objetivo}"

    def aprobar_los_que_toca(self, plan: dict):
        curso_id = plan["curso"]["id"]
        respuesta = llamar("GET", f"{self.admin}/api/problemas/pendientes/", token=self.token_docente)
        estado_esperado(respuesta, 200, "La bandeja de pendientes")
        del_curso = sorted((p for p in respuesta.json or [] if p["curso"] == curso_id), key=lambda p: p["id"])
        a_aprobar = del_curso[: max(0, len(del_curso) - plan["pendientes"])]
        sin_aprobar = []
        for problema in a_aprobar:
            aprobado = llamar("POST", f"{self.admin}/api/problemas/{problema['id']}/aprobar/", token=self.token_docente)
            if aprobado.estado != 200:
                sin_aprobar.append(problema["titulo"])
        if sin_aprobar:
            raise Fallo(f"no se pudieron aprobar (quedan en la bandeja): {', '.join(sin_aprobar)}")
        return f"{len(a_aprobar)} aprobados, {len(del_curso) - len(a_aprobar)} pendientes en la bandeja"

    # ---------- estudiantes ----------

    def detalle_docente(self, problema_id: int) -> dict:
        if problema_id not in self.problemas_docente:
            respuesta = llamar("GET", f"{self.admin}/api/problemas/{problema_id}/", token=self.token_docente)
            estado_esperado(respuesta, 200, f"El problema {problema_id} (vista del docente)")
            self.problemas_docente[problema_id] = respuesta.json or {}
        return self.problemas_docente[problema_id]

    def es_de_codigo(self, problema_id: int) -> bool:
        """La IA también genera problemas de predecir la salida, opción múltiple o completar: esos no reciben envíos de código."""
        problema = self.detalle_docente(problema_id)
        return problema.get("tipo_ejercicio") == "escritura_codigo" and bool(problema.get("casos_prueba"))

    def codigo_para(self, intento: str, problema_id: int) -> str:
        problema = self.detalle_docente(problema_id)
        casos = problema["casos_prueba"]
        lenguaje = problema.get("lenguaje", "python")
        if intento == "incorrecto":
            return INCORRECTO.get(lenguaje, INCORRECTO["python"])
        if intento == "parcial":
            publicos = [caso for caso in casos if caso.get("es_publico")]
            return programa_que_responde(lenguaje, publicos or casos[:1])
        return programa_que_responde(lenguaje, casos)

    def problemas_del_estudiante(self, curso_id: int, token: str) -> list[dict]:
        respuesta = llamar("GET", f"{self.usuario}/api/cursos/{curso_id}/problemas", token=token)
        estado_esperado(respuesta, 200, "El listado de problemas del estudiante")
        return sorted(respuesta.json or [], key=lambda p: p["id"])

    def preparar_estudiante(self, perfil: dict):
        correo = self.correo(perfil["usuario"])
        token = self.asegurar_cuenta("estudiante", perfil["nombre"], correo, "user-web")
        inscritos = enviados = omitidos = 0
        for indice_curso, intentos_por_problema in perfil["envios"].items():
            plan = CURSOS[indice_curso]
            curso = plan["curso"]
            respuesta = llamar("POST", f"{self.usuario}/api/inscripciones", {"codigo": curso["codigo_invitacion"]}, token)
            if respuesta.estado == 201:
                inscritos += 1
            elif respuesta.estado != 409:
                estado_esperado(respuesta, 201, f"La inscripción de {correo} en «{curso['nombre']}»")
            if not self.motor_real:
                continue
            # Los perfiles se aplican solo a los problemas en los que se puede enviar código, en el orden del curso.
            problemas = [p for p in self.problemas_del_estudiante(curso["id"], token) if self.es_de_codigo(p["id"])]
            for problema, intentos in zip(problemas, intentos_por_problema):
                if not intentos or problema["estado"] != "sin_abrir":
                    omitidos += bool(intentos)
                    continue
                for intento in intentos:
                    codigo = self.codigo_para(intento, problema["id"])
                    envio = llamar("POST", f"{self.usuario}/api/problemas/{problema['id']}/envios", {"codigo": codigo}, token)
                    estado_esperado(envio, 201, f"Un envío de {correo}")
                    enviados += 1
        return f"{inscritos} inscripciones nuevas, {enviados} envíos nuevos" + (f", {omitidos} problemas ya tenían intentos" if omitidos else "")

    # ---------- celular (C1) ----------

    def respuesta_para(self, problema_id: int, acierta: bool) -> dict:
        """El cuerpo de POST /api/problemas/{id}/respuesta; la respuesta esperada se lee con la vista del docente."""
        problema = self.detalle_docente(problema_id)
        if problema.get("tipo_ejercicio") == "opcion_multiple":
            opcion = next((o for o in problema.get("opciones", []) if bool(o.get("es_correcta")) == acierta), None)
            exigir(opcion is not None, f"el problema {problema_id} no tiene una alternativa para {'acertar' if acierta else 'fallar'}")
            return {"opcion_id": opcion["id"]}
        return {"respuesta": problema.get("respuesta_esperada", "") if acierta else "no lo sé"}

    def preparar_celular(self, perfil: dict):
        movil = MOVIL[perfil["usuario"]]
        correo = self.correo(perfil["usuario"])
        literal_correo = "'" + correo.replace("'", "''") + "'"
        ya = self.sql(
            "SELECT count(*) FROM app_schema.app_sesion_dojo s JOIN app_schema.app_estudiante e ON e.id = s.estudiante_id "
            f"WHERE e.email = {literal_correo};"
        )
        if ya not in ("", "0"):
            return "ya tenía sesiones del Dojo: no se repite"
        token = self.asegurar_cuenta("estudiante", perfil["nombre"], correo, "mobile")
        sesion = llamar("POST", f"{self.usuario}/api/dojo/sesiones", {"lenguaje": movil["lenguaje"]}, token)
        estado_esperado(sesion, 201, f"Empezar la sesión del Dojo de {correo}")
        sesion_id = sesion.json["id"]
        respondidos = 0
        for letra in movil["respuestas"]:
            siguiente = llamar("POST", f"{self.usuario}/api/dojo/sesiones/{sesion_id}/siguiente", token=token)
            if siguiente.estado == 204:
                break
            estado_esperado(siguiente, 200, "El siguiente ejercicio del Dojo")
            problema_id = siguiente.json["id"]
            cuerpo = {**self.respuesta_para(problema_id, letra == "A"), "sesion_id": sesion_id}
            respuesta = llamar("POST", f"{self.usuario}/api/problemas/{problema_id}/respuesta", cuerpo, token)
            estado_esperado(respuesta, 200, f"La respuesta de {correo} al ejercicio {problema_id}")
            respondidos += 1
        resumen = llamar("POST", f"{self.usuario}/api/dojo/sesiones/{sesion_id}/terminar", token=token)
        estado_esperado(resumen, 200, "Terminar la sesión del Dojo")
        datos = resumen.json or {}
        actual, maxima = movil["racha"]
        self.sql(
            "INSERT INTO app_schema.app_racha (estudiante_id, lenguaje, racha_actual, racha_maxima, fecha_ultima_actividad) "
            f"SELECT id, '{movil['lenguaje']}', {int(actual)}, {int(maxima)}, CURRENT_DATE "
            f"FROM app_schema.app_estudiante WHERE email = {literal_correo} "
            "ON CONFLICT (estudiante_id, lenguaje) DO NOTHING;"
        )
        if respondidos == 0:
            return "el curso no tiene ejercicios del celular visibles: solo se guardó la racha"
        return (
            f"{respondidos} ejercicios, {datos.get('aciertos')} aciertos, {datos.get('puntos_sumados')} puntos, "
            f"nivel {datos.get('nivel_alcanzado')}, racha {actual}"
        )

    # ---------- recorrido ----------

    def correr(self) -> int:
        print("MASTER CODE: datos de demostración")
        try:
            self.servicios()
        except Fallo as error:
            print(f"MAL   Los tres servicios responden: {error}")
            print("\nLevanta todo con `docker compose up -d --build` y espera a que arranque (docker compose ps).")
            return 1
        print("OK    Los tres servicios responden")
        self.paso("Motor de ejecución", self.motor)
        if not self.dominio:
            self.dominio = self.django("from django.conf import settings; print(settings.DOMINIO_INSTITUCIONAL)")
        print(f"      correos con el dominio {self.dominio}")

        if not self.paso("Docente: cuenta activa e inicio de sesión", self.sesion_docente):
            return 1
        for plan in CURSOS:
            if not self.paso(f"Curso «{plan['nombre']}»: problemas con IA", lambda plan=plan: self.preparar_curso(plan)):
                plan["curso"] = plan.get("curso") or next(
                    (c for c in self.listar_cursos() if c["nombre"] == plan["nombre"]), None
                )
                if plan["curso"] is None:
                    print("      sin curso no hay nada más que preparar para él")
                    return 1
            self.paso(f"Curso «{plan['nombre']}»: aprobación", lambda plan=plan: self.aprobar_los_que_toca(plan))
        for perfil in ESTUDIANTES:
            self.paso(f"Estudiante {perfil['nombre']}", lambda perfil=perfil: self.preparar_estudiante(perfil))
            if perfil["usuario"] in MOVIL:
                self.paso(
                    f"Celular de {perfil['nombre']}: Dojo, cinturón y racha", lambda perfil=perfil: self.preparar_celular(perfil)
                )

        print()
        print("Cuentas (todas con la clave que pusiste en DEMO_CLAVE):")
        print(f"  Docente     {self.correo(DOCENTE['usuario'])}   (admin-web, http://localhost:3000/login)")
        for perfil in ESTUDIANTES:
            print(f"  Estudiante  {self.correo(perfil['usuario'])}   (user-web, http://localhost:3001/login)")
        print("Cursos y códigos de invitación:")
        for plan in CURSOS:
            curso = plan.get("curso")
            if curso:
                print(f"  {curso['codigo_invitacion']}  {curso['nombre']}")
        if not self.motor_real:
            print("\nAviso: el motor de ejecución es simulado y no se enviaron soluciones. Activa Piston (docs/piston.md) y vuelve a correr el script.")
        if self.fallos:
            print(f"\n{len(self.fallos)} pasos fallaron: {'; '.join(self.fallos)}. Corrígelo y vuelve a correr el script: completa lo que falte.")
            return 1
        print("\nListo. Puedes volver a correr este script cuando quieras: no duplica nada.")
        return 0


def leer_clave() -> str:
    clave = os.environ.get("DEMO_CLAVE", "")
    if not clave and sys.stdin.isatty():
        clave = getpass.getpass("Clave para las cuentas de demostración: ")
    if len(clave) < 8:
        raise Fallo(
            "falta la clave de las cuentas de demostración (mínimo 8 caracteres). En Git Bash:\n"
            '    read -s -p "Clave de demo: " DEMO_CLAVE; export DEMO_CLAVE; echo\n'
            "    python infra/scripts/datos_demo.py"
        )
    return clave


def main() -> int:
    parser = argparse.ArgumentParser(description="Carga datos de demostración en MASTER CODE (seguro de repetir).")
    parser.add_argument("--admin-api", default="http://localhost:8000")
    parser.add_argument("--user-api", default="http://localhost:8081")
    parser.add_argument("--ai-service", default="http://localhost:8002")
    parser.add_argument("--dominio", default="", help="dominio institucional; por defecto se lee de admin-api")
    parser.add_argument(
        "--django-shell",
        default="docker compose exec -T admin-api python manage.py shell -v 0 -c",
        help="comando que ejecuta el código Python que se le pasa al final, dentro de admin-api (activa al docente)",
    )
    parser.add_argument(
        "--db-shell",
        default="docker compose exec -T db sh -c 'psql -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\" -v ON_ERROR_STOP=1 -tA'",
        help="comando que ejecuta por stdin el SQL que se le pase, dentro de la base (guarda la racha del celular)",
    )
    argumentos = parser.parse_args()
    try:
        return Demo(argumentos, leer_clave()).correr()
    except Fallo as error:
        print(f"MAL   {error}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
