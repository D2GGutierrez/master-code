claves-jwt:
	bash infra/scripts/claves-jwt.sh

up:
	docker compose up -d

# Sin -v: el volumen de la base se conserva.
down:
	docker compose down

logs:
	docker compose logs -f

# Recorre de punta a punta lo que se muestra al profesor (docente, estudiante, envío de soluciones). Necesita `make up`.
# Con IA sin configurar: python infra/scripts/humo.py --sin-ia
humo:
	python infra/scripts/humo.py

# Carga datos de demostración para mostrar la plataforma con vida: docente, 2 cursos, problemas con IA y 6 estudiantes con envíos.
# Es seguro repetirlo. La clave de las cuentas la pones tú: DEMO_CLAVE=... make datos-demo (ver infra/scripts/datos_demo.py).
datos-demo:
	python infra/scripts/datos_demo.py

# SonarQube local (docs/sonarqube.md): `make sonar-up` arranca el servidor en http://localhost:9000 y `make sonar` analiza todo el monorepo.
sonar-up:
	docker compose -f docker-compose.sonar.yml up -d

# Sin -v: el volumen se conserva y los análisis anteriores no se pierden.
sonar-down:
	docker compose -f docker-compose.sonar.yml down

sonar:
	bash infra/scripts/sonar.sh
