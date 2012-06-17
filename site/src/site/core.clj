(ns site.core
  (:require [solrclient.core :as solr])
  (:use compojure.core
        ring.adapter.jetty
        hiccup.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            ))

(defn sanitize [s]
  (when s
    (.replaceAll s "[!@#$%\\^&\\*()_\\+={}\\|\\\\\\[\\]:;\\\"\"\\'\\<\\>\\.\\,\\?\\/\\`\\~]" " "))) 

(defn search [text]
  (when-let [sanitized-text (sanitize text)]
    (solr/query
      {:q sanitized-text
       :rows 30
       :fl "score,name,doc,arglists,ns,source,var-type,artifact"
       :group true
       :group.field "doc"
       :defType "dismax"
       :qf "name^5.0 doc^1.0 ns^3.0"
       })))
  
(defn var-data [search-result]
  (->> search-result :grouped :doc :groups
       (map #(-> % :doclist :docs first))))

(defn sanitize-map [m]
  (into {}
        (for [[k v] m]
          [k (escape-html v)])))

(defn display [data]
  (when data
    (println "Matches:" (count data))
    (doseq [datum data]
      (println "<hr>")
      (let [{:keys [name arglists ns doc var-type source artifact]}
            datum]
        (println (html [:b name] "         (" ns ") -- " artifact))
        (when var-type (println (html [:i var-type])))
        (when arglists (println arglists))
        (when doc (println "\n" doc "\n"))
        (println "\nSource:\n\n" (if doc (.replace source doc "...") source) "\n")))))

(defhtml search-page [last-query results]
         [:html
          [:header
           [:title "collaj.net: clojure function search"]]
          [:body
           [:h3 "collaj: clojure function search"]
           [:form {:action "/"}
            [:input {:type "text" :autofocus "autofocus"
                     :name "q" :value last-query}]
            " "
            [:input {:type "submit" :value "Search"}]]
           [:pre results]]])

(defn show-results [q]
  (search-page q (with-out-str (display (var-data (search q))))))

(defroutes main-routes
  (GET "/" [q format] (if (= format "raw")
                        (pr-str (var-data (search q)))
                        (show-results q)))
  (GET "/data/:term" [term] (pr-str (var-data (search term))))
  (GET "/:term" [term] (show-results term))
  (route/resources "/")
  (route/not-found "<h1>Page not found!</h1>"))

(handler/api routes)

(def app (handler/site main-routes))

;; tests

(defn test-sanitize []
  (sanitize "as\\df%$#:;..~?/<>'@[]\"!^&(*+{}|g"))
