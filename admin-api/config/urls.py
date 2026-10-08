from django.contrib import admin
from django.urls import include, path

from config import views
from cuentas.views import JwksView

urlpatterns = [
    path("admin/", admin.site.urls),
    path("health", views.health),
    # Ruta estándar en la raíz, fuera de /api/, donde los clientes de JWKS la buscan.
    path(".well-known/jwks.json", JwksView.as_view()),
    path("api/auth/", include("cuentas.urls")),
    path("api/cursos/", include("cursos.urls")),
    path("api/cursos/<int:curso_id>/problemas/", include("problemas.urls")),
]
