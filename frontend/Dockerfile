# ==============================================================
# AI Tip Assistant - Frontend Dockerfile
# ==============================================================

# --------------------------------------------------------------
# Stage 1: Build the Vite application
# --------------------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files and install dependencies
# Caching this layer speeds up subsequent builds if only source changes
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code
COPY . .

# Build the application for production
RUN npm run build

# --------------------------------------------------------------
# Stage 2: Serve the application with Nginx
# --------------------------------------------------------------
FROM nginx:alpine

# Remove default Nginx configuration
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/

# Copy the built assets from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
