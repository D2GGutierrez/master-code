claves-jwt:
	bash infra/scripts/claves-jwt.sh

up:
	docker compose up -d

# Sin -v: el volumen de la base se conserva.
down:
	docker compose down

logs:
	docker compose logs -f
