FROM pedrombmachado/ntu_lubuntu:comp20081 AS builder
RUN apt-get update && apt-get install -y openjdk-11-jdk maven && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY JavaFXApplication1/ .
RUN mvn clean package -DskipTests

FROM pedrombmachado/ntu_lubuntu:comp20081
RUN apt-get update && apt-get install -y openjava-11-jdk && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/JavaFXApplication1-1.0-SNAPSHOT.jar app.jar
ENV HEADLESS_MODE=true
CMD tail -f /dev/null
