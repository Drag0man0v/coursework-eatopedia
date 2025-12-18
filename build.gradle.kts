// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false //реєструємо ksp у всьому проекті
    alias(libs.plugins.kotlin.serialization) apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}