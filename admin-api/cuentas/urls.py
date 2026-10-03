from django.urls import path

from cuentas import views

urlpatterns = [
    path("registro/docente/", views.RegistroDocenteView.as_view()),
]
