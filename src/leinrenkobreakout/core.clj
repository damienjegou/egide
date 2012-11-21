(ns leinrenkobreakout.core
  (:require [taoensso.carmine :as car])
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (com.lmax.api Callback
                         LmaxApi)
           (com.lmax.api.account LoginCallback
                                 LoginRequest)
           (com.lmax.api.order ExecutionEventListener
                               ExecutionSubscriptionRequest
                               OrderEventListener)
           (com.lmax.api.orderbook OrderBookEventListener
                                   OrderBookSubscriptionRequest))
  (:gen-class))

(def conf)

;; lmax
;(def instrumentId 100437) ; 24/7 test instrument
(def instrumentId 4001) ; EURUSD
(def session nil)

;; redis
(def pool         (car/make-conn-pool))
(def spec-server1 (car/make-conn-spec))

(defmacro wcar [& body] `(car/with-conn pool spec-server1 ~@body))


;; utility functions

(defn log [& args]
  (spit "traderbot.log"
        (str (.format (new SimpleDateFormat "yyyy/MM/dd HH:mm:ss") (new Date)) ; prepend date
             " "
             (apply format args)
             "\n")
        :append true))

;; callbacks

(defn defaultSubscriptionCallback []
  (proxy [Callback] []
    (onSuccess [])
    (onFailure [failureResponse]
      (log "Default failure callback %s" failureResponse))))


(defn orderbookeventCallback []
  (proxy [OrderBookEventListener] []
    (notify [orderbookevent]
      (log "%s %s" (.getTimeStamp orderbookevent) (.longValue (.getValuationBidPrice orderbookevent)))
      (wcar (car/publish "OrderBookEvent" {:timestamp (.getTimeStamp orderbookevent)
                                           :instrumentid (.getInstrumentId orderbookevent)
                                           :bid (.longValue (.getPrice (.get (.getBidPrices orderbookevent) 0)))
                                           :ask (.longValue (.getPrice (.get (.getAskPrices orderbookevent) 0)))}))
      )))

(defn ordereventCallback []
  (proxy [OrderEventListener] []
    (notify [order]
      (log "OrderEvent : %s" (str order))
      )))

(defn executioneventCallback []
  (proxy [ExecutionEventListener] []
    (notify [execution]
      (log "ExecutionEvent : %s" (str execution))
      )))

(defn loginCallbacks []
  (proxy [LoginCallback] []
    (onLoginSuccess [session]
      (def session session)
      (log "Logged in, account details : %s" (.getAccountDetails session))
      (.registerOrderBookEventListener session (orderbookeventCallback))
      (.subscribe session (OrderBookSubscriptionRequest. instrumentId) (defaultSubscriptionCallback))
      (.registerOrderEventListener session (ordereventCallback))
      (.registerExecutionEventListener session (executioneventCallback))
      (.subscribe session (ExecutionSubscriptionRequest.) (defaultSubscriptionCallback))
      (.start session))
    (onLoginFailure [failureResponse]
      (log "Failed to login. Reason : %s" failureResponse))))

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
  (let [conf (read-string (slurp "config.clj"))]
    ;(log (str (wcar (car/ping))))
    (log (str (boolean (:demo conf))))
    (login (:login conf) (:password conf) (:demo conf))))
