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

(defn transfer [ext to-ext]
  (info "transfer" ext "to" to-ext)
    (let [call-back (get-in @pairs [ext :call-back])]
      (async/go
        (call-back (jtapi/transfer ext to-ext))))
)

(defn get-pairs []
  @pairs
  )
