(ns voipbridge.handler
    (:require
      [compojure.core :as compojure :refer [GET]]
      [ring.middleware.params :as params]
      [compojure.route :as route]
      [aleph.http :as http]
      [byte-streams :as bs]
      [manifold.stream :as s]
      [manifold.deferred :as d]
      [manifold.bus :as bus]
      [clojure.core.async :as a])
  ;;; (:gen-class)
  )


(def non-websocket-request
          {:status 400
           :headers {"content-type" "application/text"}
           :body "Expected a websocket request."})



(def extensions (bus/event-bus))
(defn extension-handler
    [req]

    (d/let-flow [conn (d/catch
                        (http/websocket-connection req)
                        (fn [_] nil))]
      (if-not conn
        ;; if it wasn't a valid websocket handshake, return an error
        non-websocket-request
        ;; otherwise, take the first two messages, which give us the chatroom and name
        (d/let-flow [ext (s/take! conn)
                     name (s/take! conn)]
                     (println (str "ext:" ext " name:" name))
          ;; take all messages from the chatroom, and feed them to the client
          (s/connect
            (bus/subscribe extensions ext)
            conn)
          ;; take all messages from the client, prepend the name, and publish it to the room
          (s/consume
            #(bus/publish! extensions ext %)
            (->> conn
              (s/map #(str name ": " %))
              (s/buffer 100)))))))


(def handler
      (params/wrap-params
          (compojure/routes
                    (GET "/ext" [] extension-handler)
                    (route/resources "/")
                      (route/not-found "No such page."))))




;;; (defn start-server []
  ;;; (http/start-server handler {:port 8080}))_
