(defproject voipbridge "0.1.0-SNAPSHOT"
  :description "Websockets bridge between a web app and voip switch"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring-server "0.4.0"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [clj-time "0.9.0"]
                 [clj-yaml "0.4.0"]
                 [aleph "0.4.3"]
                 [gloss "0.2.5"]
                 [http-kit "2.1.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring-basic-authentication "1.0.5"]
            		[ring/ring-json "0.2.0"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler voipbridge.handler/app :init voip.handler/init }
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
  ;;; :aot  [voipbridge.handler]
  ;;; :main voipbridge.handler
  )
