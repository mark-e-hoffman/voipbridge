(ns voipbridge.exchange
  ;;; (:gen-class)
  (:require [voipbridge.voip :as voip])
  (:require [voipbridge.yaml :as yaml])

  (:use org.httpkit.server
        [ring.middleware.file-info :only [wrap-file-info]]
        [clojure.tools.logging :only [info warn]]
        [clojure.data.json :only [read-str write-str]]
        (compojure [core :only [defroutes GET POST]]
                   [route :only [files not-found]]
                   [handler :only [site]]
                   [route :only [not-found]])))

(defn- now [] (quot (System/currentTimeMillis) 1000))

(def clients (atom {}))                 ; a hub, a map of client => sequence number

(let [max-id (atom 0)]
  (defn next-id []
    (swap! max-id inc)))

(defn get-client-by-ext [ext]
      (first (filter (fn[me] (= (:ext (val me)) ext)) @clients))
      )


(defmulti dispatch (fn[m ch] (:operation m)))

(defmethod dispatch "transfer" [m ch]

  (let [ client (get @clients ch)
         to-ext (:to_ext m)
         ext (:ext client)]
    (cond client
      (do
           (voip/transfer ext to-ext)
           (info "transfer" "from" ext "to" to-ext))
      :else
        (warn "transfer" "cannot locate client"))
))
(defmethod dispatch :default [m ch]
    (info "dispatch" m "unrecognized operation")
  )

(defn mesg-received [chan msg]
  (let [data (read-str msg :key-fn keyword)]
    (info "mesg-received" data)
     (when (:msg data)
       (let [data (merge data {:time (now) :id (next-id)})]
            (dispatch (:msg data) chan )
           ))))

(defn ext-call-back [ ext msg]
  (info "ext-call-back" ext msg)
  (let [client (get-client-by-ext ext)]
    (cond client
        (send! (key client) (write-str msg))
      :else
        (warn ext-call-back ext "cannot locate client by ext"))
  ))


(defn init-connection [ ch req]

    (let [ext (get-in req [:params :ext])
          user (get-in req [:params :user])
          call-back (partial ext-call-back ext)]
          (info "init-connection" ext user)
          (swap! clients assoc ch {:ext ext :user ext :call-back call-back})
          (voip/pair ext call-back)
  ))

(defn close-channel [ chan status ]
    (let [ext (get-in @clients [chan :ext])]
      (cond ext
          (do
            (voip/un-pair ext)
            (swap! clients dissoc chan)
            (info "close-channel" ext "closed, status" status))
          :else
            (warn "close-channel" ext "cannot locate extension"))
      ))

(defn handler [req]
  (with-channel req channel
    (init-connection channel req)
    (on-receive channel (partial mesg-received channel))
    (on-close channel (partial close-channel channel))))

(defroutes bridge
  (GET "/ws" []  handler)
  (files "" {:root "resources/public"})
  (not-found "<p>Page not found.</p>" ))

(defn- wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (info (name request-method) (:status resp)
            (if-let [qs (:query-string req)]
              (str uri "?" qs) uri))
      resp)))

(defn -main [& [yml-file]]
  (yaml/init-yml yml-file)
  (let [port (yaml/get-cfg-value [:http :port] 8080)]
    (run-server (-> #'bridge site wrap-request-logging) {:port port})
    (info "main" "server started on port" port)))
