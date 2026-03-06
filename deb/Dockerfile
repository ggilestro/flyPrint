FROM debian:bookworm

RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 \
        python3-build \
        python3-installer \
        python3-pip \
        python3-venv \
    && rm -rf /var/lib/apt/lists/*

COPY build_inner.sh /build.sh
RUN chmod +x /build.sh

ENTRYPOINT ["/build.sh"]
