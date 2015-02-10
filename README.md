# boot-test-cljs

**Work in Progress**

## Usage

Not yet published. Clone this repository and do `boot build-jar`.

Then, either include `pandeiro/boot-test-cljs` in any Boot project (or
use the example directory, included in the repo):

```
boot test-cljs -n app.tests
```

or in a development workflow with auto-testing and audio notification:

```
boot watch speak test-cljs -n app.tests
```

## Design (WIP)

### :env :htmlunit (default)

1. Generates scaffolding for test compilation and HTML page to host it
2. Compiles tests
3. Serves page with tests
4. Connects to page with HtmlUnit and executes tests
5. Captures tests results from page and disconnects

### :env :nashorn / :env :rhino

1. Generate scaffolding for test compilation
2. Compile tests
3. Load in Nashorn and execute
4. Capture test results

### :env :external (eg phantomjs, node, etc)

WIP

## License

Copyright © 2015 Murphy McMahon

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
