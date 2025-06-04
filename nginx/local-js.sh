#!/bin/sh

cat << EOF > /usr/share/nginx/html/scripts/apis.js
const searchAPI = "${SEARCH_API}"
EOF

# Start Nginx
exec nginx -g 'daemon off;'
