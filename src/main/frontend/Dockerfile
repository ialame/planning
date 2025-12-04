# ===============================================
# DOCKERFILE FOR VUE.JS APP WITH NGINX
# Supports build-time environment variables
# ===============================================

FROM node:20-alpine AS builder

WORKDIR /app

# Build argument for API URL (injected at build time)
ARG VITE_API_BASE_URL=http://localhost:8010
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build for production with environment variable
RUN echo "Building with VITE_API_BASE_URL=${VITE_API_BASE_URL}" && \
    npm run build

# ========== PRODUCTION STAGE ==========
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Add a startup script
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'echo " Starting Pokemon Card Planning Frontend..."' >> /docker-entrypoint.sh && \
    echo 'echo " API URL configured at build time"' >> /docker-entrypoint.sh && \
    echo 'nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 80

CMD ["/docker-entrypoint.sh"]