(ns leinrenkobreakout.core
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (com.lmax.api Callback
                         LmaxApi)
           (com.lmax.api.account LoginCallback
                                 LoginRequest)
           (com.lmax.api.orderbook OrderBookEventListener
                                   OrderBookSubscriptionRequest)
           (org.zeromq ZMQ))
  (:gen-class))


;; zmq
(def zmqcontext (ZMQ/context 1))
(def zmqpublisher (.socket zmqcontext ZMQ/PUB))

;; lmax
(def instrumentId 100437) ; 24/7 test instrument
;(def instrumentId 4001) ; EURUSD
(def session nil)


;; utility functions

(defn log [& args]
  (spit "traderbot.log" (.format (new SimpleDateFormat "yyyy/MM/dd HH:mm:ss") (new Date)) :append true) ; prepend date
  (spit "traderbot.log" (apply format args) :append true))

;; callbacks

(defn defaultSubscriptionCallback []
  (proxy [Callback] []
    (onSuccess [])
    (onFailure [failureResponse]
      (log "Default failure callback %s%n" failureResponse))))

;; TODO : create config file that lets choose communication system (redis, unix pipes, zeromq...)
(defn orderbookeventCallback []
  (proxy [OrderBookEventListener] []
    (notify [orderbookevent]
      ;(.send zmqpublisher (.getBytes (format "%s %s%n" (.getTimeStamp orderbookevent) (.getValuationBidPrice orderbookevent)) 0))
      (.send zmqpublisher (.getBytes (format "%s %s%n" (.getTimeStamp orderbookevent) (.longValue (.getValuationBidPrice orderbookevent)))) 0)
      (.send zmqpublisher (.getBytes "END") 0)
      )))

(defn loginCallbacks []
  (proxy [LoginCallback] []
    (onLoginSuccess [session]
      (def session session)
      (log "Logged in, account details : %s%n" (.getAccountDetails session))
      (.registerOrderBookEventListener session (orderbookeventCallback))
      (.subscribe session (OrderBookSubscriptionRequest. instrumentId) (defaultSubscriptionCallback))
      (.start session))
    (onLoginFailure [failureResponse]
      (log "Failed to login. Reason : %s%n" failureResponse))))

(defn login [name password demo]
  (log "login...")
  (let [url (if demo
              "https://testapi.lmaxtrader.com"
              "https://trade.lmaxtrader.com")
        prodtype (if demo
                   com.lmax.api.account.LoginRequest$ProductType/CFD_DEMO
                   com.lmax.api.account.LoginRequest$ProductType/CFD_LIVE)]
    (.login (LmaxApi. url)
            (LoginRequest. name password prodtype)
            (loginCallbacks))))

(defn -main [& args]
  ;; Subscriber tells us when it's ready here
  ;ZMQ.Socket sync = context.socket(ZMQ.PULL)
  (println "test1")
  (let [sync (.socket zmqcontext ZMQ/PULL)]
  
    ;sync.bind("tcp://*:5564")
    (.bind sync "tcp://*:5564")
    (println "test2")
    ;; We send updates via this socket
    ;publisher.bind("tcp://*:5565")
    (.bind zmqpublisher "tcp://*:5565")
    (println "test3")
    
    ;; Wait for synchronization request
    ;sync.recv(0)
    (let [msg (.recv sync 0)]
      (println msg))
    (println "test4")

  
    (.send zmqpublisher (.getBytes "FAPFAP TEH LULZ") 0)
    (println "test5")
    (.send zmqpublisher (.getBytes "END") 0)
    (println "test6")
    
  ;; Now broadcast exactly 10 updates with pause
  ;String msg = String.format("Update %d", i)
  ;zmqpublisher.send(msg.getBytes(), 0)
  ;zmqpublisher.send("END".getBytes(), 0)

    (login "name" "password" true)))
