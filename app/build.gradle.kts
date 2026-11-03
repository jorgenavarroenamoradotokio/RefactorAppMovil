import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

val appVersionCode = providers.gradleProperty("electra.versionCode").get().toInt()
val appVersionName: String = providers.gradleProperty("electra.versionName").get()

enum class LogLevel(val priority: String) {
    VERBOSE("android.util.Log.VERBOSE"),
    DEBUG("android.util.Log.DEBUG"),
    INFO("android.util.Log.INFO"),
    WARN("android.util.Log.WARN"),
    ERROR("android.util.Log.ERROR"),
}

data class Logging(
    val toLogcat: Boolean,
    val toFile: Boolean,
    val minLevel: LogLevel,
)

data class Environment(
    /** Nombre del buildType y sufijo del applicationId (salvo en release). */
    val name: String,
    /** true → hereda de "release": sin debug, minificado, firmado con el keystore real. */
    val isRelease: Boolean,
    val appName: String,
    val baseUrl: String,
    val logging: Logging,
) {
    val environmentId: String get() = name.uppercase()
    val allowsCleartext: Boolean get() = baseUrl.startsWith("http://")
}

val environments = listOf(
    Environment(
        name = "dev",
        isRelease = false,
        appName = "Electra DEV",
        baseUrl = "http://192.168.1.17:9091/",
        logging = Logging(toLogcat = true, toFile = true, minLevel = LogLevel.VERBOSE),
    ),
    Environment(
        name = "pre",
        isRelease = false,
        appName = "Electra PRE",
        baseUrl = "https://pre.api.example.com/",
        logging = Logging(toLogcat = false, toFile = true, minLevel = LogLevel.DEBUG),
    ),
    Environment(
        name = "pro",
        isRelease = true,
        appName = "Electra",
        baseUrl = "https://api.example.com/",
        logging = Logging(toLogcat = false, toFile = true, minLevel = LogLevel.INFO),
    ),
)

// Firma
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


android {
    namespace = "com.redur.electra"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.redur.electra"
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("long", "INACTIVITY_TIMEOUT_MS", "15L * 60L * 1000L")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        viewBinding = true
    }

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
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }

        environments.forEach { env ->
            val parent = if (env.isRelease) "release" else "debug"

            create(env.name) {
                initWith(getByName(parent))
                matchingFallbacks += parent

                if (!env.isRelease) {
                    applicationIdSuffix = ".${env.name}"
                    versionNameSuffix = "-${env.name}"
                }

                resValue("string", "app_name", env.appName)
                manifestPlaceholders["usesCleartextTraffic"] = env.allowsCleartext.toString()

                buildConfigField("String", "ENVIRONMENT", "\"${env.environmentId}\"")
                buildConfigField("String", "BASE_URL", "\"${env.baseUrl}\"")
                buildConfigField("boolean", "LOG_TO_LOGCAT", env.logging.toLogcat.toString())
                buildConfigField("boolean", "LOG_TO_FILE", env.logging.toFile.toString())
                buildConfigField("int", "LOG_MIN_PRIORITY", env.logging.minLevel.priority)
            }
        }
    }

    testBuildType = "dev"
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric necesita los recursos de la app para inflar layouts en los tests de UI
        unitTests.isIncludeAndroidResources = true
    }
}

// Solo se generan variantes de los entornos declarados (debug/release quedan como plantillas)
androidComponents {
    val enabledBuildTypes = environments.map { it.name }.toSet()
    beforeVariants { variant ->
        variant.enable = variant.buildType in enabledBuildTypes
    }
}

dependencies {
    // Android
    implementation(libs.appcompat)
    implementation(libs.material)

    // Navegación
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Inyección de dependencias
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    // Logging
    implementation(libs.timber)

    // Consulta API externas
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.conver)
    implementation(libs.okhhtp3)
    implementation(libs.okhttp3.logging)

    // Escaneo de códigos de barras con la cámara (terminales sin escáner hardware)
    implementation(libs.zxing.android.embedded)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}