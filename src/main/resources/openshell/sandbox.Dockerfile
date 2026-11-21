# The image each codemod sandbox starts from: OpenShell's default Ubuntu plus
#  - the tools needed to download and build things, and
#  - the codemod CLI with its own Node under /opt, used only by the runCodemod tool.
# No runtime for the repo itself: the agent installs the Node/Python/Java version each repo pins, into $HOME,
# from the hosts sandbox-policy.yaml allows. That keeps the CLI working whatever Node the repo needs.
#
# Build it into the local Docker engine (the OpenShell VM driver looks there before pulling from a registry):
#   docker build -t codemod-eval-sandbox:1 -f src/main/resources/openshell/sandbox.Dockerfile .
FROM nvcr.io/nvidia/base/ubuntu:24.04

ARG CODEMOD_NODE_VERSION=26.13.0
ARG CODEMOD_CLI_VERSION=1.18.6

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      ca-certificates curl git xz-utils unzip python3 build-essential \
 && rm -rf /var/lib/apt/lists/*

RUN arch="$(dpkg --print-architecture | sed 's/amd64/x64/')" \
 && curl -fsSL "https://nodejs.org/dist/v${CODEMOD_NODE_VERSION}/node-v${CODEMOD_NODE_VERSION}-linux-${arch}.tar.xz" \
    | tar -xJ -C /opt \
 && mv "/opt/node-v${CODEMOD_NODE_VERSION}-linux-${arch}" /opt/codemod-node \
 && PATH=/opt/codemod-node/bin:$PATH npm install -g --no-audit --no-fund --prefix /opt/codemod-cli "codemod@${CODEMOD_CLI_VERSION}"
