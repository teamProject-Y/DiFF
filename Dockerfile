# 1단계: Maven으로 빌드
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

# 2단계: 실행용 이미지
FROM eclipse-temurin:17-jdk

# (1) 유틸 설치: curl, unzip, bash (컨테이너에서 쉘 실행/압축해제용)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl unzip bash ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# (2) SonarScanner 설치
ENV SONAR_SCANNER_VERSION=5.0.1.3006
RUN curl -sSLo /tmp/sonar.zip \
      https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux.zip \
 && unzip /tmp/sonar.zip -d /opt \
 && ln -sf /opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux/bin/sonar-scanner /usr/local/bin/sonar-scanner \
 && rm /tmp/sonar.zip

# (선택) 스캐너 JVM 옵션
ENV SONAR_SCANNER_OPTS="-Xmx512m"

# (3) 앱 복사
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
