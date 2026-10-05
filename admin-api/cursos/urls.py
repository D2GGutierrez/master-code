from django.urls import path

from cursos import views

urlpatterns = [
    path("", views.CrearCursoView.as_view()),
]
