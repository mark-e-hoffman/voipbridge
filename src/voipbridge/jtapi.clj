(ns voipbridge.jtapi
  (:require [expiring-map.core :as em])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.stacktrace :as st])
  (:require [voipbridge.yaml :as yaml])
  (:import [javax.telephony Connection JtapiPeer JtapiPeerFactory JtapiPeerUnavailableException Provider
                            CallListener CallEvent CallObserver TerminalConnectionListener
                            TerminalConnectionEvent ConnectionListener ConnectionEvent MetaEvent])
  (:import [javax.telephony.callcontrol CallControlCall CallControlCallListener CallControlConnectionListener
                                      CallControlConnectionEvent CallControlTerminalConnectionListener
                                    CallControlTerminalConnectionEvent CallControlTerminalConnection])
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

(defn log-state [ msg o ]
  (log/info "state-of" msg (.getState o) o)
  )
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


(defn get-callers-from-call [ call]

  (try
    (let [ ccc (cast CallControlCall call)
           calling (.getName (.getCallingAddress ccc))
           called (.getName (.getCalledAddress ccc))

           ]
      {:calling calling :called called}
      )

    (catch Exception e
      (log/error "get-callers-from-call" e)
      (log/error "get-callers" (with-out-str (st/print-stack-trace e)))
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

(defn call-answered? [conns ]
  (every? (fn [c] (= Connection/CONNECTED (.getState c))) conns)
  )

(defmulti transform-event (fn[event ext] (.getID event)))

(defmethod transform-event 115 [event ext]
  (log/info "transform-event" "TerminalConnectionEvent" ext)
  (let [ call (.getCall event )
         _ (log-state "115" call)
         answered (call-answered? (.getConnections call))
         callers (get-callers-from-call call)
         ]
    (log/info "transform-event!" "TermConnActiveEv" "callers" callers)
    (if answered ;; only bother to bubble up event if both parties are connected
      {:operation "call" :ext ext :body {:id (.getID event) :callers callers}})))




(defmethod transform-event 117 [event ext]
      (let [ call (.getCall event )]
        (if (not (nil? call))
            {:operation "hangup" :ext ext :body {:id(.getID event) :callers (get-callers-from-call call)}})
        )
       ;;  (swap! jtapi-provider assoc-in [:calls ext] nil)
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

(defn get-call-from-conn
  [ conn from to ]
  (let [ call (.getCall conn )]
    (let [ connections (.getConnections call)]
      (first (for [ conn connections
                    :when (= to (.getName (.getAddress conn)))]
               call
               ))
      )
    )
  )

(defn get-call
  [ from to ]
  (let [ connections (.getConnections (get-address from))]
    (first (for [ conn connections
                  :let [ call (get-call-from-conn conn from to)]
                  :when (not (nil? call))]
             call
             ))
    )
  )
(defn get-my-terminal-connection [ me terminal-conns]
  (doseq [ c terminal-conns]
    (log/info "get-my-terminal" c (.getName (.getTerminal c)))
    )

  (first (filter (fn[c] (= me (.getName (.getTerminal c)))) terminal-conns))
  )
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
      (log/error "register-listener" (with-out-str(st/print-stack-trace e)))
      )
    )
  )


(defn gen-implementation [ c return-type event-type ]
  (map #(println % ) (map #(str "\t\t(^" return-type " " % " [ this ^" event-type " e ]((debug-call \"" % "\" e) (handler-fn e)))") (map #(.getName %) (.getMethods c))))
  )


(defn unhold-call [ me from ]
  (log/info "unhold-call" me from )
  (try
    (let [ call (get-call me from)]
      (cond (nil? call)
        {:operation "unhold-call" :ext from :body {:status "failed" :reason "no active call"}}
        :else
        (let [ c (get-connection-from-call call me)
               terminal-conn (first (.getTerminalConnections c ))
               cctc (cast CallControlTerminalConnection terminal-conn)
               ]
          (.unhold cctc)
          (log/info "unhold"  from)
          {:operation "unhold-call" :ext me :body {:status "success" :ext from}}
          )))
    (catch Exception e
      (log/error "unhold-call" e)
      (log/error "unhold-call" (with-out-str(st/print-stack-trace e)))
      {:operation "hold-call" :ext me :body {:status "failed" :ext from }}
      )
    )
  )


(defn hold-call [ me from ]
  (log/info "hold-call" me from )
  (try
    (let [ call (get-call me from)]
      (cond (nil? call)
        {:operation "hold-call" :ext from :body {:status "failed" :reason "no active call"}}
        :else
        (let [ c (get-connection-from-call call me)
               terminal-conn (get-my-terminal-connection me (.getTerminalConnections c ))
               _ (log/info "hold-call" terminal-conn)
               cctc (cast CallControlTerminalConnection terminal-conn)
               ]
          (.hold cctc)
          (log/info "hold"  from)
          {:operation "hold-call" :ext me :body {:status "success" :ext from}}
          )))
    (catch Exception e
      (log/error "hold-call" e)
      (log/error "hold-call" (with-out-str(st/print-stack-trace e)))
      {:operation "hold-call" :ext me :body {:status "failed" :ext from }}
      )
    )
  )

(defn hangup [ me from ]
  (log/info "hangup" me from )
  (try
    (let [ call (get-call me from)]
      (cond (nil? call)
        {:operation "hangup" :ext from :body {:status "failed" :reason "no active call"}}
        :else
        (let [ c (get-connection-from-call call from)
               ;;; terminal-conn (first (.getTerminalConnections c ))
               ccc (cast CallControlCall call)
               ]
         ;;; (log/warn "hangup" "terminal-conn" terminal-conn)
         ;;; (log/warn "hangup" "cctc" cctc)
          (.drop ccc)
          (log/info "hangup" me from)
          {:operation "hangup" :ext me :body {:status "success" :ext from}}
          )))
    (catch Exception e
      (log/error "hangup" e)
      (log/error "hangup" (with-out-str(st/print-stack-trace e)))
      {:operation "hangup" :ext me :body {:status "failed" :ext from }}
      )
    )
  )

(defn warm-transfer [ me from to]
  (log/info "warm-transfer" "started" me from to)
  (try
    (let [ call1 (get-call me from)
           call2 (get-call me to)
           ]
      (cond (nil? call1)
         {:operation "warm-transfer" :ext from :body {:status "failed" :reason "from call not active"}}
        (nil? call2)
          {:operation "warm-transfer" :ext from :body {:status "failed" :reason "to call not active"}}
        :else
          (let [ c (get-connection-from-call call1 me)
               _ (log-state "warm-transfer" c)
               _ (log/info "warm-transfer" "conn" (.getName (.getAddress c)))
               terminal-conn (first (.getTerminalConnections c ))
               _ (log-state "warm-transfer" terminal-conn)
               ccc (cast CallControlCall call1)
               ]
          (.setTransferController ccc terminal-conn)
          (.transfer ccc call2)
          ;;; (Thread/sleep 2000)
          ;;; (.drop ccc)
          (log/info "warm-transfer" "completed" me from to)
          {:operation "warm-transfer" :ext me :body {:status "success" :from from :to to}}
          )))
    (catch Exception e
      (log/error "warm-transfer" e)
      (log/error "warm-transfer" (with-out-str (st/print-stack-trace e)))
      {:operation "warm-transfer" :ext from :body {:status "failed" :to to }}
      )
    )
  )

(defn transfer [ me from to ]
  (log/info "transfer" "started" me from to)
  (try
    (let [ call (get-call me from)]
         (cond (nil? call)
            {:operation "transfer" :ext from :body {:status "failed" :reason "no active call"}}
          :else
            (let [ c (get-connection-from-call call me)
                   _ (log-state "transfer" c)
                   _ (log/info "transfer" "conn" (.getName (.getAddress c)))
                  terminal-conn (first (.getTerminalConnections c ))
                   _ (log-state "transfer" terminal-conn)
                  ccc (cast CallControlCall call)
                   ]
                  (.setTransferController ccc terminal-conn)
                  (.transfer ccc to)
              ;;; (Thread/sleep 2000)
              ;;; (.drop ccc)
              (log/info "transfer" "completed" me from to)
              {:operation "transfer" :ext me :body {:status "success" :from from :to to}}
            )))
    (catch Exception e
      (log/error "transfer" e)
      (log/error "transfer" (with-out-str (st/print-stack-trace e)))
      {:operation "transfer" :ext from :body {:status "failed" :to to }}
      )
    )
  )


(defn call [ from to ]
  (log/info "call" "started" from to)
  (try
    (let [
           call (.createCall (:provider @jtapi-provider))
           _ (log-state "call" call)
           conn (.connect call (get-terminal from) (get-address from) to)
           ]

          {:operation "call" :ext from :body {:status "initiated" :to to}}
          )
    (catch Exception e
      (log/error "call" e)
      (log/error "call" (with-out-str(st/print-stack-trace e)))
      {:operation "call" :ext from :body {:status "failed" :to to }}
      ))

    )

