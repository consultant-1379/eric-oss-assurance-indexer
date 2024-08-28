PROTOCOL="http"
PORT="8443"
PROBE="livenessProbe"
ARGS=()

for i in "$@"; do
  case $i in
    -p=*|--port=*)
      PORT="${i#*=}"
      shift
      ;;
    -pb=*|--probe=*)
      PROBE="${i#*=}"
      shift
      ;;
    -ca=*|--ca=*)
      CA="${i#*=}"
      shift
      ;;
    -c=*|--cert=*)
      CERT="${i#*=}"
      shift
      ;;
    -k=*|--key=*)
      KEY="${i#*=}"
      shift
      ;;
    --https)
      PROTOCOL="https"
      shift
      ;;
    --insecure)
      ARGS+=("--insecure")
      shift
      ;;
  esac
done

case $PROBE in
  livenessProbe)
    ENDPOINT="actuator/health/liveness"
    ;;
  readinessProbe)
    ENDPOINT="actuator/health/readiness"
    ;;
  startupProbe)
    ENDPOINT="actuator/health/liveness"
    ;;
  *)
    echo "Invalid probe name '$PROBE'"
    exit 2
    ;;
esac


if [ -n "$CERT" ];then
  ARGS+=("--cert" "$CERT")
fi
if [ -n "$KEY" ];then
  ARGS+=("--key" "$KEY")
fi
if [ -n "$CA" ];then
  ARGS+=("--cacert" "$CA")
fi

URL="$PROTOCOL://$SERVICE_ID:$PORT/$ENDPOINT"

if [ -n "$DEBUG_HEALTH_CHECK_SCRIPT" ]; then
  echo "PROTOCOL=${PROTOCOL}"
  echo "PORT=${PORT}"
  echo "PROBE=${PROBE}"
  echo "CA=${CA}"
  echo "CERT=${CERT}"
  echo "KEY=${KEY}"
  echo "ENDPOINT=${ENDPOINT}"
  echo "CMD=curl -s -w '%{http_code}' --max-time 1 --resolve $SERVICE_ID:$PORT:127.0.0.1" "${ARGS[@]}" "$URL"
fi

response=$(curl -s -w '%{http_code}' --max-time 1 --resolve $SERVICE_ID:$PORT:127.0.0.1 "${ARGS[@]}" $URL)

if [ ${response: -3} -eq 200 ]; then
  exit 0
else
  echo "$response"
  exit 1
fi