from django.urls import path

from evaluaciones import interno

# Se monta en /internal/evaluaciones/ (config/urls.py).
urlpatterns = [
    path("<int:evaluacion_id>/", interno.EvaluacionView.as_view()),
]
