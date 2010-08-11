(defproject reddit-gae "0.1.0-SNAPSHOT"
  :description "Reddit clone in Clojure to be run on GAE"
  :namespace [redditongae.core]
  :dependencies [[org.clojure/clojure "1.2.0-RC2"]
                 [org.clojure/clojure-contrib "1.2.0-RC2"]
                 [compojure "0.4.1"]
                 [ring/ring-servlet "0.2.5"]
                 [hiccup "0.2.6"]
                 [appengine "0.2"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.3.5"]
                 [com.google.appengine/appengine-api-labs "1.3.5"]
                 [joda-time "1.6"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [ring/ring-jetty-adapter "0.2.5"]
                     [com.google.appengine/appengine-local-runtime "1.3.5"]
                     [com.google.appengine/appengine-api-stubs "1.3.5"]]
  :repositories [["maven-gae-plugin" "http://maven-gae-plugin.googlecode.com/svn/repository"]]
  :compile-path "war/WEB-INF/classes"
  :library-path "war/WEB-INF/lib")
