plugins { id("com.android.application") }

android {
    namespace = "com.barnaguer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.barnaguer.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
}
