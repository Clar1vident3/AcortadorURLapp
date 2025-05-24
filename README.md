# Acortador de URLs - App Android

![Acortador de URLs Logo](docs/logo_app.png) Una aplicación Android nativa para acortar URLs y gestionar tus enlaces, con funcionalidades para usuarios estándar y usuarios Premium.

## 🚀 Características

* **Acortar URLs:** Convierte enlaces largos en URLs cortas y fáciles de compartir.
* **Historial de URLs:** Guarda y visualiza un historial de todas las URLs que has acortado.
* **Copiar al Portapapeles:** Copia fácilmente las URLs acortadas con un solo toque.
* **Inicio de Sesión con Google:** Autenticación sencilla y segura a través de tu cuenta de Google.
* **Gestión de Intentos Gratuitos:** Los usuarios no premium tienen un límite de URLs acortadas.
* **Funcionalidad Premium:**
    * URLs ilimitadas.
    * Interfaz de usuario mejorada para usuarios Premium.
    * Simulación de proceso de suscripción (no se procesan pagos reales).
* **Persistencia de Datos:** Utiliza Firebase Firestore para almacenar y sincronizar datos de usuario e historial de URLs en tiempo real.

## 🛠️ Tecnologías Utilizadas

* **Lenguaje de Programación:** Java
* **Plataforma:** Android (SDK Nivel 21+)
* **Backend como Servicio (BaaS):**
    * **Firebase Authentication:** Para el inicio de sesión y gestión de usuarios.
    * **Firebase Firestore:** Base de datos NoSQL en la nube para datos de usuario e historial.
* **Comunicación con API de Acortamiento:**
    * **Retrofit:** Cliente HTTP Type-safe para consumir la API de acortamiento de URLs.
    * **GSON:** Librería para serialización/deserialización de objetos Java a/desde JSON.
* **Validaciones:** Expresiones regulares para validación de URLs, tarjetas, etc.

## ⚙️ Configuración y Ejecución

Para ejecutar este proyecto localmente, sigue estos pasos:

### 1. Requisitos Previos

* Android Studio instalado.
* JDK 11 o superior.
* Una cuenta de Google y un proyecto de Firebase configurado.

### 2. Configuración de Firebase

1.  **Crea un Proyecto de Firebase:** Si aún no lo tienes, crea un nuevo proyecto en la [Consola de Firebase](https://console.firebase.google.com/).
2.  **Añade una Aplicación Android:**
    * En tu proyecto de Firebase, haz clic en `Add app` y selecciona el icono de Android.
    * Registra tu aplicación con el **nombre de paquete** correcto (ej. `com.example.acortadorurlapp`).
    * **Es CRUCIAL añadir las huellas SHA-1:**
        * Para desarrollo (Debug): Obtén la SHA-1 de tu clave de depuración (`signingReport` en Gradle).
        * Para producción (Release): Obtén la SHA-1 de la clave que usas para firmar tus APKs de distribución (la que generaste como `URL_KEY`).
        * Añade AMBAS huellas SHA-1 en la configuración de tu aplicación Android en Firebase.
3.  **Descarga `google-services.json`:** Descarga este archivo desde la configuración de tu aplicación Android en Firebase y colócalo en el directorio `app/` de tu proyecto Android Studio.
4.  **Habilita Servicios:**
    * **Authentication:** Ve a `Build` -> `Authentication` y habilita `Google Sign-in`.
    * **Firestore Database:** Ve a `Build` -> `Firestore Database`, crea una nueva base de datos y configura las reglas de seguridad necesarias (puedes empezar con reglas de prueba para desarrollo, pero asegúrate de asegurarlas para producción).

### 3. Configuración de la API de Acortamiento

Este proyecto asume que tienes una API de acortamiento de URLs externa. Asegúrate de que `RetrofitClient` apunte a la URL base correcta de tu servicio de acortamiento.

* Abre `app/src/main/java/com/example/acortadorurlapp/RetrofitClient.java` (o la ubicación de tu `RetrofitClient`).
* Verifica o establece la `BASE_URL` para tu API.

### 4. Abrir y Ejecutar en Android Studio

1.  Abre el proyecto en Android Studio.
2.  Sincroniza el proyecto con los archivos Gradle (Gradle Sync).
3.  Conecta un dispositivo Android o inicia un emulador.
4.  Haz clic en `Run 'app'` (el botón de Play verde) para instalar y ejecutar la aplicación.

## Para más información consultar la documentacion

## Proyecto hecho por los estudiantes de informática de sexto semestre:
David Jerónimo Rojas Avalos
Escamilla Cuevas José Octavio
