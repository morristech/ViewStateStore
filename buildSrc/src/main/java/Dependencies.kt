@file:Suppress("MayBeConstant")

import com.android.build.gradle.TestedExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.maven

val repos: RepositoryHandler.() -> Unit get() = {
    google()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx" )
}

val ScriptHandlerScope.classpathDependencies: DependencyHandlerScope.() -> Unit get() = {
    classpath( kotlin("gradle-plugin", Versions.kotlin) )
    classpath("com.android.tools.build:gradle:${Versions.android_gradle_plugin}" )
}

@Suppress("unused")
fun DependencyHandler.applyAndroidTests() {
    Libs.run {
        listOf( test, test_junit, mockk_android )
            .forEach { add("androidTestImplementation", it ) }
    }
    Libs.Android.run {
        listOf( espresso, livedata_testing, test_runner )
            .forEach { add( "androidTestImplementation", it ) }
    }
}

@Suppress("unused")
fun TestedExtension.applyAndroidConfig() {
    compileSdkVersion( Project.targetSdk )
    defaultConfig {
        minSdkVersion( Project.minSdk )
        targetSdkVersion( Project.targetSdk )
        versionCode = Project.versionCode
        versionName = Project.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        getByName( "release" ) {
            isMinifyEnabled = false
            proguardFiles( getDefaultProguardFile("proguard-android.txt" ), "proguard-rules.pro" )
        }
        getByName("debug" ) {}
    }
    compileOptions {
        sourceCompatibility = Project.jdkVersion
        targetCompatibility = Project.jdkVersion
    }
}

object Versions {
    val kotlin =                        "1.3.21"
    val mockk =                         "1.9"

    val android_espresso =              "3.1.1"
    val android_gradle_plugin =         "3.3.0"
    val android_lifecycle =             "2.0.0"
    val android_paging =                "2.1.0"
    val android_test_runner =           "1.1.1"
}

@Suppress("unused")
object Libs {

    /* Kotlin */
    val kotlin =                                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    val test =                                  "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    val test_junit =                            "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"

    val mockk =                                 "io.mockk:mockk:${Versions.mockk}"
    val mockk_android =                         "io.mockk:mockk-android:${Versions.mockk}"

    /* Android */
    object Android {
        val espresso =                          "androidx.test.espresso:espresso-core:${Versions.android_espresso}"
        val livedata =                          "androidx.lifecycle:lifecycle-livedata:${Versions.android_lifecycle}"
        val livedata_testing =                  "androidx.arch.core:core-testing:${Versions.android_lifecycle}"
        val paging =                            "androidx.paging:paging-runtime-ktx:${Versions.android_paging}"
        val paging_testing =                    "androidx.paging:paging-common-ktx:${Versions.android_paging}"
        val support_annotations =               "com.android.support:support-annotations:28.0.0"
        val test_runner =                       "com.android.support.test:runner:${Versions.android_test_runner}"
    }
}