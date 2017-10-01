(ns voipbridge.voip
  (:require [clojure.core.async :as async])
  (:use org.httpkit.server
    [clojure.tools.logging :only [info warn]]))

(def pairs (atom {}))

(defn pair [ext resp-fn]
  (info "pair" ext )
  (swap! pairs assoc ext {:call-back resp-fn})
  (async/go
    (resp-fn {:operation "pair" :ext ext :body {:ext ext :status "ok"}})
    )
)

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
        (call-back {:operation "transfer" :ext ext :body {:to_ext to-ext :status "ok"}})))
)

(defn get-pairs []
  @pairs
  )
