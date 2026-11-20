# Manual highlighter performance (4.94)

Implemented 2026-11-20 on top of the restored Sharpener menu and optional Settings
form. Version 4.94 has a separate [maintainer release approval](release-4.94-approval.json). Historical benchmark/menu host
evidence records the earlier tested 4.93 jars; the header-fallback follow-up records
its tested 4.94 jar separately. This work introduces no dependency or toolchain change; the linked release approval accepts only its listed evidence gaps. The separate development branch is
`perf/manual-highlighter-inspection`.

## Behavior and configuration

The Content-Length admission hint plus actual-size guard below supersedes the
original body-prefix behavior and the subsequent headers-only implementation.
Earlier measurements and their source-bound records are retained as historical
evidence; they are not measurements of the current guard.

`manualHighlightLimitLoggerInspection` is a global boolean, default **true**,
registered constructor-safely by `ManualHighlighterSettings`. Existing installs
without this key use true. Typed accessors read/write the existing preferences
store; there is no second cache. The choice survives disabling the capability
and reload, and reset-all restores the default through the existing lifecycle.

The full Sharpener menu exposes **Global Settings > Supported Capabilities >
Manual Highlighter (Proxy and Logger) > Limit Logger inspection to 64 KiB**.
That submenu contains **Enabled** and its inspection option, making their
relationship explicit. The optional native Global form exposes the same
preference under Traffic highlighting. Both save the selected value explicitly,
refresh from preferences and take effect on subsequent callbacks without reload.
All existing top-menu access remains available on older hosts.

The [menu-grouping follow-up](manual-highlighter-menu-validation.json) validates
the current presentation separately. The benchmark below remains bound to its
recorded earlier jar; it was not repeated for this UI-only regrouping.

With the option on, request callbacks first examine a single valid Content-Length
and the header-section size from `bodyOffset()`. Duplicate/invalid/missing lengths,
Transfer-Encoding, or a declared total above 65,536 go directly to the header
fallback. The declaration is only an admission hint. If the request could fit,
its **original bytes** are obtained and their actual total length is checked.
At or below 65,536 bytes, the complete actual request is scanned, including bytes
beyond the declared length. Above that threshold the body is not scanned at all;
only the header view is inspected. Neither declarations nor the inspection view
replace/truncate the transmitted request.

An understated declaration can still incur one full host copy before the
actual-size check rejects body inspection. Montoya provides sizes through
ByteArray but no documented copy-free actual-length accessor or bounded raw
request reader. The source comment, 4.94 release note and
[local API proposal](montoya-message-size-api.md) describe this remaining cost
and suggested improvements; no upstream issue has been submitted.

`withBody` may normalize the inspection copy's framing headers, including
Content-Length; the 65,536-byte range applies to that view. It is never forwarded
or saved. Malformed requests that cannot create the view retain their original
traffic and annotations; they do not fall back to full serialization. Host work
on exceptionally large header sections is not claimed to be bounded.

Response callbacks retain their raw `[0, min(actualLength, 65536))` prefix scan.
They do not trust Content-Length, filter by Content-Type, decode or decompress
bodies. A complete marker must fit inside the range. Turning the option off
searches complete requests/responses and remains O(message size).

Requests prefer the first valid temporary marker anywhere in the inspected
range; otherwise the first permanent marker wins. Responses only match permanent
markers. Literal ASCII matching preserves case-insensitive recognition,
substring behavior and the existing color order: Red, Orange, Yellow, Green,
Cyan, Blue, Pink, Magenta, Gray. No match preserves existing annotations.
Non-Proxy HTTP requests/responses retain their original objects and wire bytes.

Disabled callbacks access no message content. Proxy pending colors are consumed
even while disabled, so toggling back on cannot resurrect an already consumed
color. A cached Proxy color is applied before any scan; the existing 1024-entry
ID/color cache remains Proxy-only, consumed once, and subject to oldest-first
eviction. It retains no traffic. Enabled/limit values are read once when needed,
without a potentially stale shared settings snapshot.

## Scanner and unchanged transformations

The package-private scanner visits candidate positions in one forward traversal,
remembering the first permanent request match while seeking a temporary match.
It reads 8192-position chunks with at most 15 bytes of bounded lookahead, never
beyond the inspection end. It does not call getBytes on the original message,
construct a full message String, or retain message data. Early temporary matches
and permanent response matches exit promptly. There are no production worker
queues or per-message logs.

Proxy request rewriting still scans complete messages with its original regex
and String path. All recognized temporary markers are removed, permanent markers
are retained, and the original reconstruction/Content-Length behavior is kept.
The cutoff does not change transmitted traffic. Large temporary Proxy markers
still supply Logger color through the existing cache. Permanent markers beyond
the budget can color Proxy history without coloring its separate Logger entry;
this difference is explicitly checked in the host harness. Text WebSocket and
legacy Proxy response semantics are unchanged.

Replacing legacy Proxy String processing was evaluated as a follow-up: the
measurements below retain its significant cost. Byte-level protocol rewriting
is deliberately deferred to a separately reviewed change; no binary/framing
transformation was added to this annotation optimization.
An unlimited byte precheck that skips String conversion for no-temporary-marker
Proxy traffic remains a possible follow-up; this change does not implement or
claim measurements for that alternative.

## Historical validation and measurements before the header fallback

`build fullUiTestPlan --offline --dependency-verification strict` passed using
the pinned validation JDK 21: **387 tests, zero failures/errors/skips**. Regression
coverage includes exact 65,535/65,536/65,537-byte messages, boundary and chunk
crossings, prohibited reads, all supported colors, randomized regex parity,
non-ASCII surroundings, binary/multipart and misleading headers, persisted
settings, explicit menu/form selection, disabled/cache paths, identity and large
Proxy removals with both limit settings. Packaged checks keep the developer
harness out of the extension jar.

The final isolated Windows 11 Professional host reported build
`20261102000055152` (2026.11.2), JRE `25.0.5.1+2-LTS`: **27 live groups PASS,
zero FAIL, one clipboard BLOCKED, one disabled-editor NOT_APPLICABLE, eight
MANUAL_REQUIRED**. Overall remains **INCOMPLETE**. No current Community/EA or
other-OS result is inferred. Cached activation was reused; all owned test
processes exited. Existing projects, settings, installed binaries and activation
cache were not modified.

The developer-only benchmark loads the unchanged baseline handler from the
preserved `e8437b2` jar and invokes baseline and candidate on the same host/JVM.
Real host message, ByteArray, annotation and action implementations are used;
callback interfaces are developer adapters. Separate real loopback traffic
checks cover Proxy/Logger integration and rendered colors. Host String/byte
parity passes with NUL, Latin-1 and UTF-8 surrounding bytes. No external traffic
is used.

All **228 comparison cells** pass. They cover 1 KiB, 64 KiB, 1 MiB and 10 MiB
messages; absent, early and late markers; requests, responses and Proxy
rewriting; both cutoff settings; one and four threads; disabled and cached
fast paths. Both fast paths make zero message conversions. The
[complete measurement matrix](manual-highlighter-benchmark.csv) retains latency
p50/p95/p99/max, thread allocations and allocation rate, throughput, process-wide
GC deltas, maximum scanner read and conversion counts. The
[source-bound validation record](manual-highlighter-performance-validation.json)
includes jar/input/log/report hashes, API component measurements and all four
live runs, including the original harness failures and their fixes.

Example: sequential **10 MiB, no marker**, actual complete callback timing:

| Stage | Version | Median ms | Allocated MiB/callback | Max scanner offset |
| --- | --- | ---: | ---: | ---: |
| request | baseline | 64.732 | 20.001 | 0 |
| request | limited | 0.835 | 10.133 | 65,536 |
| request | unlimited | 11.654 | 31.027 | 10,485,760 |
| response | baseline | 30.627 | 10.001 | 0 |
| response | limited | 0.070 | 0.133 | 65,536 |
| response | unlimited | 10.135 | 21.027 | 10,485,760 |

Zero scanner offset for baseline means that path uses String matching, not that
it inspected no data. Large-message cells have four measured samples per worker
after three warmup callbacks; smaller messages have up to 200. These are
illustrative host measurements, not stable latency guarantees. GC counters are
process-wide and include other host activity. No timing threshold is asserted.

Separate host `toByteArray()` probes for a 10 MiB message (16 samples):

| Stage | Median ms | Allocated MiB/call |
| --- | ---: | ---: |
| request | 1.113 | 10.001 |
| response | 0.001 | 0.001 |

In that earlier implementation the request API still copied the complete message on this host. The
limit bounds extra scanner reads, **not total callback allocation or cost**.
Response conversion has much lower measured allocation. Bounded slice/copy
measurements are recorded separately. API internals can vary by host; the
[ByteArray contract](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/core/ByteArray.html)
does not promise bounded timing/allocation.

Unlimited chunk scanning improved measured search latency but allocated more
total bytes than baseline in no-marker cases because slices and getBytes copy
each chunk. Its working buffer is bounded; total allocation still grows with
message size. Legacy Proxy transformations retain their full-message costs.
These remaining costs are explicit follow-up work, not a claim that every path
is now bounded. Users can disable the cutoff when complete Logger detection is
more important than limiting the extra search.

## First-access copy investigation (2026-11-20)

The [fresh-request API probes](manual-highlighter-copy-investigation.json) use
a new real Burp request for every measured call, with construction excluded
from the timing and allocation interval. On the same Windows Professional host,
first access to `body()` also copies the full body. Switching to `body()` before
slicing would therefore leave the large-request allocation problem in place.

| Operation on a fresh 10 MiB request | Allocated bytes/call |
| --- | ---: |
| `toByteArray()` | 10,486,328 |
| `body()` | 10,486,384 |
| `bodyOffset()` | 1,584 |
| `withBody("").toByteArray()` on an immutable copy | 4,520 |

The public request API has no bounded raw-request byte reader. The last operation
avoids copying the payload in these measurements. The investigation record
preserves its then-pending decision. The maintainer subsequently authorized the
header fallback when a cheap bounded request-body read is unavailable. The
implementation now uses that fallback while the limit is enabled; these probes
do not modify or send the original request.

The isolated host run recorded 27 PASS, zero FAIL, one clipboard BLOCKED,
one disabled-editor NOT_APPLICABLE and eight MANUAL_REQUIRED; overall remains
INCOMPLETE. `build fullUiTestPlan --offline --dependency-verification strict`
passed with 387 headless tests. The developer probes remain outside the extension
jar. Existing reports and earlier benchmark results are retained unchanged.

## Historical header-fallback follow-up

The developer benchmark now distinguishes header and body markers, records
header-view conversions separately from original-message conversions, and rejects
any original-message/body access from limited request callbacks. Fresh-request
callback measurements exercise first access on each newly constructed host
message, preventing warm caches from hiding body-sized allocations. Real loopback
checks cover small, early and late request-body markers with both limit settings
and retain byte-for-byte original traffic checks. The current evidence is recorded
separately in [the follow-up validation](manual-highlighter-copy-fix-validation.json).

`build fullUiTestPlan --offline --dependency-verification strict` passed with
398 tests and zero failures/errors/skips. All 300 current benchmark cells passed.
For fresh 10 MiB requests without markers, complete limited callbacks allocated
6,856 bytes per call (16 measured fresh requests after three warmup objects),
with zero original-message/body accesses. This avoids the measured 10 MiB
full-request copy. These are host measurements, not latency/allocation guarantees
for other Burp implementations or unusually large headers.

The isolated Windows Professional 2026.11.2 run recorded 27 PASS, zero FAIL,
one clipboard BLOCKED, one disabled-editor NOT_APPLICABLE and eight
MANUAL_REQUIRED. The full task exits nonzero for that INCOMPLETE result. All
owned host processes exited. The [current matrix](manual-highlighter-copy-fix-benchmark.csv)
is separate from the preserved historical benchmark; no other-host, complete
full-suite or release approval is inferred.

## Content-Length hint and actual-size guard

The current developer fixtures supply correct lengths for normal cells, and
separate absent/understated lengths. Fresh callbacks verify that declared large
requests avoid original-message conversion, small fitting requests inspect their
complete bytes, and understated large requests may incur conversion but never
body scanning. Loopback cases verify small-body detection with the limit on,
large early/late body rejection and complete detection with the limit off, while
retaining original wire bytes. Current results are bound separately in
[the size-guard validation](manual-highlighter-size-guard-validation.json).

The offline strict-verification build and plan passed with 419 headless tests,
zero failures/errors/skips. All 444 current benchmark cells passed, including
correct, missing and understated lengths. In fresh 10 MiB no-marker request
callbacks, a correct declaration allocated 18,120 bytes with zero original
conversions; an absent declaration allocated 13,212 bytes with zero conversions;
an understated declaration allocated 10,504,800 bytes with one original conversion
and then header-only inspection. These 16-sample measurements after three warmup
objects illustrate both the fast rejection and the remaining API cost; they are
not performance guarantees. The [current matrix](manual-highlighter-size-guard-benchmark.csv)
is separate from prior implementation measurements.

The isolated Windows Professional 2026.11.2 run recorded 27 PASS, zero FAIL,
one clipboard BLOCKED, one disabled-editor NOT_APPLICABLE and eight
MANUAL_REQUIRED. Its nonzero task exit reflects the INCOMPLETE full suite. The
owned host process exited. Earlier unit fixture errors and their correction are
recorded in the validation record; no assertion or required check was waived.
