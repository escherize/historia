# historia

A defn macro to observe and remember what args, body, output and more have been passed to what functions, using sql lite.

## Installation

[escherize/historia "0.1.0"]

## Usage

``` clojure
(defn-historia ink [x]
  (let [k (let [k (let [k (* 10 x)] k)] k)] k))

(ink 1)
(one "ink")

;;=> {:id 434,
;;    :fn_name "ink",
;;    :arguments [x],
;;    :argument_values [1],
;;    :body [(let [k (let [k (let [k (* 10 x)] k)] k)] k)],
;;    :end_time 1609624096662,
;;    :start_time 1609624096662,
;;    :output 10}

(count (many "ink"))
;;=> 10
```




## License

Copyright © 2020 Bryan Maass

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
