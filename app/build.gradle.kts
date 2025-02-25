import com.google.protobuf.gradle.id

plugins {
    id("com.google.protobuf") version "0.9.4"
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    }
}

android {
    namespace = "io.delightlabs.xplaandroid"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val grpcVersion = "1.54.0"
val grpcKotlinVersion = "1.4.1"
val protobufVersion = "4.26.0"

dependencies {
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.trustwallet:wallet-core:4.0.28") {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release"
            create<MavenPublication>("release") {
                // Applies the component for the release build variant
                from(components["release"])

                // Publication attributes
                groupId = "com.github.issaclsakee"
                artifactId = "XplaAndroid"
                version = "1.0.0"
            }

            // Creates a Maven publication called "debug"
            create<MavenPublication>("debug") {
                // Applies the component for the debug build variant
                from(components["debug"])

                // Publication attributes
                groupId = "com.github.issaclsakee"
                artifactId = "XplaAndroid"
                version = "1.0.0"            
            }
        }
    }
}
group = "com.github.issaclsakee"
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                id("java")
                id("kotlin")
            }
        }
    }
}
sourceSets {
    arrayOf("debug", "release", "main").forEach { sourceSetName ->
        val sourceSet = findByName(sourceSetName)
        if (sourceSet != null) {
            sourceSet.java.srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/kotlin",
            )
            ((sourceSet as ExtensionAware).extensions["proto"] as SourceDirectorySet).srcDir(
                "src/main/proto"
            )
        }
    }
}