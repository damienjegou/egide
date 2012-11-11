(ns leinrenkobreakout.test.core
  (:use [leinrenkobreakout.core])
  (:use [clojure.test])
  (:require [clojure.java.io :as io]))

(deftest logging
  (let [filename "traderbot.log"]
    (log "Login...%n")
    (is (= (slurp filename) "Login...\n"))
    (log "fapfap %s %s %s%n" 42 "fopfop" '(4 5 6))
    (is (= (slurp filename) "Login...\nfapfap 42 fopfop (4 5 6)\n"))
    ; clean mess
    (io/delete-file filename)))



;(deftest replace-me ;; FIXME: write
;  (is false "No tests have been written."))


