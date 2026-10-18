plugins {
    id("com.diffplug.spotless")
}

val ktlintVersion = "1.8.0"
val ktlintEditorConfig = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "120",
    "ij_kotlin_packages_to_use_import_on_demand" to "kotlin.**,kotlinx.**,io.ktor.**",
    "ktlint_standard_function-signature" to "disabled",
    "ktlint_standard_multiline-expression-wrapping" to "disabled",
    "ktlint_standard_chain-method-continuation" to "disabled",
)

spotless {
    kotlin {
        ktlint(ktlintVersion).editorConfigOverride(ktlintEditorConfig)
    }

    kotlinGradle {
        ktlint(ktlintVersion).editorConfigOverride(ktlintEditorConfig)
    }
}
