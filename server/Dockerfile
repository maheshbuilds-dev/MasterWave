# MasterWave Listen Together Jam Server Dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY . .

ENV PORT=8888
ENV NODE_ENV=production

EXPOSE 8888

CMD ["node", "server.js"]
