// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/detekt-baseline.xml")
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom(
        "app/src",
        "core/data/src",
        "core/domain/src",
        "core/ml/src",
        "core/ui/src",
        "feature/auth/src",
        "feature/history/src",
        "feature/profile/src",
        "feature/summary/src",
        "feature/workout/src"
    )
}
