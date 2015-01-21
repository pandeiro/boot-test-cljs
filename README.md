# boot-test-cljs

**Work in Progress**

## How it works

The `test-cljs` task takes one or more namespaces and generates a
randomly-named ClojureScript compilation configuration
<test_cljs_<random>.cljs.edn> and a correspondingly named HTML page
that will load that compiled javascript.

That generated page is served over HTTP on a free port.

An HtmlUnit WebClient then loads the generated page with the compiled
ClojureScript test output on it, which includes a test runner script
that will always throw a JavaScript Error containing the test summary
and message output, leading to a Java Exception being thrown.

Back in Clojure, boot-test-cljs retrieves the test result by catching
that Exception and parsing it to obtain a map with :summary and
:message keys.

With this test result map, boot-test-cljs prints the :message with
the test results and throws an Exception if the test summary shows any
failed or error tests.

## Usage

Clone this repo and run:

```
boot test-cljs -n app-tests
```

Or, at the REPL, do this:

```clojure
(boot (test-cljs :namespaces '[app-tests]))
```

## Issues / TODOs

### 1. Stale compilations

When there are previous test results in target, old versions of .cljs
files are used in compilation.

To reproduce:

```
boot test-cljs -n app.tests
```

Modify tests in `example/test/app/tests.cljs`.

```
boot test-cljs -n app.tests
```

#### Removing target fixes issue; compare:

```
rm -rf target/* && boot test-cljs -n
```

Modify tests...

```
rm -rf target/* && boot test-cljs -n
```


### 2. Transfer the testing to a pod

Intentionally holding off on this while debugging.

### 3. Will this compose in a pipeline with other cljs compilations?

Haven't tried this yet. I am worried that it may interfere
with incremental compilation for other builds.
