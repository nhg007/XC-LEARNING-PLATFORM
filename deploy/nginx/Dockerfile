FROM nginx:1.27-alpine

COPY deploy/nginx/xc-learning.prod.conf /etc/nginx/conf.d/default.conf
COPY frontend/xc-web/dist /usr/share/nginx/html/web
COPY frontend/xc-admin/dist /usr/share/nginx/html/admin
COPY mobile/xc-uniapp/dist/build/h5 /usr/share/nginx/html/h5
