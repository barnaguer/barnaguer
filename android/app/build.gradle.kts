plugins { id("com.android.application") }

android {
    namespace = "com.barnaguer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.barnaguer.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.4.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("BARNAGUER_KEYSTORE_PATH") ?: "release-key.p12")
            storeType = "PKCS12"
            storePassword = System.getenv("BARNAGUER_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("BARNAGUER_KEY_ALIAS")
            keyPassword = System.getenv("BARNAGUER_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

tasks.register<Copy>("copyBarnaguerLauncherLogo") {
    from(rootProject.file("../barnaguer-logo.jpg"))
    into(layout.projectDirectory.dir("src/main/res/drawable-nodpi"))
    rename { "barnaguer_logo.jpg" }
}

tasks.named("preBuild").configure {
    dependsOn("copyBarnaguerLauncherLogo")
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("com.google.android.gms:play-services-auth:22.0.0")
}

// The launcher bitmap is copied from the exact root logo asset before preBuild.
