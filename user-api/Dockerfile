# Etapa 1: compila con el mismo wrapper de Maven que se usa en el PC y en el CI.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Primero solo el pom: si no cambia, Docker reutiliza la capa con las dependencias.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# Etapa 2: solo el JRE y el .jar, sin Maven ni el código fuente.
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Usuario sin privilegios: si alguien toma el proceso, no es root.
RUN useradd --system app
USER app

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
