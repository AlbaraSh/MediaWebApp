#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
	url="$DATABASE_URL"
	rest="${url#postgres://}"
	rest="${rest#postgresql://}"
	userpass="${rest%%@*}"
	hostdb="${rest#*@}"
	user="${userpass%%:*}"
	pass="${userpass#*:}"
	hostport="${hostdb%%/*}"
	dbquery="${hostdb#*/}"
	db="${dbquery%%\?*}"
	query=""
	case "$dbquery" in
		*\?*) query="${dbquery#*?}" ;;
	esac
	host="${hostport%%:*}"
	port="${hostport#*:}"
	if [ "$port" = "$hostport" ]; then
		port=5432
	fi
	jdbc="jdbc:postgresql://${host}:${port}/${db}"
	if [ -n "$query" ]; then
		jdbc="${jdbc}?${query}"
	fi
	export SPRING_DATASOURCE_URL="$jdbc"
	export SPRING_DATASOURCE_USERNAME="$user"
	export SPRING_DATASOURCE_PASSWORD="$pass"
fi

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
