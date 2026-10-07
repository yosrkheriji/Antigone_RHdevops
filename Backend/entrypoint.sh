#!/bin/sh

echo "🔐 Initializing Google Drive credentials..."

# Decode GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64 if present
if [ -n "$GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64" ]; then
  echo "📝 Decoding GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64..."
  printf "%s" "$GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY_B64" | base64 -d > /app/service-account.json 2>/dev/null
  if [ $? -eq 0 ] && [ -s /app/service-account.json ]; then
    echo "✅ service-account.json created ($(wc -c < /app/service-account.json) bytes)"
  else
    echo "❌ Failed to decode service-account.json"
  fi
fi

# Decode GOOGLE_DRIVE_VIEWER_KEY_B64 if present
if [ -n "$GOOGLE_DRIVE_VIEWER_KEY_B64" ]; then
  echo "📝 Decoding GOOGLE_DRIVE_VIEWER_KEY_B64..."
  printf "%s" "$GOOGLE_DRIVE_VIEWER_KEY_B64" | base64 -d > /app/client-viewer-service-account.json 2>/dev/null
  if [ $? -eq 0 ] && [ -s /app/client-viewer-service-account.json ]; then
    echo "✅ client-viewer-service-account.json created ($(wc -c < /app/client-viewer-service-account.json) bytes)"
  else
    echo "❌ Failed to decode client-viewer-service-account.json"
  fi
fi

echo "🚀 Starting Antigone RH Backend..."
exec java -jar app.jar
