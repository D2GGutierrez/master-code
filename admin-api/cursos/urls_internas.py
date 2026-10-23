from django.urls import path

from cursos import interno

urlpatterns = [
    path("", interno.CursosPorIdsView.as_view()),
    path("por-codigo/<str:codigo>/", interno.CursoPorCodigoView.as_view()),
]
