FROM node:24-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 3000

# Servidor de desarrollo; --host 0.0.0.0 hace que escuche fuera del contenedor y no solo en su localhost.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
