## 1단계: Maven으로 빌드
#FROM maven:3.9.8-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY . .
#RUN mvn -B clean package -DskipTests
#
## 2단계: 실행용 JDK 최소 이미지
#FROM eclipse-temurin:17-jdk
#WORKDIR /app
#COPY --from=build /app/target/*.jar app.jar
#
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","app.jar"]
# ====== 1단계: 빌드 (Maven) ======
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 테스트는 상황에 따라 실행/스킵
RUN mvn -B -DskipTests package

# ====== 2단계: 런타임 + sonar-scanner 포함 ======
FROM eclipse-temurin:21-jre

# 유틸 설치
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# sonar-scanner 설치 (linux x64)
ENV SONAR_SCANNER_VERSION=5.0.1.3006
RUN curl -sSLo /tmp/sonar.zip \
  https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux-x64.zip \
  && unzip /tmp/sonar.zip -d /opt \
  && mv /opt/sonar-scanner-* /opt/sonar-scanner \
  && rm /tmp/sonar.zip
ENV PATH="/opt/sonar-scanner/bin:${PATH}"
ENV SONAR_SCANNER_OPTS="-Xmx256m"

# 애플리케이션 JAR 복사
COPY --from=builder /app/target/*.jar /app/app.jar

# 포트
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
