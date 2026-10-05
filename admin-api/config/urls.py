from django.contrib import admin
from django.urls import include, path

from config import views

urlpatterns = [
    path("admin/", admin.site.urls),
    path("health", views.health),
    path("api/auth/", include("cuentas.urls")),
    path("api/cursos/", include("cursos.urls")),
]
