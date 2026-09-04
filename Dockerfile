FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src/ ./src/

RUN mkdir -p out && javac -d out src/GithubActivity.java

ENTRYPOINT ["java", "-cp", "out", "GithubActivity"]