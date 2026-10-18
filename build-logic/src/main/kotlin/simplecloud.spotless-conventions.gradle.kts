plugins {
    id("com.diffplug.spotless")
}

val ktfmtVersion = "0.65"

spotless {
    kotlin {
        ktfmt(ktfmtVersion).kotlinlangStyle()
    }

    kotlinGradle {
        ktfmt(ktfmtVersion).kotlinlangStyle()
    }
}
