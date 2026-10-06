from django.urls import path

from problemas import views

urlpatterns = [
    path("", views.CrearProblemaView.as_view()),
]
