# Electra

## Configuración de firma Android (Release)

Para generar versiones de producción de la aplicación Android es necesario configurar un Keystore que permita firmar la aplicación.

### Generar el Keystore

Si todavía no existe el fichero de firma, ejecutar el siguiente comando desde la raíz del proyecto:

```bash
mkdir -p keystore
keytool -genkeypair -v -keystore keystore/electra-release.jks -alias electra-release -keyalg RSA -keysize 2048 -validity 10000
```

Durante el proceso se solicitarán los datos del certificado y las contraseñas correspondientes.
Importante: Guardar de forma segura el fichero electra-release.jks y sus contraseñas. No se debe perder ni sustituir este fichero una vez publicada la aplicación.

### Configurar las propiedades de firma

Crear un fichero keystore.properties en la raíz del proyecto con el siguiente contenido:

```properties
storePassword=TU_STORE_PASSWORD
keyPassword=TU_KEY_PASSWORD
keyAlias=electra-release
storeFile=keystore/electra-release.jks
```
Sustituir los valores de contraseña por los utilizados al generar el Keystore.

### Configurar Gradle

La configuración de firma debe estar definida en android/app/build.gradle.

Cargar las propiedades del Keystore:

```kotlin
val keystoreProperties: Properties? = rootProject.file("keystore.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    ?.also { props ->
        val required = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        val missing = required.filter { props.getProperty(it).isNullOrBlank() }
        require(missing.isEmpty()) { "keystore.properties incompleto, faltan: $missing" }
    }

if (keystoreProperties == null) {
    logger.warn("⚠ keystore.properties no encontrado: los builds release (pro) saldrán SIN firmar.")
}
```

Dentro del bloque android, configurar:

```gradle
signingConfigs {
    keystoreProperties?.let { props ->
        create("release") {
            storeFile = rootProject.file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
    }
}
```

### Verificar el Keystore

Para comprobar que el fichero y el alias son correctos:
```bash
keytool -list -v -keystore keystore/electra-release.jks -alias electra-release
```

### Generar una versión Release

Desde el directorio android, ejecutar:

Generar APK:
```bash
./gradlew assembleRelease
```

Generar Android App Bundle (AAB):

```bash
./gradlew bundleRelease
```

El fichero AAB generado estará disponible en:

```text
android/app/build/outputs/bundle/release/app-release.aab
```

El APK generado estará disponible en:

```text
android/app/build/outputs/apk/release/app-release.apk
```

### Seguridad

Los siguientes ficheros contienen información sensible y no deben subirse al repositorio Git:

key.properties
keystore/electra-release.jks

Añadir las siguientes entradas al fichero .gitignore:
key.properties
keystore/*.jks

⚠️ Consideraciones importantes
Mantener una copia de seguridad segura del Keystore.
No compartir las contraseñas ni el fichero .jks públicamente.
Utilizar siempre el mismo Keystore para firmar las actualizaciones de la aplicación.
En entornos de integración continua (CI/CD), almacenar las claves y contraseñas como secretos protegidos.
No generar un nuevo Keystore para cada versión de producción.