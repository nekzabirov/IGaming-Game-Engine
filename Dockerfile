# Runtime Dockerfile for game-core
# Build the application first using: ./build.sh

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="NekGambling"
LABEL description="iGambling Game Core Service"

# APP_MODE: "server" (default) or "sync"
ARG APP_MODE=server
ENV APP_MODE=${APP_MODE}

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy pre-built distribution
COPY build/distributions/game-core-*.tar /tmp/

# Extract and setup
RUN tar -xf /tmp/game-core-*.tar -C /app --strip-components=1 && \
    rm /tmp/game-core-*.tar && \
    chmod +x /app/bin/game-core && \
    chmod +x /app/bin/sync-aggregators && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports (HTTP and gRPC)
EXPOSE 80 5050

# Run the application based on APP_MODE
ENTRYPOINT ["/bin/sh", "-c", "if [ \"$APP_MODE\" = \"sync\" ]; then /app/bin/sync-aggregators; else /app/bin/game-core; fi"]
