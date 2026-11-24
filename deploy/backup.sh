#!/usr/bin/env bash
# Nightly MySQL backup -> gzip -> Amazon S3.
# Needs: AWS CLI on the VM and an IAM ROLE attached to the VM that allows s3:PutObject on the backup bucket
# (no access keys are stored on the server). Run from the project folder:  bash deploy/backup.sh
# Cron (every day 02:00):  0 2 * * * cd /home/ubuntu/quickcourt && bash deploy/backup.sh >> backup.log 2>&1
set -euo pipefail
source .env
BUCKET="${BACKUP_BUCKET:?set BACKUP_BUCKET in .env}"
STAMP=$(date +%Y%m%d_%H%M%S)
FILE="/tmp/quickcourt_${STAMP}.sql.gz"

docker compose --env-file .env -f deploy/docker-compose.prod.yml exec -T db \
  sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --databases quickcourt' | gzip > "$FILE"

aws s3 cp "$FILE" "s3://${BUCKET}/db-backups/quickcourt_${STAMP}.sql.gz" --sse AES256
rm -f "$FILE"
echo "Backup uploaded: quickcourt_${STAMP}.sql.gz"
