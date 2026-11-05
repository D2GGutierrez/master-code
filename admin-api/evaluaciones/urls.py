from django.urls import path

from evaluaciones import views

urlpatterns = [
    path("", views.CrearEvaluacionView.as_view()),
]
