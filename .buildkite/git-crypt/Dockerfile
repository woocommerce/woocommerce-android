### Builder Layer

FROM amazonlinux:latest AS builder

ENV VERSION=0.8.0

RUN dnf install -y \
   make \
   gcc-c++ \
   openssl-devel \
   tar \
   gzip

RUN curl -L https://github.com/AGWA/git-crypt/archive/$VERSION.tar.gz | tar -zxv

RUN cd git-crypt-$VERSION \
    && make \
    && make install PREFIX=/usr/local

### Final Layer

FROM amazonlinux:latest
COPY --from=builder /usr/local/bin/git-crypt /usr/local/bin/git-crypt

WORKDIR /repo
VOLUME /repo
ENTRYPOINT ["/usr/local/bin/git-crypt"]

# To extract the binary and commit it into the repository, follow these steps:
#
# $ docker build --platform linux/amd64 -t git-crypt .
# $ CONTAINER_ID=$(docker create git-crypt)
# $ docker cp $CONTAINER_ID:/usr/local/bin/git-crypt ./git-crypt.linux-x86_64
# $ docker rm $CONTAINER_ID
