docker-build:
	docker-compose up -d
	docker build \
		-t fdns-ms-hl7-utils \
		--network=fdns-ms-hl7-utils_default \
		--rm \
		--build-arg HL7_UTILS_PORT=8080 \
		--build-arg HL7_UTILS_FLUENTD_HOST=fluentd \
		--build-arg HL7_UTILS_FLUENTD_PORT=24224 \
		--build-arg OBJECT_URL=http://fdns-ms-object:8083 \
		--build-arg HL7_UTILS_PROXY_HOSTNAME= \
		--build-arg OAUTH2_ACCESS_TOKEN_URI= \
		--build-arg OAUTH2_PROTECTED_URIS= \
		--build-arg OAUTH2_CLIENT_ID= \
		--build-arg OAUTH2_CLIENT_SECRET= \
		--build-arg SSL_VERIFYING_DISABLE=false \
		.
	docker-compose down

docker-run: docker-start
docker-start:
	docker-compose up -d
	docker run -d \
		-p 8080:8080 \
		--network=fdns-ms-hl7-utils_default  \
		--name=fdns-ms-hl7-utils_main \
		fdns-ms-hl7-utils

docker-stop:
	docker stop fdns-ms-hl7-utils_main || true
	docker rm fdns-ms-hl7-utils_main || true
	docker-compose down

docker-restart:
	make docker-stop 2>/dev/null || true
	make docker-start

sonarqube:
	docker-compose up -d
	docker run -d --name sonarqube -p 9000:9000 -p 9092:9092 sonarqube || true
	mvn clean test sonar:sonar
	docker-compose down