from django.urls import path

from cursos import views

urlpatterns = [
    path("", views.CursosView.as_view()),
    path("<int:pk>/", views.EditarCursoView.as_view()),
    path("<int:pk>/inscritos/", views.InscritosView.as_view()),
]
