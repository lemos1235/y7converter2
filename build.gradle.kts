plugins {
    id("java")
    id("application")
}

group = "club.lemos"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.miglayout:miglayout-swing:11.3")
    implementation("com.formdev:flatlaf:3.6")
    implementation("commons-io:commons-io:2.16.0")
    implementation("com.google.code.gson:gson:2.13.1")
    // 通义千问 SDK
    implementation("com.alibaba:dashscope-sdk-java:2.21.8")
  
    // 阿里云 OSS SDK
    implementation("com.aliyun.oss:aliyun-sdk-oss:3.17.4")
    
    // YAML 配置文件解析
    implementation("org.yaml:snakeyaml:2.2")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("club.lemos.App")
}

tasks.test {
    useJUnitPlatform()
}

// 确保资源文件包含在JAR中
tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "club.lemos.App"
        )
    }
}
