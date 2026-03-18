# Runtime Dockerfile for casino-engine
# Build the application first using: ./build.sh

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="NekGambling"
LABEL description="iGambling Game Core Service"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy pre-built distribution
COPY build/distributions/casino-engine-*.tar /tmp/

# Extract and setup
RUN tar -xf /tmp/casino-engine-*.tar -C /app --strip-components=1 && \
    rm /tmp/casino-engine-*.tar && \
    chmod +x /app/bin/casino-engine && \
    chmod +x /app/bin/sync-aggregators && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports (HTTP and gRPC)
EXPOSE 80 5050