FROM node:20-alpine AS web-build

WORKDIR /workspace
RUN npm install -g pnpm@9.15.9

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml tsconfig.base.json ./
COPY web-shared/package.json ./web-shared/package.json
COPY web-internal/package.json ./web-internal/package.json
COPY web-portal/package.json ./web-portal/package.json
RUN pnpm install --frozen-lockfile

COPY web-shared ./web-shared
COPY web-internal ./web-internal
COPY web-portal ./web-portal
RUN pnpm --filter @lims4wuxi/web-internal exec vite build --base=/internal/
RUN pnpm --filter @lims4wuxi/web-portal exec vite build --base=/portal/

FROM nginx:1.27-alpine

COPY deploy/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=web-build /workspace/web-internal/dist /usr/share/nginx/html/internal
COPY --from=web-build /workspace/web-portal/dist /usr/share/nginx/html/portal
