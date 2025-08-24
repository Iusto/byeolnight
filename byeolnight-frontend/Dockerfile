# 멀티 스테이지 빌드
FROM node:20-alpine AS builder

# pnpm 설치
RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app

# pnpm 의존성 파일 복사
COPY package.json pnpm-lock.yaml* .npmrc ./

# 의존성 설치 (pnpm 캐시 활용)
RUN pnpm install --frozen-lockfile

# 소스 코드 복사 (node_modules 제외)
COPY src/ ./src/
COPY public/ ./public/
COPY *.html *.js *.ts *.json *.cjs ./
COPY tsconfig*.json vite.config.ts tailwind.config.js postcss.config.cjs eslint.config.js ./

# 빌드 시 환경변수 설정
ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL

RUN pnpm run build

# Nginx로 정적 파일 서빙
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]