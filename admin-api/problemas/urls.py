from django.urls import path

from problemas import views

urlpatterns = [
    path("", views.CrearProblemaView.as_view()),
    path("generar/", views.GenerarProblemasView.as_view()),
]
