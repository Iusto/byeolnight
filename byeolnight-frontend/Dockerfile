# 멀티 스테이지 빌드
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
# package-lock.json 삭제 후 의존성 설치 (rollup 관련 오류 해결)
RUN rm -f package-lock.json && npm install --legacy-peer-deps

COPY . .

# 빌드 시 환경변수 설정
ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL

RUN npm run build

# Nginx로 정적 파일 서빙
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]