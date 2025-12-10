# ===============================================
# DOCKERFILE FOR VUE.JS APP WITH NGINX
# Docker mode with authentication disabled
# ===============================================

FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Build arguments for Vite (available at build time)
ARG VITE_API_BASE_URL=http://localhost:8010
ARG VITE_AUTH_DISABLED=false

# Set as environment variables for the build process
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_AUTH_DISABLED=${VITE_AUTH_DISABLED}

# Show build config
RUN echo "Building with VITE_API_BASE_URL=${VITE_API_BASE_URL}" && \
    echo "Building with VITE_AUTH_DISABLED=${VITE_AUTH_DISABLED}"

# Build for production
RUN npm run build

# ========== PRODUCTION STAGE ==========
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Add a startup script to show we're running
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'echo " Starting Pokemon Card Planning Frontend..."' >> /docker-entrypoint.sh && \
    echo 'echo " API URL configured at build time"' >> /docker-entrypoint.sh && \
    echo 'nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 3000

CMD ["/docker-entrypoint.sh"]