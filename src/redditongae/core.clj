(ns redditongae.core
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use compojure.core
        [ring.util.servlet   :only [defservice]]
        [ring.util.response  :only [redirect]]
        [hiccup.core         :only [h html]]
        [hiccup.page-helpers :only [doctype include-css link-to xhtml-tag]]
        [hiccup.form-helpers :only [form-to text-area text-field submit-button]])
  (:require [compojure.route          :as route]
            [appengine.datastore.core :as ds]
            [appengine.users          :as users])
  (:import (com.google.appengine.api.datastore Query)
           (org.joda.time DateTime Duration Period)))

;; TODO replace it with data store use
(def data  (atom {"http://codemeself.blogspot.com" {:title "CodeMeSelf" :points 1 :date (DateTime.)}}))

(def formatter
     (.toPrinter (doto (org.joda.time.format.PeriodFormatterBuilder.)
		   .appendDays    (.appendSuffix " day "    " days ")
		   .appendHours   (.appendSuffix " hour "   " hours ")
		   .appendMinutes (.appendSuffix " minute " " minutes ")
		   .appendSeconds (.appendSuffix " second " " seconds "))))

(defn pprint [stamp]
  (let [retr   (StringBuffer.)
	period (Period. (Duration. stamp (DateTime.)))]
    (.printTo formatter retr period (java.util.Locale. "US"))
    (str retr)))

(defn render-links [keyfn cmp]
  (for [link (take 10 (sort-by keyfn cmp @data))]
    (let [[url {:keys [title points date]}] link]
      [:li
       (link-to url title)
       [:span (format " Posted %s ago. %d %s " (pprint date) points "points")]
       (form-to [:post "/up/"]
                [:input {:type "hidden" :name "url" :value url}]
                (submit-button "Up"))
       (form-to [:post "/down/"]
                [:input {:type "hidden" :name "url" :value url}]
                (submit-button "Down"))])))

(defn top-bar []
  (let [ui (users/user-info)]
    [:div#topbar
     [:h3 "Current User"]
     (if-let [user (:user ui)]
       [:span "Logged in as " (.getEmail user) " " (link-to (.createLogoutURL (:user-service ui) "/") "Logout")]
       [:span "Not logged in " (link-to (.createLoginURL (:user-service ui) "/") "Login")])
     [:h3 "Navigation"]
     [:a {:href "/"} "Refresh"] [:a {:href "/new/"} "Add link"]]))

(defn reddit-new-link [msg]
  (html
   [:head
    [:title "Reddit.Clojure.GAE - Submit to our authority"]]
   [:body
    [:h1 "Reddit.Clojure.GAE - Submit a new link"]
    [:h3 "Submit a new link"]
    (when msg [:p {:style "color: red;"} msg])
    (form-to [:post "/new/"]
     [:input {:type "Text" :name "url" :value "http://" :size 48 :title "URL"}]
     [:input {:type "Text" :name "title" :value "" :size 48 :title "Title"}]
     (submit-button "Add link"))
    (link-to "/" "Home")]))

(defn reddit-home []
  (html
   [:head
    [:title "Reddit.Clojure.GAE"]
    (include-css "/css/main.css")]
   [:body
    [:h1 "Reddit.Clojure.GAE"]
    (top-bar)
    [:h1 "Highest ranking list"]
    [:ol (render-links #(:points (val %))  >)]
    [:h1 "Latest link"]  
    [:ol (render-links #(.getMillis (Duration. (:date %) (DateTime.))) >)]]))

(defn invalid-url? [url]
  (or (empty? url)
      (not (try (java.net.URL. url) (catch Exception e nil)))))

(defn add-link [title url]
  (redirect
   (cond
    (invalid-url? url) "/new/?msg=Invalid URL"
    (empty? title)     "/new/?msg=Invalid Title"
    (@data url)        "/new/?msg=Link already submitted"
    :else
    (do
      (swap! data assoc url {:title title :date (DateTime.) :points 1})
      "/"))))

(defn rate [url mfn]
  (println "Rating " url)
  (swap! data (fn [old-data]
                (when (old-data url)
                  (update-in old-data [url :points] mfn))))
  (redirect "/"))

(defroutes public-routes
  (GET "/" [] (reddit-home))
  (GET  "/new/*" {{msg :msg} :params} (reddit-new-link msg)))

(defroutes loggedin-routes
  (POST "/new/" [url title] (add-link title url))
  (POST "/up/" [url] (rate url inc))
  (POST "/down/" [url] (rate url dec)))

(defn wrap-requiring-loggedin [application]
  (fn [request]
    (let [{:keys [user-service]} (users/user-info request)]
      (if (.isUserLoggedIn user-service)
        (application request)
        {:status 403 :body "Access denied. You must be logged in user!"}))))

(wrap! loggedin-routes
       wrap-requiring-loggedin
       users/wrap-with-user-info)

(defroutes reddit
  public-routes
  (POST "/*" []  loggedin-routes)
  (route/not-found "Page not found"))

(defn- log [msg & vals]
  (let [line (apply format msg vals)]
    (locking System/out (println line))))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start  (System/currentTimeMillis)
          resp   (handler req)
          finish (System/currentTimeMillis)
          total  (- finish start)]
      (log "request %s %s (%dms)" request-method uri total)
      resp)))

(wrap! reddit
       wrap-request-logging)

(defservice reddit)
