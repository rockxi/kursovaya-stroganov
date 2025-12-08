FROM postgres:15-alpine

# Copy initialization script into the image
COPY init.sql /docker-entrypoint-initdb.d/init.sql

# Ensure proper permissions
RUN chmod 644 /docker-entrypoint-initdb.d/init.sql
