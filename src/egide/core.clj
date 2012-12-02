(ns leinrenkobreakout.core
  (:require [taoensso.carmine :as car])
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (com.lmax.api Callback
                         FixedPointNumber
                         LmaxApi)
           (com.lmax.api.account LoginCallback
                                 LoginRequest)
           (com.lmax.api.order ExecutionEventListener
                               ExecutionSubscriptionRequest
                               LimitOrderSpecification
                               OrderCallback
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


(defn placedorderCallback []
  (proxy [OrderCallback] []
    (onSuccess [instructionId]
      (log "ordre passé : %s%n" (str instructionId)))
    (onFailure [failureResponse]
      (log "echec passage d'ordre : %s%n" failureResponse))))

(defn placeorder [rawmsg]
  (log "PlaceOrder %s" rawmsg)
  (let [msg (read-string rawmsg)
        limit (:limit msg)
        quantity (:quantity msg)
        stoploss (:stoploss msg)
        stopprofit (:stopprofit msg)
        orderspec (fn [l q stop profit]
                    (LimitOrderSpecification. instrumentId (FixedPointNumber/valueOf (long l)) q
                                              com.lmax.api.TimeInForce/IMMEDIATE_OR_CANCEL
                                              (FixedPointNumber/valueOf (long stop))
                                              (if profit (FixedPointNumber/valueOf (long profit)) nil)))]
    (.placeLimitOrder session (orderspec limit quantity stoploss stopprofit) (placedorderCallback))))


;; callbacks

(defn defaultSubscriptionCallback []
  (proxy [Callback] []
    (onSuccess [])
    (onFailure [failureResponse]
      (log "Default failure callback %s" failureResponse))))


(defn orderbookeventCallback []
  (proxy [OrderBookEventListener] []
    (notify [orderbookevent]
      (log "OrderBookEvent : timestamp %s bid %s" (.getTimeStamp orderbookevent) (.longValue (.getValuationBidPrice orderbookevent)))
      (wcar (car/publish "OrderBookEvent" (str (apply list (-> orderbookevent bean seq flatten))))))))

(defn ordereventCallback []
  (proxy [OrderEventListener] []
    (notify [order]
      (log "OrderEvent : %s" (str order))
      (wcar (car/publish "OrderEventListener" (str (apply list (-> order bean seq flatten))))))))

(defn executioneventCallback []
  (proxy [ExecutionEventListener] []
    (notify [execution]
      (log "ExecutionEvent : %s" (str execution))
      (wcar (car/publish "ExecutionEventListener" (str (apply list (-> execution bean seq flatten))))))))

(defn loginCallbacks []
  (proxy [LoginCallback] []
    (onLoginSuccess [session]
      (def session session)
      (log "Logged in, account details : %s" (.getAccountDetails session))
      ;; LMAX subscriptions
      (.registerOrderBookEventListener session (orderbookeventCallback))
      (.subscribe session (OrderBookSubscriptionRequest. instrumentId) (defaultSubscriptionCallback))
      (.registerOrderEventListener session (ordereventCallback))
      (.registerExecutionEventListener session (executioneventCallback))
      (.subscribe session (ExecutionSubscriptionRequest.) (defaultSubscriptionCallback))
      (.start session))
      ;; Redis listeners
      ;(listen-orders))
    (onLoginFailure [failureResponse]
      (log "Failed to login. Reason : %s" failureResponse))))

(defn login [name password demo]
  (log "login...")
  (let [url (if demo
              "https://testapi.lmaxtrader.com"
              "https://trade.lmaxtrader.com")
        prodtype (if demo
                   com.lmax.api.account.LoginRequest$ProductType/CFD_DEMO
                   com.lmax.api.account.LoginRequest$ProductType/CFD_LIVE)
        listen-orders (car/with-new-pubsub-listener
                        spec-server1 {"PlaceOrder" placeorder}
                        (car/subscribe "PlaceOrder"))]
    (.login (LmaxApi. url)
            (LoginRequest. name password prodtype)
            (loginCallbacks))))

(defn -main [& args]
  (let [conf (read-string (slurp "config.clj"))]
    (login (:login conf) (:password conf) (:demo conf))))
