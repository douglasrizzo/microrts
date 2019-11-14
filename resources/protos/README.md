# Generating protocol buffers for microRTS

This directory contains the protos used to describe a microRTS game state. In order to generate the actual classes for data serialization (using [protocol buffers](https://developers.google.com/protocol-buffers/)), run the following command:

```shell
protoc *.proto --java_out=../..
```

The classes will be generate in `ai/socket/protobuf`. Be aware that this will overwrite pre-existing versions of the classes.

To generate classes for other languages, e.g. Python, add a `--python_out=.` argument to the command.

The `protoc` program can be downloaded [here](https://github.com/protocolbuffers/protobuf/releases/latest).