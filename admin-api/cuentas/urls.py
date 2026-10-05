from django.urls import path

from cuentas import views

urlpatterns = [
    path("registro/docente/", views.RegistroDocenteView.as_view()),
    path("login/", views.LoginView.as_view()),
    path("refresh/", views.RefreshView.as_view()),
    path("logout/", views.LogoutView.as_view()),
    path("recuperar/", views.RecuperarView.as_view()),
    path("nueva-contrasena/", views.NuevaContrasenaView.as_view()),
]
