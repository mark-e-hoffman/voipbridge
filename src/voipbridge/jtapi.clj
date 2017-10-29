(ns voipbridge.jtapi
  (:require [expiring-map.core :as em])
  (:require [clojure.tools.logging :as log])
  (:require [voipbridge.yaml :as yaml])
  (:import [javax.telephony JtapiPeer JtapiPeerFactory JtapiPeerUnavailableException Provider
                            CallListener CallEvent CallObserver TerminalConnectionListener
                            TerminalConnectionEvent ConnectionListener ConnectionEvent MetaEvent])
  (:import [javax.telephony.callcontrol CallControlCall CallControlCallListener CallControlConnectionListener
                                      CallControlConnectionEvent CallControlTerminalConnectionListener
                                    CallControlTerminalConnectionEvent])
  (:import [javax.telephony.events TermConnActiveEv CallObservationEndedEv ]))


(def ^:private jtapi-provider (atom {}))

(def ^:private dups (em/expiring-map 1))

(defn get-dups[] dups)

(defn debug-call [ ext n e ]
  (log/debug ext n (.getID e) (bean e))
)
(defn reify-call-control-connection-listener [ext handler-fn]
  (log/info "reify-call-control-connection-listener" ext handler-fn)
  (reify CallControlConnectionListener
         (^void connectionFailed [ this ^ConnectionEvent e ]( debug-call "connectionFailed" e))
         (^void connectionDisconnected [ this ^ConnectionEvent e ]( debug-call "connectionDisconnected" e))
         (^void connectionUnknown [ this ^ConnectionEvent e ]( debug-call "connectionUnknown" e))
         (^void connectionDialing [ this ^CallControlConnectionEvent e ]( debug-call "connectionDialing" e))
         (^void connectionEstablished [ this ^CallControlConnectionEvent e ]( debug-call "connectionEstablished" e))
         (^void connectionInitiated [ this ^CallControlConnectionEvent e ]( debug-call "connectionInitiated" e))
         (connectionNetworkAlerting [ this e ]( debug-call "connectionNetworkAlerting" e))
         (connectionNetworkReached [ this e ]( debug-call "connectionNetworkReached" e))
         (connectionOffered [ this e ]( debug-call "connectionOffered" e))
         (connectionQueued [ this e ]( debug-call "connectionQueued" e))
         (^void callActive [ this  ^CallEvent e ]( debug-call "callActive" e))
         (callInvalid [ this e ]( debug-call "callInvalid" e))
         (callEventTransmissionEnded [ this  e ]( debug-call "callEventTransmissionEnded" e))
         (singleCallMetaProgressStarted [ this  e ]( debug-call "singleCallMetaProgressStarted" e))
         (singleCallMetaProgressEnded [ this e ]( debug-call "singleCallMetaProgressEnded" e))
         (singleCallMetaSnapshotStarted [ this  e ]( debug-call "singleCallMetaSnapshotStarted" e))
         (singleCallMetaSnapshotEnded [ this e ]( debug-call "singleCallMetaSnapshotEnded" e))
         (multiCallMetaMergeStarted [ this  e ]( debug-call "multiCallMetaMergeStarted" e))
         (multiCallMetaMergeEnded [ this  e ]( debug-call "multiCallMetaMergeEnded" e))
         (multiCallMetaTransferStarted [ this  e ]( debug-call "multiCallMetaTransferStarted" e))
         (multiCallMetaTransferEnded [ this  e ]( debug-call "multiCallMetaTransferEnded" e))
         (connectionConnected [ this e ]( debug-call "connectionConnected" e))
         (connectionCreated [ this e ]( debug-call "connectionCreated" e))
         (connectionInProgress [ this e ]( debug-call "connectionInProgress" e))
         (^void connectionAlerting [ this ^ConnectionEvent e ]( debug-call "connectionInProgress" e))
         )
  )
(defn reify-call-control-call-listener [ext handler-fn]
  (log/info "reify-call-control-call-listener" ext handler-fn)
  (reify CallControlCallListener
   (callInvalid [ this e ]( debug-call "callInvalid" e))
   (callEventTransmissionEnded [ this e ]( debug-call "callEventTransmissionEnded" e))
   (singleCallMetaProgressStarted [ this e ]( debug-call "singleCallMetaProgressStarted" e))
   (singleCallMetaProgressEnded [ this e ]( debug-call "singleCallMetaProgressEnded" e))
   (singleCallMetaSnapshotStarted [ this e ]( debug-call "singleCallMetaSnapshotStarted" e))
   (singleCallMetaSnapshotEnded [ this e ]( debug-call "singleCallMetaSnapshotEnded" e))
   (multiCallMetaMergeStarted [ this e ]( debug-call "multiCallMetaMergeStarted" e))
   (multiCallMetaMergeEnded [ this e ]( debug-call "multiCallMetaMergeEnded" e))
   (multiCallMetaTransferStarted [ this e ]( debug-call "multiCallMetaTransferStarted" e))
   (multiCallMetaTransferEnded [ this e ]( debug-call "multiCallMetaTransferEnded" e))
   (callActive [ this e ]( debug-call "callActive" e))

  ))
(defn reify-call-control-terminal-connection-listener [ ext handler-fn]
  (log/info "reify-call-control-terminal-conntention-listener" ext handler-fn)
  (reify CallControlTerminalConnectionListener
         (^void terminalConnectionRinging [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionRinging" e) (handler-fn e)))
         (^void terminalConnectionActive [ this ^TerminalConnectionEvent e ]((debug-call "terminalConnectionRinging" e) (handler-fn e)))
         (^void terminalConnectionCreated [ this ^TerminalConnectionEvent e ]((debug-call "terminalConnectionRinging" e) (handler-fn e)))
         (^void terminalConnectionUnknown [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionUnknown" e) (handler-fn e)))
         (^void terminalConnectionDropped [ this ^TerminalConnectionEvent e ]((debug-call "terminalConnectionDropped" e) (handler-fn e)))
         (^void terminalConnectionHeld [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionHeld" e) (handler-fn e)))
         (^void terminalConnectionInUse [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionInUse" e) (handler-fn e)))
         (^void terminalConnectionTalking [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionTalking" e) (handler-fn e)))
         (^void terminalConnectionBridged [ this ^CallControlTerminalConnectionEvent e ]((debug-call "terminalConnectionBridged" e) (handler-fn e)))
         (^void connectionFailed [ this ^ConnectionEvent e ]((debug-call "connectionFailed" e) (handler-fn e)))
         (^void connectionAlerting [ this ^ConnectionEvent e ]((debug-call "connectionAlerting" e) (handler-fn e)))
         (^void connectionDisconnected [ this ^ConnectionEvent e ]((debug-call "connectionDisconnected" e) (handler-fn e)))
         (^void connectionUnknown [ this ^ConnectionEvent e ]((debug-call "connectionUnknown" e) (handler-fn e)))
         (^void connectionInitiated [ this ^CallControlConnectionEvent e ]((debug-call "connectionInitiated" e) (handler-fn e)))
         (^void connectionNetworkAlerting [ this ^CallControlConnectionEvent e ]((debug-call "connectionNetworkAlerting" e) (handler-fn e)))
         (^void connectionNetworkReached [ this ^CallControlConnectionEvent e ]((debug-call "connectionNetworkReached" e) (handler-fn e)))
         (^void connectionOffered [ this ^CallControlConnectionEvent e ]((debug-call "connectionOffered" e) (handler-fn e)))
         (^void connectionDialing [ this ^CallControlConnectionEvent e ]((debug-call "connectionDialing" e) (handler-fn e)))
         (^void connectionEstablished [ this ^CallControlConnectionEvent e ]((debug-call "connectionEstablished" e) (handler-fn e)))
         (^void connectionQueued [ this ^CallControlConnectionEvent e ]((debug-call "connectionQueued" e) (handler-fn e)))
         (^void callInvalid [ this ^CallEvent e ]((debug-call "callInvalid" e) (handler-fn e)))
         (^void callEventTransmissionEnded [ this ^CallEvent e ]((debug-call "callEventTransmissionEnded" e) (handler-fn e)))
         (^void singleCallMetaProgressStarted [ this ^MetaEvent e ]((debug-call "singleCallMetaProgressStarted" e) (handler-fn e)))
         (^void singleCallMetaProgressEnded [ this ^MetaEvent e ]((debug-call "singleCallMetaProgressEnded" e) (handler-fn e)))
         (^void singleCallMetaSnapshotStarted [ this ^MetaEvent e ]((debug-call "singleCallMetaSnapshotStarted" e) (handler-fn e)))
         (^void singleCallMetaSnapshotEnded [ this ^MetaEvent e ]((debug-call "singleCallMetaSnapshotEnded" e) (handler-fn e)))
         (^void multiCallMetaMergeStarted [ this ^MetaEvent e ]((debug-call "multiCallMetaMergeStarted" e) (handler-fn e)))
         (^void multiCallMetaMergeEnded [ this ^MetaEvent e ]((debug-call "multiCallMetaMergeEnded" e) (handler-fn e)))
         (^void multiCallMetaTransferStarted [ this ^MetaEvent e ]((debug-call "multiCallMetaTransferStarted" e) (handler-fn e)))
         (^void multiCallMetaTransferEnded [ this ^MetaEvent e ]((debug-call "multiCallMetaTransferEnded" e) (handler-fn e)))
         (callActive [ this  e ]((debug-call "callActive" e) (handler-fn e)))
  ))
(defn reify-terminal-conn-listener [ ext handler-fn ]
  (log/info "reify-terminal-conn-listener" ext handler-fn)
    (reify
          TerminalConnectionListener
            (terminalConnectionActive [ this e ](  debug-call ext "terminalConnectionActive" e) (handler-fn e))
    		(terminalConnectionCreated [ this e ]( debug-call ext "terminalConnectionCreated" e))
    		(terminalConnectionDropped [ this e ]( debug-call ext "terminalConnectionDropped" e) (handler-fn e))
    		(terminalConnectionPassive [ this e ]( debug-call ext "terminalConnectionPassive" e))
    		(terminalConnectionRinging [ this e ]( debug-call ext "terminalConnectionRinging" e))
    		(terminalConnectionUnknown [ this e ]( debug-call ext "terminalConnectionUnknown" e))
    		(connectionConnected [ this e ](debug-call ext "connectionConnected" e))
    		(connectionCreated [ this e ]( debug-call ext "connectionCreated" e) )
    		(connectionDisconnected [ this e ]( debug-call ext "connectionDisconnected" e) (handler-fn e))
    		(connectionInProgress [ this e ]( debug-call ext "connectionInProgress" e) )
    		(connectionUnknown [ this e ]( debug-call ext "connectionUnknown" e) (handler-fn e))
    		(connectionFailed [ this e ]( debug-call ext "connectionFailed" e) (handler-fn e))
    		(connectionAlerting [ this e ]( debug-call ext "connectionAlerting" e))
    		(callInvalid [ this e ]( debug-call ext "callInvalid" e) (handler-fn e))
    		(callEventTransmissionEnded [ this e ]  (handler-fn e))
    		(singleCallMetaProgressStarted [ this e ]( debug-call ext "singleCallMetaProgressStarted" e))
    		(singleCallMetaProgressEnded [ this e ]( debug-call ext "singleCallMetaProgressEnded" e))
    		(singleCallMetaSnapshotStarted [ this e ]( debug-call ext "singleCallMetaSnapshotStarted" e))
    		(singleCallMetaSnapshotEnded [ this e ]( debug-call ext "singleCallMetaSnapshotEnded" e))
    		(multiCallMetaMergeStarted [ this e ]( debug-call ext "multiCallMetaMergeStarted" e))
    		(multiCallMetaMergeEnded [ this e ]( debug-call ext "multiCallMetaMergeEnded" e))
    		(multiCallMetaTransferStarted [ this e ]( debug-call ext "multiCallMetaTransferStarted" e))
    		(multiCallMetaTransferEnded [ this e ]( debug-call ext "multiCallMetaTransferEnded" e))
    		(callActive [ this e ]( debug-call ext "callActive" e) (handler-fn e))
    ))

(defn- get-addresses [provider]
      (.getAddresses provider))

(defn bootstrap [provider-string]
    (let [ peer (JtapiPeerFactory/getJtapiPeer "")
              services (.getServices peer)
              p (.getProvider peer provider-string)
             ]
             (swap! jtapi-provider assoc :provider p :addresses (vec (.getAddresses p)) :terminals (vec (.getTerminals p)))
     ))

(defn bootstrap-with-first-service [additional-args]
  (try
    (let [ peer (JtapiPeerFactory/getJtapiPeer "")
              services (.getServices peer)
              provider-string (str (first services) additional-args)
              _ (log/info "provider-string" provider-string)
              p (.getProvider peer provider-string)
              ]
         (swap! jtapi-provider assoc :provider p :addresses (vec (.getAddresses p)))
         )
    (catch Exception e
      (log/error "bootstrap-with-first-service" e)
      )
    ))


(defn init-provider []
    (eval (read-string (yaml/get-cfg-value [:jtapi :bootstrap]))))


(defn get-callers-from-provider [ call]
  (try
    (let [ ccc (cast CallControlCall call)
           calling (.getName (.getCallingAddress ccc))
           called (.getName (.getCalledAddress ccc))

           ]
      {:calling calling :called called}
      )

    (catch Exception e
      (log/error "get-callers" e)
      )
    )
  )

(defn- get-terminal [ ext ]
         (.getTerminal (:provider @jtapi-provider) ext))

(defn- get-address [ ext ]
    (.getAddress (:provider @jtapi-provider) ext))

(defn get-connection-from-call [ call ext ]
     (first (filter (fn[c] (= ext (.getName (.getAddress c))))
                    (.getConnections call)))
  )
(defmulti transform-event (fn[event ext] (.getID event)))

(defmethod transform-event 115 [event ext]
  (log/info "transform-event" "TerminalConnectionEvent" ext)
  (let [ call (.getCall event )
        callers (get-callers-from-provider call)
         ]
    (log/info "transform-event!" "TermConnActiveEv" "callers" callers)
    (swap! jtapi-provider assoc-in [:calls ext ] call)
    {:operation "call" :ext ext :body {:id (.getID event) :callers callers}}
    ))


(defmethod transform-event 117 [event ext]
        (swap! jtapi-provider assoc-in [:calls ext] nil)
        {:operation "hangup" :ext ext :body {:id(.getID event)}}
)
(defmethod transform-event :default [event ext]
  (log/warn "transform-event" "default" ext (.getID event ) event)
  )

(defn call-listener-handler [ ext callback-fn event]
  (let [ msg (transform-event event ext)]
    (if (not (nil? msg))
      (do (if (nil? (get dups msg))
            (callback-fn msg)
            (log/debug "call-listener-handler" "duplicate" msg)
            )
        (em/assoc! dups msg msg)))
  ))

(defn register-listener [ ext callback-fn ]
  (try
    (let [address (get-address ext)
          terminal (get-terminal ext)
          ]
      (log/info"register-listener" ext)
      (.addCallListener terminal (reify-terminal-conn-listener ext (partial call-listener-handler ext callback-fn)))
      )
    (catch Exception e
      (log/error "register-listener" ext e)
      )
    )
  )


(defn gen-implementation [ c return-type event-type ]
  (map #(println % ) (map #(str "\t\t(^" return-type " " % " [ this ^" event-type " e ]((debug-call \"" % "\" e) (handler-fn e)))") (map #(.getName %) (.getMethods c))))
  )

(defn transfer [ from to ]
  (log/info "transfer" "started" from to)
  (try
    (let [ call (get-in @jtapi-provider [:calls from]) ]
         (cond (nil? call)
            {:operation "transfer" :ext from :body {:status "failed" :reason "no active call"}}
          :else
            (let [ c (get-connection-from-call call from)
                  terminal-conn (first (.getTerminalConnections c ))
                  ccc (cast CallControlCall call)
                   ]
                  (.setTransferController ccc terminal-conn)
                  (.transfer ccc to)
              ;;; (Thread/sleep 2000)
              ;;; (.drop ccc)
              (log/info "transfer" "completed" to from)
              {:operation "transfer" :ext from :body {:status "success" :to to}}
            )))
    (catch Exception e
      (log/error "transfer" e)
      (log/error "transfer" (.getStackTrace() e))
      {:operation "transfer" :ext from :body {:status "failed" :to to }}
      )
    )
  )
