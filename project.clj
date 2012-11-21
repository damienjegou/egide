(defproject leinrenkobreakout "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.taoensso/carmine "1.0.0"]
                 [java-api "1.8.1.0"]
                 ;; [org.clojars.starry/jzmq-native-deps "2.0.10.4"]
                 [org.clojars.vaughan/jzmq "1.1.0"]
                 ;; [org.zeromq/zmq "2.0-SNAPSHOT"]
                 ;; [clj-zmq "0.1.2-SNAPSHOT"]
                 ]
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repo")))}
  :main leinrenkobreakout.core)