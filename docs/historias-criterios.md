# Historias de Usuario — Criterios de Aceptación

> Versión 2 · 7 de octubre de 2026. Usa el número de HU del documento de tareas (la tabla de equivalencias está en la sección 6 de ese documento). Cada criterio indica qué tareas lo cubren.

Etiquetas: *(modificado)* el criterio tenía una regla sin valor concreto y ahora lo tiene; *(agregado)* criterio que ya estaba decidido en el contrato JWT o en el sprint backlog y faltaba en este documento. Los cambios están en «Registro de cambios y decisiones». Los valores marcados como propuesta se confirman en el Planning del sprint correspondiente.

## Sprint 1

- **HU-01 — Registro de docente**: Como docente, quiero registrarme con mi correo institucional, para que solo el personal autorizado pueda acceder al panel.
    
    - CA-01.1 — El sistema debe permitir el registro cuando el correo pertenece al dominio institucional permitido. · Tareas: HU01-T1, HU01-T2, HU01-T3, HU01-T4
        
    - CA-01.2 — El sistema debe rechazar correos que no pertenezcan al dominio institucional permitido. · Tareas: HU01-T2, HU01-T3, HU01-T4
        
    - CA-01.3 — El sistema debe impedir el registro de un correo que ya se encuentre registrado. · Tareas: HU01-T2, HU01-T3, HU01-T4
        
    - CA-01.4 — Tras registrarse, la cuenta queda pendiente de activación: mientras el Product Owner no la active, el inicio de sesión la rechaza con un mensaje claro. *(agregado)* · Tareas: HU01-T2, HU01-T3, HU01-T4
        
- **HU-02 — Inicio de sesión del docente**: Como docente, quiero iniciar sesión y que la sesión se mantenga activa durante la clase.
    
    - CA-02.1 — El sistema debe permitir al docente iniciar sesión utilizando credenciales válidas. · Tareas: HU02-T1, HU02-T2
        
    - CA-02.2 — La sesión debe renovarse automáticamente mientras corresponda. · Tareas: HU02-T2, HU02-T3, HU02-T5
        
    - CA-02.3 — Las solicitudes autenticadas deben utilizar la credencial de sesión correspondiente. · Tareas: HU02-T3
        
    - CA-02.4 — Cuando la sesión venza, el sistema debe dirigir al usuario al inicio de sesión conservando la ruta a la que intentaba acceder. · Tareas: HU02-T4, HU02-T5
        
    - CA-02.5 — Con credenciales incorrectas, el mensaje no revela si falló el correo o la contraseña. *(agregado)* · Tareas: HU02-T2
        
- **HU-03 — Recuperación de contraseña**: Como docente, quiero recuperar mi contraseña mediante un enlace enviado al correo.
    
    - CA-03.1 — El sistema debe permitir solicitar la recuperación de contraseña. · Tareas: HU03-T2, HU03-T3
        
    - CA-03.2 — El sistema debe enviar un enlace de recuperación al correo correspondiente. · Tareas: HU03-T1, HU03-T2
        
    - CA-03.3 — El enlace de recuperación vence a los 30 minutos y deja de ser válido después. *(modificado)* · Tareas: HU03-T2, HU03-T4
        
    - CA-03.4 — Un enlace previamente utilizado no debe permitir un nuevo cambio de contraseña. · Tareas: HU03-T2, HU03-T3, HU03-T4
        
- **HU-04 — Protección de cuentas**: Como administrador, quiero contraseñas cifradas y sesión que caduque por inactividad, para proteger las cuentas en las PC compartidas.
    
    - CA-04.1 — Las contraseñas no deben almacenarse en texto legible. · Tareas: HU04-T1
        
    - CA-04.2 — En la web, 30 minutos sin actividad impiden renovar la sesión y obligan a iniciar sesión de nuevo. *(modificado)* · Tareas: HU04-T2
        
    - CA-04.3 — El usuario puede cerrar su sesión manualmente: el refresh token se invalida al instante y el access token vence solo (15 minutos como máximo). *(modificado)* · Tareas: HU04-T3
        
    - CA-04.4 — Las contraseñas no deben aparecer en los registros del sistema. · Tareas: HU04-T4
        
- **HU-09 — Crear curso**: Como docente, quiero crear un curso indicando nombre y lenguaje de programación.
    
    - CA-09.1 — El sistema debe permitir crear un curso indicando un nombre. · Tareas: HU09-T1, HU09-T2, HU09-T3
        
    - CA-09.2 — El sistema debe permitir seleccionar el lenguaje de programación del curso. · Tareas: HU09-T2, HU09-T3
        
    - CA-09.3 — El sistema no debe guardar un curso sin nombre. · Tareas: HU09-T2, HU09-T4
        
    - CA-09.4 — El sistema no debe guardar un curso sin lenguaje de programación. · Tareas: HU09-T2, HU09-T4
        
- **HU-10 — Editar curso**: Como docente, quiero editar los datos de un curso ya creado.
    
    - CA-10.1 — El docente debe poder modificar los datos de sus propios cursos. · Tareas: HU10-T1, HU10-T2
        
    - CA-10.2 — El sistema debe solicitar confirmación antes de guardar los cambios. · Tareas: HU10-T3
        
    - CA-10.3 — Un docente no debe poder modificar el curso perteneciente a otro docente. · Tareas: HU10-T1, HU10-T4
        
- **HU-11 — Listado de cursos**: Como docente, quiero ver el listado de mis cursos con la cantidad de problemas de cada uno.
    
    - CA-11.1 — El sistema debe mostrar únicamente los cursos correspondientes al docente. · Tareas: HU11-T1, HU11-T2
        
    - CA-11.2 — Cada curso debe mostrar la cantidad de problemas asociados. · Tareas: HU11-T1, HU11-T2
        
    - CA-11.3 — El docente debe poder buscar cursos por nombre. · Tareas: HU11-T4
        
    - CA-11.4 — Cuando no existan cursos, el sistema debe mostrar un mensaje de estado vacío. · Tareas: HU11-T3
        
    - CA-11.5 — El listado debe admitir paginación. · Tareas: HU11-T3
        
- **HU-12 — Protección de eliminación de curso**: Como docente, quiero que el sistema impida eliminar un curso con problemas o inscritos.
    
    - CA-12.1 — El sistema debe permitir eliminar un curso vacío cuando no existan restricciones. · Tareas: HU12-T4
        
    - CA-12.2 — El sistema debe impedir eliminar un curso que contenga problemas. · Tareas: HU12-T1, HU12-T2, HU12-T4
        
    - CA-12.3 — El sistema debe impedir eliminar un curso que tenga estudiantes inscritos. Se cumple desde el Sprint 2 (HU28 T5), cuando existe la inscripción; hasta entonces este criterio está diferido. *(modificado)* · Tareas: HU28-T5
        
    - CA-12.4 — Cuando la eliminación no sea posible, el sistema debe mostrar un mensaje explicando la causa. · Tareas: HU12-T2, HU12-T3
        
- **HU-13 — Crear problema**: Como docente, quiero crear un problema con título, enunciado, lenguaje y dificultad dentro de uno de mis cursos.
    
    - CA-13.1 — El sistema debe permitir crear un problema dentro de un curso del docente. · Tareas: HU13-T2, HU13-T3
        
    - CA-13.2 — El problema debe permitir registrar título, enunciado, lenguaje y dificultad. · Tareas: HU13-T1, HU13-T2, HU13-T3
        
    - CA-13.3 — El sistema no debe guardar un problema que no tenga enunciado. · Tareas: HU13-T2, HU13-T4
        
- **HU-14 — Plataforma y tipo de ejercicio**: Como docente, quiero indicar la plataforma y el tipo de ejercicio de cada problema.
    
    - CA-14.1 — El docente debe poder seleccionar la plataforma del problema. · Tareas: HU14-T1, HU14-T2
        
    - CA-14.2 — El docente debe poder seleccionar el tipo de ejercicio. · Tareas: HU14-T1, HU14-T2
        
    - CA-14.3 — El formulario debe mostrar los campos de respuesta correspondientes al tipo de ejercicio seleccionado. · Tareas: HU14-T3
        
    - CA-14.4 — El sistema debe aceptar únicamente combinaciones de plataforma y tipo consideradas válidas. · Tareas: HU14-T4
        
- **HU-15 — Validación de combinaciones**: Como docente, quiero que el sistema rechace las combinaciones imposibles, como pedir escritura de código en el celular.
    
    - CA-15.1 — El sistema debe validar la relación entre plataforma y tipo de ejercicio. · Tareas: HU15-T1, HU15-T4
        
    - CA-15.2 — El sistema debe rechazar las combinaciones definidas como imposibles. · Tareas: HU15-T1, HU15-T2, HU15-T4
        
    - CA-15.3 — La escritura de código no debe aceptarse cuando la plataforma seleccionada sea únicamente móvil. · Tareas: HU15-T1, HU15-T4
        
    - CA-15.4 — El sistema debe mostrar el error asociado al campo correspondiente. · Tareas: HU15-T3
        
- **HU-16 — Casos de prueba**: Como docente, quiero registrar los casos de prueba de un problema, distinguiendo los públicos de los ocultos.
    
    - CA-16.1 — El docente debe poder agregar y eliminar casos de prueba. · Tareas: HU16-T2
        
    - CA-16.2 — Cada caso de prueba debe poder marcarse como público u oculto. · Tareas: HU16-T1, HU16-T2, HU16-T3
        
    - CA-16.3 — Un problema debe tener como mínimo un caso de prueba. · Tareas: HU16-T3
        
    - CA-16.4 — Los casos ocultos no deben enviarse al estudiante. · Tareas: HU16-T4
        
- **HU-21 — Registro del estudiante**: Como estudiante, quiero registrarme con mi correo institucional.
    
    - CA-21.1 — El sistema debe permitir el registro utilizando un correo institucional válido. · Tareas: HU21-T1, HU21-T2, HU21-T3, HU21-T4
        
    - CA-21.2 — El sistema debe aplicar la validación del dominio institucional. · Tareas: HU21-T2, HU21-T3
        
    - CA-21.3 — El sistema debe impedir registrar un correo previamente utilizado. · Tareas: HU21-T2, HU21-T3, HU21-T4
        
- **HU-22 — Inicio de sesión multiplataforma**: Como estudiante, quiero iniciar sesión con la misma cuenta en la web y en la app móvil.
    
    - CA-22.1 — Una cuenta válida debe permitir iniciar sesión en la plataforma web. · Tareas: HU22-T3, HU22-T5, HU22-T7
        
    - CA-22.2 — La misma cuenta debe permitir iniciar sesión en la aplicación móvil. · Tareas: HU22-T4, HU22-T5
        
    - CA-22.3 — Las credenciales emitidas por el sistema de autenticación deben ser reconocidas por los servicios utilizados por el estudiante. · Tareas: HU22-T1, HU22-T2, HU22-T5, HU22-T6
        
    - CA-22.4 — Una credencial ausente, vencida, alterada o firmada con otra clave es rechazada por user-api con un 401. *(agregado)* · Tareas: HU22-T1, HU22-T5
        
    - CA-22.5 — Un token de docente no abre la API del estudiante (recibe 403). *(agregado)* · Tareas: HU22-T1
        
    - CA-22.6 — La primera petición con un token válido crea el perfil del estudiante en app_schema si todavía no existe. *(agregado)* · Tareas: HU22-T1
        
- **HU-23 — Recuperación de contraseña del estudiante**: Como estudiante, quiero recuperar mi contraseña desde la pantalla de inicio de sesión.
    
    - CA-23.1 — La web debe proporcionar acceso al proceso de recuperación de contraseña. · Tareas: HU23-T2
        
    - CA-23.2 — La aplicación móvil debe proporcionar acceso al proceso de recuperación. · Tareas: HU23-T2
        
    - CA-23.3 — El enlace recibido por correo debe poder abrirse correctamente desde un dispositivo móvil. · Tareas: HU23-T3
        
    - CA-23.4 — El proceso debe permitir completar el cambio de contraseña. · Tareas: HU23-T1, HU23-T4
        
- **HU-24 — Sincronización del progreso**: Como estudiante, quiero que mi perfil y progreso estén asociados a mi cuenta y no al dispositivo.
    
    - CA-24.1 — La actividad realizada debe quedar asociada a la cuenta del estudiante. · Tareas: HU24-T1, HU24-T2
        
    - CA-24.2 — El sistema debe poder identificar si un envío proviene de web o móvil. · Tareas: HU24-T1
        
    - CA-24.3 — El progreso debe poder consultarse de forma agregada. · Tareas: HU24-T2, HU24-T3
        
    - CA-24.4 — Una actividad realizada en móvil debe reflejarse posteriormente en la web. Se prueba con datos sembrados en el Sprint 1 y con un envío real en HU45 T3. *(modificado)* · Tareas: HU24-T3, HU24-T4, HU45-T3
        

## Sprint 2

- **HU-05 — Editor de programación**: Como estudiante, quiero escribir mi solución en un editor con resaltado de sintaxis y numeración de líneas, para programar con comodidad desde el navegador.
    
    - CA-05.1 — El editor debe mostrar numeración de líneas. · Tareas: HU05-T1
        
    - CA-05.2 — Debe proporcionar resaltado de sintaxis para Python, Java y JavaScript. · Tareas: HU05-T2
        
    - CA-05.3 — Debe permitir editar código extenso y caracteres especiales. · Tareas: HU05-T4
        
    - CA-05.4 — El editor debe poder utilizarse en pantallas pequeñas. · Tareas: HU05-T3
        
- **HU-06 — Ejecutar sin enviar**: Como estudiante, quiero ejecutar mi código y ver la salida sin registrarlo como intento oficial, para probar ideas antes de comprometer una calificación.
    
    - CA-06.1 — El estudiante debe poder ejecutar el código desde el editor. · Tareas: HU06-T2
        
    - CA-06.2 — La ejecución de prueba no debe registrarse como intento oficial. · Tareas: HU06-T1
        
    - CA-06.3 — El sistema debe mostrar la salida obtenida. · Tareas: HU06-T1, HU06-T2
        
    - CA-06.4 — Mientras una ejecución esté en proceso, el sistema debe impedir iniciar accidentalmente otra mediante doble clic. · Tareas: HU06-T3
        
    - CA-06.5 — El sistema debe aplicar el límite configurado de ejecuciones consecutivas. · Tareas: HU06-T4
        
- **HU-07 — Sandbox**: Como administrador, quiero que el código de los estudiantes se ejecute en un entorno aislado con límites de tiempo, memoria y sin acceso a la red.
    
    - CA-07.1 — El código debe ejecutarse dentro del entorno aislado definido para el sistema. · Tareas: HU07-T1, HU07-T3, HU07-T4
        
    - CA-07.2 — Las ejecuciones deben respetar el límite de tiempo configurado. · Tareas: HU07-T2, HU07-T5
        
    - CA-07.3 — Las ejecuciones deben respetar el límite de memoria configurado. · Tareas: HU07-T2
        
    - CA-07.4 — El código ejecutado no debe disponer de acceso a la red. · Tareas: HU07-T2
        
    - CA-07.5 — Un programa que entre en un ciclo que exceda el límite permitido debe ser detenido por el sistema. · Tareas: HU07-T5
        
- **HU-08 — Motor configurable**: Como responsable del proyecto, quiero poder cambiar de motor de ejecución por configuración, para no depender de un proveedor de pago.
    
    - CA-08.1 — El motor de ejecución utilizado debe poder seleccionarse mediante configuración. · Tareas: HU08-T3
        
    - CA-08.2 — El cambio de motor no debe requerir modificar el flujo funcional de envío de soluciones. · Tareas: HU08-T1, HU08-T3
        
    - CA-08.3 — El sistema debe disponer de las implementaciones previstas para Judge0 y Piston. · Tareas: HU08-T2, HU08-T3
        
    - CA-08.4 — Debe existir un modo simulado que pueda utilizarse cuando así se configure. · Tareas: HU08-T4
        
- **HU-17 — Generar problemas**: Como docente, quiero generar varios problemas indicando tema, nivel, lenguaje y cantidad, para preparar una práctica nueva rápidamente.
    
    - CA-17.1 — El docente debe poder indicar tema, nivel, lenguaje y cantidad de problemas. · Tareas: HU17-T2, HU17-T4
        
    - CA-17.2 — El sistema debe procesar la solicitud mediante el servicio de generación configurado. · Tareas: HU17-T1, HU17-T3
        
    - CA-17.3 — La interfaz debe indicar que la generación se encuentra en proceso. · Tareas: HU17-T4
        
    - CA-17.4 — La respuesta generada debe validarse antes de almacenarse. · Tareas: HU17-T2, HU17-T5
        
    - CA-17.5 — El sistema debe manejar una respuesta inválida o una indisponibilidad del servicio sin guardar información inválida. · Tareas: HU17-T6
        
- **HU-18 — Bandeja de problemas generados**: Como docente, quiero ver los problemas generados en una bandeja de pendientes, claramente marcados como creados por IA, para revisarlos antes de que lleguen a mis estudiantes.
    
    - CA-18.1 — Los problemas generados mediante IA deben mostrarse en una bandeja de revisión. · Tareas: HU18-T1, HU18-T2
        
    - CA-18.2 — Los problemas generados deben identificarse como creados mediante IA. · Tareas: HU18-T1, HU18-T2
        
    - CA-18.3 — El docente debe poder filtrar la bandeja por curso. · Tareas: HU18-T3
        
    - CA-18.4 — El docente debe poder filtrar por estado de revisión. · Tareas: HU18-T3
        
    - CA-18.5 — El sistema debe mostrar la cantidad de problemas pendientes. · Tareas: HU18-T4
        
    - CA-18.6 — Un problema generado mediante IA no debe publicarse automáticamente. · Tareas: HU18-T4
        
- **HU-19 — Revisar y aprobar problema**: Como docente, quiero editar el enunciado, los casos de prueba o la respuesta esperada antes de aprobar un problema, para corregir lo que la IA haya generado mal.
    
    - CA-19.1 — El docente debe poder modificar el enunciado antes de aprobar el problema. · Tareas: HU19-T1
        
    - CA-19.2 — El docente debe poder modificar los casos de prueba. · Tareas: HU19-T1
        
    - CA-19.3 — El docente debe poder modificar la respuesta esperada cuando corresponda. · Tareas: HU19-T1
        
    - CA-19.4 — La aprobación debe cambiar el problema al estado que permita su publicación. · Tareas: HU19-T2
        
    - CA-19.5 — El sistema debe registrar quién aprobó el problema y la fecha de aprobación. · Tareas: HU19-T3
        
    - CA-19.6 — Un problema incompleto no debe poder aprobarse. · Tareas: HU19-T4
        
- **HU-20 — Descartar problema generado**: Como docente, quiero descartar un problema generado que no me sirve, para que no ocupe espacio en el catálogo de mi curso.
    
    - CA-20.1 — El docente debe poder iniciar el descarte desde la bandeja de revisión. · Tareas: HU20-T3
        
    - CA-20.2 — El sistema debe solicitar confirmación antes de completar el descarte. · Tareas: HU20-T1
        
    - CA-20.3 — El problema descartado debe quedar archivado en lugar de eliminarse definitivamente. · Tareas: HU20-T2
        
    - CA-20.4 — Un problema descartado no debe volver a aparecer entre los problemas pendientes. · Tareas: HU20-T4
        
- **HU-25 — Buscar cursos publicados**: Como estudiante, quiero buscar los cursos publicados de mi institución, para encontrar el que corresponda a mi sección.
    
    - CA-25.1 — El sistema debe mostrar los cursos publicados disponibles. · Tareas: HU25-T1, HU25-T2, HU25-T3
        
    - CA-25.2 — El estudiante debe poder buscar cursos. · Tareas: HU25-T2, HU25-T3
        
    - CA-25.3 — Los resultados se limitan a la institución del estudiante (claim institucion del token). *(modificado)* · Tareas: HU25-T4
        
    - CA-25.4 — Cuando no existan resultados, debe mostrarse un mensaje indicándolo. · Tareas: HU25-T4
        
- **HU-26 — Inscribirse en un curso**: Como estudiante, quiero inscribirme usando el código de invitación que me da mi docente, para asegurar que entro al curso correcto.
    
    - CA-26.1 — Cada curso debe disponer del código de invitación definido por el sistema. · Tareas: HU26-T1
        
    - CA-26.2 — Un código válido debe permitir realizar la inscripción. · Tareas: HU26-T2, HU26-T3
        
    - CA-26.3 — Un código incorrecto debe ser rechazado. · Tareas: HU26-T3, HU26-T4
        
    - CA-26.4 — Un código vencido (30 días después de generarse) debe ser rechazado. *(modificado)* · Tareas: HU26-T1, HU26-T3, HU26-T4
        
    - CA-26.5 — El sistema debe impedir una inscripción duplicada del mismo estudiante al mismo curso. · Tareas: HU26-T2, HU26-T4
        
- **HU-27 — Mis cursos**: Como estudiante, quiero ver mis cursos inscritos en la pantalla principal, para entrar directamente a practicar.
    
    - CA-27.1 — El estudiante debe visualizar sus cursos inscritos. · Tareas: HU27-T1, HU27-T2, HU27-T3
        
    - CA-27.2 — La funcionalidad debe estar disponible en web. · Tareas: HU27-T2
        
    - CA-27.3 — La funcionalidad debe estar disponible en la aplicación móvil. · Tareas: HU27-T3
        
    - CA-27.4 — Cada curso mostrado debe permitir acceder a su contenido. · Tareas: HU27-T2, HU27-T3
        
- **HU-28 — Lista de inscritos**: Como docente, quiero ver la lista de estudiantes inscritos en mi curso, para confirmar que toda mi sección se registró.
    
    - CA-28.1 — El docente debe poder consultar los estudiantes inscritos en un curso propio. · Tareas: HU28-T1, HU28-T2
        
    - CA-28.2 — La información debe mostrarse en el panel docente. · Tareas: HU28-T2
        
    - CA-28.3 — El docente debe poder descargar la lista de inscritos. · Tareas: HU28-T3
        
    - CA-28.4 — Un docente no debe poder consultar los inscritos de cursos pertenecientes a otro docente. · Tareas: HU28-T4
        
- **HU-29 — Problemas disponibles**: Como estudiante, quiero ver la lista de problemas de un curso con su dificultad y tipo de ejercicio, para elegir cuál resolver.
    
    - CA-29.1 — Solo un estudiante inscrito debe poder consultar los problemas del curso. · Tareas: HU29-T1, HU29-T4
        
    - CA-29.2 — Cada problema debe mostrar su dificultad. · Tareas: HU29-T2
        
    - CA-29.3 — Cada problema debe mostrar su tipo de ejercicio. · Tareas: HU29-T2
        
    - CA-29.4 — La lista debe permitir orden por dificultad y fecha. · Tareas: HU29-T3
        
    - CA-29.5 — El listado debe admitir paginación. · Tareas: HU29-T3
        
- **HU-30 — Filtrar problemas**: Como estudiante, quiero filtrar los problemas por dificultad y por estado, para enfocarme en lo que todavía me falta.
    
    - CA-30.1 — El estudiante debe poder filtrar por dificultad. · Tareas: HU30-T1, HU30-T3
        
    - CA-30.2 — Debe poder filtrar por estado. · Tareas: HU30-T1, HU30-T3
        
    - CA-30.3 — El sistema debe distinguir problemas resueltos, intentados y no abiertos. · Tareas: HU30-T2
        
    - CA-30.4 — El sistema debe recordar el último filtro utilizado. · Tareas: HU30-T3
        
    - CA-30.5 — Una combinación sin resultados debe manejarse correctamente. · Tareas: HU30-T4
        
- **HU-31 — Problemas aprobados**: Como estudiante, quiero que solo aparezcan los problemas aprobados por el docente, para no toparme con ejercicios a medio revisar.
    
    - CA-31.1 — El listado debe incluir únicamente problemas aprobados. · Tareas: HU31-T1, HU31-T4
        
    - CA-31.2 — El detalle de un problema no aprobado no debe estar disponible para el estudiante. · Tareas: HU31-T2, HU31-T4
        
    - CA-31.3 — Ninguna ruta destinada al estudiante debe permitir omitir esta restricción. · Tareas: HU31-T3
        
- **HU-32 — Detalle del problema**: Como estudiante, quiero abrir el detalle de un problema antes de empezar, para leer el enunciado y decidir si lo intento ahora.
    
    - CA-32.1 — La pantalla debe mostrar el enunciado. · Tareas: HU32-T1, HU32-T2
        
    - CA-32.2 — Debe mostrar los casos de ejemplo disponibles. · Tareas: HU32-T1, HU32-T2
        
    - CA-32.3 — Debe proporcionar acceso al editor de código cuando corresponda. · Tareas: HU32-T3
        
    - CA-32.4 — Los casos ocultos no deben incluirse en la información enviada al estudiante. · Tareas: HU32-T1, HU32-T4
        

## Sprint 3

- **HU-33 (backlog 29) — Enunciado junto al editor**: Como estudiante, quiero ver el enunciado y los casos de ejemplo junto al editor, para no perder de vista lo que me piden mientras escribo la solución.
    
    - CA-33.1 — La interfaz debe disponer de un panel para el enunciado y otro para el editor. · Tareas: HU33-T1, HU33-T2
        
    - CA-33.2 — Los paneles deben poder redimensionarse. · Tareas: HU33-T1
        
    - CA-33.3 — Los ejemplos de entrada y salida deben mostrarse junto al enunciado. · Tareas: HU33-T3
        
    - CA-33.4 — El sistema debe recordar la posición del divisor entre paneles. · Tareas: HU33-T4
        
    - CA-33.5 — La interfaz debe funcionar tanto en pantallas pequeñas como en monitores de mayor tamaño. · Tareas: HU33-T5
        
- **HU-34 (backlog 30) — Plantilla inicial de código**: Como estudiante, quiero que el editor cargue un código base con la firma de la función, para empezar a resolver sin perder tiempo en la estructura.
    
    - CA-34.1 — El problema debe poder almacenar un código base. · Tareas: HU34-T1, HU34-T2
        
    - CA-34.2 — Al abrir el problema, el código base debe cargarse en el editor. · Tareas: HU34-T3
        
    - CA-34.3 — El estudiante debe poder restaurar el código base original. · Tareas: HU34-T4
        
- **HU-35 (backlog 31) — Borrador automático**: Como estudiante, quiero que mi código se guarde automáticamente como borrador, para no perder el avance si se cierra el navegador.
    
    - CA-35.1 — El código debe guardarse automáticamente como borrador. · Tareas: HU35-T1, HU35-T2, HU35-T3
        
    - CA-35.2 — El guardado espera a que el estudiante deje de escribir 3 segundos antes de llamar al servidor, para no enviar una solicitud por tecla. *(modificado)* · Tareas: HU35-T3
        
    - CA-35.3 — La interfaz debe mostrar información del último guardado. · Tareas: HU35-T4
        
    - CA-35.4 — Al volver al mismo problema debe recuperarse el último borrador guardado. · Tareas: HU35-T1, HU35-T2, HU35-T5
        
- **HU-36 (backlog 32) — Enviar solución**: Como estudiante, quiero enviar mi solución como intento oficial desde el mismo editor y recibir el resultado ahí mismo.
    
    - CA-36.1 — El estudiante debe poder enviar la solución como intento oficial. · Tareas: HU36-T2, HU36-T6
        
    - CA-36.2 — El sistema debe solicitar confirmación antes del envío. · Tareas: HU36-T3
        
    - CA-36.3 — El intento debe registrarse con su número correspondiente. · Tareas: HU36-T1, HU36-T2
        
    - CA-36.4 — El código debe evaluarse mediante el mecanismo de ejecución establecido. · Tareas: HU36-T2
        
    - CA-36.5 — El resultado debe mostrarse sin recargar la página. · Tareas: HU36-T4, HU36-T6
        
    - CA-36.6 — Mientras el código está siendo evaluado, el botón de envío debe permanecer bloqueado. · Tareas: HU36-T5
        
- **HU-37 (backlog 33) — Detalle de casos evaluados**: Como estudiante, quiero ver qué casos de prueba pasaron y cuáles no, comparando la salida esperada con la obtenida en los casos públicos.
    
    - CA-37.1 — El sistema debe indicar qué casos públicos fueron superados. · Tareas: HU37-T1, HU37-T2, HU37-T5
        
    - CA-37.2 — Debe indicar qué casos públicos fallaron. · Tareas: HU37-T1, HU37-T2, HU37-T5
        
    - CA-37.3 — Para los casos públicos debe mostrarse la salida esperada y la obtenida. · Tareas: HU37-T3
        
    - CA-37.4 — Las diferencias entre la salida esperada y la obtenida deben poder identificarse visualmente. · Tareas: HU37-T3
        
    - CA-37.5 — De un caso oculto solo debe mostrarse si pasa o falla. · Tareas: HU37-T4
        
- **HU-38 (backlog 34) — Calificación parcial**: Como estudiante, quiero recibir un puntaje proporcional a la cantidad de casos superados, para saber si mi solución está parcialmente correcta.
    
    - CA-38.1 — El puntaje debe calcularse utilizando la fórmula definida para el sistema. · Tareas: HU38-T1, HU38-T2
        
    - CA-38.2 — Una solución que supere todos los casos debe obtener el resultado correspondiente a una solución totalmente correcta. · Tareas: HU38-T2, HU38-T4
        
    - CA-38.3 — Una solución que supere solo parte de los casos debe obtener un resultado parcial. · Tareas: HU38-T2, HU38-T4
        
    - CA-38.4 — Una solución que no supere casos debe obtener el resultado correspondiente. · Tareas: HU38-T2, HU38-T4
        
    - CA-38.5 — El puntaje y el resultado deben mostrarse al estudiante. · Tareas: HU38-T3
        
- **HU-39 (backlog 35) — Ocultamiento de datos de prueba**: Como docente, quiero que los casos de prueba ocultos no revelen su entrada al estudiante, para que la calificación mida la solución y no la capacidad de adivinar los ejemplos.
    
    - CA-39.1 — La entrada de los casos ocultos no debe ser enviada al estudiante. · Tareas: HU39-T1, HU39-T4
        
    - CA-39.2 — La salida esperada de los casos ocultos no debe exponerse. · Tareas: HU39-T1, HU39-T4
        
    - CA-39.3 — Para un caso oculto únicamente debe indicarse si fue superado o no. · Tareas: HU39-T2, HU39-T4
        
    - CA-39.4 — Ninguna ruta accesible por el estudiante debe revelar el contenido del caso oculto. · Tareas: HU39-T3, HU39-T4
        
- **HU-40 (backlog 36) — Errores comprensibles**: Como estudiante, quiero un mensaje claro cuando mi código exceda el tiempo límite o falle al compilar, para distinguir un error mío de una falla de la plataforma.
    
    - CA-40.1 — Un tiempo de ejecución excedido debe mostrarse mediante un mensaje comprensible en español. · Tareas: HU40-T1, HU40-T5
        
    - CA-40.2 — Un error de compilación o sintaxis debe mostrarse mediante un mensaje comprensible. · Tareas: HU40-T1, HU40-T5
        
    - CA-40.3 — Cuando sea posible, el sistema debe indicar la línea relacionada con el error de sintaxis. · Tareas: HU40-T2
        
    - CA-40.4 — El sistema debe diferenciar los errores del código del estudiante de las fallas del servicio. · Tareas: HU40-T3, HU40-T5
        
    - CA-40.5 — El mensaje de resultado debe almacenarse junto con el envío correspondiente. · Tareas: HU40-T4
        
- **HU-41 (backlog 37) — Consultar intentos**: Como estudiante, quiero ver la lista de mis envíos de un problema con su fecha, resultado y puntaje, para saber cuántas veces lo he intentado.
    
    - CA-41.1 — El historial debe mostrar los envíos correspondientes al estudiante y problema consultado. · Tareas: HU41-T1, HU41-T2
        
    - CA-41.2 — Cada envío debe mostrar fecha, resultado y puntaje. · Tareas: HU41-T2
        
    - CA-41.3 — Los envíos deben mostrarse ordenados por fecha. · Tareas: HU41-T1, HU41-T2
        
    - CA-41.4 — El historial debe admitir paginación cuando existan muchos intentos. · Tareas: HU41-T3
        
    - CA-41.5 — Un estudiante no debe poder consultar envíos pertenecientes a otro estudiante. · Tareas: HU41-T4
        
- **HU-42 (backlog 38) — Recuperar una solución anterior**: Como estudiante, quiero abrir un envío anterior y ver el código exacto que mandé, para recuperar una solución que funcionaba mejor.
    
    - CA-42.1 — Cada envío debe conservar el código enviado. · Tareas: HU42-T1
        
    - CA-42.2 — El estudiante debe poder abrir el detalle de un envío anterior. · Tareas: HU42-T2, HU42-T3
        
    - CA-42.3 — El código del envío anterior debe mostrarse inicialmente en modo lectura. · Tareas: HU42-T3
        
    - CA-42.4 — El estudiante debe poder copiar ese código al editor actual. · Tareas: HU42-T4
        
- **HU-43 (backlog 39) — Resumen de progreso**: Como estudiante, quiero ver un resumen de mi progreso en cada curso, con los problemas resueltos, intentados y pendientes.
    
    - CA-43.1 — El sistema debe identificar problemas resueltos. · Tareas: HU43-T1, HU43-T3
        
    - CA-43.2 — Debe identificar problemas intentados. · Tareas: HU43-T1
        
    - CA-43.3 — Debe identificar problemas pendientes. · Tareas: HU43-T1
        
    - CA-43.4 — La información debe representarse mediante una tarjeta con barra de avance. · Tareas: HU43-T2
        
    - CA-43.5 — El cálculo debe manejar correctamente un curso vacío. · Tareas: HU43-T4
        
    - CA-43.6 — El cálculo debe manejar correctamente un curso completamente terminado. · Tareas: HU43-T4
        
- **HU-44 (backlog 40) — Historial de envíos del grupo**: Como docente, quiero revisar el historial de envíos de mis estudiantes en un problema, para detectar en qué tema está trabando el grupo.
    
    - CA-44.1 — El docente debe poder consultar información de envíos correspondiente a sus cursos. · Tareas: HU44-T1, HU44-T4
        
    - CA-44.2 — El sistema debe mostrar el porcentaje de acierto de cada problema. · Tareas: HU44-T1, HU44-T2
        
    - CA-44.3 — Los problemas deben poder ordenarse por porcentaje de acierto. · Tareas: HU44-T3
        
    - CA-44.4 — Un docente no debe acceder a información perteneciente a cursos de otro docente. · Tareas: HU44-T4
        
- **HU-61 (backlog 57) — Programar una evaluación**: Como docente, quiero crear una evaluación con fecha de inicio, fecha de fin y límite de tiempo, para programar un examen o una práctica calificada.
    
    - CA-61.1 — El docente debe poder indicar una fecha de inicio. · Tareas: HU61-T1, HU61-T2, HU61-T3
        
    - CA-61.2 — Debe poder indicar una fecha de fin. · Tareas: HU61-T1, HU61-T2, HU61-T3
        
    - CA-61.3 — Debe poder definir un límite de tiempo. · Tareas: HU61-T1, HU61-T2, HU61-T3
        
    - CA-61.4 — La fecha de fin debe ser posterior a la fecha de inicio. · Tareas: HU61-T4
        
- **HU-62 (backlog 58) — Agregar problemas a una evaluación**: Como docente, quiero seleccionar qué problemas de mis cursos formarán parte de la evaluación.
    
    - CA-62.1 — El docente debe poder seleccionar problemas del banco correspondiente al curso. · Tareas: HU62-T1, HU62-T2
        
    - CA-62.2 — El sistema debe admitir la selección de varios problemas. · Tareas: HU62-T1, HU62-T2
        
    - CA-62.3 — Una evaluación debe contener al menos un problema. · Tareas: HU62-T3
        
- **HU-63 (backlog 59) — Asignación de puntos**: Como docente, quiero definir el valor en puntos de cada problema dentro de la evaluación, para dar mayor peso a los ejercicios más complejos.
    
    - CA-63.1 — El docente debe poder asignar puntos a cada problema seleccionado. · Tareas: HU63-T1, HU63-T2
        
    - CA-63.2 — Los valores deben poder modificarse mientras se configura la evaluación. · Tareas: HU63-T2
        
    - CA-63.3 — El sistema debe mostrar el total de puntos resultante. · Tareas: HU63-T3
        
    - CA-63.4 — La suma mostrada debe corresponder a los valores asignados a los problemas. · Tareas: HU63-T3, HU63-T4
        
- **HU-64 (backlog 60) — Control automático de acceso**: Como docente, quiero que la evaluación se active y se cierre automáticamente según el horario programado, para no controlar el acceso manualmente.
    
    - CA-64.1 — Antes de la fecha de inicio, la evaluación no debe permitir el acceso correspondiente. · Tareas: HU64-T1, HU64-T2, HU64-T4
        
    - CA-64.2 — Durante la ventana programada, la evaluación debe encontrarse disponible. · Tareas: HU64-T1, HU64-T2, HU64-T4
        
    - CA-64.3 — Después de la fecha de finalización, el acceso debe cerrarse automáticamente. · Tareas: HU64-T1, HU64-T2, HU64-T4
        
    - CA-64.4 — El intento debe cerrarse cuando venza el límite de tiempo aplicable. · Tareas: HU64-T3, HU64-T4
        

## Sprint 4

- **HU-45 (backlog 41) — Predecir salida en consola**: Como estudiante, quiero ver un fragmento de código y escribir lo que imprime en consola, para entrenar la lectura de código sin necesidad de un teclado.
    
    - CA-45.1 — La aplicación debe mostrar el fragmento de código correspondiente. · Tareas: HU45-T2
        
    - CA-45.2 — El estudiante debe poder introducir la salida que considera correcta. · Tareas: HU45-T2
        
    - CA-45.3 — La respuesta se compara con la esperada ignorando los espacios al inicio y al final, reduciendo los espacios repetidos a uno y sin distinguir mayúsculas de minúsculas. *(modificado)* · Tareas: HU45-T1, HU45-T4
        
    - CA-45.4 — El sistema debe distinguir respuestas correctas e incorrectas. · Tareas: HU45-T1, HU45-T4
        
    - CA-45.5 — Debe aplicarse el control de intentos definido para el ejercicio. · Tareas: HU45-T3
        
- **HU-46 (backlog 42) — Responder alternativa**: Como estudiante, quiero responder ejercicios de opción múltiple tocando una alternativa, para practicar con una sola mano mientras me traslado.
    
    - CA-46.1 — La aplicación debe mostrar las alternativas disponibles. · Tareas: HU46-T1, HU46-T3
        
    - CA-46.2 — El estudiante debe poder seleccionar una alternativa mediante la interfaz móvil. · Tareas: HU46-T3
        
    - CA-46.3 — El orden de las alternativas debe poder presentarse mezclado. · Tareas: HU46-T2
        
    - CA-46.4 — La respuesta correcta no debe enviarse al dispositivo como parte de la información que permita identificarla antes de responder. · Tareas: HU46-T2, HU46-T4
        
    - CA-46.5 — Las alternativas son botones de al menos 48 dp de alto, cómodos para el pulgar. *(modificado)* · Tareas: HU46-T3
        
- **HU-47 (backlog 43) — Completar código**: Como estudiante, quiero completar el espacio en blanco de un fragmento de código, para reforzar la sintaxis que más se me olvida.
    
    - CA-47.1 — El fragmento debe identificar el espacio que debe completar el estudiante. · Tareas: HU47-T1, HU47-T3
        
    - CA-47.2 — El estudiante debe poder escribir la respuesta correspondiente. · Tareas: HU47-T3
        
    - CA-47.3 — La evaluación ignora los espacios sobrantes y las diferencias de mayúsculas. *(modificado)* · Tareas: HU47-T2, HU47-T4
        
    - CA-47.4 — Respuestas escritas de forma diferente deben aceptarse cuando sean equivalentes según las reglas de normalización establecidas. · Tareas: HU47-T2, HU47-T4
        
- **HU-48 (backlog 44) — Feedback inmediato**: Como estudiante, quiero que la corrección sea inmediata al responder, para aprovechar los ratos cortos sin esperar a que se ejecute nada.
    
    - CA-48.1 — Los ejercicios móviles contemplados no deben utilizar el sandbox. · Tareas: HU48-T1
        
    - CA-48.2 — Después de responder debe indicarse inmediatamente si la respuesta fue correcta. · Tareas: HU48-T2
        
    - CA-48.3 — Si el estudiante falla, el sistema le muestra cuál era la respuesta correcta. *(modificado)* · Tareas: HU48-T2
        
    - CA-48.4 — Una pérdida de conectividad debe manejarse sin descartar necesariamente la interacción pendiente. · Tareas: HU48-T3, HU48-T4
        
    - CA-48.5 — Al restablecerse la conexión, la app reenvía automáticamente la respuesta pendiente y se registra una sola vez. *(modificado)* · Tareas: HU48-T3, HU48-T4
        
- **HU-49 (backlog 45) — Mantener racha**: Como estudiante, quiero mantener una racha de días consecutivos practicando, para darme una razón de volver a la aplicación todos los días.
    
    - CA-49.1 — El sistema suma un día a la racha cuando el estudiante resuelve al menos un ejercicio ese día. *(modificado)* · Tareas: HU49-T1, HU49-T2, HU49-T4
        
    - CA-49.2 — El cálculo debe considerar la hora local del estudiante según lo definido por el sistema. · Tareas: HU49-T2
        
    - CA-49.3 — La racha actual debe mostrarse en la pantalla principal. · Tareas: HU49-T3
        
    - CA-49.4 — La racha vuelve a cero cuando el estudiante pasa un día sin resolver ningún ejercicio. *(modificado)* · Tareas: HU49-T4
        
- **HU-50 (backlog 46) — Subir de cinturón**: Como estudiante, quiero subir de cinturón en un lenguaje conforme acumulo puntos, para tener una medida visible de mi avance.
    
    - CA-50.1 — El sistema debe acumular puntos obtenidos mediante los ejercicios según las reglas definidas. · Tareas: HU50-T1, HU50-T2
        
    - CA-50.2 — Cada tipo de ejercicio otorga los puntos definidos en docs/puntos-cinturon.md. *(modificado)* · Tareas: HU50-T4
        
    - CA-50.3 — Cuando el estudiante alcance el umbral definido para un cinturón, su nivel debe actualizarse. · Tareas: HU50-T1, HU50-T4
        
    - CA-50.4 — El cinturón alcanzado debe mostrarse mediante la representación visual correspondiente. · Tareas: HU50-T3
        
- **HU-51 (backlog 47) — Puntos restantes**: Como estudiante, quiero ver cuántos puntos me faltan para el siguiente cinturón, para saber cuánto esfuerzo me queda por delante.
    
    - CA-51.1 — El sistema debe calcular los puntos restantes para alcanzar el siguiente cinturón. · Tareas: HU51-T1, HU51-T4
        
    - CA-51.2 — La información debe mostrarse en el perfil. · Tareas: HU51-T1
        
    - CA-51.3 — Debe mostrarse una barra de avance hacia el siguiente cinturón. · Tareas: HU51-T2
        
    - CA-51.4 — Un estudiante con el cinturón máximo (negro) ve un mensaje especial en lugar de la barra. *(modificado)* · Tareas: HU51-T3, HU51-T4
        
- **HU-52 (backlog 48) — Recordatorio de racha**: Como estudiante, quiero recibir un aviso cuando esté por perder mi racha, para no romperla por olvido.
    
    - CA-52.1 — Cada día el sistema identifica a los estudiantes con racha activa que todavía no practicaron. *(modificado)* · Tareas: HU52-T2
        
    - CA-52.2 — El estudiante debe poder recibir el aviso en su dispositivo. · Tareas: HU52-T1, HU52-T2, HU52-T4
        
    - CA-52.3 — El estudiante debe poder desactivar estos avisos. · Tareas: HU52-T3
        
    - CA-52.4 — El estudiante puede elegir la hora a la que recibe el aviso. *(modificado)* · Tareas: HU52-T3
        
- **HU-53 (backlog 49) — Iniciar Dojo**: Como estudiante, quiero iniciar una sesión de práctica continua sin tener que elegir un problema, para ponerme a entrenar de inmediato.
    
    - CA-53.1 — El estudiante debe poder iniciar una sesión Dojo. · Tareas: HU53-T1, HU53-T3
        
    - CA-53.2 — El sistema debe entregar automáticamente el siguiente ejercicio. · Tareas: HU53-T2
        
    - CA-53.3 — Un mismo ejercicio no debe repetirse dentro de la misma sesión cuando así esté contemplado. · Tareas: HU53-T2, HU53-T4
        
    - CA-53.4 — La pantalla debe mostrar el contador de aciertos de la sesión. · Tareas: HU53-T3
        
- **HU-54 (backlog 50) — Ajuste automático de dificultad**: Como estudiante, quiero que la dificultad suba o baje según mis aciertos, para practicar siempre en un nivel que me exija sin frustrarme.
    
    - CA-54.1 — La dificultad sube un nivel tras dos aciertos seguidos y baja un nivel tras dos errores seguidos (regla escrita en docs/, HU54 T1). *(modificado)* · Tareas: HU54-T1, HU54-T2
        
    - CA-54.2 — El siguiente ejercicio debe seleccionarse teniendo en cuenta el nivel resultante. · Tareas: HU54-T2
        
    - CA-54.3 — Al finalizar una sesión debe conservarse el nivel alcanzado. · Tareas: HU54-T3
        
    - CA-54.4 — Un estudiante nuevo debe comenzar con el nivel inicial definido por el sistema. · Tareas: HU54-T4
        
    - CA-54.5 — La regla debe responder correctamente tanto a secuencias de aciertos como de errores. · Tareas: HU54-T5
        
- **HU-55 (backlog 51) — Salir y reanudar Dojo**: Como estudiante, quiero salir del Dojo en cualquier momento y conservar lo avanzado, para poder practicar en sesiones cortas.
    
    - CA-55.1 — Al abandonar una sesión debe guardarse su estado. · Tareas: HU55-T1
        
    - CA-55.2 — Una sesión interrumpida debe poder reanudarse. · Tareas: HU55-T2
        
    - CA-55.3 — El estudiante debe disponer de una opción para terminar definitivamente la sesión. · Tareas: HU55-T3
        
    - CA-55.4 — El sistema debe solicitar confirmación cuando se utilice la acción prevista para terminar la sesión. · Tareas: HU55-T3
        
    - CA-55.5 — Un cierre de la aplicación durante una sesión no debe provocar la pérdida del estado que ya haya sido guardado. · Tareas: HU55-T1, HU55-T4
        
- **HU-56 (backlog 52) — Resultado de sesión**: Como estudiante, quiero ver al terminar la sesión un resumen de mis aciertos y los temas en los que más fallé, para saber qué repasar.
    
    - CA-56.1 — El sistema debe mostrar la cantidad de aciertos obtenidos. · Tareas: HU56-T2
        
    - CA-56.2 — Debe identificar los temas en los que se produjeron más errores. · Tareas: HU56-T1, HU56-T2
        
    - CA-56.3 — Los puntos obtenidos en la sesión deben sumarse al cinturón correspondiente de acuerdo con las reglas definidas. · Tareas: HU56-T3
        
    - CA-56.4 — El resumen debe manejar correctamente una sesión sin errores. · Tareas: HU56-T2, HU56-T4
        
    - CA-56.5 — El resumen debe manejar correctamente una sesión sin aciertos. · Tareas: HU56-T2, HU56-T4
        
- **HU-57 (backlog 53) — Retar a un compañero**: Como estudiante, quiero retar a un compañero de mi curso a una partida de ejercicios cortos, para practicar compitiendo en lugar de solo.
    
    - CA-57.1 — El estudiante debe poder seleccionar un compañero perteneciente a su curso. · Tareas: HU57-T2, HU57-T3
        
    - CA-57.2 — El sistema debe permitir crear la partida e invitar al estudiante seleccionado. · Tareas: HU57-T1, HU57-T2
        
    - CA-57.3 — La pantalla de reto debe mostrar los compañeros disponibles del curso. · Tareas: HU57-T3
        
    - CA-57.4 — Un reto que el compañero no responde en 24 horas caduca. *(modificado)* · Tareas: HU57-T4
        
- **HU-58 (backlog 54) — Misma secuencia de ejercicios**: Como estudiante, quiero que ambos jugadores reciban la misma secuencia de ejercicios, para que la comparación del resultado sea justa.
    
    - CA-58.1 — Al crear la partida debe establecerse la secuencia de ejercicios correspondiente. · Tareas: HU58-T1, HU58-T4
        
    - CA-58.2 — Ambos jugadores deben recibir el mismo ejercicio en cada turno equivalente. · Tareas: HU58-T2, HU58-T4
        
    - CA-58.3 — Los jugadores deben mantener el orden establecido de la secuencia. · Tareas: HU58-T2, HU58-T4
        
    - CA-58.4 — Un jugador no debe poder adelantarse en la secuencia respecto de las reglas de la partida. · Tareas: HU58-T3, HU58-T4
        
- **HU-59 (backlog 55) — Marcador de partida**: Como estudiante, quiero ver el marcador en vivo durante la partida, para saber si voy ganando y mantener la tensión del reto.
    
    - CA-59.1 — La pantalla de partida debe mostrar el marcador. · Tareas: HU59-T3
        
    - CA-59.2 — El marcador debe actualizarse durante el desarrollo de la partida. · Tareas: HU59-T1, HU59-T2, HU59-T3
        
    - CA-59.3 — Si un jugador se desconecta, tiene 30 segundos para volver a la partida. *(modificado)* · Tareas: HU59-T1, HU59-T4
        
    - CA-59.4 — Si no vuelve en ese tiempo, pierde la partida. *(modificado)* · Tareas: HU59-T1, HU59-T4
        
    - CA-59.5 — El marcador debe mantener un comportamiento funcional bajo las condiciones de conexión lenta contempladas en las pruebas. · Tareas: HU59-T5
        
- **HU-60 (backlog 56) — Historial y clasificación**: Como estudiante, quiero consultar el historial de mis partidas y el ranking de mi curso, para medir mi desempeño frente al de mis compañeros.
    
    - CA-60.1 — El estudiante debe poder visualizar el historial de las partidas jugadas. · Tareas: HU60-T1, HU60-T3
        
    - CA-60.2 — El estudiante debe poder visualizar el ranking del curso basado en el desempeño documentado. · Tareas: HU60-T2, HU60-T3, HU60-T4
        
    - CA-60.3 — El ranking debe mostrar la posición del estudiante incluso cuando se encuentre en una página diferente del listado. · Tareas: HU60-T2
        
    - CA-60.4 — Un estudiante no debe poder visualizar un ranking correspondiente a un curso en el que no está inscrito. · Tareas: HU60-T4
        

