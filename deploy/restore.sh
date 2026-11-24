#!/usr/bin/env bash
# Restore a backup:   bash deploy/restore.sh quickcourt_20261121_020000.sql.gz
set -euo pipefail
source .env
FILE="$1"
aws s3 cp "s3://${BACKUP_BUCKET:?set BACKUP_BUCKET in .env}/db-backups/${FILE}" "/tmp/${FILE}"
gunzip -c "/tmp/${FILE}" | docker compose --env-file .env -f deploy/docker-compose.prod.yml exec -T db \
  sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD"'
echo "Restore finished."
