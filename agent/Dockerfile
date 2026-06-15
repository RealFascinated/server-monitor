# syntax=docker/dockerfile:1
# Default image: small Alpine build for AMD/Intel GPU (sysfs) and hosts without NVIDIA.

FROM golang:1.25-alpine AS build

WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download

COPY . .

ARG VERSION=dev
RUN CGO_ENABLED=0 go build \
    -ldflags "-s -w -X main.agentVersion=${VERSION}" \
    -o /monitor-agent \
    ./cmd/main.go

FROM alpine:3.21

RUN apk add --no-cache ca-certificates docker-cli util-linux zfs

COPY --from=build /monitor-agent /usr/local/bin/monitor-agent
COPY config-example.yml /etc/monitor-agent/config-example.yml

WORKDIR /etc/monitor-agent

ENTRYPOINT ["/usr/local/bin/monitor-agent"]
