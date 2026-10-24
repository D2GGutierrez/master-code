from django.urls import path

from problemas import views

# Bandeja de revisión y acciones sobre un problema (HU18, HU19, HU20). Van fuera de
# /api/cursos/<id>/problemas/ porque la bandeja mezcla problemas de varios cursos.
urlpatterns = [
    path("pendientes/", views.PendientesView.as_view()),
    path("<int:pk>/", views.ProblemaView.as_view()),
    path("<int:pk>/aprobar/", views.AprobarProblemaView.as_view()),
    path("<int:pk>/descartar/", views.DescartarProblemaView.as_view()),
]
