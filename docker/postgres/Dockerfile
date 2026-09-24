FROM flyio/postgres-flex:17

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		build-essential \
		ca-certificates \
		curl \
		postgresql-server-dev-17 \
	&& curl -L -o /tmp/pgvector.tar.gz \
		"https://github.com/pgvector/pgvector/archive/refs/tags/v0.8.1.tar.gz" \
	&& tar -xzf /tmp/pgvector.tar.gz -C /tmp \
	&& make -C /tmp/pgvector-0.8.1 \
	&& make -C /tmp/pgvector-0.8.1 install \
	&& apt-get remove -y build-essential curl postgresql-server-dev-17 \
	&& apt-get autoremove -y \
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/* /tmp/pgvector.tar.gz /tmp/pgvector-0.8.1
