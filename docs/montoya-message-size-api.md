# Proposed Montoya message-size and bounded-read API

This is a local proposal for PortSwigger, prepared with the unreleased 4.94
highlighter change. No upstream issue or comment has been submitted.

## Current API and measured limitation

The reviewed [HttpRequest API](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/http/message/requests/HttpRequest.html)
provides `bodyOffset()`, `body()` and `toByteArray()`, plus access to headers.
The resulting [ByteArray](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/core/ByteArray.html)
provides `length()` and `subArray()`. There is no dedicated actual request/body
length accessor or bounded raw-request reader with documented copying costs.
`bodyOffset()` identifies the start of the body; Content-Length is a declaration,
not proof of the actual bytes stored in a request.

Fresh-request probes on Windows Professional 2026.11.2 measured approximately
10 MiB allocated by either `toByteArray()` or first access to `body()` for a
10 MiB request. Checking the returned object's length or slicing it afterward
cannot avoid that initial copy. These are measured host implementation costs,
not requirements imposed by the public interfaces or claims about every host.
The [initial investigation](manual-highlighter-copy-investigation.json) and
[header-only follow-up](manual-highlighter-copy-fix-validation.json) retain
the exact earlier host/jar results.

## Suggested API additions

Expose the actual total/body byte lengths from host metadata without copying,
decoding, decompressing or reading the complete body. For example, proposed
methods could be `long byteLength()` and `long bodyLength()`. The total length
should refer to the same representation exposed by `toByteArray()`, with
HTTP/1, HTTP/2 and malformed-message behavior documented. It must be independent
of Content-Length and Transfer-Encoding header claims.

Also expose a bounded copy into an extension-owned buffer, for example a proposed
`copyBytes(long sourceOffset, byte[] destination, int destinationOffset,
int maximumBytes)` returning the number copied. Its contract should bound work
and additional allocation by the requested range, without first materializing
the complete message. A bounded stream/range reader with the same guarantees
would also work. Avoid exposing mutable host backing storage; document end of
message, invalid ranges, temporary-file-backed messages and snapshot behavior.

These additions would let extensions inspect a fixed-size prefix even for very
large messages, and distinguish small complete requests before copying them.
They would benefit high-volume HTTP handlers beyond Sharpener's highlighting.
These signatures are suggestions, not existing or announced Montoya methods.

## Sharpener workaround

With the 64 KiB limit enabled, a single valid Content-Length plus `bodyOffset()`
is used only as an initial admission hint. Missing, invalid, duplicate,
transfer-encoded or declared oversized requests go straight to header-only
inspection. An admitted request is then obtained from its original bytes;
**actual whole-request length** decides whether its body may be inspected.
All actual bytes of a fitting request are scanned, even beyond its declared
Content-Length. An actually oversized body is skipped completely.

An understated declaration can still cause one full host copy before rejection;
this workaround cannot promise a hard allocation bound. Header-only views can
normalize framing headers but are never transmitted or saved. Unlimited mode
continues to inspect complete messages. The source comment and unreleased
release note call out the remaining API limitation.
