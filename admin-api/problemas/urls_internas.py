from django.urls import path

from problemas import interno

# Se monta en /internal/ (config/urls.py): las dos rutas cuelgan de cursos/ y de problemas/.
urlpatterns = [
    path("cursos/<int:curso_id>/problemas/", interno.ProblemasDelCursoView.as_view()),
    path("problemas/<int:problema_id>/", interno.ProblemaView.as_view()),
]
