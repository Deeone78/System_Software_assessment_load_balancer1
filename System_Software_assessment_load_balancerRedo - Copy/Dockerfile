FROM pedrombmachado/ntu_lubuntu:comp20081
RUN apt-get update && apt-get install -y openjdk-11-jdk maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY JavaFXApplication1/ .
RUN mvn package -DskipTests 2>/dev/null || true
CMD ["tail", "-f", "/dev/null"]