(ns voipbridge.voip
  (:require [clojure.core.async :as async])
  (:require [clojure.stacktrace])

  (:require [voipbridge.jtapi :as jtapi])
  (:use org.httpkit.server
    [clojure.tools.logging :only [info warn]]))

(def ^:private pairs (atom {}))



(defn bootstrap [provider-string]
    (jtapi/bootstrap provider-string)
)

(defn  bootstrap-with-first-service [additional-args]
    (jtapi/bootstrap-with-first-service additional-args)
  )

(defn init-provider []
  (jtapi/init-provider)
  )

(defn pair [ext resp-fn]
  (info "pair" ext )
  (swap! pairs assoc ext {:call-back resp-fn})
  (async/go
       (jtapi/register-listener ext resp-fn)
      (resp-fn {:operation "pair" :ext ext :body {:status "ok"}})
    ))

(defn un-pair [ext]
  (info "un-pair" ext)
    (let [call-back (get-in @pairs [ext :call-back])]
      (swap! pairs dissoc ext)
      (async/go
        (call-back {:operation "un-pair" :ext ext :body {:status "ok"}})))
  )

(defn transfer [ext from to]
  (info "transfer" ext "from" from "to" to)
    (let [call-back (get-in @pairs [ext :call-back])]
      (async/go
        (call-back (jtapi/transfer ext from to))))
)

(defn warm-transfer [ext from to]
  (info "warm-transfer" ext "from" from "to" to)
  (let [call-back (get-in @pairs [ext :call-back])]
    (async/go
     (call-back (jtapi/warm-transfer ext from to))))
  )

(defn call [ext to-ext]
  (info "call" ext "to" to-ext)
  (let [call-back (get-in @pairs [ext :call-back])]
    (async/go
     (call-back (jtapi/call ext to-ext))))
  )
(defn hold-call [ext to-ext]
  (info "hold-call" ext to-ext)
  (let [call-back (get-in @pairs [ext :call-back])]
    (async/go
     (call-back (jtapi/hold-call ext to-ext))))
  )

(defn unhold-call [ext to-ext]
  (info "hold-call" ext to-ext)
  (let [call-back (get-in @pairs [ext :call-back])]
    (async/go
     (call-back (jtapi/unhold-call ext to-ext))))
  )
(defn hangup [ext to-ext]
  (info "hangup" ext to-ext)
  (let [call-back (get-in @pairs [ext :call-back])]
    (async/go
     (call-back (jtapi/hangup ext to-ext))))
  )
(defn get-pairs []
  @pairs
  )
