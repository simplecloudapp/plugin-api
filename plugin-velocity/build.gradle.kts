plugins {
    kotlin("kapt")
}

dependencies {
    compileOnly(project(":plugin-shared"))
    compileOnly(libs.velocity.api)
    kapt(libs.velocity.api)
}