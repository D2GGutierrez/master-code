from django.urls import path

from cursos import views

urlpatterns = [
    path("", views.CursosView.as_view()),
]
